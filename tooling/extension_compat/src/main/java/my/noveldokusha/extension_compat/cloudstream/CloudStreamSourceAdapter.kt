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
 * Minimal implementation for compilation - returns empty results.
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

    private fun getIntProperty(prop: String): Int? {
        return try {
            val getter = mainApiClass.getMethod(prop)
            getter.invoke(mainApi) as? Int
        } catch (e: Exception) {
            null
        }
    }

    private fun getListProperty(prop: String): List<Any>? {
        return try {
            val getter = mainApiClass.getMethod(prop)
            val value = getter.invoke(mainApi)
            if (value is Iterable<*>) value.toList() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getMapProperty(prop: String): Map<String, String> {
        return try {
            val getter = mainApiClass.getMethod(prop)
            val value = getter.invoke(mainApi)
            if (value is Map<*, *>) {
                value.mapNotNull { (k, v) -> (k as? String)?.let { it to (v as? String) ?: "" } }.toMap()
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── SourceInterface.Catalog Implementation ─────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                // Call getMainPage(page, request)
                // For now, return empty list
                Success(PagedList(emptyList(), index, true))
            } catch (e: Exception) {
                Timber.e(e, "getCatalogList failed [$name]")
                Error(e.message ?: "Catalog list failed", e)
            }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                // For now, return empty list
                Success(PagedList(emptyList(), index, true))
            } catch (e: Exception) {
                Timber.e(e, "getCatalogSearch failed [$name]")
                Error(e.message ?: "Search failed", e)
            }
        }
    }

    override suspend fun getBookTitle(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                Success(null)
            } catch (e: Exception) {
                Error(e.message ?: "Title failed", e)
            }
        }
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                Success(null)
            } catch (e: Exception) {
                Error(e.message ?: "Description failed", e)
            }
        }
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                Success(null)
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
                // For now, return empty list
                Success(emptyList())
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
                // For now, return empty list
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
}