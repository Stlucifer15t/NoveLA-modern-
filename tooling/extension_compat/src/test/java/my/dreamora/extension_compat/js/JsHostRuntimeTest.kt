package my.dreamora.extension_compat.js

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import my.dreamora.core.Response
import my.dreamora.extension_compat.lnreader.LnReaderSourceAdapter
import my.dreamora.extension_compat.mangayomi.MangayomiSourceAdapter
import my.dreamora.network.ImageRequestHeaders
import my.dreamora.scraper.ActiveFilters
import my.dreamora.scraper.LuaFilter
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Runs the real JS host (bundle + host-common + ecosystem host) in QuickJS, the same
 * path the app takes, with a canned HTTP layer. Covers the adapter mapping: catalog,
 * filters, book details, chapter lists and chapter text.
 */
class JsHostRuntimeTest {

    private val scripts = JsHostScripts.load { path -> File("src/main/assets/$path").readText() }

    /** Serves [body] for every request and records the URLs asked for. */
    private class CannedHttp(var body: String = "") : JsHttp {
        val urls = mutableListOf<String>()
        val requests = mutableListOf<JsHttpRequest>()

        override suspend fun execute(request: JsHttpRequest): JsHttpResponse {
            urls += request.url
            requests += request
            return JsHttpResponse(
                status = 200,
                url = request.url,
                headers = mapOf("content-type" to "text/html; charset=utf-8"),
                body = body.toByteArray(Charsets.UTF_8),
            )
        }
    }

    private val lnPlugin = """
        const cheerio = require('cheerio');
        const { fetchText, fetchApi } = require('@libs/fetch');
        const { FilterTypes } = require('@libs/filterInputs');
        const { NovelStatus } = require('@libs/novelStatus');
        const dayjs = require('dayjs');
        class Sample {
          constructor() {
            this.id = 'sample'; this.name = 'Sample Site'; this.site = 'https://sample.test'; this.version = '1.0.0';
            this.imageRequestInit = { headers: { Referer: this.site } };
            this.filters = {
              genre: { type: FilterTypes.Picker, label: 'Genre', value: 'all',
                       options: [{ label: 'All', value: 'all' }, { label: 'Fantasy', value: 'fantasy' }] },
            };
          }
          async popularNovels(page, { filters }) {
            const html = await fetchText('https://sample.test/list?p=' + page + '&g=' + filters.genre.value);
            const $ = cheerio.load(html);
            return $('.novel').map((i, el) => ({ name: $(el).find('a').text(), path: $(el).find('a').attr('href'), cover: $(el).find('img').attr('src') })).get();
          }
          async parseNovel(path) {
            const $ = cheerio.load(await fetchText('https://sample.test' + path));
            return { name: $('h1').text(), summary: $('.sum').text(), author: 'A', genres: 'Fantasy, Action', status: NovelStatus.Ongoing,
              chapters: $('.ch').map((i, el) => ({ name: $(el).text(), path: $(el).attr('href'), releaseTime: '2024-01-02T10:00:00Z' })).get() };
          }
          async parseChapter(path) { return '<p>Chapter body ' + dayjs('2024-01-02').format('YYYY') + '</p>'; }
          async searchNovels(term, page) { return [{ name: 'Found ' + term, path: '/novel/9' }]; }
          async submitForm() {
            const form = new FormData();
            form.append('q', 'hello world');
            form.append('page', '2');
            const res = await fetchApi('https://sample.test/submit', { method: 'POST', body: form });
            return await res.text();
          }
        }
        module.exports = { default: new Sample() };
    """.trimIndent()

