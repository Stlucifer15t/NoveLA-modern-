package my.dreamora.extension_compat.mangayomi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import my.dreamora.core.LanguageCode
import my.dreamora.core.PagedList
import my.dreamora.core.Response
import my.dreamora.extension_compat.js.BoundedCache
import my.dreamora.extension_compat.js.JsPluginRuntime
import my.dreamora.extension_compat.js.absoluteUrl
import my.dreamora.extension_compat.js.array
import my.dreamora.extension_compat.js.asObjectOrEmpty
import my.dreamora.extension_compat.js.bool
import my.dreamora.extension_compat.js.int
import my.dreamora.extension_compat.js.obj
import my.dreamora.extension_compat.js.parseEpochSeconds
import my.dreamora.extension_compat.js.str
import my.dreamora.extension_compat.js.stringOrNull
import my.dreamora.network.ImageRequestHeaders
import my.dreamora.scraper.ActiveFilters
import my.dreamora.scraper.LuaFilter
import my.dreamora.scraper.LuaFilterOption
import my.dreamora.scraper.SourceInterface
import my.dreamora.scraper.domain.BookResult
import my.dreamora.scraper.domain.ChapterResult
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import timber.log.Timber

/**
 * Adapts a Mangayomi JavaScript source (`class DefaultExtension extends MProvider`)
 * to [SourceInterface.FilterableCatalog]: getPopular / getLatestUpdates / search,
 * getDetail, getPageList (manga pages) and getHtmlContent (novel text).
 *
 * Filters follow Mangayomi's model. The app keeps the user's state per filter index,
 * and the host writes it back into the source's own filter objects before `search`.
 */
