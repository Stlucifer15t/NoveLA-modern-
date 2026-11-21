package my.noveldokusha.extension_compat.cloudstream

import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.core.Response.Error
import my.noveldokusha.core.Response.Success
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import my.noveldokusha.scraper.domain.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.lang.reflect.Method

/**
 * Adapter that wraps a CloudStream MainAPI and exposes it as NoveLA's SourceInterface.Catalog.
 */
class CloudStreamSourceAdapter(
    private val pluginInstance: CloudStreamRuntime.CloudStreamPluginInstance,
    private val runtime: CloudStreamRuntime,
    private val networkClient: NetworkClient,
    private val extensionId: String,
    private val apiIndex: Int,
    private val mainApi: Any,
) : SourceInterface.Catalog {

    private val mainApiClass = mainApi.javaClass

    // Cache API properties
    private var cachedName: String? = null
    private var cachedMainUrl: String? = null

    override val id: String = "${extensionId}#api$apiIndex"
    override val name: String = cachedName ?: getStringProperty("name") ?: "CloudStream Source"
    override val nameStrId: Int = 0
    override val baseUrl: String = cachedMainUrl?.trimEnd('/') ?: getStringProperty("mainUrl")?.trimEnd('/') ?: ""
    override val contentType: String = "video"
    override val referer: String = "$baseUrl/"

    override val catalogUrl: String = baseUrl
    override val language: LanguageCode? = LanguageCode.fromIso6391("en")
    override val languageTag: String? = "en"
    override val iconUrl: String? = null
    override val iconResId: Int? = null

    init {
        loadApiProperties()
    }

    private fun loadApiProperties() {
        cachedName = getStringProperty("name")
        cachedMainUrl = getStringProperty("mainUrl")
    }

    private fun getStringProperty(prop: String): String? {
        return try {
            val getter = mainApiClass.getMethod(prop)
            getter.invoke(mainApi) as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun getBooleanProperty(prop: String): Boolean? {
        return try {
            val getter = mainApiClass.getMethod(prop)
            getter.invoke(mainApi) as? Boolean
        } catch (e: Exception) {
            null
        }
    }

    // ── SourceInterface.Catalog Implementation ─────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                // Call getMainPage(page, request)
                val mainPageRequestClass = pluginInstance.classLoader.loadClass("com.lagradost.cloudstream3.MainPageRequest")
                val request = mainPageRequestClass.getConstructor(Int::class.java, String::class.java, String::class.java)
                    .newInstance(index + 1, "", baseUrl)

                val result = callMainApiMethod("getMainPage", arrayOf(index + 1, request))
                return@withContext result.fold(
                    onSuccess = { homePage ->
                        val items = convertHomePageToBookResults(homePage)
                        Success(PagedList(items, index, items.isEmpty()))
                    },
                    onFailure = { e ->
                        Timber.e(e, "getCatalogList failed [$name]")
                        Error(e.message ?: "Catalog list failed", e)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "getCatalogList failed [$name]")
                Error(e.message ?: "Catalog list failed", e)
            }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = callMainApiMethod("search", arrayOf(input))
                return@withContext result.fold(
                    onSuccess = { searchResults ->
                        val items = convertSearchResultsToBookResults(searchResults)
                        Success(PagedList(items, index, items.isEmpty()))
                    },
                    onFailure = { e ->
                        Timber.e(e, "getCatalogSearch failed [$name]")
                        Error(e.message ?: "Search failed", e)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "getCatalogSearch failed [$name]")
                Error(e.message ?: "Search failed", e)
            }
        }
    }

    override suspend fun getBookTitle(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = callMainApiMethod("load", arrayOf(bookUrl))
                return@withContext result.fold(
                    onSuccess = { loadResponse ->
                        Success(getLoadResponseName(loadResponse))
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Title failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Title failed", e)
            }
        }
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = callMainApiMethod("load", arrayOf(bookUrl))
                return@withContext result.fold(
                    onSuccess = { loadResponse ->
                        Success(getLoadResponseDescription(loadResponse))
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Description failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Description failed", e)
            }
        }
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = callMainApiMethod("load", arrayOf(bookUrl))
                return@withContext result.fold(
                    onSuccess = { loadResponse ->
                        Success(getLoadResponsePosterUrl(loadResponse))
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Cover failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Cover failed", e)
            }
        }
    }

    override suspend fun getBookGenres(bookUrl: String): Response<List<String>> {
        return withContext(Dispatchers.IO) { Success(emptyList()) }
    }

    override suspend fun getBookRating(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) { Success(null) }
    }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) { Success(null) }
    }

    override suspend fun getBookLastUpdate(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) { Success(null) }
    }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = callMainApiMethod("load", arrayOf(bookUrl))
                return@withContext result.fold(
                    onSuccess = { loadResponse ->
                        val episodes = getLoadResponseEpisodes(loadResponse)
                        val items = episodes.mapNotNull { toChapterResult(it) }
                        Success(items)
                    },
                    onFailure = { e ->
                        Timber.e(e, "getChapterList failed [$name]")
                        Error(e.message ?: "Chapter list failed", e)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "getChapterList failed [$name]")
                Error(e.message ?: "Chapter list failed", e)
            }
        }
    }

    override suspend fun getChapterText(doc: Document): String? {
        return null // Video source
    }

    override suspend fun getChapterPages(doc: Document): List<String>? {
        return null // Video source
    }

    override suspend fun getVideoList(episodeUrl: String): Response<List<VideoSource>>? {
        return withContext(Dispatchers.IO) {
            try {
                // Call ExtractorApi to get video links
                val extractorApis = runtime.getExtractorApis(pluginInstance.pluginId)
                if (extractorApis.isEmpty()) {
                    return@withContext Success(emptyList())
                }

                // Use the first extractor that matches the URL
                for ((i, extractor) in extractorApis.withIndex()) {
                    val extractorMainUrl = getStringPropertyFromExtractor(extractor, "mainUrl") ?: ""
                    if (episodeUrl.startsWith(extractorMainUrl)) {
                        val callbackResults = mutableListOf<VideoSource>()
                        val callback: (Any) -> Unit = { link ->
                            val videoSource = convertExtractorLinkToVideoSource(link, episodeUrl)
                            videoSource?.let { callbackResults.add(it) }
                        }

                        val subtitleCallback: (Any) -> Unit = { _ -> }

                        val result = runtime.callExtractorApiMethod(pluginInstance.pluginId, i, "getUrl", arrayOf(episodeUrl, null, subtitleCallback, callback))
                        result.onSuccess { _ ->
                            if (callbackResults.isNotEmpty()) {
                                return@withContext Success(callbackResults)
                            }
                        }
                    }
                }

                Success(emptyList())
            } catch (e: Exception) {
                Error(e.message ?: "Video list failed", e)
            }
        }
    }

    // ── FilterableCatalog support (not applicable for CloudStream) ────────────

    override suspend fun getFilterList(): Response<List<my.noveldokusha.scraper.LuaFilter>> {
        return withContext(Dispatchers.IO) { Success(emptyList()) }
    }

    override suspend fun getCatalogFiltered(index: Int, filters: my.noveldokusha.scraper.ActiveFilters): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) { Success(PagedList(emptyList(), index, true)) }
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private suspend fun callMainApiMethod(methodName: String, args: Array<Any>): Result<Any> {
        return runtime.callMainApiMethod(pluginInstance.pluginId, apiIndex, methodName, args)
    }

    private fun convertHomePageToBookResults(homePage: Any): List<BookResult> {
        val items = getListProperty(homePage, "items") ?: emptyList()
        return items.mapNotNull { convertSearchResponseToBookResult(it) }
    }

    private fun convertSearchResultsToBookResults(searchResults: Any): List<BookResult> {
        val list = when (searchResults) {
            is List<*> -> searchResults
            else -> emptyList<Any>()
        }
        return list.mapNotNull { convertSearchResponseToBookResult(it) }
    }

    private fun convertSearchResponseToBookResult(response: Any): BookResult? {
        val name = getStringPropertyFromSearchResponse(response, "name") ?: return null
        val url = getStringPropertyFromSearchResponse(response, "url") ?: return null
        val posterUrl = getStringPropertyFromSearchResponse(response, "posterUrl") ?: ""
        return BookResult(
            title = name,
            url = url,
            coverImageUrl = posterUrl,
            description = "",
            contentType = "video",
        )
    }

    private fun getLoadResponseName(loadResponse: Any): String? {
        return getStringPropertyFromLoadResponse(loadResponse, "name")
    }

    private fun getLoadResponseDescription(loadResponse: Any): String? {
        return getStringPropertyFromLoadResponse(loadResponse, "description") ?: getStringPropertyFromLoadResponse(loadResponse, "summary")
    }

    private fun getLoadResponsePosterUrl(loadResponse: Any): String? {
        return getStringPropertyFromLoadResponse(loadResponse, "posterUrl")
    }

    private fun getLoadResponseEpisodes(loadResponse: Any): List<Any> {
        return getListProperty(loadResponse, "episodes") ?: getListProperty(loadResponse, "seasons")?.flatMap { getListProperty(it, "episodes") } ?: emptyList()
    }

    private fun toChapterResult(episode: Any): ChapterResult? {
        val name = getStringProperty(episode, "name") ?: return null
        val url = getStringProperty(episode, "url") ?: return null
        val number = getIntProperty(episode, "number")?.toDouble()
        val season = getIntProperty(episode, "season")
        val thumbnail = getStringProperty(episode, "thumbnail")
        val title = if (season != null && season > 0) "S${season}E${number?.toInt() ?: 0} - $name" else name
        return ChapterResult(
            title = title,
            url = url,
            thumbnail = thumbnail,
            chapter = number,
        )
    }

    private fun convertExtractorLinkToVideoSource(link: Any, referer: String): VideoSource? {
        val url = getStringProperty(link, "url") ?: return null
        val quality = getIntProperty(link, "quality")?.toString() ?: "Auto"
        val type = getStringProperty(link, "type") ?: "VIDEO"
        val headers = getMapProperty(link, "headers") + mapOf("Referer" to referer)
        return VideoSource(
            url = url,
            quality = quality,
            headers = headers,
        )
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private fun getStringProperty(obj: Any, prop: String): String? {
        return try {
            val getter = obj.javaClass.getMethod(prop)
            getter.invoke(obj) as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun getIntProperty(obj: Any, prop: String): Int? {
        return try {
            val getter = obj.javaClass.getMethod(prop)
            getter.invoke(obj) as? Int
        } catch (e: Exception) {
            null
        }
    }

    private fun getListProperty(obj: Any, prop: String): List<Any>? {
        return try {
            val getter = obj.javaClass.getMethod(prop)
            val value = getter.invoke(obj)
            if (value is Iterable<*>) value.toList() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getMapProperty(obj: Any, prop: String): Map<String, String> {
        return try {
            val getter = obj.javaClass.getMethod(prop)
            val value = getter.invoke(obj)
            if (value is Map<*, *>) {
                value.mapNotNull { (k, v) -> (k as? String)?.let { it to (v as? String) ?: "" } }.toMap()
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getStringPropertyFromSearchResponse(obj: Any, prop: String): String? {
        return getStringProperty(obj, prop)
    }

    private fun getStringPropertyFromLoadResponse(obj: Any, prop: String): String? {
        return getStringProperty(obj, prop)
    }

    private fun getStringPropertyFromExtractor(obj: Any, prop: String): String? {
        return getStringProperty(obj, prop)
    }
}