package my.noveldokusha.features.foryou

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.foryou.NovelaGenre
import my.noveldokusha.feature.local_database.DAOs.LibraryDao
import my.noveldokusha.feature.local_database.DAOs.ReadingHistoryDao
import my.noveldokusha.scraper.SourceInterface
import javax.inject.Inject

/** Why the grid is empty — the tab says this instead of showing a blank wall. */
enum class ForYouEmpty { NoSourcesOfType, NoAnswer }

/** One installed source, as the binding picker lists it. */
data class ForYouSourceOption(val id: String, val name: String)

/** The shelf's most recently open book, which is what the top card is allowed to talk about. */
data class ForYouReading(
    val title: String,
    val bookUrl: String,
    val coverUrl: String,
    val percentRead: Int,
    val chaptersRead: Int,
    val chaptersTotal: Int,
)

data class ForYouUiState(
    val type: ForYouContentType = ForYouContentType.Novel,
    val books: List<ForYouBook> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val sourcesOfThisType: Int = 0,
    val queriedSources: Int = 0,
    val boundSourceName: String? = null,
    val usingFallback: Boolean = false,
    val empty: ForYouEmpty? = null,
    /** Every installed source of the current type, as the binding picker lists them. */
    val sources: List<ForYouSourceOption> = emptyList(),
    val boundSourceId: String = "",
    val genres: List<NovelaGenre> = emptyList(),
    val reading: ForYouReading? = null,
    val provision: DefaultSourceStatus = DefaultSourceStatus(),
    val error: String? = null,
)

