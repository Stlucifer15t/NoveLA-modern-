package my.dreamora.feature.local_database.tables

/*
 * Table definitions for the For You recommender. Ported from Novery
 * (https://github.com/1Finn2me/Novery, GPL-3.0); see NOTICE.md.
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A novel the recommender has seen in a source catalogue, cached for scoring. */
@Entity(
    tableName = "discovered_novels",
    indices = [
        Index(value = ["apiName"]),
        Index(value = ["discoveredAt"])
    ]
)
data class DiscoveredNovelEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val apiName: String,
    val posterUrl: String? = null,
    val rating: Int? = null,
    val tagsString: String? = null,
    val author: String? = null,
    val status: String? = null,
    val synopsis: String? = null,
    /** browse, related, search, popular */
    val source: String = "browse",
    val discoveredAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis()
) {
    val tags: List<String>
        get() = tagsString?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
}
