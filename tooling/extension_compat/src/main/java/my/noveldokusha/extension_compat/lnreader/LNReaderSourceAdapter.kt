package my.noveldokusha.extension_compat.lnreader

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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Adapter that wraps an LNReader plugin and exposes it as NoveLA's SourceInterface.Catalog.
 */
class LNReaderSourceAdapter(
    private val pluginInstance: LNReaderRuntime.LNReaderPluginInstance,
    private val runtime: LNReaderRuntime,
    private val networkClient: NetworkClient,
    private val extensionId: String,
    private val pluginMetadata: LNReaderRuntime.LNReaderPluginMetadata,
) : SourceInterface.Catalog {

    private val pluginId = pluginInstance.pluginId
    private val site = pluginMetadata.site
    private val contentType: String = "" // LNReader is primarily novels

    override val id: String = "$extensionId#$pluginId"
    override val name: String = pluginMetadata.name
    override val nameStrId: Int = 0
    override val baseUrl: String = site.trimEnd('/')
    override val contentType: String = contentType
    override val referer: String = "$baseUrl/"

    override val catalogUrl: String = baseUrl
    override val language: LanguageCode? = LanguageCode.fromIso6391("en")
    override val languageTag: String? = "en"
    override val iconUrl: String? = pluginMetadata.icon.ifBlank { null }
    override val iconResId: Int? = null

    // ── SourceInterface.Catalog Implementation ─────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val options = PopularNovelsOptions(showLatestNovels = false, filters = null)
                val result = runtime.callPopularNovels(pluginId, index + 1, options)
                return@withContext result.fold(
                    onSuccess = { novels ->
                        val items = novels.mapNotNull { toBookResult(it) }
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
                val result = runtime.callSearchNovels(pluginId, input, index + 1)
                return@withContext result.fold(
                    onSuccess = { novels ->
                        val items = novels.mapNotNull { toBookResult(it) }
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
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        Success(novel.name.ifBlank { null })
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
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        Success(novel.summary.ifBlank { null })
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
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        Success(novel.cover.ifBlank { null })
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
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        val genres = novel.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        Success(genres)
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Genres failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Genres failed", e)
            }
        }
    }

    override suspend fun getBookRating(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        Success(novel.rating?.toString())
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Rating failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Rating failed", e)
            }
        }
    }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        Success(novel.status.ifBlank { null })
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Status failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Status failed", e)
            }
        }
    }

    override suspend fun getBookLastUpdate(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        // LNReader doesn't have a standard lastUpdate field
                        // We could use the last chapter's releaseTime
                        val lastChapter = novel.chapters.lastOrNull()
                        Success(lastChapter?.releaseTime?.ifBlank { null })
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Last update failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Last update failed", e)
            }
        }
    }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callParseNovel(pluginId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { novel ->
                        var allChapters = novel.chapters
                        // If paginated, fetch additional pages
                        if (novel.totalPages != null && novel.totalPages!! > 1) {
                            for (page in 2..novel.totalPages!!) {
                                val pageResult = runtime.callParsePage(pluginId, bookUrl, page.toString())
                                pageResult.onSuccess { pageNovel ->
                                    allChapters += pageNovel.chapters
                                }
                            }
                        }
                        val items = allChapters.mapNotNull { toChapterResult(it) }
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
        // LNReader plugins provide parseChapter which returns HTML
        // We'd need the chapter URL, which we don't have from doc alone
        return null
    }

    override suspend fun getChapterPages(doc: Document): List<String>? {
        // LNReader is for novels, not manga
        return null
    }

    override suspend fun getVideoList(episodeUrl: String): Response<List<VideoSource>>? {
        // LNReader is primarily for novels
        return null
    }

    // ── FilterableCatalog support (if plugin has filters) ──────────────────────

    override suspend fun getFilterList(): Response<List<my.noveldokusha.scraper.LuaFilter>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.getFilters(pluginId)
                return@withContext result.fold(
                    onSuccess = { filters ->
                        val luaFilters = convertFilters(filters)
                        Success(luaFilters)
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Filter list failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Filter list failed", e)
            }
        }
    }

    override suspend fun getCatalogFiltered(index: Int, filters: my.noveldokusha.scraper.ActiveFilters): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert NoveLA filters to LNReader filter format
                val filterOptions = PopularNovelsOptions(
                    showLatestNovels = false,
                    filters = filters.filters.associate { (k, v) ->
                        k to FilterValue(type = "Picker", value = v)
                    },
                )
                val result = runtime.callPopularNovels(pluginId, index + 1, filterOptions)
                return@withContext result.fold(
                    onSuccess = { novels ->
                        val items = novels.mapNotNull { toBookResult(it) }
                        Success(PagedList(items, index, items.isEmpty()))
                    },
                    onFailure = { e ->
                        Error(e.message ?: "Filtered catalog failed", e)
                    }
                )
            } catch (e: Exception) {
                Error(e.message ?: "Filtered catalog failed", e)
            }
        }
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private fun toBookResult(novel: NovelItem): BookResult? {
        val path = novel.path.ifBlank { return null }
        val title = novel.name.ifBlank { return null }
        val cover = novel.cover.ifBlank { "" }
        val url = absolutize(path)
        return BookResult(
            title = title,
            url = url,
            coverImageUrl = absolutize(cover),
            description = "",
            contentType = contentType,
        )
    }

    private fun toChapterResult(chapter: ChapterItem): ChapterResult? {
        val path = chapter.path.ifBlank { return null }
        val name = chapter.name.ifBlank { return null }
        val url = absolutize(path)
        val uploaded = chapter.releaseTime.ifBlank { null }?.let { parseDateToMillis(it) }
        return ChapterResult(
            title = name,
            url = url,
            uploaded = uploaded,
            chapter = chapter.chapterNumber,
        )
    }

    private fun convertFilters(filterDef: FilterDefinition): List<my.noveldokusha.scraper.LuaFilter> {
        // LNReader filters are dynamic - we'd need to parse the JsonObject
        // For now, return empty list
        return emptyList()
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "MMM dd, yyyy",
            "dd MMM yyyy",
        )
        for (format in formats) {
            try {
                return java.text.SimpleDateFormat(format, java.util.Locale.US).parse(dateStr)?.time
            } catch (e: Exception) {
            }
        }
        return null
    }

    private fun absolutize(url: String): String {
        if (url.startsWith("http")) return url
        return baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }
}