package my.dreamora.features.foryou

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.core.appPreferences.ListLayoutMode
import my.dreamora.core.foryou.DreamoraGenre
import my.dreamora.feature.local_database.DAOs.LibraryDao
import my.dreamora.feature.local_database.DAOs.ReadingHistoryDao
import my.dreamora.scraper.SourceInterface
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
    val genres: List<DreamoraGenre> = emptyList(),
    val reading: ForYouReading? = null,
    /** The most recently read books, newest first — the "Continue" row. Real history rows only. */
    val continueRows: List<ForYouReading> = emptyList(),
    /** The last card hidden with a long-press; the screen shows it with an Undo action. */
    val lastHidden: ForYouBook? = null,
    /** Bumps on every hide, so the snackbar re-appears even for the same title. */
    val hideToken: Int = 0,
    /**
     * The sideways rails, grouped by the lane that answered. Kept in the state rather than
     * derived in the composable so the ordering rule (picked genres, min three) lives in one
     * place and the screen stays a renderer.
     */
    val rails: List<ForYouRail> = emptyList(),
    /**
     * What the sideways rails did not claim. The grid shows this rather than the whole pool,
     * so a book is not printed twice on one screen; when no rail formed, it is the whole pool.
     */
    val pool: List<ForYouBook> = emptyList(),
    /** Grid or list, and how many columns the grid uses. Read from preferences, not stored. */
    val layoutMode: ListLayoutMode = ListLayoutMode.VerticalGrid,
    val gridColumns: Int = 0,
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
    /** Keyed by (source, lane) — see [ForYouSlot.key]. */
    private var cursors: Map<String, ForYouCursor> = emptyMap()
    private var seen: MutableSet<String> = HashSet()
    /** Every book this session fetched, in display order, before hidden ones are taken out. */
    private var allBooks: List<ForYouBook> = emptyList()
    /** Hidden with a long-press. Session-only: a fresh launch shows everything again. */
    private val hiddenKeys = HashSet<String>()
    private var activated = false
    private var signature = ""
    private var fetchJob: Job? = null

    init {
        _state.value = _state.value.copy(
            genres = DreamoraGenre.fromSlugs(appPreferences.FOR_YOU_GENRES.value),
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
                allBooks = allBooks.map { it.copy(inLibrary = it.key in libraryUrls) }
                publish(allBooks)
            }
        }
        viewModelScope.launch {
            readingHistoryDao.getAllFlow().collect { rows ->
                historyTitles = rows.map { it.bookTitle }
                _state.value = _state.value.copy(
                    reading = rows.firstOrNull()?.toReading(),
                    continueRows = rows.take(10).map { it.toReading() },
                )
            }
        }
        viewModelScope.launch {
            provisioner.status.collect { status ->
                _state.value = _state.value.copy(provision = status)
            }
        }
        viewModelScope.launch {
            appPreferences.FOR_YOU_GENRES.flow().collect { slugs ->
                val genres = DreamoraGenre.fromSlugs(slugs)
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
        // The tab's layout comes from the same preferences the Settings → Layout screen writes,
        // so changing columns there moves this grid without reopening anything.
        viewModelScope.launch {
            appPreferences.FOR_YOU_LIST_LAYOUT_MODE.flow().collect { mode ->
                _state.value = _state.value.copy(layoutMode = mode)
            }
        }
        viewModelScope.launch {
            appPreferences.FOR_YOU_GRID_COLUMNS.flow().collect { columns ->
                _state.value = _state.value.copy(gridColumns = columns)
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
            allBooks = emptyList()
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                books = emptyList(),
                rails = emptyList(),
                pool = emptyList(),
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
        // One lane per picked genre, plus the source's own front page. The words are the
        // genre's, not the slug: "slice of life" searches, "slice-of-life" does not.
        val lanes = ForYouLane.forGenres(_state.value.genres) + ForYouLane.Catalog
        val step = try {
            engine.step(resolved.active, cursors, lanes)
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
        // A hidden book is dropped here too, or a "Load more" would bring the dismissed cover back.
        val fresh = decorated.filter { (replace || it.key !in seen) && it.key !in hiddenKeys }
        seen += decorated.map { it.key }
        val combined = (if (replace) emptyList<ForYouBook>() else allBooks) + fresh
        // Only a fresh pass gets sorted: reshuffling on "Load more" would move the rows
        // the reader is already looking at.
        allBooks = if (replace) combined.sortedBy { it.inLibrary } else combined
        publish(allBooks)
        _state.value = _state.value.copy(
            loading = false,
            loadingMore = false,
            canLoadMore = cursors.values.any { !it.exhausted },
        )
    }

    /**
     * Lays the visible books out: sideways rails first, and the pool gets whatever no rail
     * claimed. Hide and Undo go through here too, so they re-lay the screen with no network call.
     */
    private fun publish(all: List<ForYouBook>) {
        val visible = all.filterNot { it.key in hiddenKeys }
        val rails = visible.railsFor(_state.value.genres)
        val claimed = rails.flatMap { rail -> rail.books }.mapTo(HashSet()) { it.key }
        _state.value = _state.value.copy(
            books = visible,
            rails = rails,
            pool = if (rails.isEmpty()) visible else visible.filter { it.key !in claimed },
            empty = if (visible.isNotEmpty()) null else (_state.value.empty ?: ForYouEmpty.NoAnswer),
        )
    }

    /** Takes a card off the screen for this session, and offers Undo. */
    fun hide(book: ForYouBook) {
        hiddenKeys.add(book.key)
        publish(allBooks)
        _state.value = _state.value.copy(
            lastHidden = book,
            hideToken = _state.value.hideToken + 1,
        )
    }

    fun undoHide(key: String) {
        if (!hiddenKeys.remove(key)) return
        publish(allBooks)
    }

    private fun signatureOf(list: List<SourceInterface.Catalog>): String =
        list.joinToString("|") { "${it.id}:${it.contentType}" } + "#" + _state.value.type.scope

    private fun my.dreamora.feature.local_database.tables.ReadingHistory.toReading(): ForYouReading {
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
