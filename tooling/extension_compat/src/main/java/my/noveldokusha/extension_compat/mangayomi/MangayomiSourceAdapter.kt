package my.noveldokusha.extension_compat.mangayomi

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

/**
 * Adapter that wraps a Mangayomi source and exposes it as NoveLA's SourceInterface.Catalog.
 */
class MangayomiSourceAdapter(
    private val sourceInstance: MangayomiRuntime.MangayomiSourceInstance,
    private val runtime: MangayomiRuntime,
    private val networkClient: NetworkClient,
    private val extensionId: String,
    private val sourceMetadata: MangayomiRuntime.MangayomiSourceMetadata,
) : SourceInterface.Catalog {

    private val sourceId = sourceInstance.sourceId
    private val baseUrl = sourceMetadata.baseUrl
    private val apiUrl = sourceMetadata.apiUrl
    private val isManga = sourceMetadata.isManga

    override val id: String = "$extensionId#$sourceId"
    override val name: String = sourceMetadata.name
    override val nameStrId: Int = 0
    override val baseUrl: String = baseUrl.trimEnd('/')
    override val contentType: String = if (isManga) "manga" else "video"
    override val referer: String = "$baseUrl/"

    override val catalogUrl: String = if (apiUrl.isNotBlank()) apiUrl else baseUrl
    override val language: LanguageCode? = LanguageCode.fromIso6391(sourceMetadata.lang)
    override val languageTag: String? = sourceMetadata.lang
    override val iconUrl: String? = sourceMetadata.iconUrl.ifBlank { null }
    override val iconResId: Int? = null

    // ── SourceInterface.Catalog Implementation ─────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callGetPopular(sourceId, index + 1)
                return@withContext result.fold(
                    onSuccess = { pages ->
                        val items = pages.list.mapNotNull { toBookResult(it) }
                        Success(PagedList(items, index, !pages.hasNextPage))
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
                val result = runtime.callSearch(sourceId, input, index + 1, emptyList())
                return@withContext result.fold(
                    onSuccess = { pages ->
                        val items = pages.list.mapNotNull { toBookResult(it) }
                        Success(PagedList(items, index, !pages.hasNextPage))
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
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        Success(manga.title.ifBlank { null })
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
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        Success(manga.description.ifBlank { null })
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
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        Success(manga.cover.ifBlank { null })
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
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        Success(manga.genre)
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
        // Mangayomi doesn't have a standard rating field
        return withContext(Dispatchers.IO) { Success(null) }
    }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        Success(MangayomiStatus.toString(manga.status))
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
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        // Use last chapter's dateUpload
                        val lastChapter = (manga.chapters + manga.episodes).maxByOrNull { it.dateUpload ?: 0L }
                        val date = lastChapter?.dateUpload?.let { java.util.Date(it).toString() }
                        Success(date)
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
                val result = runtime.callGetDetail(sourceId, bookUrl)
                return@withContext result.fold(
                    onSuccess = { manga ->
                        val allChapters = manga.chapters + manga.episodes
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
        // For novels, we'd need to call getHtmlContent
        return null
    }

    override suspend fun getChapterPages(doc: Document): List<String>? {
        // For manga, we'd need to call getPageList with the chapter URL
        // The doc doesn't contain the chapter URL
        return null
    }

    override suspend fun getVideoList(episodeUrl: String): Response<List<VideoSource>>? {
        if (!isManga) {
            return withContext(Dispatchers.IO) {
                try {
                    val result = runtime.callGetVideoList(sourceId, episodeUrl)
                    return@withContext result.fold(
                        onSuccess = { videos ->
                            val videoSources = videos.map { v ->
                                VideoSource(
                                    url = v.url,
                                    quality = v.quality,
                                    headers = v.headers ?: mapOf("Referer" to episodeUrl),
                                )
                            }
                            Success(videoSources)
                        },
                        onFailure = { e ->
                            Error(e.message ?: "Video list failed", e)
                        }
                    )
                } catch (e: Exception) {
                    Error(e.message ?: "Video list failed", e)
                }
            }
        }
        return null
    }

    // ── FilterableCatalog support ──────────────────────────────────────────────

    override suspend fun getFilterList(): Response<List<my.noveldokusha.scraper.LuaFilter>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = runtime.callGetFilterList(sourceId)
                return@withContext result.fold(
                    onSuccess = { filterList ->
                        val luaFilters = filterList.filters.mapNotNull { toLuaFilter(it) }
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
                // Convert NoveLA filters to Mangayomi filter format
                val mangayomiFilters = filters.filters.map { (k, v) ->
                    mapOf(
                        "key" to k,
                        "value" to v,
                    )
                }
                val result = runtime.callSearch(sourceId, "", index + 1, mangayomiFilters)
                return@withContext result.fold(
                    onSuccess = { pages ->
                        val items = pages.list.mapNotNull { toBookResult(it) }
                        Success(PagedList(items, index, !pages.hasNextPage))
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

    private fun toBookResult(item: MItem): BookResult? {
        val url = item.url.ifBlank { item.link }.ifBlank { return null }
        val title = item.name.ifBlank { return null }
        val cover = item.cover.ifBlank { "" }
        return BookResult(
            title = title,
            url = absolutize(url),
            coverImageUrl = absolutize(cover),
            description = "",
            contentType = contentType,
        )
    }

    private fun toChapterResult(chapter: MChapter): ChapterResult? {
        val url = chapter.url.ifBlank { return null }
        val name = chapter.name.ifBlank { return null }
        val uploaded = chapter.dateUpload?.let { java.util.Date(it).time }
        return ChapterResult(
            title = name,
            url = absolutize(url),
            uploaded = uploaded,
        )
    }

    private fun toLuaFilter(filter: MFilter): my.noveldokusha.scraper.LuaFilter? {
        val type = when (filter.type.lowercase()) {
            "selectfilter" -> my.noveldokusha.scraper.LuaFilter.Type.Picker
            "groupfilter" -> my.noveldokusha.scraper.LuaFilter.Type.Picker
            "sortfilter" -> my.noveldokusha.scraper.LuaFilter.Type.Picker
            "checkfilter" -> my.noveldokusha.scraper.LuaFilter.Type.CheckboxGroup
            "separatorfilter" -> null
            else -> my.noveldokusha.scraper.LuaFilter.Type.Picker
        } ?: return null

        return my.noveldokusha.scraper.LuaFilter(
            key = filter.key,
            label = filter.label,
            type = type,
            value = filter.value?.toString() ?: "",
            options = filter.options?.map { o -> my.noveldokusha.scraper.LuaFilter.Option(o.label, o.value) },
        )
    }

    private fun absolutize(url: String): String {
        if (url.startsWith("http")) return url
        return baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }
}