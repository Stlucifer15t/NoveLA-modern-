package my.noveldokusha.extension_compat.tachiyomi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridge interface that runs inside the extension's ClassLoader and calls suspend functions
 * on the KeiSource instance, then returns results to the host app.
 *
 * This class is compiled into the host app but instantiated in the extension's ClassLoader
 * via reflection, allowing it to call suspend functions directly.
 */
@Suppress("UNUSED_PARAMETER")
interface TachiyomiAdapterBridge {

    /**
     * Get popular manga page
     */
    suspend fun getPopularManga(page: Int): MangasPageResult

    /**
     * Get latest updates page
     */
    suspend fun getLatestUpdates(page: Int): MangasPageResult

    /**
     * Search manga with query and filters
     */
    suspend fun getSearchMangaList(page: Int, query: String, filters: FilterListWrapper): MangasPageResult

    /**
     * Fetch manga details and/or chapters
     */
    suspend fun fetchMangaUpdate(
        manga: MangaWrapper,
        chapters: List<ChapterWrapper>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): MangaUpdateResult

    /**
     * Get page list for a chapter
     */
    suspend fun getPageList(chapter: ChapterWrapper): List<PageWrapper>

    /**
     * Get manga by URL
     */
    suspend fun getMangaByUrl(url: String): MangaWrapper?

    /**
     * Get mangas by URL (for category/list pages)
     */
    suspend fun getMangasByUrl(url: String, page: Int): MangasPageResult

    /**
     * Get filter list
     */
    suspend fun getFilterList(): FilterListWrapper?

    /**
     * Fetch filter data from network
     */
    suspend fun fetchFilterData(): Any?

    /**
     * Get manga URL from manga object
     */
    fun getMangaUrl(manga: MangaWrapper): String

    /**
     * Get chapter URL from chapter object
     */
    fun getChapterUrl(chapter: ChapterWrapper): String

    /**
     * Whether source supports latest updates
     */
    val supportsLatest: Boolean

    /**
     * Whether source supports filter fetching
     */
    val supportsFilterFetching: Boolean

    /**
     * Configure headers for requests
     */
    fun configureHeaders(builder: okhttp3.Headers.Builder): okhttp3.Headers.Builder

    /**
     * Image fetch type
     */
    val imageFetch: Int

    // Source properties
    val name: String
    val lang: String
    val baseUrl: String
    val id: String
}

/**
 * Result wrapper for MangasPage
 */
data class MangasPageResult(
    val mangas: List<MangaWrapper>,
    val hasNextPage: Boolean,
)

/**
 * Wrapper for SManga
 */
data class MangaWrapper(
    val url: String,
    val title: String?,
    val name: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val author: String?,
    val description: String?,
    val genres: List<String>?,
    val status: Any?,
    val rating: Any?,
    val lastUpdate: Any?,
    val chapters: List<ChapterWrapper>?,
)

/**
 * Wrapper for SChapter
 */
data class ChapterWrapper(
    val url: String,
    val name: String,
    val chapter: Double?,
    val date: String?,
)

/**
 * Wrapper for Page
 */
data class PageWrapper(
    val url: String,
    val headers: Map<String, String>?,
    val imageUrl: String?,
)

/**
 * Wrapper for FilterList
 */
data class FilterListWrapper(
    val filters: List<FilterWrapper>,
)

/**
 * Wrapper for Filter
 */
data class FilterWrapper(
    val key: String,
    val label: String,
    val type: String, // Picker, TextInput, Switch, CheckboxGroup, ExcludableCheckboxGroup
    val value: Any,
    val options: List<FilterOptionWrapper>?,
)

/**
 * Wrapper for FilterOption
 */
data class FilterOptionWrapper(
    val label: String,
    val value: String,
)

/**
 * Result wrapper for fetchMangaUpdate
 */
data class MangaUpdateResult(
    val manga: MangaWrapper?,
    val chapters: List<ChapterWrapper>?,
)

/**
 * Implementation of TachiyomiAdapterBridge that delegates to a real KeiSource.
 * This class must be instantiated in the extension's ClassLoader.
 */
