package my.dreamora.extension_compat.js

import kotlinx.coroutines.runBlocking
import my.dreamora.core.Response
import my.dreamora.extension_compat.lnreader.LnReaderSourceAdapter
import my.dreamora.extension_compat.mangayomi.MangayomiSourceAdapter
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

        override suspend fun execute(request: JsHttpRequest): JsHttpResponse {
            urls += request.url
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
        const { fetchText } = require('@libs/fetch');
        const { FilterTypes } = require('@libs/filterInputs');
        const { NovelStatus } = require('@libs/novelStatus');
        const dayjs = require('dayjs');
        class Sample {
          constructor() {
            this.id = 'sample'; this.name = 'Sample Site'; this.site = 'https://sample.test'; this.version = '1.0.0';
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
        }
        module.exports = { default: new Sample() };
    """.trimIndent()

    private val mangayomiSource = """
        const mangayomiSources = [{ name: "Mg Sample", baseUrl: "https://m.test", lang: "en", isManga: false, itemType: 2, version: "1" }];
        class DefaultExtension extends MProvider {
          constructor() { super(); this.base = this.source.baseUrl; }
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
            assertTrue(meta!!.contains("\"hasSearch\":true"))

            val adapter = LnReaderSourceAdapter(
                id = "lnreader:sample",
                name = "Sample Site",
                baseUrl = "https://sample.test",
                runtime = runtime,
                filters = null,
                hasSearch = true,
                hasPages = false,
            )

            http.body = """<div class="novel"><a href="/novel/1">First</a><img src="/c1.jpg"></div>""" +
                """<div class="novel"><a href="/novel/2">Second</a></div>"""
            val popular = adapter.getCatalogList(0)
            assertTrue(popular is Response.Success)
            val items = (popular as Response.Success).data.list
            assertEquals(listOf("First", "Second"), items.map { it.title })
            assertEquals("https://sample.test/novel/1", items[0].url)

            val filters = adapter.getFilterList()
            assertTrue(filters is Response.Success)
            assertTrue((filters as Response.Success).data.isEmpty())

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

            val doc = Jsoup.parse("<html></html>", "https://m.test/ch/1")
            assertEquals("<p>Novel text for Ch1</p>", adapter.getChapterText(doc))
            assertEquals(null, adapter.getChapterPages(doc))
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
