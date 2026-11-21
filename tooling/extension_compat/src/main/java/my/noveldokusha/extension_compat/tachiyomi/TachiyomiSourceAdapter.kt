package my.noveldokusha.extension_compat.tachiyomi

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Adapter that wraps a Tachiyomi/Mihon/Aniyomi source and exposes it as NoveLA's SourceInterface.Catalog.
 *
 * Communicates with the extension running in its isolated ClassLoader via TachiyomiHostBridge.
 */
class TachiyomiSourceAdapter(
    private val loadedSource: TachiyomiLoadedSource,
    private val networkClient: NetworkClient,
    private val extensionId: String,
    private val extensionMetadata: TachiyomiManifestParser.ExtensionMetadata,
    private val classLoader: ClassLoader,
    private val hostBridge: TachiyomiHostBridge,
    private val pendingRequests: ConcurrentHashMap<String, CompletableDeferred<Any>>,
    private val requestIdCounter: AtomicLong,
) : SourceInterface.Catalog, SourceInterface.FilterableCatalog {

    private val source = loadedSource.source
    private val sourceClass = loadedSource.sourceClass

    // Source properties (read once via bridge)
    private var cachedName: String? = null
    private var cachedLang: String? = null
    private var cachedBaseUrl: String? = null
    private var cachedId: String? = null
    private var cachedSupportsLatest: Boolean? = null
    private var cachedSupportsFilters: Boolean? = null

    override val id: String = "$extensionId#$sourceClass"
    override val name: String
        get() = cachedName ?: loadStringProperty("name") ?: extensionMetadata.name
    override val nameStrId: Int = 0
    override val baseUrl: String
        get() = cachedBaseUrl?.trimEnd('/') ?: loadStringProperty("baseUrl")?.trimEnd('/') ?: ""
    override val contentType: String = when {
        isAnimeSource() -> "video"
        isMangaSource() -> "manga"
        else -> ""
    }
    override val referer: String = "$baseUrl/"

    override val catalogUrl: String = baseUrl
    override val language: LanguageCode? = cachedLang?.let { LanguageCode.fromIso6391(it) } ?: loadLanguageCode()
    override val languageTag: String? = language?.iso639_1
    override val iconUrl: String? = null
    override val iconResId: Int? = null

    // ── Initialize source properties ──────────────────────────────────────────

    init {
        // Pre-load properties via bridge
        loadSourceProperties()
    }

    private fun loadSourceProperties() {
        // Use reflection directly on the source instance since we're in the same process
        // (the source instance was created in the extension's ClassLoader but we hold a reference)
        try {
            cachedName = getStringProperty(source, "name")
            cachedLang = getStringProperty(source, "lang")
            cachedBaseUrl = getStringProperty(source, "baseUrl")
            cachedId = getStringProperty(source, "id")
            cachedSupportsLatest = getBooleanProperty(source, "supportsLatest")
            cachedSupportsFilters = getBooleanProperty(source, "supportsFilterFetching")
        } catch (e: Exception) {
            Timber.w(e, "Failed to load source properties")
        }
    }

    private fun loadStringProperty(prop: String): String? {
        return try {
            val getter = sourceClass.javaClass.getMethod(prop)
            getter.invoke(source) as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun loadLanguageCode(): LanguageCode? {
        return cachedLang?.let { LanguageCode.fromIso6391(it) }
    }

    private fun isMangaSource(): Boolean {
        val className = sourceClass.simpleName.lowercase()
        return !className.contains("anime") && !className.contains("aniyomi")
    }

    private fun isAnimeSource(): Boolean {
        val className = sourceClass.simpleName.lowercase()
        return className.contains("anime") || className.contains("aniyomi") ||
            extensionMetadata.contentWarning > 0
    }

    // ── Bridge communication ──────────────────────────────────────────────────

    private suspend fun <T> sendRequest(request: TachiyomiRequest): T {
        val requestId = "${requestIdCounter.incrementAndGet()}"
        val deferred = CompletableDeferred<Any>()
        pendingRequests[requestId] = deferred

        // Load TachiyomiExtensionRunner in extension's ClassLoader and execute
        val runnerClass = classLoader.loadClass("my.noveldokusha.extension_compat.tachiyomi.TachiyomiExtensionRunner")
        val runnerInstance = runnerClass.getConstructor(
            TachiyomiHostBridge::class.java,
            Any::class.java,
        ).newInstance(hostBridge, source)

        val executeMethod = runnerClass.getMethod("execute", TachiyomiRequest::class.java)
        executeMethod.invoke(runnerInstance, request.copy(requestId = requestId))

        return deferred.await() as T
    }

    // ── SourceInterface.Catalog Implementation ─────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.POPULAR,
                    page = index + 1,
                )
                val result = sendRequest<TachiyomiExtensionLoader.MangaPageResult>(request)
                val items = result.mangas.mapNotNull { toBookResult(it) }
                Success(PagedList(items, index, !result.hasNextPage))
            } catch (e: Exception) {
                Timber.e(e, "getCatalogList failed [$name]")
                Error(e.message ?: "Catalog list failed", e)
            }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.SEARCH,
                    page = index + 1,
                    query = input,
                    filters = emptyList(),
                )
                val result = sendRequest<TachiyomiExtensionLoader.MangaPageResult>(request)
                val items = result.mangas.mapNotNull { toBookResult(it) }
                Success(PagedList(items, index, !result.hasNextPage))
            } catch (e: Exception) {
                Timber.e(e, "getCatalogSearch failed [$name]")
                Error(e.message ?: "Search failed", e)
            }
        }
    }

    override suspend fun getBookTitle(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.title ?: manga?.name)
            } catch (e: Exception) {
                Error(e.message ?: "Title failed", e)
            }
        }
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.description)
            } catch (e: Exception) {
                Error(e.message ?: "Description failed", e)
            }
        }
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.imageUrl ?: manga?.thumbnailUrl)
            } catch (e: Exception) {
                Error(e.message ?: "Cover failed", e)
            }
        }
    }

    override suspend fun getBookGenres(bookUrl: String): Response<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.genres ?: emptyList())
            } catch (e: Exception) {
                Error(e.message ?: "Genres failed", e)
            }
        }
    }

    override suspend fun getBookRating(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.rating)
            } catch (e: Exception) {
                Error(e.message ?: "Rating failed", e)
            }
        }
    }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.status)
            } catch (e: Exception) {
                Error(e.message ?: "Status failed", e)
            }
        }
    }

    override suspend fun getBookLastUpdate(bookUrl: String): Response<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_BY_URL,
                    url = bookUrl,
                )
                val manga = sendRequest<MangaData?>(request)
                Success(manga?.lastUpdate)
            } catch (e: Exception) {
                Error(e.message ?: "Last update failed", e)
            }
        }
    }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> {
        return withContext(Dispatchers.IO) {
            try {
                // First get manga details to ensure chapters are loaded
                val detailRequest = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.MANGA_DETAIL,
                    manga = MangaData(url = bookUrl, title = null, name = null, imageUrl = null, thumbnailUrl = null, author = null, description = null, genres = null, status = null, rating = null, lastUpdate = null, chapters = null),
                    fetchDetails = false,
                    fetchChapters = true,
                )
                val detailResult = sendRequest<TachiyomiExtensionLoader.MangaDetailResult>(detailRequest)
                val chapters = detailResult.chapters ?: emptyList()
                val result = chapters.mapNotNull { toChapterResult(it) }
                Success(result)
            } catch (e: Exception) {
                Timber.e(e, "getChapterList failed [$name]")
                Error(e.message ?: "Chapter list failed", e)
            }
        }
    }

    override suspend fun getChapterText(doc: Document): String? {
        // Tachiyomi is for manga - no chapter text
        return null
    }

    override suspend fun getChapterPages(doc: Document): List<String>? {
        // For manga, we'd need to call getPageList with the chapter
        // This requires the chapter URL, which we don't have from the doc alone
        return null
    }

    override suspend fun getVideoList(episodeUrl: String): Response<List<VideoSource>>? {
        if (!isAnimeSource()) return null
        return withContext(Dispatchers.IO) {
            try {
                // Anime sources would use extractor API - not implemented yet
                Success(emptyList())
            } catch (e: Exception) {
                Error(e.message ?: "Video list failed", e)
            }
        }
    }

    // ── FilterableCatalog Implementation ──────────────────────────────────────

    override suspend fun getFilterList(): Response<List<my.noveldokusha.scraper.LuaFilter>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.FILTER_LIST,
                )
                val filters = sendRequest<List<FilterData>>(request)
                val luaFilters = filters.map { toLuaFilter(it) }
                Success(luaFilters)
            } catch (e: Exception) {
                Error(e.message ?: "Filter list failed", e)
            }
        }
    }

    override suspend fun getCatalogFiltered(index: Int, filters: my.noveldokusha.scraper.ActiveFilters): Response<PagedList<BookResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val tachiyomiFilters = filters.filters.map { f ->
                    FilterData(
                        key = f.key,
                        label = f.key,
                        type = "Picker",
                        value = f.value.toString(),
                        options = null,
                    )
                }
                val request = TachiyomiRequest(
                    requestId = "",
                    type = RequestType.SEARCH,
                    page = index + 1,
                    query = "",
                    filters = tachiyomiFilters,
                )
                val result = sendRequest<TachiyomiExtensionLoader.MangaPageResult>(request)
                val items = result.mangas.mapNotNull { toBookResult(it) }
                Success(PagedList(items, index, !result.hasNextPage))
            } catch (e: Exception) {
                Error(e.message ?: "Filtered catalog failed", e)
            }
        }
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private fun toBookResult(manga: MangaData): BookResult? {
        val url = manga.url.takeIf { it.isNotEmpty() } ?: return null
        val title = manga.title ?: manga.name ?: return null
        val cover = manga.imageUrl ?: manga.thumbnailUrl ?: ""
        val author = manga.author ?: ""
        val desc = manga.description ?: ""

        return BookResult(
            title = title,
            url = absolutize(url),
            coverImageUrl = absolutize(cover),
            description = buildDescription(author, desc),
            contentType = contentType,
        )
    }

    private fun toChapterResult(chapter: ChapterData): ChapterResult? {
        val url = chapter.url.takeIf { it.isNotEmpty() } ?: return null
        val name = chapter.name.takeIf { it.isNotEmpty() } ?: return null
        return ChapterResult(
            title = name,
            url = absolutize(url),
            uploaded = chapter.date?.let { parseDateToMillis(it) },
            chapter = chapter.chapter,
        )
    }

    private fun toLuaFilter(filter: FilterData): my.noveldokusha.scraper.LuaFilter {
        // Convert Tachiyomi filter to NoveLA LuaFilter
        return my.noveldokusha.scraper.LuaFilter(
            key = filter.key,
            label = filter.label,
            type = when (filter.type.lowercase()) {
                "picker" -> my.noveldokusha.scraper.LuaFilter.Type.Picker
                "textinput" -> my.noveldokusha.scraper.LuaFilter.Type.TextInput
                "switch" -> my.noveldokusha.scraper.LuaFilter.Type.Switch
                "checkboxgroup", "xcheckboxgroup" -> my.noveldokusha.scraper.LuaFilter.Type.CheckboxGroup
                else -> my.noveldokusha.scraper.LuaFilter.Type.Picker
            },
            value = filter.value,
            options = filter.options?.map { o -> my.noveldokusha.scraper.LuaFilter.Option(o.label, o.value) },
        )
    }

    private fun buildDescription(author: String, desc: String): String {
        return listOfNotNull(
            author.takeIf { it.isNotEmpty() }?.let { "Author: $it" },
            desc.takeIf { it.isNotEmpty() },
        ).joinToString("\n")
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
        return if (url.startsWith("http")) url else baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private fun getStringProperty(obj: Any, name: String): String? {
        return try {
            val getter = obj.javaClass.getMethod(name)
            getter.invoke(obj) as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun getBooleanProperty(obj: Any, name: String): Boolean? {
        return try {
            val getter = obj.javaClass.getMethod(name)
            getter.invoke(obj) as? Boolean
        } catch (e: Exception) {
            null
        }
    }
}