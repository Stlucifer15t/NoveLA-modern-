package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import my.dreamora.core.foryou.recommendation.model.AuthorAffinity
import my.dreamora.core.foryou.recommendation.model.NovelVector
import my.dreamora.core.foryou.recommendation.model.TrackedBook
import my.dreamora.core.foryou.recommendation.model.TrackedStatus
import my.dreamora.feature.local_database.DAOs.AuthorPreferenceDao
import my.dreamora.feature.local_database.tables.AuthorPreferenceEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthorPreferenceManager"

/**
 * Tracks which authors the reader finishes, reads and adds.
 *
 * The app does not store authors yet, so callers pass `null` and this manager stays empty
 * until a source reports one. It is kept whole so that the day authors arrive, the signal is
 * already wired end to end.
 */
@Singleton
class AuthorPreferenceManager @Inject constructor(
    private val authorDao: AuthorPreferenceDao,
) {

    /** Affinity 0.0..1.0, or null when the author is unknown. */
    suspend fun getAuthorAffinity(authorNormalized: String?): Float? = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank()) return@withContext null
        authorDao.getAuthor(authorNormalized)?.let { it.affinityScore / 1000f }
    }

    suspend fun clearAllPreferences() = withContext(Dispatchers.IO) {
        authorDao.clearAllAuthors()
    }

    suspend fun getAuthorAffinities(authorNormalized: List<String>): Map<String, Float> =
        withContext(Dispatchers.IO) {
            authorDao.getAllAuthors()
                .filter { it.authorNormalized in authorNormalized }
                .associate { it.authorNormalized to (it.affinityScore / 1000f) }
        }

    suspend fun isAuthorLiked(authorNormalized: String?): Boolean = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank()) return@withContext false
        authorDao.getAuthor(authorNormalized)?.isLiked ?: false
    }

    suspend fun isAuthorFavorite(authorNormalized: String?): Boolean = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank()) return@withContext false
        authorDao.getAuthor(authorNormalized)?.isFavorite ?: false
    }

    suspend fun getFavoriteAuthors(limit: Int = 10): List<AuthorPreferenceEntity> =
        withContext(Dispatchers.IO) { authorDao.getFavoriteAuthors(limit) }

    suspend fun getLikedAuthors(limit: Int = 20): List<AuthorPreferenceEntity> =
        withContext(Dispatchers.IO) { authorDao.getLikedAuthors(limit) }

    /**
     * The seam to the scorer. The database rows are mapped here, so [NovelScorer] never sees
     * storage types.
     */
    suspend fun getLikedAffinities(limit: Int = 50): List<AuthorAffinity> =
        getLikedAuthors(limit).map {
            AuthorAffinity(
                authorNormalized = it.authorNormalized,
                affinityScore = it.affinityScore,
                isFavorite = it.isFavorite,
            )
        }

    suspend fun getTopAuthors(minScore: Int = 600, limit: Int = 10): List<AuthorPreferenceEntity> =
        withContext(Dispatchers.IO) { authorDao.getTopAuthors(minScore, limit) }

    fun observeAllAuthors(): Flow<List<AuthorPreferenceEntity>> = authorDao.observeAllAuthors()

    /** The reader added [author]'s book to the library. */
    suspend fun onNovelAddedToLibrary(author: String?, bookUrl: String) = withContext(Dispatchers.IO) {
        val display = author?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext
        val normalized = NovelVector.normalizeAuthor(display) ?: return@withContext
        val existing = authorDao.getAuthor(normalized)
        if (existing != null) {
            val updated = existing.copy(
                novelsRead = existing.novelsRead + 1,
                affinityScore = (existing.affinityScore + 50).coerceAtMost(1000),
                updatedAt = System.currentTimeMillis(),
            ).withNovelUrl(bookUrl)
            authorDao.updateAuthor(updated)
        } else {
            authorDao.insertAuthor(
                AuthorPreferenceEntity(
                    authorNormalized = normalized,
                    displayName = display,
                    affinityScore = 550,
                    novelsRead = 1,
                    novelUrlsInLibrary = bookUrl,
                ),
            )
        }
    }

    /** Reading progress for [author]. */
    suspend fun onChaptersRead(
        author: String?,
        chaptersRead: Int,
        readingTimeSeconds: Long,
    ) = withContext(Dispatchers.IO) {
        val display = author?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext
        val normalized = NovelVector.normalizeAuthor(display) ?: return@withContext
        val boost = calculateReadingBoost(chaptersRead, readingTimeSeconds)
        if (authorDao.getAuthor(normalized) != null) {
            authorDao.updateReadingProgress(
                authorNormalized = normalized,
                chapters = chaptersRead,
                seconds = readingTimeSeconds,
                scoreBoost = boost,
            )
        } else {
            authorDao.insertAuthor(
                AuthorPreferenceEntity(
                    authorNormalized = normalized,
                    displayName = display,
                    affinityScore = 500 + boost,
                    novelsRead = 1,
                    totalChaptersRead = chaptersRead,
                    totalReadingTimeSeconds = readingTimeSeconds,
                ),
            )
        }
    }

    /** The reader changed [author]'s book from [oldStatus] to [newStatus]. */
    suspend fun onStatusChanged(
        author: String?,
        bookUrl: String,
        newStatus: TrackedStatus,
        oldStatus: TrackedStatus?,
    ) = withContext(Dispatchers.IO) {
        val display = author?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext
        val normalized = NovelVector.normalizeAuthor(display) ?: return@withContext
        val existing = authorDao.getAuthor(normalized)
        if (existing == null) {
            onNovelAddedToLibrary(display, bookUrl)
            return@withContext
        }
        var completedDelta = 0
        var scoreDelta = 0
        if (oldStatus == TrackedStatus.COMPLETED) {
            completedDelta -= 1
            scoreDelta -= 100
        }
        when (newStatus) {
            TrackedStatus.COMPLETED -> {
                completedDelta += 1
                scoreDelta += 150
            }
            TrackedStatus.READING -> scoreDelta += 25
            TrackedStatus.PLAN_TO_READ -> scoreDelta += 10
            // Our choice, not Novery's: on hold is neutral; dropped costs a little.
            TrackedStatus.ON_HOLD, TrackedStatus.SPICY -> Unit
            TrackedStatus.DROPPED -> scoreDelta -= 50
        }
        authorDao.updateAuthor(
            existing.copy(
                novelsCompleted = (existing.novelsCompleted + completedDelta).coerceAtLeast(0),
                affinityScore = (existing.affinityScore + scoreDelta).coerceIn(0, 1000),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** The reader removed [author]'s book from the library. */
    suspend fun onNovelRemoved(author: String?, bookUrl: String, wasCompleted: Boolean) =
        withContext(Dispatchers.IO) {
            val display = author?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext
            val normalized = NovelVector.normalizeAuthor(display) ?: return@withContext
            val existing = authorDao.getAuthor(normalized) ?: return@withContext
            // Affinity is left alone: removing a book is an ambiguous signal.
            authorDao.updateAuthor(
                existing.copy(
                    novelsRead = (existing.novelsRead - 1).coerceAtLeast(0),
                    novelsCompleted = if (wasCompleted) {
                        (existing.novelsCompleted - 1).coerceAtLeast(0)
                    } else {
                        existing.novelsCompleted
                    },
                    updatedAt = System.currentTimeMillis(),
                ).withoutNovelUrl(bookUrl),
            )
        }

    /** Rebuilds every author row from the library. */
    suspend fun recalculateAllPreferences(books: List<TrackedBook>) = withContext(Dispatchers.IO) {
        authorDao.clearAllAuthors()
        val byAuthor = mutableMapOf<String, MutableList<TrackedBook>>()
        for (book in books) {
            val display = book.author?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val normalized = NovelVector.normalizeAuthor(display) ?: continue
            byAuthor.getOrPut(normalized) { mutableListOf() }.add(book)
        }
        for ((normalized, items) in byAuthor) {
            val displayName = items.first().author ?: continue
            var score = 500
            var completed = 0
            var chapters = 0
            var seconds = 0L
            for (item in items) {
                chapters += item.chaptersRead
                seconds += item.readingTimeSeconds
                when (item.status) {
                    TrackedStatus.COMPLETED -> {
                        completed++
                        score += 150
                    }
                    TrackedStatus.READING -> score += 50 + (item.progressPercent * 50).toInt()
                    TrackedStatus.PLAN_TO_READ -> score += 30
                    TrackedStatus.ON_HOLD, TrackedStatus.SPICY -> Unit
                    TrackedStatus.DROPPED -> score -= 50
                }
                score += calculateReadingBoost(item.chaptersRead, item.readingTimeSeconds)
            }
            authorDao.insertAuthor(
                AuthorPreferenceEntity(
                    authorNormalized = normalized,
                    displayName = displayName,
                    affinityScore = score.coerceIn(0, 1000),
                    novelsRead = items.size,
                    novelsCompleted = completed,
                    totalChaptersRead = chapters,
                    totalReadingTimeSeconds = seconds,
                    novelUrlsInLibrary = items.joinToString(",") { it.url },
                ),
            )
        }
        Log.d(TAG, "Recalculated preferences for ${byAuthor.size} authors")
    }

    private fun calculateReadingBoost(chapters: Int, timeSeconds: Long): Int {
        val chapterBoost = (chapters * 2).coerceAtMost(50)
        val timeBoost = ((timeSeconds / 3600f) * 10).toInt().coerceAtMost(30)
        return chapterBoost + timeBoost
    }
}
