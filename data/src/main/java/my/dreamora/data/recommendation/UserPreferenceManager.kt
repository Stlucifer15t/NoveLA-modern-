package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import my.dreamora.core.foryou.recommendation.TagNormalizer
import my.dreamora.core.foryou.recommendation.TagNormalizer.TagCategory
import my.dreamora.core.foryou.recommendation.model.TagAffinity
import my.dreamora.core.foryou.recommendation.model.TrackedBook
import my.dreamora.core.foryou.recommendation.model.TrackedStatus
import my.dreamora.core.foryou.recommendation.model.UserTasteProfile
import my.dreamora.feature.local_database.DAOs.RecommendationDao
import my.dreamora.feature.local_database.tables.UserPreferenceEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Interactions that move a tag's affinity score. */
enum class InteractionType {
    READ_CHAPTER,
    COMPLETED_NOVEL,
    ADDED_TO_LIBRARY,
    REMOVED_FROM_LIBRARY,
    LONG_READ,
    EXPLICIT_LIKE,
    EXPLICIT_DISLIKE,
    STATUS_READING,
    STATUS_PLAN_TO_READ,
    UNDO_COMPLETED,
}

/**
 * Builds the reader's tag taste profile from their library. Each book's genre tags move the
 * scores of those tags, and the scores feed [NovelScorer].
 */