/**
 * The For You tab.
 *
 * Three things this does on purpose — all of them the reason the previous strip felt broken:
 * it waits until the tab is actually looked at before spending a request; it re-reads when the
 * installed sources change, so installing an extension in the other tab fills this one in
 * without restarting the app; and it keeps a pager per source, so "Load more" asks each source
 * for its next page instead of pretending there is more to show.
 */
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val engine: ForYouEngine,
    private val provisioner: ForYouSourceProvisioner,
    private val libraryDao: LibraryDao,
    readingHistoryDao: ReadingHistoryDao,
) : ViewModel() {

    private val _state = mutableStateOf(ForYouUiState())
    val state: ForYouUiState get() = _state.value

    private var sources: List<SourceInterface.Catalog> = emptyList()
    private var libraryUrls: Set<String> = emptySet()
    private var historyTitles: List<String> = emptyList()
    private var cursors: Map<String, ForYouCursor> = emptyMap()
    private var seen: MutableSet<String> = HashSet()
    private var activated = false
    private var signature = ""
    private var fetchJob: Job? = null

    init {
        _state.value = _state.value.copy(
            genres = NovelaGenre.fromSlugs(appPreferences.FOR_YOU_GENRES.value),
        )
        viewModelScope.launch {
            engine.sourcesFlow().collect { list ->
                sources = list
                val next = signatureOf(list)
                if (next == signature) return@collect
                signature = next
                // This is the fix for "I installed an extension and For You stayed empty":
                // the list is a flow, so a new source re-triggers the pass by itself.
                if (activated) refresh()
            }
        }
        viewModelScope.launch {
            libraryDao.booksInLibraryFlow().collect { books ->
                libraryUrls = books.mapTo(HashSet()) { it.url }
                // Re-tagging the grid needs no network: the shelf is local.
                _state.value = _state.value.copy(
                    books = _state.value.books.map { it.copy(inLibrary = it.key in libraryUrls) },
                )
            }
        }
        viewModelScope.launch {
            readingHistoryDao.getAllFlow().collect { rows ->
                historyTitles = rows.map { it.bookTitle }
                _state.value = _state.value.copy(reading = rows.firstOrNull()?.toReading())
            }
        }
        viewModelScope.launch {
            provisioner.status.collect { status ->
                _state.value = _state.value.copy(provision = status)
            }
        }
        viewModelScope.launch {
            appPreferences.FOR_YOU_GENRES.flow().collect { slugs ->
                val genres = NovelaGenre.fromSlugs(slugs)
                if (genres != _state.value.genres) {
                    _state.value = _state.value.copy(genres = genres)
                    if (activated) refresh()
                }
            }
        }
        viewModelScope.launch {
            appPreferences.FOR_YOU_SOURCE_ID.flow().collect {
                if (activated) refresh()
            }
        }
    }

    /**
     * Installs the default source without fetching any catalog data. Run at app start so the
     * tab has something bound by the time it is first looked at, while the requests that fill
     * the grid still wait for that look.
     */
    fun prepareDefaultSource() {
        viewModelScope.launch { provisioner.ensureOnce(sources) }
    }

    /** The tab became visible. The only thing that triggers the first pass. */
    fun activate() {
        if (activated) return
        activated = true
        viewModelScope.launch { provisioner.ensureOnce(sources) }
        refresh()
    }

    fun setType(type: ForYouContentType) {
        if (type == _state.value.type) return
        _state.value = _state.value.copy(type = type)
        signature = signatureOf(sources)
        refresh()
    }

    fun setBoundSource(id: String) {
        appPreferences.FOR_YOU_SOURCE_ID.value = id
    }

    fun refresh() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val resolved = engine.resolve(
                sources = sources,
                type = _state.value.type,
                boundId = appPreferences.FOR_YOU_SOURCE_ID.value,
            )
            cursors = emptyMap()
            seen = HashSet()
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                books = emptyList(),
                sourcesOfThisType = resolved.all.size,
                sources = resolved.all.map { ForYouSourceOption(it.id, engine.sourceName(it)) },
                boundSourceId = appPreferences.FOR_YOU_SOURCE_ID.value,
                queriedSources = resolved.active.size,
                boundSourceName = resolved.bound?.let { engine.sourceName(it) },
                usingFallback = resolved.bound == null && resolved.all.isNotEmpty(),
                empty = if (resolved.active.isEmpty()) ForYouEmpty.NoSourcesOfType else null,
                canLoadMore = false,
            )
            if (resolved.active.isEmpty()) {
                // Nothing to read for this type. The picker still shows what is installed for
                // the others, and the empty state says which way to go.
                _state.value = _state.value.copy(loading = false, canLoadMore = false)
                return@launch
            }
            applyStep(resolved, replace = true)
        }
    }

    fun loadMore() {
        if (state.loadingMore || !state.canLoadMore || state.loading) return
        viewModelScope.launch {
            val resolved = engine.resolve(
                sources = sources,
                type = _state.value.type,
                boundId = appPreferences.FOR_YOU_SOURCE_ID.value,
            )
            if (resolved.active.isEmpty()) return@launch
            _state.value = _state.value.copy(loadingMore = true)
            applyStep(resolved, replace = false)
        }
    }

    private suspend fun applyStep(resolved: ForYouSources, replace: Boolean) {
        // The genre words, not the slugs: "slice of life" searches, "slice-of-life" does not.
        val terms = _state.value.genres.flatMap { it.terms }.distinct()
        val step = try {
            engine.step(resolved.active, cursors, terms)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                loadingMore = false,
                error = e.message,
                empty = if (_state.value.books.isEmpty()) ForYouEmpty.NoAnswer else _state.value.empty,
            )
            return
        }
        val (nextCursors, raw) = step
        cursors = nextCursors
        val decorated = engine.decorate(raw, libraryUrls, historyTitles)
        val fresh = decorated.filter { replace || it.key !in seen }
        if (replace) {
            seen = fresh.mapTo(HashSet()) { it.key }
        } else {
            seen += fresh.map { it.key }
        }
        val books = (if (replace) emptyList() else _state.value.books) + fresh
        _state.value = _state.value.copy(
            // Only a fresh pass gets sorted: reshuffling on "Load more" would move the rows
            // the reader is already looking at.
            books = if (replace) books.sortedBy { it.inLibrary } else books,
            loading = false,
            loadingMore = false,
            canLoadMore = cursors.values.any { !it.exhausted },
            empty = when {
                books.isNotEmpty() -> null
                else -> ForYouEmpty.NoAnswer
            },
        )
    }

    private fun signatureOf(list: List<SourceInterface.Catalog>): String =
        list.joinToString("|") { "${it.id}:${it.contentType}" } + "#" + _state.value.type.scope

    private fun my.noveldokusha.feature.local_database.tables.ReadingHistory.toReading(): ForYouReading {
        val total = totalChapters.coerceAtLeast(0)
        val read = readChapters.coerceIn(0, if (total > 0) total else readChapters.coerceAtLeast(0))
        return ForYouReading(
            title = bookTitle,
            bookUrl = bookUrl,
            coverUrl = bookCoverUrl,
            percentRead = if (total > 0) (read * 100) / total else 0,
            chaptersRead = read,
            chaptersTotal = total,
        )
    }
}
