package my.dreamora.feature.local_database.tables

/*
 * Table definitions for the For You recommender. Ported from Novery
 * (https://github.com/1Finn2me/Novery, GPL-3.0); see NOTICE.md.
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A tag the user blocked, boosted or reduced. Neutral tags are not stored. */
@Entity(
    tableName = "user_tag_filters",
    indices = [Index(value = ["filterType"])]
)
data class UserTagFilterEntity(
    /** TagCategory name */
    @PrimaryKey
    val tag: String,
    /** BLOCKED, BOOSTED or REDUCED */
    val filterType: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** A novel the user hid from For You. */
@Entity(
    tableName = "hidden_novels",
    indices = [Index(value = ["hiddenAt"])]
)
data class HiddenNovelEntity(
    @PrimaryKey
    val novelUrl: String,
    val novelName: String,
    /** NOT_INTERESTED, ALREADY_READ_ELSEWHERE or DISLIKED */
    val reason: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

/** An author the user blocked. Their novels never appear in For You. */
@Entity(
    tableName = "blocked_authors",
    indices = [Index(value = ["blockedAt"])]
)
data class BlockedAuthorEntity(
    @PrimaryKey
    val authorNormalized: String,
    val displayName: String,
    val blockedAt: Long = System.currentTimeMillis()
)

enum class TagFilterType {
    BLOCKED,
    BOOSTED,
    REDUCED,
    NEUTRAL
}

enum class HideReason {
    NOT_INTERESTED,
    ALREADY_READ_ELSEWHERE,
    DISLIKED
}
