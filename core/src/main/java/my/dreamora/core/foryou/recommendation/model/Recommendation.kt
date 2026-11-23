package my.dreamora.core.foryou.recommendation.model

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

/**
 * Reading state as this app can express it from its own library data. Novery's DROPPED,
 * ON_HOLD and SPICY states have no counterpart here, so they are not invented.
 */
enum class TrackedStatus {
    READING,
    COMPLETED,
    PLAN_TO_READ,
    /** The reader parked it. Gives no taste signal either way. */
    ON_HOLD,
    /** The reader stopped. Counts as a negative signal, as removing it from the library would. */
    DROPPED,
}

/**
 * One library book as the recommender sees it: only facts the app stores.
 *
 * @param tags raw genre strings from the book's stored genres
 * @param author the author when a source reported one; the app does not store authors yet
 * @param readingTimeSeconds 0 when the app has no per-book reading time
 */
data class TrackedBook(
    val url: String,
    val title: String,
    val tags: List<String>,
    val author: String?,
    val status: TrackedStatus,
    val chaptersRead: Int,
    val chapterCount: Int,
    val readingTimeSeconds: Long,
    val lastReadEpochMilli: Long,
) {
    /** Share of chapters read, 0..1. */
    val progressPercent: Float
        get() = if (chapterCount > 0) chaptersRead.toFloat() / chapterCount else 0f
}

/**
 * A recommendation result with scoring and explanation.
 *
 * @param rating the source's rating on a 0..1000 scale, or null when the source gave none
 */
data class Recommendation(
    val bookUrl: String,
    val title: String,
    val coverUrl: String,
    val sourceId: String,
    val rating: Int?,
    val score: Float,
    val type: RecommendationType,
    val reason: String,
    val scoreBreakdown: ScoreBreakdown,
    val sourceBookUrl: String? = null,
    val sourceBookTitle: String? = null,
    val isCrossSource: Boolean = false,
)

/** A group of recommendations sharing one theme, rendered as one rail. */
data class RecommendationGroup(
    val type: RecommendationType,
    val title: String,
    val subtitle: String? = null,
    val recommendations: List<Recommendation>,
    val sourceBookTitle: String? = null,
)
