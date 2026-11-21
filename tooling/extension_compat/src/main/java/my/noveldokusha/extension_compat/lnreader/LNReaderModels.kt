package my.noveldokusha.extension_compat.lnreader

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * LNReader plugin model (TypeScript compiled to ES5 JS).
 * Based on LNReader plugin API: https://github.com/LNReader/lnreader-plugins/blob/master/docs/docs.md
 */
@Serializable
data class LNReaderPlugin(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("icon") val icon: String = "",
    @SerialName("site") val site: String = "",
    @SerialName("version") val version: String = "1.0.0",
    @SerialName("imageRequestInit") val imageRequestInit: ImageRequestInit? = null,
    @SerialName("filters") val filters: FilterDefinition? = null,
    @SerialName("pluginSettings") val pluginSettings: Map<String, SettingDefinition>? = null,
    @SerialName("webStorageUtilized") val webStorageUtilized: Boolean = false,
    @SerialName("customJS") val customJS: String? = null,
    @SerialName("customCSS") val customCSS: String? = null,
)

@Serializable
data class ImageRequestInit(
    @SerialName("headers") val headers: Map<String, String>? = null,
)

@Serializable
data class FilterDefinition(
    // Dynamic keys for each filter
) {
    // Filters are dynamic, parsed as JsonObject
}

@Serializable
data class FilterProperties(
    @SerialName("label") val label: String = "",
    @SerialName("type") val type: String = "Picker", // Picker, TextInput, Switch, CheckboxGroup, ExcludableCheckboxGroup
    @SerialName("value") val value: Any? = null,
    @SerialName("options") val options: List<FilterOption>? = null,
)

@Serializable
data class FilterOption(
    @SerialName("label") val label: String = "",
    @SerialName("value") val value: String = "",
)

@Serializable
data class SettingDefinition(
    @SerialName("value") val value: Any? = null,
    @SerialName("label") val label: String = "",
    @SerialName("type") val type: String = "Text", // Text, Switch, Select, CheckboxGroup
    @SerialName("options") val options: List<FilterOption>? = null,
)

/**
 * Plugin API input/output types
 */
@Serializable
data class PopularNovelsOptions(
    @SerialName("showLatestNovels") val showLatestNovels: Boolean = false,
    @SerialName("filters") val filters: Map<String, FilterValue>? = null,
)

@Serializable
data class FilterValue(
    @SerialName("type") val type: String = "",
    @SerialName("value") val value: Any? = null,
)

@Serializable
data class NovelItem(
    @SerialName("name") val name: String = "",
    @SerialName("path") val path: String = "",
    @SerialName("cover") val cover: String = "",
)

@Serializable
data class ChapterItem(
    @SerialName("name") val name: String = "",
    @SerialName("path") val path: String = "",
    @SerialName("releaseTime") val releaseTime: String = "",
    @SerialName("chapterNumber") val chapterNumber: Double = 0.0,
    @SerialName("page") val page: String? = null,
    @SerialName("scanlator") val scanlator: Any? = null,
)

@Serializable
data class SourceNovel(
    @SerialName("path") val path: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("genres") val genres: String = "",
    @SerialName("summary") val summary: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("artist") val artist: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("rating") val rating: Double? = null,
    @SerialName("chapters") val chapters: List<ChapterItem> = emptyList(),
    @SerialName("totalPages") val totalPages: Int? = null,
)

@Serializable
data class SourcePage(
    @SerialName("chapters") val chapters: List<ChapterItem> = emptyList(),
)

/**
 * NovelStatus enum values (from LNReader)
 */
object NovelStatus {
    const val Ongoing = "Ongoing"
    const val Completed = "Completed"
    const val Hiatus = "Hiatus"
    const val Unknown = "Unknown"
}

/**
 * Internal data classes for bridging to NoveLA
 */
data class LNReaderMangaData(
    val url: String,
    val title: String?,
    val cover: String?,
    val author: String?,
    val description: String?,
    val genres: List<String>,
    val status: String?,
    val rating: String?,
    val chapters: List<LNReaderChapterData>,
)

data class LNReaderChapterData(
    val url: String,
    val name: String,
    val chapterNumber: Double?,
    val releaseTime: String?,
    val page: String?,
)