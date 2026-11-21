package my.noveldokusha.extension_compat.mangayomi

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Mangayomi source model (JavaScript/Dart).
 * Based on Mangayomi extension API: https://kodjodevf.github.io/mangayomi-extensions/CONTRIBUTING-JS.html
 */
@Serializable
data class MangayomiSource(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("baseUrl") val baseUrl: String = "",
    @SerialName("apiUrl") val apiUrl: String = "",
    @SerialName("lang") val lang: String = "en",
    @SerialName("iconUrl") val iconUrl: String = "",
    @SerialName("version") val version: Int = 1,
    @SerialName("dateFormat") val dateFormat: String = "",
    @SerialName("dateFormatLocale") val dateFormatLocale: String = "",
    @SerialName("isManga") val isManga: Boolean = true,
    @SerialName("isNsfw") val isNsfw: Boolean = false,
)

/**
 * Source code structure for Mangayomi JS extensions
 */
@Serializable
data class MangayomiSourceCode(
    val source: MangayomiSource,
    val code: String, // JavaScript source code
)

/**
 * Mangayomi API response types
 */
@Serializable
data class MPages(
    @SerialName("list") val list: List<MItem>,
    @SerialName("hasNextPage") val hasNextPage: Boolean = false,
)

@Serializable
data class MItem(
    @SerialName("url") val url: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("link") val link: String = "",
    @SerialName("cover") val cover: String = "",
)

@Serializable
data class MManga(
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("artist") val artist: String = "",
    @SerialName("genre") val genre: List<String> = emptyList(),
    @SerialName("status") val status: Int = 0, // 0=ongoing, 1=complete, 2=hiatus, 3=canceled, 4=publishingFinished, 5=unknown
    @SerialName("chapters") val chapters: List<MChapter> = emptyList(),
    @SerialName("episodes") val episodes: List<MChapter> = emptyList(),
    @SerialName("cover") val cover: String = "",
)

@Serializable
data class MChapter(
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("scanlator") val scanlator: String = "",
    @SerialName("dateUpload") val dateUpload: Long? = null,
    @SerialName("page") val page: String? = null,
)

@Serializable
data class PageUrl(
    @SerialName("url") val url: String = "",
    @SerialName("headers") val headers: Map<String, String>? = null,
)

@Serializable
data class Video(
    @SerialName("url") val url: String = "",
    @SerialName("originalUrl") val originalUrl: String = "",
    @SerialName("quality") val quality: String = "",
    @SerialName("headers") val headers: Map<String, String>? = null,
)

@Serializable
data class FilterList(
    @SerialName("filters") val filters: List<MFilter>,
)

@Serializable
data class MFilter(
    @SerialName("key") val key: String = "",
    @SerialName("label") val label: String = "",
    @SerialName("type") val type: String = "", // SelectFilter, SeparatorFilter, GroupFilter, SortFilter, CheckFilter
    @SerialName("value") val value: Any? = null,
    @SerialName("options") val options: List<MFilterOption>? = null,
)

@Serializable
data class MFilterOption(
    @SerialName("label") val label: String = "",
    @SerialName("value") val value: String = "",
)

@Serializable
data class SourcePreference(
    @SerialName("key") val key: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("summary") val summary: String = "",
    @SerialName("type") val type: String = "", // ListPreference, EditTextPreference, SwitchPreference
    @SerialName("value") val value: Any? = null,
    @SerialName("entries") val entries: List<String>? = null,
    @SerialName("entryValues") val entryValues: List<String>? = null,
)

/**
 * Internal data classes for bridging to NoveLA
 */
data class MangayomiMangaData(
    val url: String,
    val title: String?,
    val cover: String?,
    val author: String?,
    val description: String?,
    val genres: List<String>,
    val status: String?,
    val chapters: List<MangayomiChapterData>,
)

data class MangayomiChapterData(
    val url: String,
    val name: String,
    val dateUpload: Long?,
    val scanlator: String?,
)

object MangayomiStatus {
    const val ONGOING = 0
    const val COMPLETE = 1
    const val HIATUS = 2
    const val CANCELED = 3
    const val PUBLISHING_FINISHED = 4
    const val UNKNOWN = 5

    fun toString(status: Int): String = when (status) {
        ONGOING -> "Ongoing"
        COMPLETE -> "Completed"
        HIATUS -> "Hiatus"
        CANCELED -> "Canceled"
        PUBLISHING_FINISHED -> "Publishing Finished"
        else -> "Unknown"
    }
}