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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * Адаптер bookSource (Legado) → SourceInterface.Catalog.
 *
 * Покрытие: search (GET/POST, {{key}}/{{page}}, заголовки, charset),
 * explore/catalog, детали книги, ТОЦ с nextTocUrl-пагинацией,
 * контент с nextContentUrl-пагинацией и replaceRegex, манга (type=2) через
 * список картинок, видео (type=4) через извлечение m3u8/mp4.
 *
 * Ограничения (см. docs/EXTENSION_COMPATIBILITY.md): webJs/WebView-правила,
 * loginUrl/авторизация, payAction не реализованы.
 */
class LegadoSourceAdapter(
    private val bookSource: LegadoBookSource,
    private val networkClient: NetworkClient,
    private val engine: LegadoRuleEngine = LegadoRuleEngine(),
    override val id: String,
) : SourceInterface.Catalog {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Кэш страницы книги (детали + ТОЦ часто запрашиваются подряд). */
    @Volatile private var pageCacheUrl: String = ""
    @Volatile private var pageCacheDoc: Document? = null
    @Volatile private var pageCacheAt: Long = 0

    /**
     * Кодировка контента. Legado не хранит её в bookSource (кроме нестандартного
     * поля charset и searchUrl-конфига) — обновляется из Content-Type при каждом
     * своём запросе, чтобы последующий fetch глав (через source.charset) был верен.
     */
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

    override val referer: String = parseHeaders()[REFERER] ?: "$baseUrl/"

    override val catalogUrl: String
        get() = bookSource.exploreUrl.ifBlank { bookSource.searchUrl }.ifBlank { baseUrl }

    override val language: LanguageCode? = null
    override val languageTag: String? = null
    override val iconUrl: String? = null

    // ── HTTP ─────────────────────────────────────────────────────────────────

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

    private fun updateCharsetFrom(response: OkHttpResponse) {
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

    private fun buildRequest(
        url: String,
        method: String,
        body: String?,
        extraHeaders: Map<String, String>,
    ): Request.Builder {
        val headers = parseHeaders() + extraHeaders
        val builder = if (method.equals("POST", ignoreCase = true) && body != null) {
            if (body.trimStart().startsWith("{")) {
                // JSON-тело (Legado: {"method":"POST","body":"{...}"})
                val mt = "application/json; charset=$charset".toMediaTypeOrNull()
                    ?: "application/json".toMediaTypeOrNull()!!
                postRequest(url).post(okhttp3.RequestBody.create(mt, body))
            } else {
                // form-тело: key=value&key2=value2 (значения уже URL-encoded в шаблоне)
                val form = FormBody.Builder()
                body.split('&').forEach { pair ->
                    val eq = pair.indexOf('=')
                    if (eq > 0) form.add(pair.substring(0, eq), pair.substring(eq + 1))
                    else if (pair.isNotBlank()) form.add(pair, "")
                }
                postRequest(url).post(form.build())
            }
        } else {
            getRequest(url)
        }
        for ((k, v) in headers) builder.header(k, v)
        return builder
    }

    /** Сырой запрос: возвращает Response с уже прочитанным телом? — нет, вызывающий закрывает. */
    private suspend fun rawCall(url: String, method: String, body: String?, extra: Map<String, String> = emptyMap()): OkHttpResponse {
        val builder = buildRequest(url, method, body, extra)
        return networkClient.call(builder, followRedirects = true)
    }

    private suspend fun fetchDocument(url: String, method: String = "GET", body: String? = null): Document {
        rawCall(url, method, body).use { resp ->
            updateCharsetFrom(resp)
            val bytes = resp.body.bytes()
            val html = String(bytes, Charset.forName(charset))
            return Jsoup.parse(html, resp.request.url.toString())
        }
    }

    private suspend fun fetchString(url: String, method: String = "GET", body: String? = null): String {
        rawCall(url, method, body).use { resp ->
            updateCharsetFrom(resp)
            return String(resp.body.bytes(), Charset.forName(charset))
        }
    }

    private suspend fun cachedPage(url: String): Document {
        val now = System.currentTimeMillis()
        val cached = pageCacheDoc
        if (cached != null && pageCacheUrl == url && now - pageCacheAt < 60_000) return cached
        val doc = fetchDocument(url)
        pageCacheUrl = url
        pageCacheDoc = doc
        pageCacheAt = now
        return doc
    }

    private fun invalidatePageCache() {
        pageCacheUrl = ""
        pageCacheDoc = null
    }

    // ── searchUrl ────────────────────────────────────────────────────────────

    private data class SearchRequest(
        val url: String,
        val method: String,
        val body: String?,
        val charsetInConfig: String?,
    )

    /**
     * Разбор searchUrl/exploreUrl: plain URL или `url,{json-config}`.
     * Подстановка {{key}}/{{searchKey}}/{{page}}.
     */
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
        val cfgCharset = config?.get("charset")?.let { (it as? JsonPrimitive)?.contentOrNull }
        val effectiveCharset = cfgCharset ?: charset
        val encodedKey = try {
            URLEncoder.encode(key, effectiveCharset)
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
        return SearchRequest(url, method, body, cfgCharset)
    }

    private fun parseExtraHeaders(configJson: String?): Map<String, String> {
        if (configJson.isNullOrBlank()) return emptyMap()
        return try {
            val obj = json.parseToJsonElement(configJson).jsonObject
            val headers = obj["headers"] as? JsonObject ?: return emptyMap()
            headers.entries.mapNotNull { (k, v) ->
                (v as? JsonPrimitive)?.contentOrNull?.let { k to it }
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── Catalog API ──────────────────────────────────────────────────────────

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> =
        withContext(Dispatchers.IO) {
            try {
                if (bookSource.searchUrl.isBlank()) {
                    return@withContext Success(PagedList(emptyList(), index, true))
                }
                val page = index + 1 // Legado считает страницы с 1
                val req = buildSearchRequest(bookSource.searchUrl, input, page)
                val vars = mutableMapOf("key" to input, "page" to page.toString(), "baseURL" to baseUrl, "source" to baseUrl)
                val docOrJson = fetchPageForRule(req)
                val items = parseSearchResults(docOrJson, vars)
                Success(PagedList(items, index, items.isEmpty()))
            } catch (e: Exception) {
                Timber.e(e, "Legado search failed [${bookSource.bookSourceName}]")
                Error(e.message ?: "Search failed", e)
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.IO) {
            try {
                val explore = bookSource.exploreUrl
                if (explore.isBlank()) {
                    // Нет explore — каталог = поиск с пустым ключом
                    return@withContext getCatalogSearch(index, "")
                }
                val page = index + 1
                // exploreUrl может быть JSON-объектом {name: url,...} — берём первую запись
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
                val items = elements.mapNotNull { el -> parseBookElement(el, rule, vars, searchFallback = false) }
                Success(PagedList(items, index, items.isEmpty()))
            } catch (e: Exception) {
                Timber.e(e, "Legado explore failed [${bookSource.bookSourceName}]")
                Error(e.message ?: "Explore failed", e)
            }
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
        // Формат "name\nurl" через `\n` (Legado поддерживает списки explore)
        if ('\n' in trimmed) {
            val lines = trimmed.lines().filter { it.isNotBlank() }
            // чётные — имена, нечётные — URL (или наоборот); ищем первый URL
            return lines.firstOrNull { it.startsWith("http") } ?: trimmed
        }
        return trimmed
    }

    /** GET/POST страницы: возвращает Document для HTML или строку JSON. */
    private suspend fun fetchPageForRule(req: SearchRequest): Any {
        val extraHeaders = parseExtraHeaders(null)
        rawCall(req.url, req.method, req.body, extraHeaders).use { resp ->
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

    private fun parseSearchResults(content: Any, vars: MutableMap<String, String>): List<BookResult> {
        val rule = bookSource.ruleSearch
        val listRule = rule.bookList
        if (listRule.isBlank()) return emptyList()
        val elements = engine.getElements(content, listRule, vars)
        return elements.mapNotNull { el -> parseBookElement(el, rule, vars, searchFallback = true) }
    }

    private fun parseBookElement(
        el: org.jsoup.nodes.Element,
        rule: Any,
        vars: MutableMap<String, String>,
        searchFallback: Boolean,
    ): BookResult? {
        val (nameRule, authorRule, coverRule, introRule, urlRule, kindRule, lastRule) = when (rule) {
            is LegadoRuleSearch -> Triple(rule.name.ifBlank { rule.bookName }, rule.author, rule.coverUrl)
                .let { (n, a, c) -> Quint(n, a, c, rule.intro, rule.bookUrl, rule.kind, rule.lastChapter.ifBlank { rule.lastUpdate }) }
            is LegadoRuleExplore -> Quint(
                rule.name.ifBlank { rule.bookName }, rule.author, rule.coverUrl,
                rule.intro, rule.bookUrl, rule.kind, rule.lastUpdate,
            )
            else -> return null
        }
        val title = engine.getString(el, nameRule, vars).trim()
        if (title.isEmpty()) return null
        val author = engine.getString(el, authorRule, vars).trim()
        val cover = engine.getString(el, coverRule, vars).trim().let { absolutize(it, baseUrl) }
        val intro = engine.getString(el, introRule, vars).trim()
        val kind = engine.getString(el, kindRule, vars).trim()
        val last = engine.getString(el, lastRule, vars).trim()
        var bookUrl = engine.getString(el, urlRule, vars).trim()
        if (bookUrl.isEmpty()) return null
        bookUrl = absolutize(bookUrl, baseUrl)

        val description = listOfNotNull(
            author.takeIf { it.isNotEmpty() }?.let { "Author: $it" },
            kind.takeIf { it.isNotEmpty() },
            last.takeIf { it.isNotEmpty() }?.let { "Last: $it" },
            intro.takeIf { it.isNotEmpty() },
        ).joinToString("\n")

        return BookResult(
            title = title,
            url = bookUrl,
            coverImageUrl = cover,
            description = description,
            contentType = contentType,
        )
    }

    private data class Quint<A, B, C, D, E, F, G>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G,
    )

    // ── Детали книги ─────────────────────────────────────────────────────────

    private suspend fun bookPage(bookUrl: String): Document = cachedPage(bookUrl)

    override suspend fun getBookTitle(bookUrl: String): Response<String?> = wrapDetail(bookUrl, null) { doc, vars ->
        engine.getString(doc, bookSource.ruleBookInfo.name, vars).trim().ifBlank { null }
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> = wrapDetail(bookUrl, null) { doc, vars ->
        engine.getString(doc, bookSource.ruleBookInfo.intro, vars).trim().ifBlank { null }
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> = wrapDetail(bookUrl, null) { doc, vars ->
        engine.getString(doc, bookSource.ruleBookInfo.coverUrl, vars).trim()
            .ifBlank { null }?.let { absolutize(it, bookUrl) }
    }

    override suspend fun getBookStatus(bookUrl: String): Response<String?> = wrapDetail(bookUrl, null) { doc, vars ->
        engine.getString(doc, bookSource.ruleBookInfo.kind, vars).trim().ifBlank { null }
    }

    override suspend fun getBookLastUpdate(bookUrl: String): Response<String?> = wrapDetail(bookUrl, null) { doc, vars ->
        engine.getString(doc, bookSource.ruleBookInfo.lastChapter.ifBlank { bookSource.ruleBookInfo.lastUpdate }, vars)
            .trim().ifBlank { null }
    }

    override suspend fun getBookGenres(bookUrl: String): Response<List<String>> =
        wrapDetail(bookUrl, emptyList<String>()) { doc, vars ->
            engine.getStrings(doc, bookSource.ruleBookInfo.kind, vars)
                .flatMap { it.split(Regex("[,、/|]")) }
                .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        }

    override suspend fun getBookRating(bookUrl: String): Response<String?> =
        Success(null) // Legado не имеет стандартизированного правила рейтинга

    private suspend fun <T> wrapDetail(
        bookUrl: String,
        default: T,
        block: suspend (Document, MutableMap<String, String>) -> T?,
    ): Response<T> = withContext(Dispatchers.IO) {
        try {
            val rule = bookSource.ruleBookInfo
            if (rule.name.isBlank() && rule.intro.isBlank() && rule.coverUrl.isBlank()) {
                return@withContext Success(default)
            }
            val doc = bookPage(bookUrl)
            val vars = mutableMapOf("baseURL" to baseUrl, "source" to baseUrl, "key" to "", "page" to "1")
            Success(block(doc, vars) ?: default)
        } catch (e: Exception) {
            Timber.e(e, "Legado detail failed [${bookSource.bookSourceName}]")
            Error(e.message ?: "Detail failed", e)
        }
    }

    // ── ТОЦ ──────────────────────────────────────────────────────────────────

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.IO) {
            try {
                val infoRule = bookSource.ruleBookInfo
                val tocRule = bookSource.ruleToc
                if (tocRule.chapterList.isBlank()) {
                    return@withContext Success(emptyList<ChapterResult>())
                }
                val vars = mutableMapOf("baseURL" to baseUrl, "source" to baseUrl, "key" to "", "page" to "1")
                var doc = bookPage(bookUrl)
                // tocUrl: отдельная страница оглавления
                val tocUrlRule = infoRule.tocUrl
                if (tocUrlRule.isNotBlank()) {
                    val tocUrl = engine.getString(doc, tocUrlRule, vars).trim()
                    if (tocUrl.isNotBlank() && absolutize(tocUrl, bookUrl) != bookUrl) {
                        doc = fetchDocument(absolutize(tocUrl, bookUrl))
                    }
                }
                val chapters = mutableListOf<ChapterResult>()
                val seenUrls = mutableSetOf<String>()
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
                            (vol + " " + ch).trim()
                        } else {
                            engine.getString(el, tocRule.chapterName, vars).trim()
                        }
                        val url = engine.getString(el, tocRule.chapterUrl, vars).trim()
                        if (url.isEmpty() || !seenUrls.add(url)) continue
                        val uploaded = if (tocRule.updateTime.isNotBlank()) {
                            parseEpoch(engine.getString(el, tocRule.updateTime, vars))
                        } else null
                        chapters += ChapterResult(
                            title = title.ifBlank { url },
                            url = absolutize(url, doc.location().ifBlank { bookUrl }),
                            uploaded = uploaded,
                        )
                    }
                    // Пагинация nextTocUrl
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

    private fun parseEpoch(raw: String): Long? {
        val digits = raw.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return null
        return when {
            digits.length >= 13 -> digits.take(13).toLongOrNull()
            digits.length >= 10 -> digits.take(10).toLongOrNull()
            else -> null
        }
    }

    // ── Контент главы ────────────────────────────────────────────────────────

    override suspend fun getChapterText(doc: Document): String? =
        withContext(Dispatchers.IO) {
            try {
                val rule = bookSource.ruleContent
                if (rule.content.isBlank()) return@withContext null
                val vars = mutableMapOf(
                    "baseURL" to baseUrl,
                    "source" to baseUrl,
                    "key" to "",
                    "page" to "1",
                )
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

    /** Манга (type=2): страницы-картинки из content-правила. */
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
            // Правило вернуло HTML — вытаскиваем <img src>
            val html = strings.joinToString("\n")
            val imgs = Jsoup.parseBodyFragment(html).select("img[src]")
                .map { el ->
                    val src = el.attr("src").ifBlank { el.attr("data-src") }
                    absolutize(src, doc.location().ifBlank { baseUrl })
                }.filter { it.isNotBlank() }
            imgs.ifEmpty { null }
        } catch (e: Exception) {
            Timber.e(e, "Legado pages failed [${bookSource.bookSourceName}]")
            null
        }
    }

    // ── Видео (type=4) ───────────────────────────────────────────────────────

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
                // Fallback: прямой поиск потоков в HTML
                val html = doc.html()
                candidates += Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
                    .findAll(html).map { it.value }.toList()
                candidates += Regex("""https?://[^\s"'<>]+\.mp4[^\s"'<>]*""")
                    .findAll(html).map { it.value }.toList()
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

    // ── utils ────────────────────────────────────────────────────────────────

    /** Абсолютизация относительных URL по правилам Legado (base = baseUrl или страница). */
    internal fun absolutize(url: String, base: String): String {
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

    companion object {
        private const val REFERER = "Referer"
    }
}

private fun String.toMediaTypeOrNull(): okhttp3.MediaType? =
    okhttp3.MediaType.Companion.toMediaTypeOrNull(this)