    private val mangayomiSource = """
        const mangayomiSources = [{ name: "Mg Sample", baseUrl: "https://m.test", lang: "en", isManga: false, itemType: 2, version: "1" }];
        class DefaultExtension extends MProvider {
          constructor() { super(); this.base = this.source.baseUrl; }
          getSourcePreferences() {
            return [{ key: "base", editTextPreference: { title: "Base", value: "https://pref.test" } }];
          }
          getHeaders(url) { return { Referer: this.source.baseUrl }; }
          async prefBase() { return new SharedPreferences().get("base"); }
          async elementLinks() {
            const doc = new Document('<a href="/x"><img data-src="/lazy.jpg"></a>');
            return [doc.selectFirst("a").getHref, doc.selectFirst("img").getSrc];
          }
          async getPageList(url) { return [{ url: "/p/1.jpg" }, "/p/2.jpg"]; }
          async getPopular(page) {
            const res = await new Client().get(this.base + "/popular?page=" + page, {});
            const doc = new Document(res.body);
            return { list: doc.select(".item").map(e => ({ name: e.selectFirst("a").text, link: e.selectFirst("a").attr("href"), imageUrl: e.selectFirst("img").attr("src") })), hasNextPage: true };
          }
          getFilterList() {
            return [
              { type_name: "SelectFilter", name: "Sort", state: 0, values: [{ type_name: "SelectOption", name: "New", value: "new" }, { type_name: "SelectOption", name: "Old", value: "old" }] },
              { type_name: "CheckBox", name: "Adult", state: false },
              { type_name: "GroupFilter", name: "Genres", state: [{ type_name: "CheckBox", name: "Action", value: "action", state: false }] },
              { type_name: "TextFilter", name: "Author", state: "" },
              { type_name: "SortFilter", name: "Order", state: { index: 0, ascending: false }, values: [{ type_name: "SelectOption", name: "Date", value: "date" }] },
            ];
          }
          async search(query, page, filters) {
            const sort = filters[0].values[filters[0].state].value;
            const adult = filters[1].state;
            const genres = filters[2].state.filter(g => g.state).map(g => g.value);
            return { list: [{ name: [query, sort, adult, genres.join("+"), filters[3].state, filters[4].state.index, filters[4].state.ascending].join("|"), link: "/s/1", imageUrl: "" }], hasNextPage: false };
          }
          async getDetail(url) {
            return { name: "Detail " + url, imageUrl: "/cover.jpg", description: "Desc", genre: ["x", "y"], status: 0,
              chapters: [{ name: "Ch1", url: "/ch/1", dateUpload: "1700000000000" }] };
          }
          async getHtmlContent(name, url) { return "<p>Novel text for " + name + "</p>"; }
        }
    """.trimIndent()

    @Test
    fun lnReaderAdapterMapsCatalogDetailsAndChapters() = runBlocking {
        val http = CannedHttp()
        val runtime = JsPluginRuntime.create(http, MemoryJsStorage(), scripts, scripts.lnReader, "lnreader")
        try {
            val meta = runtime.evaluate("__lnLoad(${jsString(lnPlugin)})", "plugin.js")
            assertNotNull(meta)
            // The plugin's imageRequestInit.headers travel in the metadata, as the loader reads them.
            assertTrue(meta!!.contains("\"imageHeaders\":{\"Referer\":\"https://sample.test\"}"))
            assertTrue(meta!!.contains("\"hasSearch\":true"))
            // The loader hands the plugin's own filter definitions to the adapter.
            val pluginFilters = Json.parseToJsonElement(meta!!).jsonObject["filters"]?.jsonObject
            val imageHeaders = Json.parseToJsonElement(meta).jsonObject["imageHeaders"]?.jsonObject
                ?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()

            val adapter = LnReaderSourceAdapter(
                id = "lnreader:sample",
                name = "Sample Site",
                baseUrl = "https://sample.test",
                runtime = runtime,
                filters = pluginFilters,
                hasSearch = true,
                hasPages = false,
                imageHeaders = imageHeaders,
            )

            http.body = """<div class="novel"><a href="/novel/1">First</a><img src="/c1.jpg"></div>""" +
                """<div class="novel"><a href="/novel/2">Second</a></div>"""
            val popular = adapter.getCatalogList(0)
            assertTrue(popular is Response.Success)
            val items = (popular as Response.Success).data.list
            assertEquals(listOf("First", "Second"), items.map { it.title })
            assertEquals("https://sample.test/novel/1", items[0].url)
            // A relative cover becomes absolute, and the plugin's image headers are registered for it.
            assertEquals("https://sample.test/c1.jpg", items[0].coverImageUrl)
            assertEquals(mapOf("Referer" to "https://sample.test"), ImageRequestHeaders.headersFor(items[0].coverImageUrl))

            val filters = adapter.getFilterList()
            assertTrue(filters is Response.Success)
            assertEquals(listOf("genre"), (filters as Response.Success).data.map { it.key })

            adapter.getCatalogFiltered(0, ActiveFilters(selectValues = mapOf("genre" to "fantasy")))
            assertTrue("filter was not sent: ${http.urls}", http.urls.last().contains("g=fantasy"))

            http.body = """<h1>Title</h1><div class="sum">Summary text</div>""" +
                """<a class="ch" href="/c/1">Ch 1</a><a class="ch" href="/c/2">Ch 2</a>"""
            val chapters = adapter.getChapterList("https://sample.test/novel/1")
            assertTrue(chapters is Response.Success)
            val list = (chapters as Response.Success).data
            assertEquals(listOf("Ch 1", "Ch 2"), list.map { it.title })
            assertEquals(1704189600L, list[0].uploaded)

            val title = adapter.getBookTitle("https://sample.test/novel/1")
            assertEquals("Title", (title as Response.Success).data)
            val genres = adapter.getBookGenres("https://sample.test/novel/1")
            assertEquals(listOf("Fantasy", "Action"), (genres as Response.Success).data)

            val doc = Jsoup.parse("<html></html>", "https://sample.test/c/1")
            assertEquals("<p>Chapter body 2024</p>", adapter.getChapterText(doc))
        } finally {
            runtime.close()
        }
    }

