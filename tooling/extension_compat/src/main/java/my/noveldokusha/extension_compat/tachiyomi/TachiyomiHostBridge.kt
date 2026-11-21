package my.noveldokusha.extension_compat.tachiyomi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

/**
 * Interface implemented by the host app to receive callbacks from the extension's ClassLoader.
 * This is the CONTRACT between host and extension - both sides have this interface.
 */
interface TachiyomiHostBridge {

    /**
     * Called by the extension's ClassLoader to return a MangaPage result
     */
    fun onMangaPageResult(requestId: String, mangas: List<MangaData>, hasNextPage: Boolean)

    /**
     * Called by the extension's ClassLoader to return a Manga detail result
     */
    fun onMangaDetailResult(requestId: String, manga: MangaData?, chapters: List<ChapterData>?)

    /**
     * Called by the extension's ClassLoader to return a Page list result
     */
    fun onPageListResult(requestId: String, pages: List<PageData>)

    /**
     * Called by the extension's ClassLoader to return a FilterList result
     */
    fun onFilterListResult(requestId: String, filters: List<FilterData>)

    /**
     * Called by the extension's ClassLoader to return filter data
     */
    fun onFilterDataResult(requestId: String, data: Any?)

    /**
     * Called by the extension's ClassLoader to return a Manga by URL
     */
    fun onMangaByUrlResult(requestId: String, manga: MangaData?)

    /**
     * Called by the extension's ClassLoader for errors
     */
    fun onError(requestId: String, error: String)
}

/**
 * Data classes for crossing ClassLoader boundaries (serializable, no external deps)
 */
data class MangaData(
    val url: String,
    val title: String?,
    val name: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val author: String?,
    val description: String?,
    val genres: List<String>?,
    val status: String?,
    val rating: String?,
    val lastUpdate: String?,
    val chapters: List<ChapterData>?,
)

data class ChapterData(
    val url: String,
    val name: String,
    val chapter: Double?,
    val date: String?,
)

data class PageData(
    val url: String,
    val headers: Map<String, String>?,
    val imageUrl: String?,
)

data class FilterData(
    val key: String,
    val label: String,
    val type: String,
    val value: String,
    val options: List<FilterOptionData>?,
)

data class FilterOptionData(
    val label: String,
    val value: String,
)

/**
 * Request wrapper for async calls
 */
data class TachiyomiRequest(
    val requestId: String,
    val type: RequestType,
    val page: Int? = null,
    val query: String? = null,
    val url: String? = null,
    val filters: List<FilterData>? = null,
    val manga: MangaData? = null,
    val chapters: List<ChapterData>? = null,
    val fetchDetails: Boolean = true,
    val fetchChapters: Boolean = true,
    val chapter: ChapterData? = null,
)

enum class RequestType {
    POPULAR,
    LATEST,
    SEARCH,
    MANGA_DETAIL,
    PAGE_LIST,
    FILTER_LIST,
    FILTER_DATA,
    MANGA_BY_URL,
    MANGA_BY_URL_PAGE,
}

/**
 * Extension-side runner that executes in the extension's ClassLoader.
 * This class is instantiated via reflection in the extension's ClassLoader.
 */