class TachiyomiAdapterBridgeImpl(
    private val source: Any, // The actual KeiSource instance
) : TachiyomiAdapterBridge {

    // Use reflection to call methods on the source since we don't have the actual types at compile time
    // But since this runs in the extension's ClassLoader, we CAN cast to the actual types
    // if we use the tachiyomix interfaces. However, since tachiyomix is compileOnly,
    // we use reflection here too, but now the classes ARE available.

    private val sourceClass = source.javaClass

    override suspend fun getPopularManga(page: Int): MangasPageResult {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("getPopularManga", Int::class.java)
            val result = method.invoke(source, page) as Any // Suspend function returns Continuation
            // Actually call the suspend function properly
            @Suppress("UNCHECKED_CAST")
            val mangasPage = callSuspendFunction(result) as Any // MangasPage
            return@withContext MangasPageResult(
                mangas = convertMangaList(mangasPage),
                hasNextPage = getBooleanProperty(mangasPage, "hasNextPage"),
            )
        }
    }

    override suspend fun getLatestUpdates(page: Int): MangasPageResult {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("getLatestUpdates", Int::class.java)
            val result = method.invoke(source, page) as Any
            @Suppress("UNCHECKED_CAST")
            val mangasPage = callSuspendFunction(result) as Any
            return@withContext MangasPageResult(
                mangas = convertMangaList(mangasPage),
                hasNextPage = getBooleanProperty(mangasPage, "hasNextPage"),
            )
        }
    }

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterListWrapper): MangasPageResult {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("getSearchMangaList", Int::class.java, String::class.java, Any::class.java)
            val filterList = createFilterList(filters)
            val result = method.invoke(source, page, query, filterList) as Any
            @Suppress("UNCHECKED_CAST")
            val mangasPage = callSuspendFunction(result) as Any
            return@withContext MangasPageResult(
                mangas = convertMangaList(mangasPage),
                hasNextPage = getBooleanProperty(mangasPage, "hasNextPage"),
            )
        }
    }

    override suspend fun fetchMangaUpdate(
        manga: MangaWrapper,
        chapters: List<ChapterWrapper>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): MangaUpdateResult {
        return withContext(Dispatchers.IO) {
            val mangaObj = createMangaObject(manga)
            val chaptersObj = chapters.map { createChapterObject(it) }
            val method = sourceClass.getMethod("fetchMangaUpdate", Any::class.java, List::class.java, Boolean::class.java, Boolean::class.java)
            val result = method.invoke(source, mangaObj, chaptersObj, fetchDetails, fetchChapters) as Any
            @Suppress("UNCHECKED_CAST")
            val update = callSuspendFunction(result) as Any
            return@withContext MangaUpdateResult(
                manga = update?.let { convertManga(it) },
                chapters = update?.let { getListProperty(it, "chapters")?.map { convertChapter(it) } },
            )
        }
    }

    override suspend fun getPageList(chapter: ChapterWrapper): List<PageWrapper> {
        return withContext(Dispatchers.IO) {
            val chapterObj = createChapterObject(chapter)
            val method = sourceClass.getMethod("getPageList", Any::class.java)
            val result = method.invoke(source, chapterObj) as Any
            @Suppress("UNCHECKED_CAST")
            val pages = callSuspendFunction(result) as List<*>
            return@withContext pages.map { convertPage(it) }
        }
    }

    override suspend fun getMangaByUrl(url: String): MangaWrapper? {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("getMangaByUrl", okhttp3.HttpUrl::class.java)
            val httpUrl = okhttp3.HttpUrl.parse(url)
            val result = method.invoke(source, httpUrl) as Any?
            val manga = result?.let { callSuspendFunction(it) }
            return@withContext manga?.let { convertManga(it) }
        }
    }

    override suspend fun getMangasByUrl(url: String, page: Int): MangasPageResult {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("getMangasByUrl", okhttp3.HttpUrl::class.java, Int::class.java)
            val httpUrl = okhttp3.HttpUrl.parse(url)
            val result = method.invoke(source, httpUrl, page) as Any
            @Suppress("UNCHECKED_CAST")
            val mangasPage = callSuspendFunction(result) as Any
            return@withContext MangasPageResult(
                mangas = convertMangaList(mangasPage),
                hasNextPage = getBooleanProperty(mangasPage, "hasNextPage"),
            )
        }
    }

    override suspend fun getFilterList(): FilterListWrapper? {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("getFilterList")
            val result = method.invoke(source) as Any?
            return@withContext result?.let { convertFilterList(it) }
        }
    }

    override suspend fun fetchFilterData(): Any? {
        return withContext(Dispatchers.IO) {
            val method = sourceClass.getMethod("fetchFilterData")
            val result = method.invoke(source) as Any?
            return@withContext result?.let { callSuspendFunction(it) }
        }
    }

    override fun getMangaUrl(manga: MangaWrapper): String {
        val mangaObj = createMangaObject(manga)
        val method = sourceClass.getMethod("getMangaUrl", Any::class.java)
        return method.invoke(mangaObj) as String
    }

    override fun getChapterUrl(chapter: ChapterWrapper): String {
        val chapterObj = createChapterObject(chapter)
        val method = sourceClass.getMethod("getChapterUrl", Any::class.java)
        return method.invoke(chapterObj) as String
    }

    override val supportsLatest: Boolean
        get() = getBooleanProperty(source, "supportsLatest") ?: true

    override val supportsFilterFetching: Boolean
        get() = getBooleanProperty(source, "supportsFilterFetching") ?: false

    override fun configureHeaders(builder: okhttp3.Headers.Builder): okhttp3.Headers.Builder {
        val method = sourceClass.getMethod("configureHeaders", okhttp3.Headers.Builder::class.java)
        return method.invoke(source, builder) as okhttp3.Headers.Builder
    }

    override val imageFetch: Int
        get() = getIntProperty(source, "imageFetch") ?: 0

    override val name: String
        get() = getStringProperty(source, "name") ?: ""

    override val lang: String
        get() = getStringProperty(source, "lang") ?: "en"

    override val baseUrl: String
        get() = getStringProperty(source, "baseUrl") ?: ""

    override val id: String
        get() = getStringProperty(source, "id") ?: ""

    // ── Helper methods ────────────────────────────────────────────────────────

    private suspend fun callSuspendFunction(continuation: Any): Any {
        // The suspend function returns a Continuation. We need to resume it.
        // This is a simplified approach - in reality we'd use kotlin.coroutines.intrinsics
        // For now, we assume the tachiyomix stub provides a blocking version or we use runBlocking
        // But since we're in a suspend function already, we can use a CompletableDeferred
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            // Try to call the continuation's resume method
            try {
                val resumeMethod = continuation.javaClass.getMethod("resumeWith", Any::class.java)
                // Wrap our continuation
                resumeMethod.invoke(continuation, kotlinx.coroutines.Result.success(cont))
            } catch (e: Exception) {
                // Fallback: the stub might return the value directly
                cont.resume(continuation)
            }
        }
    }

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

    private fun getIntProperty(obj: Any, name: String): Int? {
        return try {
            val getter = obj.javaClass.getMethod(name)
            getter.invoke(obj) as? Int
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

    private fun getProperty(obj: Any, name: String): Any? {
        return try {
            val getter = obj.javaClass.getMethod(name)
            getter.invoke(obj)
        } catch (e: Exception) {
            null
        }
    }

    private fun convertMangaList(mangasPage: Any): List<MangaWrapper> {
        val mangas = getListProperty(mangasPage, "mangas") ?: emptyList()
        return mangas.map { convertManga(it) }
    }

    private fun convertManga(manga: Any): MangaWrapper {
        return MangaWrapper(
            url = getStringProperty(manga, "url") ?: "",
            title = getStringProperty(manga, "title"),
            name = getStringProperty(manga, "name"),
            imageUrl = getStringProperty(manga, "imageUrl"),
            thumbnailUrl = getStringProperty(manga, "thumbnailUrl"),
            author = getStringProperty(manga, "author"),
            description = getStringProperty(manga, "description"),
            genres = getListProperty(manga, "genres")?.map { it.toString() },
            status = getProperty(manga, "status"),
            rating = getProperty(manga, "rating"),
            lastUpdate = getProperty(manga, "lastUpdate"),
            chapters = getListProperty(manga, "chapters")?.map { convertChapter(it) },
        )
    }

    private fun convertChapter(chapter: Any): ChapterWrapper {
        return ChapterWrapper(
            url = getStringProperty(chapter, "url") ?: "",
            name = getStringProperty(chapter, "name") ?: "",
            chapter = getProperty(chapter, "chapter")?.let { it.toString().toDoubleOrNull() },
            date = getStringProperty(chapter, "date"),
        )
    }

    private fun convertPage(page: Any): PageWrapper {
        return PageWrapper(
            url = getStringProperty(page, "url") ?: "",
            headers = getProperty(page, "headers") as? Map<String, String>,
            imageUrl = getStringProperty(page, "imageUrl"),
        )
    }

    private fun convertFilterList(filterList: Any): FilterListWrapper {
        val filters = getListProperty(filterList, "filters") ?: emptyList()
        return FilterListWrapper(
            filters = filters.map { convertFilter(it) }
        )
    }

    private fun convertFilter(filter: Any): FilterWrapper {
        return FilterWrapper(
            key = getStringProperty(filter, "key") ?: "",
            label = getStringProperty(filter, "label") ?: "",
            type = getStringProperty(filter, "type") ?: "Picker",
            value = getProperty(filter, "value") ?: "",
            options = getListProperty(filter, "options")?.map { convertFilterOption(it) },
        )
    }

    private fun convertFilterOption(option: Any): FilterOptionWrapper {
        return FilterOptionWrapper(
            label = getStringProperty(option, "label") ?: "",
            value = getStringProperty(option, "value") ?: "",
        )
    }

    private fun createMangaObject(manga: MangaWrapper): Any {
        // Create a minimal SManga-like object
        // This is a placeholder - actual implementation would create the proper type
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

    private fun createChapterObject(chapter: ChapterWrapper): Any {
        return mapOf(
            "url" to chapter.url,
            "name" to chapter.name,
            "chapter" to chapter.chapter,
            "date" to chapter.date,
        )
    }

    private fun createFilterList(filters: FilterListWrapper): Any {
        // Create a FilterList-compatible object
        return mapOf(
            "filters" to filters.filters.map { f ->
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
}