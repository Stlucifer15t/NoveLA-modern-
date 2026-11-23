package my.dreamora.feature.local_database.tables

/*
 * Table definitions for the For You recommender. Ported from Novery
 * (https://github.com/1Finn2me/Novery, GPL-3.0); see NOTICE.md.
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "author_preferences",
    indices = [
        Index(value = ["affinityScore"]),
        Index(value = ["displayName"])
    ]
)
data class AuthorPreferenceEntity(
    @PrimaryKey
    val authorNormalized: String,
    val displayName: String,
    val affinityScore: Int = 500,
    val novelsRead: Int = 0,
    val novelsCompleted: Int = 0,
    val novelsDropped: Int = 0,
    val totalChaptersRead: Int = 0,
    val totalReadingTimeSeconds: Long = 0,
    val novelUrlsInLibrary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val completionRate: Float
        get() = if (novelsRead > 0) novelsCompleted.toFloat() / novelsRead else 0f

    val dropRate: Float
        get() = if (novelsRead > 0) novelsDropped.toFloat() / novelsRead else 0f

    val avgChaptersPerNovel: Float
        get() = if (novelsRead > 0) totalChaptersRead.toFloat() / novelsRead else 0f

    val isLiked: Boolean
        get() = affinityScore >= 600 && novelsRead >= 1

    val isFavorite: Boolean
        get() = affinityScore >= 800 && novelsCompleted >= 1

    fun getNovelUrls(): List<String> =
        if (novelUrlsInLibrary.isBlank()) emptyList()
        else novelUrlsInLibrary.split(",").filter { it.isNotBlank() }

    fun withNovelUrl(url: String): AuthorPreferenceEntity {
        val urls = getNovelUrls().toMutableSet()
        urls.add(url)
        return copy(novelUrlsInLibrary = urls.joinToString(","))
    }

    fun withoutNovelUrl(url: String): AuthorPreferenceEntity {
        val urls = getNovelUrls().toMutableSet()
        urls.remove(url)
        return copy(novelUrlsInLibrary = urls.joinToString(","))
    }
}