    @Test
    fun mangayomiAdapterMapsFiltersSearchDetailsAndContent() = runBlocking {
        val http = CannedHttp()
        val runtime = JsPluginRuntime.create(http, MemoryJsStorage(), scripts, scripts.mangayomi, "mangayomi")
        try {
            val meta = runtime.evaluate("__mgLoad(${jsString(mangayomiSource)})", "source.js")
            assertTrue(meta!!.contains("\"name\":\"Mg Sample\""))

            val adapter = MangayomiSourceAdapter(
                id = "mangayomi:sample",
                name = "Mg Sample",
                baseUrl = "https://m.test",
                contentType = "",
                runtime = runtime,
            )

            http.body = """<div class="item"><a href="/m/1">Manga One</a><img src="/m1.jpg"></div>"""
            val popular = adapter.getCatalogList(0)
            assertTrue(popular is Response.Success)
            val items = (popular as Response.Success).data.list
            assertEquals(listOf("Manga One"), items.map { it.title })
            assertEquals("https://m.test/m/1", items[0].url)
            assertEquals("https://m.test/popular?page=1", http.urls.last())
            // Covers are absolute, and the source's own getHeaders(url) is registered for them.
            assertEquals("https://m.test/m1.jpg", items[0].coverImageUrl)
            assertEquals(mapOf("Referer" to "https://m.test"), ImageRequestHeaders.headersFor("https://m.test/m1.jpg"))
            // SharedPreferences falls back to the default the source declares.
            assertEquals("\"https://pref.test\"", runtime.call("__mgDispatch", "prefBase", "[]"))
            // Element.getHref and getSrc are properties in Mangayomi sources; lazy images use data-src.
            assertEquals("[\"/x\",\"/lazy.jpg\"]", runtime.call("__mgDispatch", "elementLinks", "[]"))

            val filters = adapter.getFilterList()
            assertTrue(filters is Response.Success)
            val list = (filters as Response.Success).data
            assertEquals(5, list.size)
            assertTrue(list[0] is LuaFilter.Select)
            assertEquals("0", list[0].key)

            val filtered = adapter.getCatalogFiltered(
                0,
                ActiveFilters(
                    selectValues = mapOf("0" to "1"),
                    switchValues = mapOf("1" to true),
                    checkboxIncluded = mapOf("2" to listOf("action")),
                    textValues = mapOf("3" to "Tolkien"),
                ),
            )
            assertTrue(filtered is Response.Success)
            assertEquals(
                "|old|true|action|Tolkien|0|false",
                (filtered as Response.Success).data.list.single().title,
            )

            val detailUrl = "https://m.test/novel/1"
            val chapters = adapter.getChapterList(detailUrl)
            assertTrue(chapters is Response.Success)
            val chapterList = (chapters as Response.Success).data
            assertEquals("https://m.test/ch/1", chapterList.single().url)
            assertEquals(1700000000L, chapterList.single().uploaded)

            val status = adapter.getBookStatus(detailUrl)
            assertEquals("Ongoing", (status as Response.Success).data)
            val cover = adapter.getBookCoverImageUrl(detailUrl)
            assertEquals("https://m.test/cover.jpg", (cover as Response.Success).data)

            val doc = Jsoup.parse("<html></html>", "https://m.test/ch/1")
            assertEquals("<p>Novel text for Ch1</p>", adapter.getChapterText(doc))
            assertEquals(null, adapter.getChapterPages(doc))

            // Manga pages: absolute URLs, each with the source's headers registered for the image.
            val mangaAdapter = MangayomiSourceAdapter(
                id = "mangayomi:sample-manga",
                name = "Mg Sample",
                baseUrl = "https://m.test",
                contentType = "manga",
                runtime = runtime,
            )
            val pages = mangaAdapter.getChapterPages(Jsoup.parse("<html></html>", "https://m.test/ch/2"))
            assertEquals(listOf("https://m.test/p/1.jpg", "https://m.test/p/2.jpg"), pages)
            assertEquals(mapOf("Referer" to "https://m.test"), ImageRequestHeaders.headersFor("https://m.test/p/2.jpg"))
        } finally {
            runtime.close()
        }
    }

