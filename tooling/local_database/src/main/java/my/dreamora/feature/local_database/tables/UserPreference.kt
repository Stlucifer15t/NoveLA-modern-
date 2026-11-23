package my.dreamora.feature.local_database.tables

/*
 * Table definitions for the For You recommender. Ported from Novery
 * (https://github.com/1Finn2me/Novery, GPL-3.0); see NOTICE.md.
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Per-tag taste profile: how the user reacts to novels with this tag. */
@Entity(
    tableName = "user_preferences",
    indices = [Index(value = ["tag"])]
)
data class UserPreferenceEntity(
    /** TagCategory name */
    @PrimaryKey
    val tag: String,
    val affinityScore: Int = 0,
    val novelCount: Int = 0,
    val chaptersRead: Int = 0,
    val readingTimeSeconds: Long = 0,
    val completedCount: Int = 0,
    val droppedCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val completionRate: Float
        get() = if (novelCount > 0) completedCount.toFloat() / novelCount else 0f

    val dropRate: Float
        get() = if (novelCount > 0) droppedCount.toFloat() / novelCount else 0f
}
