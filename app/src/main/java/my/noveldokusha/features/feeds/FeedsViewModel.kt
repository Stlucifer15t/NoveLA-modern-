package my.noveldokusha.features.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import my.noveldokusha.core.domain.LibraryCategory
import my.noveldokusha.feature.local_database.BookWithContext
import my.noveldokusha.feature.local_database.DAOs.LibraryDao
import my.noveldokusha.interactor.WorkersInteractions
import javax.inject.Inject

/** One shelf title, as the feed shows it. Every field is a column this device already has. */
data class FeedRow(
    val bookUrl: String,
    val title: String,
    val coverUrl: String,
    val unread: Int,
    val chaptersTotal: Int,
    /** The source's own "last updated" text, verbatim — blank when it does not state one. */
    val sourceUpdateDate: String,
    val lastCheckedMillis: Long,
    val isVideo: Boolean,
)

/** What feeds are in the shelf, so the segmented control can say a real number. */
enum class FeedFilter { All, Books, Episodes }

data class FeedsState(
    val rows: List<FeedRow> = emptyList(),
    val checking: Boolean = false,
    val filter: FeedFilter = FeedFilter.All,
) {
    val libraryEmpty: Boolean get() = rows.isEmpty() && !checking

    fun visible(): List<FeedRow> = when (filter) {
        FeedFilter.All -> rows
        FeedFilter.Books -> rows.filter { !it.isVideo }
        FeedFilter.Episodes -> rows.filter { it.isVideo }
    }
}

/**
 * The feeds tab: what the shelf has that this device has not opened.
 *
 * There is no push channel here and none is pretended at. The count comes from the same
 * aggregate the library uses (`chaptersCount - chaptersReadCount` per book), and "Check for
 * updates" starts the library update worker — the one path in this app that asks the sources for
 * new chapters. A real notification path would be the worker posting into the notification
 * drawer, which is a separate piece of work; this screen reads the state it leaves behind.
 */
@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val workersInteractions: WorkersInteractions,
) : ViewModel() {

    private var filter = FeedFilter.All

    /**
     * Bumping this re-runs the combine below. It has to be declared before [state]: a property
     * read from an earlier property's initializer is null at that moment, and the crash would
     * only show up on the first collect.
     */
    private val _tick = MutableStateFlow(0)

    val state = combine(
        libraryDao.getBooksInLibraryWithContextFlow(),
        workersInteractions.isManualUpdateRunning(),
        _tick,
    ) { books, running, _ ->
        FeedsState(
            rows = books.asSequence()
                .map { it.toRow() }
                .sortedWith(
                    compareByDescending<FeedRow> { it.unread }
                        .thenByDescending { it.lastCheckedMillis }
                        .thenBy { it.title.lowercase() }
                )
                .toList(),
            checking = running,
            filter = filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedsState(),
    )

    fun setFilter(value: FeedFilter) {
        filter = value
        refresh()
    }

    /**
     * The filter lives in the view model rather than in the composable, so the tab keeps the
     * reader's choice when they leave and come back. Re-running the combine is the cheap way to
     * publish it: the query is a local flow, no network.
     */
    private fun refresh() {
        _tick.value++
    }

    fun checkForUpdates() {
        workersInteractions.checkForLibraryUpdates(LibraryCategory.DEFAULT)
    }

    fun cancelUpdate() {
        workersInteractions.cancelLibraryUpdates()
    }

    private fun BookWithContext.toRow(): FeedRow {
        val read = chaptersReadCount.coerceIn(0, chaptersCount)
        return FeedRow(
            bookUrl = book.url,
            title = book.title,
            coverUrl = book.coverImageUrl,
            unread = (chaptersCount - read).coerceAtLeast(0),
            chaptersTotal = chaptersCount,
            sourceUpdateDate = book.lastUpdateDate,
            lastCheckedMillis = book.lastUpdateEpochTimeMilli,
            isVideo = book.contentType.equals("video", ignoreCase = true),
        )
    }
}
