package my.dreamora.extension_compat.lnreader

import my.dreamora.core.LanguageCode
import my.dreamora.core.PagedList
import my.dreamora.core.Response
import my.dreamora.extension_compat.js.BoundedCache
import my.dreamora.extension_compat.js.JsPluginRuntime
import my.dreamora.extension_compat.js.absoluteUrl
import my.dreamora.extension_compat.js.array
import my.dreamora.extension_compat.js.asObjectOrEmpty
import my.dreamora.extension_compat.js.int
import my.dreamora.extension_compat.js.parseEpochSeconds
import my.dreamora.extension_compat.js.str
import my.dreamora.network.ImageRequestHeaders
import my.dreamora.extension_compat.js.stringOrNull
import my.dreamora.scraper.ActiveFilters
import my.dreamora.scraper.LuaFilter
import my.dreamora.scraper.LuaFilterOption
import my.dreamora.scraper.SourceInterface
import my.dreamora.scraper.domain.BookResult
import my.dreamora.scraper.domain.ChapterResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber

/**
 * Adapts an LNReader plugin (a JavaScript module running in [JsPluginRuntime]) to
 * [SourceInterface.FilterableCatalog]. Plugin paths are relative to the site; the app
 * sees absolute URLs, and [pathFor] maps them back when a call needs the path.
 */
class LnReaderSourceAdapter(
    override val id: String,
    override val name: String,
    override val baseUrl: String,
    private val runtime: JsPluginRuntime,
    filters: JsonObject?,
    private val hasSearch: Boolean,
    private val hasPages: Boolean,
    /** The plugin's `imageRequestInit.headers`, sent with every cover it returns. */
    private val imageHeaders: Map<String, String> = emptyMap(),
) : SourceInterface.FilterableCatalog {

    private val filterDefs: List<FilterDef> = parseFilterDefs(filters)
    private val pathByUrl = BoundedCache<String, String>(4096)

    override val nameStrId: Int = 0
    override val contentType: String = ""
    override val catalogUrl: String = baseUrl
    override val language: LanguageCode? = null
    override val iconUrl: String? = null

    suspend fun close() = runtime.close()

    // ── catalog ───────────────────────────────────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> = novelPage(
        index = index,
        operation = "popular",
        method = "popularNovels",
        args = popularArgs(index, filterValues(ActiveFilters())),
    )

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> {
        if (!hasSearch) return Response.Success(PagedList(emptyList(), index, true))
        return novelPage(
            index = index,
            operation = "search",
            method = "searchNovels",
            args = buildJsonArray {
                add(JsonPrimitive(input))
                add(JsonPrimitive(index + 1))
            },
        )
    }

    override suspend fun getFilterList(): Response<List<LuaFilter>> =
        Response.Success(filterDefs.map { it.toLuaFilter() })

    override suspend fun getCatalogFiltered(index: Int, filters: ActiveFilters): Response<PagedList<BookResult>> =
        novelPage(
            index = index,
            operation = "filtered",
            method = "popularNovels",
            args = popularArgs(index, filterValues(filters)),
        )

    // ── book and chapters ─────────────────────────────────────────────────────

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> = try {
        val path = pathFor(bookUrl)
        val novel = fetchDetails(path)
        val chapters = mutableListOf<ChapterResult>()
        chapters += chaptersOf(novel)
        if (hasPages) {
            val total = novel.int("totalPages") ?: 1
            for (page in 2..minOf(total, MAX_CHAPTER_PAGES)) {
                val result = invoke(
                    "parsePage",
                    buildJsonArray {
                        add(JsonPrimitive(path))
                        add(JsonPrimitive(page.toString()))
                    },
                ).asObjectOrEmpty()
                chapters += chaptersOf(result)
            }
        }
        Response.Success(chapters.distinctBy { it.url })
    } catch (e: TimeoutCancellationException) {
        Response.Error("Chapter list timed out", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "LNReader chapters failed [$name]")
        Response.Error(e.message ?: "Chapter list failed", e)
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        details(bookUrl) { it.str("summary") }

    override suspend fun getBookTitle(bookUrl: String): Response<String?> =
        details(bookUrl) { it.str("name") }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        details(bookUrl) { novel -> coverUrl(novel.str("cover")).takeIf { it.isNotEmpty() } }

    override suspend fun getBookGenres(bookUrl: String): Response<List<String>> =
        details(bookUrl) { novel ->
            novel.str("genres").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> =
        details(bookUrl) { it.str("status") }

    override suspend fun getChapterText(doc: Document): String? = try {
        val path = pathFor(doc.location())
        val html = invoke(
            "parseChapter",
            buildJsonArray { add(JsonPrimitive(path)) },
        ).stringOrNull()?.takeIf { it.isNotBlank() }
        if (html != null) rememberChapterImages(html, doc.location())
        html
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "LNReader chapter failed [$name]")
        null
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private suspend fun novelPage(
        index: Int,
        operation: String,
        method: String,
        args: JsonArray,
    ): Response<PagedList<BookResult>> = try {
        val items = itemsOf(invoke(method, args))
        Response.Success(PagedList(items, index, items.isEmpty()))
    } catch (e: TimeoutCancellationException) {
        Response.Error("$operation timed out", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "LNReader $operation failed [$name]")
        Response.Error(e.message ?: "$operation failed", e)
    }

    private suspend fun <T> details(bookUrl: String, map: (JsonObject) -> T): Response<T> = try {
        Response.Success(map(fetchDetails(pathFor(bookUrl))))
    } catch (e: TimeoutCancellationException) {
        Response.Error("Book details timed out", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "LNReader details failed [$name]")
        Response.Error(e.message ?: "Book details failed", e)
    }

    /** The result of `parseNovel(path)`, cached per path for the calls that follow. */
    private suspend fun fetchDetails(path: String): JsonObject {
        return invoke("parseNovel", buildJsonArray { add(JsonPrimitive(path)) }).asObjectOrEmpty()
    }

    private suspend fun invoke(method: String, args: JsonArray): JsonElement {
        val raw = runtime.call("__lnDispatch", method, args.toString()) ?: "null"
        return Json.parseToJsonElement(raw)
    }

    private fun popularArgs(index: Int, filters: JsonElement): JsonArray = buildJsonArray {
        add(JsonPrimitive(index + 1))
        add(
            buildJsonObject {
                put("showLatestNovels", false)
                put("filters", filters)
            },
        )
    }

    private fun itemsOf(element: JsonElement): List<BookResult> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val path = obj.str("path")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            BookResult(
                title = obj.str("name")?.takeIf { it.isNotBlank() } ?: path,
                url = urlFor(path),
                coverImageUrl = coverUrl(obj.str("cover")),
            )
        }
    }

    private fun chaptersOf(novel: JsonObject): List<ChapterResult> {
        val array = novel.array("chapters") ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val path = obj.str("path")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ChapterResult(
                title = obj.str("name")?.takeIf { it.isNotBlank() } ?: path,
                url = urlFor(path),
                uploaded = parseEpochSeconds(obj.str("releaseTime")),
            )
        }
    }

    /**
     * Absolute cover URL, or "" when the plugin gave none. Plugins return covers relative to
     * the site as often as absolute, and a relative URL cannot be loaded. The plugin's image
     * headers are registered for the URL so the image request carries them.
     */
    /**
     * Pictures inside a chapter body are fetched by the reader with the same headers the plugin
     * declared for its images (usually Referer: site). Resolved against the chapter URL, as a
     * browser would resolve them.
     */
    private fun rememberChapterImages(html: String, chapterUrl: String) {
        if (imageHeaders.isEmpty()) return
        Jsoup.parseBodyFragment(html, chapterUrl).select("img[src]").forEach { img ->
            val src = img.attr("src").trim()
            if (src.isNotEmpty() && !src.startsWith("data:")) {
                ImageRequestHeaders.remember(absoluteUrl(chapterUrl, src), imageHeaders)
            }
        }
    }

    private fun coverUrl(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return ""
        val url = absoluteUrl(baseUrl, text)
        ImageRequestHeaders.remember(url, imageHeaders)
        return url
    }

    private fun urlFor(path: String): String {
        val url = absoluteUrl(baseUrl, path)
        pathByUrl.put(url, path)
        return url
    }

    private fun pathFor(url: String): String {
        pathByUrl.get(url)?.let { return it }
        val root = baseUrl.trimEnd('/') + "/"
        return if (url.startsWith(root)) url.removePrefix(root) else url
    }

    /** `ActiveFilters` → the `{ key: { type, value } }` object LNReader plugins read. */
    private fun filterValues(active: ActiveFilters): JsonObject = buildJsonObject {
        for (def in filterDefs) {
            val value: JsonElement = when (def.type) {
                "Picker" -> JsonPrimitive(active.selectValues[def.key] ?: def.defaultText())
                "Checkbox" -> JsonArray(
                    (active.checkboxIncluded[def.key] ?: def.defaultList()).map { JsonPrimitive(it) },
                )
                "XCheckbox" -> buildJsonObject {
                    put(
                        "include",
                        JsonArray((active.triIncluded[def.key] ?: emptyList()).map { JsonPrimitive(it) }),
                    )
                    put(
                        "exclude",
                        JsonArray((active.triExcluded[def.key] ?: emptyList()).map { JsonPrimitive(it) }),
                    )
                }
                "Switch" -> JsonPrimitive(active.switchValues[def.key] ?: def.defaultBool())
                "Text" -> JsonPrimitive(active.textValues[def.key] ?: def.defaultText())
                else -> continue
            }
            putJsonObject(def.key) {
                put("type", def.type)
                put("value", value)
            }
        }
    }

    companion object {
        private const val MAX_CHAPTER_PAGES = 200
    }
}