class TachiyomiExtensionRunner(
    private val bridge: TachiyomiHostBridge,
    private val source: Any, // The actual KeiSource instance
) {

    private val sourceClass = source.javaClass
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    fun execute(request: TachiyomiRequest) {
        scope.launch {
            try {
                when (request.type) {
                    RequestType.POPULAR -> {
                        val method = sourceClass.getMethod("getPopularManga", Int::class.java)
                        val result = method.invoke(source, request.page ?: 1) as Any
                        val mangasPage = awaitSuspend(result)
                        val mangas = convertMangaList(mangasPage)
                        val hasNext = getBooleanProperty(mangasPage, "hasNextPage") ?: false
                        bridge.onMangaPageResult(request.requestId, mangas, hasNext)
                    }
                    RequestType.LATEST -> {
                        val method = sourceClass.getMethod("getLatestUpdates", Int::class.java)
                        val result = method.invoke(source, request.page ?: 1) as Any
                        val mangasPage = awaitSuspend(result)
                        val mangas = convertMangaList(mangasPage)
                        val hasNext = getBooleanProperty(mangasPage, "hasNextPage") ?: false
                        bridge.onMangaPageResult(request.requestId, mangas, hasNext)
                    }
                    RequestType.SEARCH -> {
                        val method = sourceClass.getMethod("getSearchMangaList", Int::class.java, String::class.java, Any::class.java)
                        val filterList = createFilterList(request.filters ?: emptyList())
                        val result = method.invoke(source, request.page ?: 1, request.query ?: "", filterList) as Any
                        val mangasPage = awaitSuspend(result)
                        val mangas = convertMangaList(mangasPage)
                        val hasNext = getBooleanProperty(mangasPage, "hasNextPage") ?: false
                        bridge.onMangaPageResult(request.requestId, mangas, hasNext)
                    }
                    RequestType.MANGA_DETAIL -> {
                        val mangaObj = createMangaObject(request.manga!!)
                        val chaptersObj = request.chapters?.map { createChapterObject(it) } ?: emptyList()
                        val method = sourceClass.getMethod("fetchMangaUpdate", Any::class.java, List::class.java, Boolean::class.java, Boolean::class.java)
                        val result = method.invoke(source, mangaObj, chaptersObj, request.fetchDetails, request.fetchChapters) as Any
                        val update = awaitSuspend(result)
                        val manga = update?.let { convertManga(it) }
                        val chapters = update?.let { getListProperty(it, "chapters")?.map { convertChapter(it) } }
                        bridge.onMangaDetailResult(request.requestId, manga, chapters)
                    }
                    RequestType.PAGE_LIST -> {
                        val chapterObj = createChapterObject(request.chapter!!)
                        val method = sourceClass.getMethod("getPageList", Any::class.java)
                        val result = method.invoke(source, chapterObj) as Any
                        val pages = awaitSuspend(result) as List<*>
                        val pageData = pages.map { convertPage(it) }
                        bridge.onPageListResult(request.requestId, pageData)
                    }
                    RequestType.FILTER_LIST -> {
                        val method = sourceClass.getMethod("getFilterList")
                        val result = method.invoke(source) as Any?
                        val filterList = result?.let { convertFilterList(it) } ?: emptyList()
                        bridge.onFilterListResult(request.requestId, filterList)
                    }
                    RequestType.FILTER_DATA -> {
                        val method = sourceClass.getMethod("fetchFilterData")
                        val result = method.invoke(source) as Any?
                        val data = result?.let { awaitSuspend(it) }
                        bridge.onFilterDataResult(request.requestId, data)
                    }
                    RequestType.MANGA_BY_URL -> {
                        val method = sourceClass.getMethod("getMangaByUrl", HttpUrl::class.java)
                        val httpUrl = HttpUrl.parse(request.url!!)
                        val result = method.invoke(source, httpUrl) as Any?
                        val manga = result?.let { awaitSuspend(it) }?.let { convertManga(it) }
                        bridge.onMangaByUrlResult(request.requestId, manga)
                    }
                    RequestType.MANGA_BY_URL_PAGE -> {
                        val method = sourceClass.getMethod("getMangasByUrl", HttpUrl::class.java, Int::class.java)
                        val httpUrl = HttpUrl.parse(request.url!!)
                        val result = method.invoke(source, httpUrl, request.page ?: 1) as Any
                        val mangasPage = awaitSuspend(result)
                        val mangas = convertMangaList(mangasPage)
                        val hasNext = getBooleanProperty(mangasPage, "hasNextPage") ?: false
                        bridge.onMangaPageResult(request.requestId, mangas, hasNext)
                    }
                }
            } catch (e: Exception) {
                bridge.onError(request.requestId, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private suspend fun awaitSuspend(continuation: Any): Any {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                val resumeMethod = continuation.javaClass.getMethod("resumeWith", Any::class.java)
                resumeMethod.invoke(continuation, kotlinx.coroutines.Result.success(cont))
            } catch (e: Exception) {
                cont.resume(continuation)
            }
        }
    }

    // ── Conversion helpers ──────────────────────────────────────────────────

    private fun convertMangaList(mangasPage: Any): List<MangaData> {
        val mangas = getListProperty(mangasPage, "mangas") ?: emptyList()
        return mangas.map { convertManga(it) }
    }

    private fun convertManga(manga: Any): MangaData {
        return MangaData(
            url = getStringProperty(manga, "url") ?: "",
            title = getStringProperty(manga, "title"),
            name = getStringProperty(manga, "name"),
            imageUrl = getStringProperty(manga, "imageUrl"),
            thumbnailUrl = getStringProperty(manga, "thumbnailUrl"),
            author = getStringProperty(manga, "author"),
            description = getStringProperty(manga, "description"),
            genres = getListProperty(manga, "genres")?.map { it.toString() },
            status = getProperty(manga, "status")?.toString(),
            rating = getProperty(manga, "rating")?.toString(),
            lastUpdate = getProperty(manga, "lastUpdate")?.toString(),
            chapters = getListProperty(manga, "chapters")?.map { convertChapter(it) },
        )
    }

    private fun convertChapter(chapter: Any): ChapterData {
        return ChapterData(
            url = getStringProperty(chapter, "url") ?: "",
            name = getStringProperty(chapter, "name") ?: "",
            chapter = getProperty(chapter, "chapter")?.let { it.toString().toDoubleOrNull() },
            date = getStringProperty(chapter, "date"),
        )
    }

    private fun convertPage(page: Any): PageData {
        return PageData(
            url = getStringProperty(page, "url") ?: "",
            headers = getProperty(page, "headers") as? Map<String, String>,
            imageUrl = getStringProperty(page, "imageUrl"),
        )
    }

    private fun convertFilterList(filterList: Any): List<FilterData> {
        val filters = getListProperty(filterList, "filters") ?: emptyList()
        return filters.map { convertFilter(it) }
    }

    private fun convertFilter(filter: Any): FilterData {
        return FilterData(
            key = getStringProperty(filter, "key") ?: "",
            label = getStringProperty(filter, "label") ?: "",
            type = getStringProperty(filter, "type") ?: "Picker",
            value = getProperty(filter, "value")?.toString() ?: "",
            options = getListProperty(filter, "options")?.map { convertFilterOption(it) },
        )
    }

    private fun convertFilterOption(option: Any): FilterOptionData {
        return FilterOptionData(
            label = getStringProperty(option, "label") ?: "",
            value = getStringProperty(option, "value") ?: "",
        )
    }

    private fun createMangaObject(manga: MangaData): Any {
        return mapOf(
            "url" to manga.url,
            "title" to manga.title,
            "name" to manga.name,
            "imageUrl" to manga.imageUrl,
            "thumbnailUrl" to manga.thumbnailUrl,
            "author" to manga.author,
            "description" to manga.description,
            "genres" to manga.genres,
            "status" to manga.status,
            "rating" to manga.rating,
            "lastUpdate" to manga.lastUpdate,
            "chapters" to manga.chapters?.map { createChapterObject(it) },
        )
    }

    private fun createChapterObject(chapter: ChapterData): Any {
        return mapOf(
            "url" to chapter.url,
            "name" to chapter.name,
            "chapter" to chapter.chapter,
            "date" to chapter.date,
        )
    }

    private fun createFilterList(filters: List<FilterData>): Any {
        return mapOf(
            "filters" to filters.map { f ->
                mapOf(
                    "key" to f.key,
                    "label" to f.label,
                    "type" to f.type,
                    "value" to f.value,
                    "options" to f.options?.map { o ->
                        mapOf("label" to o.label, "value" to o.value)
                    },
                )
            }
        )
    }

    // ── Property getters ────────────────────────────────────────────────────

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

    private fun getProperty(obj: Any, name: String): Any? {
        return try {
            val getter = obj.javaClass.getMethod(name)
            getter.invoke(obj)
        } catch (e: Exception) {
            null
        }
    }

    private fun getListProperty(obj: Any, name: String): List<*>? {
        return try {
            val getter = obj.javaClass.getMethod(name)
            val value = getter.invoke(obj)
            if (value is Iterable<*>) value.toList() else null
        } catch (e: Exception) {
            null
        }
    }
}