@Singleton
class UserPreferenceManager @Inject constructor(
    private val recommendationDao: RecommendationDao,
    private val authorPreferenceManager: AuthorPreferenceManager,
) {

    companion object {
        /** 30 minutes of continuous reading. Only meaningful once reading time is stored per book. */
        private const val LONG_READ_THRESHOLD_SECONDS = 30 * 60L
    }

    private fun scoreChange(type: InteractionType, weight: Float = 1.0f): Int {
        val base = when (type) {
            InteractionType.EXPLICIT_LIKE -> 200
            InteractionType.EXPLICIT_DISLIKE -> -150
            InteractionType.READ_CHAPTER -> 10
            InteractionType.COMPLETED_NOVEL -> 100
            InteractionType.ADDED_TO_LIBRARY -> 50
            InteractionType.REMOVED_FROM_LIBRARY -> 0
            InteractionType.LONG_READ -> 30
            InteractionType.STATUS_READING -> 25
            InteractionType.STATUS_PLAN_TO_READ -> 10
            InteractionType.UNDO_COMPLETED -> -100
        }
        return (base * weight).toInt()
    }

    suspend fun getUserProfile(): UserTasteProfile = withContext(Dispatchers.IO) {
        buildProfile(recommendationDao.getAllPreferences())
    }

    fun observeUserProfile(): Flow<UserTasteProfile> =
        recommendationDao.observeAllPreferences().map { buildProfile(it) }

    private fun buildProfile(preferences: List<UserPreferenceEntity>): UserTasteProfile {
        if (preferences.isEmpty()) return UserTasteProfile.EMPTY
        val affinities = preferences.mapNotNull { pref ->
            val tag = runCatching { TagCategory.valueOf(pref.tag) }.getOrNull() ?: return@mapNotNull null
            TagAffinity(
                tag = tag,
                score = pref.affinityScore / 1000f,
                confidence = calculateConfidence(pref.novelCount, pref.readingTimeSeconds),
                novelCount = pref.novelCount,
                completionRate = pref.completionRate,
                dropRate = pref.dropRate,
            )
        }
        val preferred = affinities
            .filter { it.score >= 0.5f }
            .sortedByDescending { it.score * it.confidence }
        val avoided = affinities
            .filter { it.dropRate > 0.5f && it.novelCount >= 2 }
            .sortedByDescending { it.completionRate }
        return UserTasteProfile(
            preferredTags = preferred,
            avoidedTags = avoided,
            diversityScore = calculateDiversity(preferred),
            preferredRatingMin = null,
            sampleSize = preferences.maxOfOrNull { it.novelCount } ?: 0,
        )
    }

    private fun calculateConfidence(novelCount: Int, readingTimeSeconds: Long): Float {
        val novelFactor = (novelCount / 10f).coerceAtMost(1f)
        val timeFactor = (readingTimeSeconds / (10 * 60 * 60f)).coerceAtMost(1f)
        return ((novelFactor + timeFactor) / 2).coerceIn(0.1f, 1f)
    }

    private fun calculateDiversity(preferences: List<TagAffinity>): Float {
        if (preferences.size <= 1) return 0f
        val scores = preferences.map { it.score }
        val mean = scores.average().toFloat()
        val variance = scores.map { (it - mean) * (it - mean) }.average().toFloat()
        return (1f - variance).coerceIn(0f, 1f)
    }

    /** A library book was added. */
    suspend fun onNovelAddedToLibrary(rawTags: List<String>, author: String?, bookUrl: String) =
        withContext(Dispatchers.IO) {
            for (tag in TagNormalizer.normalizeAll(rawTags)) {
                recordInteraction(tag.name, InteractionType.ADDED_TO_LIBRARY, novelDelta = 1)
            }
            authorPreferenceManager.onNovelAddedToLibrary(author, bookUrl)
        }

    /** Chapters were read in a book with these tags. */
    suspend fun onChaptersRead(
        rawTags: List<String>,
        author: String?,
        chaptersRead: Int,
        readingTimeSeconds: Long,
    ) = withContext(Dispatchers.IO) {
        val chapterWeight = chaptersRead.coerceIn(1, 10).toFloat()
        val isLongRead = readingTimeSeconds >= LONG_READ_THRESHOLD_SECONDS
        for (tag in TagNormalizer.normalizeAll(rawTags)) {
            recordInteraction(
                tagName = tag.name,
                type = InteractionType.READ_CHAPTER,
                weight = chapterWeight,
                chaptersDelta = chaptersRead,
                timeDelta = readingTimeSeconds,
            )
            if (isLongRead) {
                recordInteraction(
                    tagName = tag.name,
                    type = InteractionType.LONG_READ,
                    weight = (readingTimeSeconds / LONG_READ_THRESHOLD_SECONDS.toFloat()).coerceAtMost(3f),
                )
            }
        }
        authorPreferenceManager.onChaptersRead(author, chaptersRead, readingTimeSeconds)
    }

    /** A book's reading status changed. */
    suspend fun onStatusChanged(
        rawTags: List<String>,
        author: String?,
        bookUrl: String,
        newStatus: TrackedStatus,
        oldStatus: TrackedStatus?,
    ) = withContext(Dispatchers.IO) {
        val (type, completedDelta) = when (newStatus) {
            TrackedStatus.COMPLETED -> InteractionType.COMPLETED_NOVEL to 1
            TrackedStatus.READING -> InteractionType.STATUS_READING to 0
            TrackedStatus.PLAN_TO_READ -> InteractionType.STATUS_PLAN_TO_READ to 0
        }
        for (tag in TagNormalizer.normalizeAll(rawTags)) {
            if (oldStatus == TrackedStatus.COMPLETED) {
                recordInteraction(tag.name, InteractionType.UNDO_COMPLETED, completedDelta = -1)
            }
            recordInteraction(tag.name, type, completedDelta = completedDelta)
        }
        authorPreferenceManager.onStatusChanged(author, bookUrl, newStatus, oldStatus)
    }

    /** A book was removed from the library. */
    suspend fun onNovelRemoved(
        rawTags: List<String>,
        author: String?,
        bookUrl: String,
        wasCompleted: Boolean,
    ) = withContext(Dispatchers.IO) {
        for (tag in TagNormalizer.normalizeAll(rawTags)) {
            recordInteraction(
                tagName = tag.name,
                type = InteractionType.REMOVED_FROM_LIBRARY,
                novelDelta = -1,
                completedDelta = if (wasCompleted) -1 else 0,
            )
        }
        authorPreferenceManager.onNovelRemoved(author, bookUrl, wasCompleted)
    }

    /** Rebuilds every tag row and the author rows from the library's books. */
    suspend fun recalculateAllPreferences(books: List<TrackedBook>) = withContext(Dispatchers.IO) {
        recommendationDao.clearAllPreferences()
        val tagScores = mutableMapOf<TagCategory, MutablePreferenceData>()

        for (book in books) {
            val tags = TagNormalizer.normalizeAll(book.tags)
            val progress = book.progressPercent
            for (tag in tags) {
                val data = tagScores.getOrPut(tag) { MutablePreferenceData() }
                data.novelCount++
                data.chaptersRead += book.chaptersRead
                data.readingTimeSeconds += book.readingTimeSeconds
                val statusScore = when (book.status) {
                    TrackedStatus.COMPLETED -> {
                        data.completedCount++
                        scoreChange(InteractionType.COMPLETED_NOVEL) +
                            scoreChange(InteractionType.ADDED_TO_LIBRARY)
                    }
                    TrackedStatus.READING ->
                        scoreChange(InteractionType.STATUS_READING) +
                            scoreChange(InteractionType.ADDED_TO_LIBRARY) +
                            (progress * scoreChange(InteractionType.READ_CHAPTER) * 10).toInt()
                    TrackedStatus.PLAN_TO_READ ->
                        scoreChange(InteractionType.STATUS_PLAN_TO_READ) +
                            scoreChange(InteractionType.ADDED_TO_LIBRARY)
                }
                val chapterBonus = (book.chaptersRead * scoreChange(InteractionType.READ_CHAPTER))
                    .coerceAtMost(200)
                data.scoreAccumulator += statusScore + chapterBonus
            }
        }

        val rows = tagScores.map { (tag, data) ->
            UserPreferenceEntity(
                tag = tag.name,
                affinityScore = (500 + data.scoreAccumulator / data.novelCount).coerceIn(0, 1000),
                novelCount = data.novelCount,
                chaptersRead = data.chaptersRead,
                readingTimeSeconds = data.readingTimeSeconds,
                completedCount = data.completedCount,
                droppedCount = data.droppedCount,
            )
        }
        recommendationDao.insertPreferences(rows)
        authorPreferenceManager.recalculateAllPreferences(books)
    }

    suspend fun clearAllPreferences() = withContext(Dispatchers.IO) {
        recommendationDao.clearAllPreferences()
        authorPreferenceManager.clearAllPreferences()
    }

    private suspend fun recordInteraction(
        tagName: String,
        type: InteractionType,
        weight: Float = 1.0f,
        novelDelta: Int = 0,
        chaptersDelta: Int = 0,
        timeDelta: Long = 0,
        completedDelta: Int = 0,
    ) {
        val existing = recommendationDao.getPreference(tagName)
        val change = scoreChange(type, weight)
        if (existing != null) {
            recommendationDao.updatePreference(
                tag = tagName,
                score = (existing.affinityScore + change).coerceIn(0, 1000),
                novelDelta = novelDelta,
                chaptersDelta = chaptersDelta,
                timeDelta = timeDelta,
                completedDelta = completedDelta,
            )
        } else {
            recommendationDao.insertPreference(
                UserPreferenceEntity(
                    tag = tagName,
                    affinityScore = (500 + change).coerceIn(0, 1000),
                    novelCount = novelDelta.coerceAtLeast(0),
                    chaptersRead = chaptersDelta.coerceAtLeast(0),
                    readingTimeSeconds = timeDelta.coerceAtLeast(0),
                    completedCount = completedDelta.coerceAtLeast(0),
                ),
            )
        }
    }

    private class MutablePreferenceData(
        var novelCount: Int = 0,
        var chaptersRead: Int = 0,
        var readingTimeSeconds: Long = 0,
        var completedCount: Int = 0,
        var droppedCount: Int = 0,
        var scoreAccumulator: Int = 0,
    )
}
