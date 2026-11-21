package my.noveldokusha.extension_compat.cloudstream

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * CloudStream plugin models.
 * Based on CloudStream plugin API: https://cloudstream.miraheze.org/wiki/Programming_extensions
 */
@Serializable
data class CloudStreamPluginManifest(
    @SerialName("name") val name: String = "",
    @SerialName("pluginClassName") val pluginClassName: String = "",
    @SerialName("version") val version: Int = 1,
    @SerialName("requiresResources") val requiresResources: Boolean = false,
)

/**
 * SearchResponse types
 */
sealed class SearchResponse {
    @Serializable
    data class Anime(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("apiName") val apiName: String = "",
        @SerialName("type") val type: String = "Anime",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("posterHeaders") val posterHeaders: Map<String, String>? = null,
        @SerialName("id") val id: Int? = null,
        @SerialName("quality") val quality: String? = null,
    ) : SearchResponse()

    @Serializable
    data class Movie(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("apiName") val apiName: String = "",
        @SerialName("type") val type: String = "Movie",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("posterHeaders") val posterHeaders: Map<String, String>? = null,
        @SerialName("id") val id: Int? = null,
        @SerialName("quality") val quality: String? = null,
    ) : SearchResponse()

    @Serializable
    data class TvSeries(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("apiName") val apiName: String = "",
        @SerialName("type") val type: String = "TvSeries",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("posterHeaders") val posterHeaders: Map<String, String>? = null,
        @SerialName("id") val id: Int? = null,
        @SerialName("quality") val quality: String? = null,
    ) : SearchResponse()

    @Serializable
    data class Torrent(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("apiName") val apiName: String = "",
        @SerialName("type") val type: String = "Torrent",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("posterHeaders") val posterHeaders: Map<String, String>? = null,
        @SerialName("id") val id: Int? = null,
        @SerialName("quality") val quality: String? = null,
    ) : SearchResponse()

    @Serializable
    data class Live(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("apiName") val apiName: String = "",
        @SerialName("type") val type: String = "Live",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("posterHeaders") val posterHeaders: Map<String, String>? = null,
        @SerialName("id") val id: Int? = null,
        @SerialName("quality") val quality: String? = null,
    ) : SearchResponse()
}

/**
 * MainPageRequest
 */
@Serializable
data class MainPageRequest(
    @SerialName("page") val page: Int = 1,
    @SerialName("name") val name: String = "",
    @SerialName("data") val data: String = "",
)

/**
 * HomePageResponse
 */
@Serializable
data class HomePageResponse(
    @SerialName("name") val name: String = "",
    @SerialName("items") val items: List<SearchResponse> = emptyList(),
    @SerialName("hasNextPage") val hasNextPage: Boolean = false,
)

/**
 * LoadResponse types
 */
sealed class LoadResponse {
    @Serializable
    data class Anime(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("type") val type: String = "Anime",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("episodes") val episodes: List<Episode> = emptyList(),
        @SerialName("seasons") val seasons: List<Season> = emptyList(),
    ) : LoadResponse()

    @Serializable
    data class Movie(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("type") val type: String = "Movie",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("videoUrl") val videoUrl: String? = null,
    ) : LoadResponse()

    @Serializable
    data class TvSeries(
        @SerialName("name") val name: String = "",
        @SerialName("url") val url: String = "",
        @SerialName("type") val type: String = "TvSeries",
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("seasons") val seasons: List<Season> = emptyList(),
    ) : LoadResponse()
}

@Serializable
data class Episode(
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("thumbnail") val thumbnail: String = "",
    @SerialName("number") val number: Int = 0,
    @SerialName("season") val season: Int = 0,
)

@Serializable
data class Season(
    @SerialName("name") val name: String = "",
    @SerialName("episodes") val episodes: List<Episode> = emptyList(),
)

/**
 * ExtractorLink types
 */
@Serializable
data class ExtractorLink(
    @SerialName("source") val source: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("referer") val referer: String = "",
    @SerialName("quality") val quality: Int = 0,
    @SerialName("headers") val headers: Map<String, String> = emptyMap(),
    @SerialName("extractorData") val extractorData: String? = null,
    @SerialName("type") val type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    @SerialName("audioTracks") val audioTracks: List<AudioTrack> = emptyList(),
)

enum class ExtractorLinkType {
    @SerialName("VIDEO") VIDEO,
    @SerialName("M3U8") M3U8,
    @SerialName("DASH") DASH,
    @SerialName("TORRENT") TORRENT,
    @SerialName("MAGNET") MAGNET,
}

@Serializable
data class AudioTrack(
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("language") val language: String = "",
)

/**
 * SubtitleFile
 */
@Serializable
data class SubtitleFile(
    @SerialName("url") val url: String = "",
    @SerialName("language") val language: String = "",
    @SerialName("headers") val headers: Map<String, String> = emptyMap(),
)

/**
 * Qualities
 */
@Serializable
data class Qualities(
    @SerialName("value") val value: Int = 0,
    @SerialName("defaultPriority") val defaultPriority: Int = 0,
) {
    companion object {
        val Unknown = Qualities(400, 4)
        val P144 = Qualities(144, 0)
        val P240 = Qualities(240, 2)
        val P360 = Qualities(360, 3)
        val P480 = Qualities(480, 4)
        val P720 = Qualities(720, 5)
        val P1080 = Qualities(1080, 6)
        val P1440 = Qualities(1440, 7)
        val P2160 = Qualities(2160, 8)
    }
}

/**
 * Internal data classes for bridging to NoveLA
 */
data class CloudStreamMangaData(
    val url: String,
    val title: String?,
    val cover: String?,
    val description: String?,
    val episodes: List<CloudStreamEpisodeData>,
    val seasons: List<CloudStreamSeasonData>,
)

data class CloudStreamEpisodeData(
    val url: String,
    val name: String,
    val number: Int,
    val season: Int,
    val thumbnail: String?,
)

data class CloudStreamSeasonData(
    val name: String,
    val episodes: List<CloudStreamEpisodeData>,
)