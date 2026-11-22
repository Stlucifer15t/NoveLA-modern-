package my.noveldokusha.features.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import my.noveldokusha.core.domain.LibraryCategory
import my.noveldokusha.core.feeds.NewRelease
import my.noveldokusha.core.feeds.NewReleaseLog
import my.noveldokusha.feature.local_database.DAOs.LibraryDao
import my.noveldokusha.interactor.WorkersInteractions
import javax.inject.Inject

/**
 * One title with releases the update check found and the reader has not opened yet.
 * [releases] is newest first.
 */
data class FeedRow(
    val bookUrl: String,
    val title: String,
    val coverUrl: String,
    val isVideo: Boolean,
    val releases: List<NewRelease>,
) {
    val unread: Int get() = releases.size
    val latest: NewRelease? get() = releases.firstOrNull()
}

/** What feeds are in the shelf, so the segmented control can say a real number. */
enum class FeedFilter { All, Books, Episodes }

data class FeedsState(
    val rows: List<FeedRow> = emptyList(),
    /** How many books are on the shelf at all. Tells an empty shelf from an empty feed. */
    val shelfCount: Int = 0,
    val checking: Boolean = false,
    val filter: FeedFilter = FeedFilter.All,
) {
    val libraryEmpty: Boolean get() = shelfCount == 0 && !checking

    fun visible(): List<FeedRow> = when (filter) {
        FeedFilter.All -> rows
        FeedFilter.Books -> rows.filter { !it.isVideo }
        FeedFilter.Episodes -> rows.filter { it.isVideo }
    }
}

/**
 * The feeds tab: new releases the library update check found, one row per title.
 *
 * A row exists only for a release the check recorded (see [NewReleaseLog]). Unread chapters
 * that were already on the device are not listed here, so the feed never claims a release it
 * cannot date. A title leaves the feed when the reader opens it.
 *
 * "Check for updates" starts the library update worker, the one path in this app that asks the
 * sources for new chapters.
 */
@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val workersInteractions: WorkersInteractions,
    private val newReleaseLog: NewReleaseLog,
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
        newReleaseLog.flow(),
        _tick,
    ) { books, running, releases, _ ->
        val byUrl = books.associateBy { it.book.url }
        val rows = releases.mapNotNull { (bookUrl, found) ->
            val entry = byUrl[bookUrl] ?: return@mapNotNull null
            if (found.isEmpty()) return@mapNotNull null
            FeedRow(
                bookUrl = bookUrl,
                title = entry.book.title,
                coverUrl = entry.book.coverImageUrl,
                isVideo = entry.book.contentType.equals("video", ignoreCase = true),
                releases = found.sortedByDescending { it.foundAtMillis },
            )
        }.sortedByDescending { it.latest?.foundAtMillis ?: 0L }
        FeedsState(
            rows = rows,
            shelfCount = books.size,
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

    /** The reader opened this title, so its releases are no longer new. */
    fun markSeen(bookUrl: String) {
        newReleaseLog.markSeen(bookUrl)
    }

    fun checkForUpdates() {
        workersInteractions.checkForLibraryUpdates(LibraryCategory.DEFAULT)
    }

    fun cancelUpdate() {
        workersInteractions.cancelLibraryUpdates()
    }
}