class MangayomiSourceAdapter(
    override val id: String,
    override val name: String,
    override val baseUrl: String,
    override val contentType: String,
    private val runtime: JsPluginRuntime,
) : SourceInterface.FilterableCatalog {

    private val detailByUrl = BoundedCache<String, JsonObject>(64)
    private val titleByUrl = BoundedCache<String, String>(2048)

    /** The source's `getHeaders(url)` per image host: most sources return the same headers for a host. */
    private val imageHeadersByHost = BoundedCache<String, Map<String, String>>(32)

    @Volatile
    private var cachedTemplate: List<JsonObject>? = null

    override val nameStrId: Int = 0
    override val catalogUrl: String = baseUrl
    override val language: LanguageCode? = null
    override val iconUrl: String? = null

    suspend fun close() = runtime.close()

    // ── catalog ───────────────────────────────────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        browse(index, "getPopular", buildJsonArray { add(JsonPrimitive(index + 1)) })

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> =
        browse(index, "search", searchArgs(input, index, JsonArray(emptyList())))

    override suspend fun getFilterList(): Response<List<LuaFilter>> = try {
        Response.Success(template().mapIndexedNotNull { i, filter -> filter.toLuaFilter(i) })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Mangayomi filters failed [$name]")
        Response.Error(e.message ?: "Filters failed", e)
    }

    override suspend fun getCatalogFiltered(index: Int, filters: ActiveFilters): Response<PagedList<BookResult>> =
        browse(index, "search", searchArgs("", index, overridesOf(filters)))

    // ── book, chapters and content ────────────────────────────────────────────

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> = try {
        val detail = detailOf(bookUrl)
        val chapters = detail.array("chapters").orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val url = obj.str("url")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val absolute = absoluteUrl(baseUrl, url)
            val title = obj.str("name")?.takeIf { it.isNotBlank() } ?: url
            titleByUrl.put(absolute, title)
            ChapterResult(
                title = title,
                url = absolute,
                uploaded = parseEpochSeconds(obj.str("dateUpload")),
            )
        }
        Response.Success(chapters)
    } catch (e: TimeoutCancellationException) {
        Response.Error("Chapter list timed out", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Mangayomi chapters failed [$name]")
        Response.Error(e.message ?: "Chapter list failed", e)
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        details(bookUrl) { it.str("description") }

    override suspend fun getBookTitle(bookUrl: String): Response<String?> =
        details(bookUrl) { it.str("name") }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        details(bookUrl) { detail -> imageUrlFor(detail.str("imageUrl")).takeIf { it.isNotEmpty() } }

    override suspend fun getBookGenres(bookUrl: String): Response<List<String>> =
        details(bookUrl) { detail ->
            detail.array("genre").orEmpty().mapNotNull { it.stringOrNull()?.takeIf { g -> g.isNotBlank() } }
        }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> =
        details(bookUrl) { detail -> statusName(detail.int("status")) }

    /** Manga: the page image URLs. Novels return null so the app takes the text path. */
    override suspend fun getChapterPages(doc: Document): List<String>? {
        if (contentType != "manga") return null
        return try {
            val pages = invoke(
                "getPageList",
                buildJsonArray { add(JsonPrimitive(doc.location())) },
            ) as? JsonArray ?: return null
            pages.mapNotNull { page ->
                val url = page.stringOrNull() ?: (page as? JsonObject)?.str("url")
                url?.takeIf { it.isNotBlank() }?.let { imageUrlFor(it).takeIf { u -> u.isNotEmpty() } }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Mangayomi pages failed [$name]")
            null
        }
    }

    override suspend fun getChapterText(doc: Document): String? {
        if (contentType == "manga") return null
        return try {
            val url = doc.location()
            val title = titleByUrl.get(url).orEmpty()
            invoke(
                "getHtmlContent",
                buildJsonArray {
                    add(JsonPrimitive(title))
                    add(JsonPrimitive(url))
                },
            ).stringOrNull()?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Mangayomi content failed [$name]")
            null
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private suspend fun browse(index: Int, method: String, args: JsonArray): Response<PagedList<BookResult>> = try {
        val result = invoke(method, args).asObjectOrEmpty()
        val items = result.array("list").orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val link = obj.str("link")?.takeIf { it.isNotBlank() }
                ?: obj.str("url")?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            BookResult(
                title = obj.str("name")?.takeIf { it.isNotBlank() } ?: link,
                url = absoluteUrl(baseUrl, link),
                coverImageUrl = imageUrlFor(obj.str("imageUrl")),
            )
        }
        val hasNext = result.bool("hasNextPage") ?: false
        Response.Success(PagedList(items, index, !hasNext || items.isEmpty()))
    } catch (e: TimeoutCancellationException) {
        Response.Error("$method timed out", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Mangayomi $method failed [$name]")
        Response.Error(e.message ?: "$method failed", e)
    }

    private suspend fun <T> details(bookUrl: String, map: suspend (JsonObject) -> T): Response<T> = try {
        Response.Success(map(detailOf(bookUrl)))
    } catch (e: TimeoutCancellationException) {
        Response.Error("Book details timed out", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Mangayomi details failed [$name]")
        Response.Error(e.message ?: "Book details failed", e)
    }

    /**
     * Absolute URL of an image the source named (cover, page), or "" when there is none.
     * The source's own headers for the image are registered so the request carries them.
     */
    private suspend fun imageUrlFor(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return ""
        val url = absoluteUrl(baseUrl, text)
        ImageRequestHeaders.remember(url, imageHeadersFor(url))
        return url
    }

    private suspend fun imageHeadersFor(url: String): Map<String, String> {
        val host = url.toHttpUrlOrNull()?.host ?: return emptyMap()
        imageHeadersByHost.get(host)?.let { return it }
        val headers = try {
            val declared = invoke("getHeaders", buildJsonArray { add(JsonPrimitive(url)) }) as? JsonObject
            declared?.entries
                ?.mapNotNull { (name, value) -> value.stringOrNull()?.let { name to it } }
                ?.toMap()
                ?: emptyMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Mangayomi getHeaders failed [$name]")
            emptyMap()
        }
        imageHeadersByHost.put(host, headers)
        return headers
    }

    private suspend fun detailOf(bookUrl: String): JsonObject {
        detailByUrl.get(bookUrl)?.let { return it }
        val detail = invoke("getDetail", buildJsonArray { add(JsonPrimitive(bookUrl)) }).asObjectOrEmpty()
        detailByUrl.put(bookUrl, detail)
        return detail
    }

    private suspend fun invoke(method: String, args: JsonArray): JsonElement {
        val raw = runtime.call("__mgDispatch", method, args.toString()) ?: "null"
        return Json.parseToJsonElement(raw)
    }

    private suspend fun template(): List<JsonObject> {
        cachedTemplate?.let { return it }
        val raw = runtime.callNoArgs("__mgFilters()")
            ?: throw IllegalStateException("the source returned no filter list")
        val list = (Json.parseToJsonElement(raw) as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
        cachedTemplate = list
        return list
    }

    private fun searchArgs(query: String, index: Int, overrides: JsonArray): JsonArray = buildJsonArray {
        add(JsonPrimitive(query))
        add(JsonPrimitive(index + 1))
        add(overrides)
    }

    /**
     * The app's filter state as one entry per template filter, in template order. The
     * host applies each entry to the source's own filter object (see mangayomi-host.js).
     */
    private suspend fun overridesOf(active: ActiveFilters): JsonArray {
        val filters = template()
        return buildJsonArray {
            filters.forEachIndexed { index, filter ->
                val key = index.toString()
                val value: JsonElement? = when (filter.str("type_name")) {
                    "SelectFilter" -> active.selectValues[key]?.toIntOrNull()?.let { JsonPrimitive(it) }
                    "CheckBox" -> active.switchValues[key]?.let { JsonPrimitive(it) }
                    "GroupFilter" -> active.checkboxIncluded[key]?.let { selected ->
                        JsonArray(selected.map { JsonPrimitive(it) })
                    }
                    "TextFilter" -> active.textValues[key]?.let { JsonPrimitive(it) }
                    "SortFilter" -> active.sortValues[key]?.let { option ->
                        val defaultAscending = (filter.obj("state"))?.bool("ascending") ?: false
                        buildJsonObject {
                            put("index", option.toIntOrNull() ?: 0)
                            put("ascending", active.sortAscending[key] ?: defaultAscending)
                        }
                    }
                    else -> null
                }
                add(value ?: JsonNull)
            }
        }
    }

    private fun statusName(code: Int?): String = when (code) {
        0 -> "Ongoing"
        1 -> "Completed"
        2 -> "On Hiatus"
        3 -> "Cancelled"
        4 -> "Publishing Finished"
        else -> "Unknown"
    }
}

/** One Mangayomi filter object as a [LuaFilter]. Keys are template indexes, as in overridesOf. */
private fun JsonObject.toLuaFilter(index: Int): LuaFilter? {
    val key = index.toString()
    val label = str("name")?.takeIf { it.isNotBlank() } ?: key
    val state = this["state"]
    return when (str("type_name")) {
        "SelectFilter" -> LuaFilter.Select(
            key = key,
            label = label,
            options = optionsOf(this),
            defaultValue = ((state as? JsonPrimitive)?.intOrNull ?: 0).toString(),
        )
        "CheckBox" -> LuaFilter.Switch(
            key = key,
            label = label,
            defaultValue = (state as? JsonPrimitive)?.booleanOrNull ?: false,
        )
        "GroupFilter" -> LuaFilter.CheckboxGroup(
            key = key,
            label = label,
            options = (state as? JsonArray).orEmpty().mapNotNull { child ->
                val box = child as? JsonObject ?: return@mapNotNull null
                val value = box.str("value") ?: box.str("name") ?: return@mapNotNull null
                LuaFilterOption(value = value, label = box.str("name") ?: value)
            },
            multiselect = true,
        )
        "TextFilter" -> LuaFilter.TextInput(
            key = key,
            label = label,
            defaultValue = (state as? JsonPrimitive)?.contentOrNull.orEmpty(),
        )
        "SortFilter" -> {
            val sortState = state as? JsonObject
            LuaFilter.Sort(
                key = key,
                label = label,
                options = optionsOf(this),
                defaultValue = (sortState?.int("index") ?: 0).toString(),
                defaultAscending = sortState?.bool("ascending") ?: false,
            )
        }
        else -> null
    }
}

/** Options of a select or sort filter. The option value is its position, which is what the host expects. */
private fun optionsOf(filter: JsonObject): List<LuaFilterOption> =
    filter.array("values").orEmpty().mapIndexed { position, option ->
        LuaFilterOption(
            value = position.toString(),
            label = (option as? JsonObject)?.str("name") ?: position.toString(),
        )
    }

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