    @Test
    fun formDataIsSentAsMultipart() = runBlocking {
        val http = CannedHttp(body = "ok")
        val runtime = JsPluginRuntime.create(http, MemoryJsStorage(), scripts, scripts.lnReader, "lnreader")
        try {
            runtime.evaluate("__lnLoad(${jsString(lnPlugin)})", "plugin.js")
            val result = runtime.call("__lnDispatch", "submitForm", "[]")
            assertEquals("\"ok\"", result)
            val sent = http.requests.single()
            assertEquals("POST", sent.method)
            val contentType = sent.headers["Content-Type"].orEmpty()
            assertTrue("not multipart: $contentType", contentType.startsWith("multipart/form-data; boundary="))
            val boundary = contentType.substringAfter("boundary=")
            val body = sent.body.orEmpty()
            assertTrue(body.contains("Content-Disposition: form-data; name=\"q\"\r\n\r\nhello world\r\n"))
            assertTrue(body.endsWith("--$boundary--\r\n"))
        } finally {
            runtime.close()
        }
    }

    @Test
    fun aesGcmIsAvailableToLnReaderPlugins() = runBlocking {
        val runtime = JsPluginRuntime.create(CannedHttp(), MemoryJsStorage(), scripts, scripts.lnReader, "lnreader")
        try {
            val result = runtime.evaluate(
                """
                (function () {
                  var gcm = __hostRequire('@libs/aes').gcm;
                  var key = new Uint8Array(16); for (var i = 0; i < 16; i++) key[i] = i + 1;
                  var iv = new Uint8Array(12); for (var j = 0; j < 12; j++) iv[j] = j + 7;
                  var sealed = gcm(key, iv).encrypt(new TextEncoder().encode('hello novel'));
                  return new TextDecoder().decode(gcm(key, iv).decrypt(sealed));
                })()
                """.trimIndent(),
                "aes.js",
            )
            assertEquals("hello novel", result)
        } finally {
            runtime.close()
        }
    }

    @Test
    fun brokenPluginReportsAnErrorInsteadOfCrashing() = runBlocking {
        val runtime = JsPluginRuntime.create(CannedHttp(), MemoryJsStorage(), scripts, scripts.lnReader, "lnreader")
        try {
            val result = runCatching { runtime.evaluate("__lnLoad(\"throw new Error('boom')\")", "bad.js") }
            assertTrue(result.isFailure)
        } finally {
            runtime.close()
        }
    }
}