/** One entry of a plugin's `filters` object: `{ type, label, value, options }`. */
private class FilterDef(
    val key: String,
    val type: String,
    val label: String,
    val value: JsonElement?,
    val options: List<LuaFilterOption>,
) {
    fun defaultText(): String = (value as? JsonPrimitive)?.contentOrNull.orEmpty()

    fun defaultBool(): Boolean = (value as? JsonPrimitive)?.booleanOrNull ?: false

    fun defaultList(): List<String> =
        (value as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()

    fun toLuaFilter(): LuaFilter = when (type) {
        "Picker" -> LuaFilter.Select(key, label, options, defaultText())
        "Checkbox" -> LuaFilter.CheckboxGroup(key, label, options, multiselect = true)
        "XCheckbox" -> LuaFilter.TriState(key, label, options)
        "Switch" -> LuaFilter.Switch(key, label, defaultBool())
        else -> LuaFilter.TextInput(key, label, defaultText())
    }
}

private fun parseFilterDefs(filters: JsonObject?): List<FilterDef> {
    if (filters == null) return emptyList()
    return filters.entries.mapNotNull { (key, element) ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val type = obj.str("type") ?: return@mapNotNull null
        val options = obj.array("options").orEmpty().mapNotNull { option ->
            val o = option as? JsonObject ?: return@mapNotNull null
            val value = o.str("value") ?: return@mapNotNull null
            LuaFilterOption(value = value, label = o.str("label") ?: value)
        }
        FilterDef(
            key = key,
            type = type,
            label = obj.str("label") ?: key,
            value = obj["value"],
            options = options,
        )
    }
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
