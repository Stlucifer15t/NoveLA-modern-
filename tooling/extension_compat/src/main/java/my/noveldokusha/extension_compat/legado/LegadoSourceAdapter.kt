package my.noveldokusha.extension_compat.legado

import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.core.Response.Error
import my.noveldokusha.core.Response.Success
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.getRequest
import my.noveldokusha.network.postRequest
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import my.noveldokusha.scraper.domain.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.nio.charset.Charset
import java.net.URLEncoder

/**
 * Адаптер bookSource из экосистемы Legado под [SourceInterface.Catalog] NoveLA.
 *
 * Сетевые запросы идут через [NetworkClient] — Cloudflare-интерцепторы и
 * cookie-jar применяются так же, как для собственных Lua-источников.
 */
class LegadoSourceAdapter(
    private val bookSource: LegadoBookSource,
    private val networkClient: NetworkClient,
    private val engine: LegadoRuleEngine = LegadoRuleEngine(),
    override val id: String,
) : SourceInterface.Catalog {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    override var charset: String = bookSource.charset.ifBlank { "UTF-8" }
        private set

    override val name: String = bookSource.bookSourceName.ifBlank { bookSource.bookSourceUrl }
    override val nameStrId: Int = 0
    override val baseUrl: String = bookSource.bookSourceUrl.trimEnd('/')
    override val contentType: String = when (bookSource.bookSourceType) {
        2 -> "manga"
        4 -> "video"
        else -> ""
    }
    override val referer: String = parseHeaders()["Referer"] ?: "$baseUrl/"
    override val catalogUrl: String = bookSource.exploreUrl.ifBlank { bookSource.searchUrl }.ifBlank { baseUrl }
    override val language: LanguageCode? = null
    override val iconUrl: String? = null

    // ── Каталог / поиск ──────────────────────────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.IO) {
            try {
                val explore = bookSource.exploreUrl
                if (explore.isBlank()) {
                    return@withContext getCatalogSearch(index, "")
                }
                val page = index + 1
                val template = parseExploreTemplate(explore)
                val req = buildSearchRequest(template, "", page)
                val vars = mutableMapOf("key" to "", "page" to page.toString(), "baseURL" to baseUrl, "source" to baseUrl)
                val content = fetchPageForRule(req)
                val rule = bookSource.ruleExplore
                val listRule = rule.bookList.ifBlank { bookSource.ruleSearch.bookList }
                if (listRule.isBlank()) {
                    return@withContext Success(PagedList(emptyList(), index, true))
                }
                val elements = engine.getElements(content, listRule, vars)
                val items = elements.mapNotNull { el -> parseExploreElement(el, rule, vars) }
                Success(PagedList(items, index, items.isEmpty()))
            } catch (e: Exception) {
                Timber.e(e, "Legado explore failed [${bookSource.bookSourceName}]")
                Error(e.message ?: "Explore failed", e)
            }
        }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> =
        withContext(Dispatchers.IO) {
            try {
                if (bookSource.searchUrl.isBlank()) {
                    return@withContext Success(PagedList(emptyList(), index, true))
                }
                val page = index + 1
                val req = buildSearchRequest(bookSource.searchUrl, input, page)
                val vars = mutableMapOf("key" to input, "page" to page.toString(), "baseURL" to baseUrl, "source" to baseUrl)
                val content = fetchPageForRule(req)
                val items = parseSearchItems(content, bookSource.ruleSearch, vars)
                Success(PagedList(items, index, items.isEmpty()))
            } catch (e: Exception) {
                Timber.e(e, "Legado search failed [${bookSource.bookSourceName}]")
                Error(e.message ?: "Search failed", e)
            }
        }

    // ── ОГЛ / главы / содержимое ────────────────────────────────────────────

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.IO) {
            try {
                val tocRule = bookSource.ruleToc
                if (tocRule.chapterList.isBlank()) {
                    return@withContext Success(emptyList())
                }
                val vars = mutableMapOf("baseURL" to baseUrl, "source" to baseUrl, "key" to "", "page" to "1")
                var doc = fetchDocument(bookUrl)
                val tocUrlRule = bookSource.ruleBookInfo.tocUrl
                if (tocUrlRule.isNotBlank()) {
                    val tocUrl = engine.getString(doc, tocUrlRule, vars).trim()
                    if (tocUrl.isNotBlank()) {
                        val abs = absolutize(tocUrl, bookUrl)
                        if (abs != bookUrl) doc = fetchDocument(abs)
                    }
                }
                val chapters = mutableListOf<ChapterResult>()
                val seen = mutableSetOf<String>()
                var nextUrl: String? = null
                var hops = 0
                do {
                    val listEls = engine.getElements(doc, tocRule.chapterList, vars)
                    for (el in listEls) {
                        val isVolume = tocRule.isVolume.isNotBlank() &&
                            engine.getString(el, tocRule.isVolume, vars).trim().toBoolean()
                        if (isVolume) continue
                        val title = if (tocRule.volumeName.isNotBlank()) {
                            val vol = engine.getString(el, tocRule.volumeName, vars).trim()
                            val ch = engine.getString(el, tocRule.chapterName, vars).trim()
                            "$vol $ch".trim()
                        } else {
                            engine.getString(el, tocRule.chapterName, vars).trim()
                        }
                        val url = engine.getString(el, tocRule.chapterUrl, vars).trim()
                        if (url.isEmpty() || !seen.add(url)) continue
                        val uploaded = if (tocRule.updateTime.isNotBlank()) {
                            parseEpoch(engine.getString(el, tocRule.updateTime, vars))
                        } else null
                        chapters += ChapterResult(
                            title = title.ifBlank { url },
                            url = absolutize(url, doc.location().ifBlank { bookUrl }),
                            uploaded = uploaded,
                        )
                    }
                    nextUrl = if (tocRule.nextTocUrl.isNotBlank() && hops < 50) {
                        engine.getString(doc, tocRule.nextTocUrl, vars).trim()
                            .takeIf { it.isNotBlank() && it != doc.location() }
                            ?.let { absolutize(it, doc.location().ifBlank { bookUrl }) }
                    } else null
                    if (nextUrl != null) {
                        doc = fetchDocument(nextUrl!!)
                        hops++
                    }
                } while (nextUrl != null && hops < 50)
                Success(chapters)
            } catch (e: Exception) {
                Timber.e(e, "Legado TOC failed [${bookSource.bookSourceName}]")
                Error(e.message ?: "TOC failed", e)
            }
        }

    override suspend fun getChapterText(doc: Document): String? =
        withContext(Dispatchers.IO) {
            try {
                val rule = bookSource.ruleContent
                if (rule.content.isBlank()) return@withContext null
                val vars = mutableMapOf("baseURL" to baseUrl, "source" to baseUrl, "key" to "", "page" to "1")
                var currentDoc = doc
                val parts = mutableListOf<String>()
                var hops = 0
                while (hops < 30) {
                    val content = engine.getString(currentDoc, rule.content, vars)
                    if (content.isNotBlank()) parts += content
                    val next = if (rule.nextContentUrl.isNotBlank()) {
                        engine.getString(currentDoc, rule.nextContentUrl, vars).trim()
                            .takeIf { it.isNotBlank() }
                            ?.let { absolutize(it, currentDoc.location().ifBlank { baseUrl }) }
                    } else null
                    if (next == null || hops >= 29) break
                    currentDoc = fetchDocument(next)
                    hops++
                }
                var text = parts.joinToString("\n")
                if (rule.replaceRegex.isNotBlank()) {
                    text = try {
                        Regex(rule.replaceRegex).replace(text, "")
                    } catch (e: Exception) {
                        text
                    }
                }
                text.ifBlank { null }
            } catch (e: Exception) {
                Timber.e(e, "Legado content failed [${bookSource.bookSourceName}]")
                null
            }
        }

    override suspend fun getChapterPages(doc: Document): List<String>? {
        if (bookSource.bookSourceType != 2) return null
        return try {
            val rule = bookSource.ruleContent
            if (rule.content.isBlank()) return null
            val vars = mutableMapOf("baseURL" to baseUrl, "source" to baseUrl, "key" to "", "page" to "1")
            val strings = engine.getStrings(doc, rule.content, vars)
            val urlLike = strings.filter {
                it.startsWith("http") || it.startsWith("//") || it.startsWith("/")
            }.map { absolutize(it, doc.location().ifBlank { baseUrl }) }
            if (urlLike.isNotEmpty()) return urlLike
            val html = strings.joinToString("\n")
            val imgs = Jsoup.parseBodyFragment(html).select("img[src]")
                .map { el ->
                    val src = el.attr("src").ifBlank { el.attr("data-src") }
                    absolutize(src, doc.location().ifBlank { baseUrl })
                }
                .filter { it.isNotBlank() }
            imgs.ifEmpty { null }
        } catch (e: Exception) {
            Timber.e(e, "Legado pages failed [${bookSource.bookSourceName}]")
            null
        }
    }

    override suspend fun getVideoList(episodeUrl: String): Response<List<VideoSource>>? {
        if (bookSource.bookSourceType != 4) return null
        return withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument(episodeUrl)
                val rule = bookSource.ruleContent
                val vars = mutableMapOf("baseURL" to baseUrl, "source" to baseUrl, "key" to "", "page" to "1")
                val candidates = mutableListOf<String>()
                if (rule.content.isNotBlank()) {
                    candidates += engine.getStrings(doc, rule.content, vars)
                }
                val html = doc.html()
                candidates += Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""").findAll(html).map { it.value }.toList()
                candidates += Regex("""https?://[^\s"'<>]+\.mp4[^\s"'<>]*""").findAll(html).map { it.value }.toList()
                val videos = candidates
                    .map { it.trim().trim('"', '\'') }
                    .filter { it.startsWith("http") && (it.contains(".m3u8") || it.contains(".mp4")) }
                    .distinct()
                    .map { url ->
                        VideoSource(
                            url = url,
                            quality = if (url.contains("m3u8")) "HLS" else "MP4",
                            headers = mapOf("Referer" to episodeUrl),
                        )
                    }
                Success(videos)
            } catch (e: Exception) {
                Timber.e(e, "Legado video failed [${bookSource.bookSourceName}]")
                Error(e.message ?: "Video failed", e)
            }
        }
    }

    // ── Вспомогательное ──────────────────────────────────────────────────────

    private data class SearchRequest(
        val url: String,
        val method: String,
        val body: String?,
    )

    private fun buildSearchRequest(template: String, key: String, page: Int): SearchRequest {
        var urlPart = template
        var config: JsonObject? = null
        val comma = template.indexOf(',')
        if (comma > 0 && template.indexOf('{', comma) == comma + 1) {
            urlPart = template.substring(0, comma)
            config = try {
                json.parseToJsonElement(template.substring(comma + 1)).jsonObject
            } catch (e: Exception) {
                null
            }
        }
        val encodedKey = try {
            URLEncoder.encode(key, charset)
        } catch (e: Exception) {
            URLEncoder.encode(key, "UTF-8")
        }
        fun subst(s: String): String = s
            .replace("{{key}}", encodedKey)
            .replace("{{searchKey}}", encodedKey)
            .replace("{{page}}", page.toString())
        val url = subst(urlPart)
        val method = config?.get("method")?.let { (it as? JsonPrimitive)?.contentOrNull } ?: "GET"
        val bodyTemplate = config?.get("body")?.let { (it as? JsonPrimitive)?.contentOrNull }
        val body = bodyTemplate?.let { subst(it) }
        return SearchRequest(url, method, body)
    }

    private suspend fun fetchPageForRule(req: SearchRequest): Any {
        val builder = if (req.method.equals("POST", ignoreCase = true) && req.body != null) {
            val mt = "application/x-www-form-urlencoded; charset=$charset".toMediaTypeOrNull()
            postRequest(req.url).post(req.body.toRequestBody(mt))
        } else {
            getRequest(req.url)
        }
        for ((k, v) in parseHeaders()) builder.header(k, v)
        networkClient.call(builder, followRedirects = true).use { resp ->
            updateCharsetFrom(resp)
            val bytes = resp.body.bytes()
            val text = String(bytes, Charset.forName(charset))
            val ct = resp.header("Content-Type") ?: ""
            val url = resp.request.url.toString()
            return if (ct.contains("json", ignoreCase = true) || text.trimStart().startsWith("{") || text.trimStart().startsWith("[")) {
                text
            } else {
                Jsoup.parse(text, url)
            }
        }
    }

    private suspend fun fetchDocument(url: String, method: String = "GET", body: String? = null): Document {
        val builder = if (method.equals("POST", ignoreCase = true) && body != null) {
            val mt = "application/x-www-form-urlencoded; charset=$charset".toMediaTypeOrNull()
            postRequest(url).post(body.toRequestBody(mt))
        } else {
            getRequest(url)
        }
        for ((k, v) in parseHeaders()) builder.header(k, v)
        networkClient.call(builder, followRedirects = true).use { resp ->
            updateCharsetFrom(resp)
            val bytes = resp.body.bytes()
            val html = String(bytes, Charset.forName(charset))
            return Jsoup.parse(html, resp.request.url.toString())
        }
    }

    private fun parseSearchItems(content: Any, rule: LegadoRuleSearch, vars: MutableMap<String, String>): List<BookResult> {
        if (rule.bookList.isBlank()) return emptyList()
        val elements = engine.getElements(content, rule.bookList, vars)
        return elements.mapNotNull { el ->
            parseSearchElement(el, rule, vars)
        }
    }

    private fun parseSearchElement(el: Element, rule: LegadoRuleSearch, vars: MutableMap<String, String>): BookResult? {
        val title = engine.getString(el, rule.bookName.ifBlank { rule.name }, vars).trim()
        if (title.isEmpty()) return null
        val url = engine.getString(el, rule.bookUrl, vars).trim()
        if (url.isEmpty()) return null
        val cover = engine.getString(el, rule.coverUrl, vars).trim()
        val author = engine.getString(el, rule.author, vars).trim()
        val intro = engine.getString(el, rule.intro, vars).trim()
        val kind = engine.getString(el, rule.kind, vars).trim()
        val last = engine.getString(el, rule.lastChapter.ifBlank { rule.lastUpdate }, vars).trim()
        val description = listOfNotNull(
            author.takeIf { it.isNotEmpty() }?.let { "Author: $it" },
            kind.takeIf { it.isNotEmpty() },
            last.takeIf { it.isNotEmpty() }?.let { "Last: $it" },
            intro.takeIf { it.isNotEmpty() },
        ).joinToString("\n")
        return BookResult(
            title = title,
            url = absolutize(url, baseUrl),
            coverImageUrl = absolutize(cover, baseUrl),
            description = description,
            contentType = contentType,
        )
    }

    private fun parseExploreElement(el: Element, rule: LegadoRuleExplore, vars: MutableMap<String, String>): BookResult? {
        val title = engine.getString(el, rule.bookName.ifBlank { rule.name }, vars).trim()
        if (title.isEmpty()) return null
        val url = engine.getString(el, rule.bookUrl, vars).trim()
        if (url.isEmpty()) return null
        val cover = engine.getString(el, rule.coverUrl, vars).trim()
        val author = engine.getString(el, rule.author, vars).trim()
        val intro = engine.getString(el, rule.intro, vars).trim()
        val kind = engine.getString(el, rule.kind, vars).trim()
        val last = engine.getString(el, rule.lastUpdate, vars).trim()
        val description = listOfNotNull(
            author.takeIf { it.isNotEmpty() }?.let { "Author: $it" },
            kind.takeIf { it.isNotEmpty() },
            last.takeIf { it.isNotEmpty() }?.let { "Last: $it" },
            intro.takeIf { it.isNotEmpty() },
        ).joinToString("\n")
        return BookResult(
            title = title,
            url = absolutize(url, baseUrl),
            coverImageUrl = absolutize(cover, baseUrl),
            description = description,
            contentType = contentType,
        )
    }

    private fun parseExploreTemplate(explore: String): String {
        val trimmed = explore.trim()
        if (trimmed.startsWith("{")) {
            return try {
                val obj = json.parseToJsonElement(trimmed).jsonObject
                obj.entries.firstOrNull { (it.value as? JsonPrimitive)?.contentOrNull?.isNotBlank() == true }
                    ?.let { (it.value as JsonPrimitive).contentOrNull } ?: trimmed
            } catch (e: Exception) {
                trimmed
            }
        }
        if ('\n' in trimmed) {
            val lines = trimmed.lines().filter { it.isNotBlank() }
            return lines.firstOrNull { it.startsWith("http") } ?: trimmed
        }
        return trimmed
    }

    private fun parseHeaders(): Map<String, String> {
        val raw = bookSource.header
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj.entries.mapNotNull { (k, v) ->
                (v as? JsonPrimitive)?.contentOrNull?.let { k to it }
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun updateCharsetFrom(response: okhttp3.Response) {
        val ct = response.header("Content-Type") ?: return
        val m = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(ct) ?: return
        val cs = m.groupValues[1].lowercase()
        if (cs != charset.lowercase()) {
            try {
                Charset.forName(cs)
                charset = cs
            } catch (_: Exception) {
            }
        }
    }

    private fun parseEpoch(raw: String): Long? {
        val digits = raw.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return null
        return when {
            digits.length >= 13 -> digits.take(13).toLongOrNull()
            digits.length >= 10 -> digits.take(10).toLongOrNull()
            else -> null
        }
    }

    private fun absolutize(url: String, base: String): String {
        val u = url.trim()
        if (u.isEmpty()) return u
        if (u.startsWith("http://") || u.startsWith("https://")) return u
        val b = base.ifBlank { baseUrl }
        return try {
            java.net.URI(b).resolve(u).toString()
        } catch (e: Exception) {
            when {
                u.startsWith("//") -> "https:$u"
                u.startsWith("/") -> baseUrl.trimEnd('/') + u
                else -> baseUrl.trimEnd('/') + "/" + u
            }
        }
    }
}
