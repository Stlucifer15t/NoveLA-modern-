package my.noveldokusha.core.feeds

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import my.noveldokusha.core.appPreferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** One chapter the library update check found on a source, and when the check found it. */
@Serializable
data class NewRelease(
    val chapterUrl: String,
    val chapterTitle: String,
    val foundAtMillis: Long,
)

/**
 * The feed's own record of new releases.
 *
 * The update check is the only place that knows a chapter is new (it is compared against the
 * chapter list the device already had), so it writes here at that moment. The feed reads this
 * log, not the unread count of the whole shelf. A book leaves the feed when the reader opens it.
 *
 * Scope: only checks that run in this version are recorded. Chapters that were already on the
 * device before that are not guessed at, so the feed starts empty and fills from real checks.
 */
@Singleton
class NewReleaseLog @Inject constructor(
    private val appPreferences: AppPreferences,
) {
    private val lock = Any()

    /** Book url → releases not yet opened, in the order they were found. */
    fun flow(): Flow<Map<String, List<NewRelease>>> = appPreferences.NEW_RELEASE_LOG.flow()

    fun record(bookUrl: String, releases: List<NewRelease>) {
        if (releases.isEmpty()) return
        synchronized(lock) {
            val current = appPreferences.NEW_RELEASE_LOG.value
            val known = current[bookUrl].orEmpty().map { it.chapterUrl }.toSet()
            val fresh = releases.filter { it.chapterUrl !in known }
            if (fresh.isEmpty()) return
            appPreferences.NEW_RELEASE_LOG.value = current + (bookUrl to current[bookUrl].orEmpty() + fresh)
        }
    }

    fun markSeen(bookUrl: String) {
        synchronized(lock) {
            val current = appPreferences.NEW_RELEASE_LOG.value
            if (bookUrl !in current) return
            appPreferences.NEW_RELEASE_LOG.value = current - bookUrl
        }
    }
}
