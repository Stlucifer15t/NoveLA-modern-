package my.noveldokusha.extension_compat.repo

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatPayloadKind
import my.noveldokusha.extension_compat.core.CompatRepoParsers
import my.noveldokusha.extension_compat.core.CompatRepoEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Index parsers are tested against live formats (2026-11): keiyoushi v2/v1,
 * Legado collections and subscription lists, LNReader, Mangayomi, CloudStream.
 */
class CompatRepoParsersTest {

    private val repoId = "test-repo"

    // ── Mihon / keiyoushi ────────────────────────────────────────────────────

    private val keiyoushiV2 = """
{
  "name": "Keiyoushi",
  "badgeLabel": "KEI",
  "signingKey": "9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2",
  "contact": { "website": "https://keiyoushi.github.io" },
  "extensionList": {
    "extensions": [
      {
        "name": "AchyManga",
        "packageName": "eu.kanade.tachiyomi.extension.all.achymanga",
        "resources": {
          "apkUrl": "https://github.com/keiyoushi/extensions/releases/download/80af09c/tachiyomi-all.achymanga-v1.6.1.apk",
          "iconUrl": "https://cdn.jsdelivr.net/gh/keiyoushi/extensions-source@main/src/all/achymanga/res/mipmap-xhdpi/ic_launcher.png",
          "jarUrl": "https://github.com/keiyoushi/extensions/releases/download/80af09c/tachiyomi-all.achymanga-v1.6.1.jar"
        },
        "extensionLib": "1.6",
        "versionCode": "106001",
        "versionName": "1.6.1",
        "contentWarning": "CONTENT_WARNING_MIXED",
        "sources": [
          { "id": "9124545289113350724", "name": "AchyManga", "language": "es", "homeUrl": "https://achymanga.online" },
          { "id": "2311891545723109443", "name": "AchyManga EN", "language": "en", "homeUrl": "https://achymanga.online" }
        ]
      },
      {
        "name": "AHottie",
        "packageName": "eu.kanade.tachiyomi.extension.all.ahottie",
        "resources": { "apkUrl": "https://github.com/keiyoushi/extensions/releases/download/b035310/tachiyomi-all.ahottie-v1.6.5.apk" },
        "extensionLib": "1.6",
        "versionCode": "106005",
        "versionName": "1.6.5",
        "contentWarning": "CONTENT_WARNING_NSFW",
        "sources": [ { "id": "1", "name": "AHottie", "language": "all", "homeUrl": "https://ahottie.example" } ]
      }
    ]
  }
}
    """.trimIndent()

    @Test
    fun `keiyoushi v2 index is sniffed as mihon repo`() {
        assertEquals(CompatEcosystem.TACHIYOMI, CompatRepoParsers.sniff(keiyoushiV2))
    }

    @Test
    fun `keiyoushi v2 entries carry version, urls and signing key`() {
        val parsed = CompatRepoParsers.parseMihon(repoId, "https://x/index.json", keiyoushiV2)
        assertEquals("Keiyoushi", parsed.repoName)
        assertTrue(parsed.signerFingerprint.startsWith("9add655"))
        assertEquals(2, parsed.entries.size)

        val first = parsed.entries.first { it.key.endsWith("achymanga") }
        assertEquals("AchyManga", first.name)
        // versionCode arrives as a STRING — it must still become a number for update checks
        assertEquals(106001L, first.versionCode)
        assertEquals("1.6.1", first.version)
        assertEquals("https://achymanga.online", first.baseUrl)
        assertEquals("es", first.language)
        assertEquals("manga", first.contentType)
        assertEquals(CompatPayloadKind.APK, first.payloadKind)
        assertEquals("1.6", first.extra["extensionLib"])
        assertNotNull(first.iconUrl)
        assertTrue(first.description.contains("AchyManga EN"))
        assertEquals("2", first.extra["sourceCount"])
        assertFalse("CONTENT_WARNING_MIXED is not 18+", first.nsfw)

        val nsfw = parsed.entries.first { it.key.endsWith("ahottie") }
        assertTrue(nsfw.nsfw)
    }

    private val mihonV1 = """
[
  {
    "name": "Outdated App",
    "pkg": "eu.kanade.tachiyomi.extension.all.keiyoushi",
    "apk": "tachiyomi-all.keiyoushi-v1.4.1.apk",
    "lang": "all",
    "code": 1,
    "version": "1.4.1",
    "nsfw": 0,
    "sources": [ { "name": "Outdated App", "lang": "all", "id": "1", "baseUrl": "https://keiyoushi.github.io" } ]
  },
  {
    "name": "MangaDex",
    "pkg": "eu.kanade.tachiyomi.extension.en.mangadex",
    "apk": "tachiyomi-en.mangadex-v1.7.9.apk",
    "lang": "en",
    "code": 179,
    "version": "1.7.9",
    "nsfw": 0,
    "sources": [ { "name": "MangaDex", "lang": "en", "id": "1333311212882262001", "baseUrl": "https://mangadex.org" } ]
  },
  {
    "name": "AniExample",
    "pkg": "eu.kanade.tachiyomi.animeextension.en.aniexample",
    "apk": "animeexample.apk",
    "lang": "en",
    "code": 12,
    "version": "0.12",
    "nsfw": 1,
    "sources": [ { "name": "AniExample", "lang": "en", "id": "7", "baseUrl": "https://ani.example" } ]
  }
]
    """.trimIndent()

    @Test
    fun `mihon v1 stub entries are dropped and apk path resolved against index dir`() {
        val parsed = CompatRepoParsers.parseMihon(
            repoId,
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            mihonV1,
        )
        // "Outdated App" is a stub for old clients, not a source
        assertEquals(2, parsed.entries.size)
        val dex = parsed.entries.first { it.key.endsWith("mangadex") }
        assertEquals(
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/tachiyomi-en.mangadex-v1.7.9.apk",
            dex.payloadUrl,
        )
        assertEquals(179L, dex.versionCode)
        assertEquals("https://mangadex.org", dex.baseUrl)

        val anime = parsed.entries.first { it.key.contains("aniexample") }
        // a package from animeextension → Aniyomi, content = video
        assertEquals(CompatEcosystem.ANIYOMI, anime.ecosystem)
        assertEquals("video", anime.contentType)
        assertTrue(anime.nsfw)
    }

    @Test
    fun `repo json pointer prefers json index next to protobuf`() {
        val pointer = """
{
  "index_v2": "https://github.com/keiyoushi/extensions/raw/repo/index.pb",
  "meta": {
    "name": "Keiyoushi",
    "website": "https://keiyoushi.github.io",
    "signingKeyFingerprint": "9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2"
  }
}
        """.trimIndent()
        assertEquals(CompatEcosystem.TACHIYOMI, CompatRepoParsers.sniff(pointer))
        val parsed = CompatRepoParsers.parseRepoPointer(pointer)!!
        assertEquals("index.pb is not read, the JSON variant is used", 2, parsed.indexUrls.size)
        assertTrue(parsed.indexUrls[0].endsWith("index.json"))
        assertEquals("Keiyoushi", parsed.repoName)
        assertTrue(parsed.signerFingerprint.length > 40)
    }

    // ── Legado ───────────────────────────────────────────────────────────────

    private val legadoCollection = """
[
  {
    "bookSourceName": "First Novel",
    "bookSourceUrl": "https://novel-one.example",
    "bookSourceGroup": "Novel\n自用",
    "bookSourceType": 0,
    "customOrder": 3,
    "searchUrl": "/search.php?keyword={{key}}&page={{page}}",
    "ruleSearch": { "bookList": ".result-item", "bookName": ".title@text" },
    "ruleBookInfo": { "name": "[property=bookName]" },
    "ruleToc": { "chapterList": "#chapterList a" },
    "lastUpdateTime": 1710000000000
  },
  {
    "bookSourceName": "Manga Two",
    "bookSourceUrl": "https://manga-two.example",
    "bookSourceGroup": "18+ 漫画",
    "bookSourceType": 2,
    "ruleSearch": { "bookList": ".box1", "bookUrl": ".box2 a@href" }
  },
  {
    "bookSourceName": "Broken Without Rules",
    "bookSourceUrl": "https://broken.example"
  },
  {
    "bookSourceName": "Broken Without Rules 2",
    "bookSourceUrl": "https://broken2.example",
    "ruleSearch": { "bookList": "" }
  }
]
    """.trimIndent()

    @Test
    fun `legado collection keeps only usable sources and maps content type`() {
        assertEquals(CompatEcosystem.LEGADO, CompatRepoParsers.sniff(legadoCollection))
        val parsed = CompatRepoParsers.parseLegado(repoId, "https://host/bookSource.json", legadoCollection)
        assertEquals(2, parsed.entries.size)

        val novel = parsed.entries.first { it.key == "https://novel-one.example" }
        assertEquals("First Novel", novel.name)
        assertEquals("https://novel-one.example", novel.baseUrl)
        assertEquals("", novel.contentType)
        assertFalse(novel.nsfw)
        assertEquals("https://novel-one.example", novel.inlineKey)
        assertEquals("3", novel.extra["customOrder"])

        val manga = parsed.entries.first { it.key == "https://manga-two.example" }
        assertEquals("manga", manga.contentType)
        assertTrue("an 18+ group marks the entry nsfw", manga.nsfw)
    }

    @Test
    fun `legado inline payload keeps fields our model does not know`() {
        val raw = CompatRepoParsers.extractSourceJson(legadoCollection, "https://novel-one.example")
        assertNotNull(raw)
        assertTrue(raw!!.contains("customOrder"))
        assertTrue(raw.contains("lastUpdateTime"))
        assertTrue(raw.contains("ruleBookInfo"))
        // The neighbouring object must not leak in
        assertFalse(raw.contains("manga-two"))
        assertNull(CompatRepoParsers.extractSourceJson(legadoCollection, "https://nope.example"))
    }

    @Test
    fun `legado wrapped export form is supported`() {
        val wrapped = """
{
  "manifestVersion": 1,
  "bookSources": [
    {
      "bookSourceName": "Wrapped One",
      "bookSourceUrl": "https://wrapped.example",
      "searchUrl": "/s/{{key}}",
      "ruleSearch": { "bookList": ".a" }
    }
  ]
}
        """.trimIndent()
        val parsed = CompatRepoParsers.parseLegado(repoId, "https://host/export.json", wrapped)
        assertEquals(1, parsed.entries.size)
        assertEquals("Wrapped One", parsed.entries.first().name)
        assertNotNull(CompatRepoParsers.extractSourceJson(wrapped, "https://wrapped.example"))
    }

    @Test
    fun `legado single source file yields one entry`() {
        val single = """
{
  "bookSourceName": "Solo",
  "bookSourceUrl": "https://solo.example",
  "searchUrl": "/q/{{key}}",
  "ruleSearch": { "bookList": ".li" }
}
        """.trimIndent()
        val parsed = CompatRepoParsers.parseLegado(repoId, "https://host/solo.json", single)
        assertEquals(1, parsed.entries.size)
        assertEquals("https://solo.example", parsed.entries.first().key)
    }

    @Test
    fun `legado url list forms produce downloadable entries`() {
        val asJsonArray = """
[
  "https://alpha.example/bookSource.json",
  "https://beta.example/source.json"
]
        """.trimIndent()
        val fromArray = CompatRepoParsers.parseLegado(repoId, "https://host/list.json", asJsonArray)
        assertEquals(2, fromArray.entries.size)
        assertEquals("https://alpha.example/bookSource.json", fromArray.entries.first().payloadUrl)
        assertEquals(CompatPayloadKind.JSON, fromArray.entries.first().payloadKind)

        val asText = """
            https://gamma.example/books.json
            # a comment line, must be ignored
            https://delta.example/books.json
        """.trimIndent()
        val fromText = CompatRepoParsers.parseLegado(repoId, "https://host/sources.txt", asText)
        assertEquals(2, fromText.entries.size)
        assertEquals("https://gamma.example", fromText.entries.first().baseUrl)
    }

    // ── LNReader ─────────────────────────────────────────────────────────────

    @Test
    fun `lnreader plugins index maps site and code url`() {
        val index = """
[
  {
    "id": "arnovel",
    "name": "ArNovel",
    "site": "https://ar-no.com/",
    "lang": "العربية",
    "version": "2.4.1",
    "url": "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/.js/src/plugins/arabic/ArNovel[madara].js",
    "iconUrl": "https://raw.githubusercontent.com/lnreader/lnreader-plugins/main/public/icon.png"
  },
  { "id": "mangaline", "name": "MangaLine", "site": "https://mangaline.example", "lang": "en", "version": "1.0.0", "url": "https://cdn.example/MangaLine.js", "isManga": true }
]
        """.trimIndent()
        assertEquals(CompatEcosystem.LNREADER, CompatRepoParsers.sniff(index, "https://host/plugins.min.json"))
        val parsed = CompatRepoParsers.parseLNReader(repoId, "https://host/.dist/plugins.min.json", index)
        assertEquals(2, parsed.entries.size)
        val ar = parsed.entries.first { it.key == "arnovel" }
        assertEquals("ArNovel", ar.name)
        assertEquals("https://ar-no.com", ar.baseUrl)
        assertEquals(CompatPayloadKind.JS, ar.payloadKind)
        assertEquals("2.4.1", ar.version)
        assertNotNull(ar.iconUrl)
        assertEquals("manga", parsed.entries.first { it.key == "mangaline" }.contentType)
    }

    // ── Mangayomi ────────────────────────────────────────────────────────────

    @Test
    fun `mangayomi novel index keeps js payload and novel content type`() {
        val index = """
[{"name":"kolnovel","id":756554088,"baseUrl":"https://kolnovel.com","lang":"ar","typeSource":"single","iconUrl":"https://x/icon.png","isNsfw":false,"hasCloudflare":false,"sourceCodeUrl":"https://raw.githubusercontent.com/entityJY/mangayomi-extensions/main/javascript/novel/src/ar/kolnovel.js","version":"0.0.1","isManga":false,"itemType":2,"appMinVerReq":"0.5.0","sourceCodeLanguage":1,"notes":""}]
        """.trimIndent()
        assertEquals(CompatEcosystem.MANGAYOMI, CompatRepoParsers.sniff(index))
        val parsed = CompatRepoParsers.parseMangayomi(repoId, "https://host/novel_index.json", index)
        val entry = parsed.entries.single()
        assertEquals("756554088", entry.key)
        assertEquals("", entry.contentType)
        assertEquals(CompatPayloadKind.JS, entry.payloadKind)
        assertEquals("https://kolnovel.com", entry.baseUrl)
        assertEquals("single", entry.extra["typeSource"])
        assertEquals("true", entry.extra["cloudflare"])
    }

    @Test
    fun `mangayomi dart source is marked as dart payload`() {
        val index = """
[{"name":"1st Kiss-Manga","id":638504049,"baseUrl":"https://1stkissmanga.org","lang":"en","typeSource":"madara","sourceCodeUrl":"https://host/dart/manga/multisrc/madara/madara.dart","version":"0.1.3","isManga":true,"itemType":0,"sourceCodeLanguage":0}]
        """.trimIndent()
        val parsed = CompatRepoParsers.parseMangayomi(repoId, "https://host/index.json", index)
        val entry = parsed.entries.single()
        assertEquals("manga", entry.contentType)
        assertEquals(CompatPayloadKind.DART, entry.payloadKind)
    }

    // ── CloudStream ──────────────────────────────────────────────────────────

    @Test
    fun `cloudstream repo json exposes nested plugin lists`() {
        val repo = """
{
  "name": "CloudStream Community",
  "status": 0,
  "pluginLists": [
    { "name": "v1.0", "url": "https://raw.githubusercontent.com/repo/plugins/main/index.min.json" },
    { "name": "v0.4", "url": "https://raw.githubusercontent.com/repo/plugins/legacy/index.min.json" }
  ]
}
        """.trimIndent()
        assertEquals(CompatEcosystem.CLOUDSTREAM, CompatRepoParsers.sniff(repo))
        val parsed = CompatRepoParsers.parseCloudStream(repoId, "https://host/repo.json", repo)
        assertEquals("CloudStream Community", parsed.repoName)
        assertEquals(2, parsed.nestedIndexUrls.size)
        assertTrue(parsed.nestedIndexUrls.first().endsWith("index.min.json"))
        assertTrue(parsed.entries.isEmpty())
    }

    @Test
    fun `cloudstream plugin list yields cs3 payloads`() {
        val list = """
[
  { "name": "Animes", "version": "26", "url": "https://host/releases/Animes.cs3", "iconUrl": null, "description": "Anime provider", "authors": ["someone"] }
]
        """.trimIndent()
        val entries = CompatRepoParsers.parseCloudStreamList(repoId, "https://host/index.min.json", list)
        val entry = entries.single()
        assertEquals("Animes", entry.name)
        assertEquals(CompatPayloadKind.CS3, entry.payloadKind)
        assertEquals("video", entry.contentType)
        assertEquals("someone", entry.description)
    }

    // ── TREE scan ───────────────────────────────────────────────────────────────

    @Test
    fun `tree scan turns json files into legado entries and skips junk`() {
        val files = listOf(
            CompatRepoParsers.TreeFile("bilinovel.json", 54_411),
            CompatRepoParsers.TreeFile("masiro.json", 7_686),
            CompatRepoParsers.TreeFile("README.md", 14_455),
            CompatRepoParsers.TreeFile("package.json", 900),
            CompatRepoParsers.TreeFile("utils/icon.png", 12),
            CompatRepoParsers.TreeFile("huge.json", 90_000_000),
        )
        val entries = CompatRepoParsers.parseTree(
            repoId = repoId,
            rawBase = "https://raw.githubusercontent.com/o/r/main",
            files = files,
            ecosystem = null,
        )
        assertEquals(2, entries.size)
        val first = entries.first()
        assertEquals("bilinovel.json", first.key)
        assertEquals("bilinovel", first.name)
        assertEquals(CompatEcosystem.LEGADO, first.ecosystem)
        assertEquals(
            "https://raw.githubusercontent.com/o/r/main/bilinovel.json",
            first.payloadUrl,
        )
    }

    @Test
    fun `tree scan is capped so a giant repo cannot explode the list`() {
        val files = (1..3000).map { CompatRepoParsers.TreeFile("src/s$it.json", 1_000) }
        val entries = CompatRepoParsers.parseTree(repoId, "https://raw/x/y/main", files, CompatEcosystem.LEGADO)
        assertEquals(CompatRepoParsers.TREE_ENTRY_LIMIT, entries.size)
    }

    // ── utilities ───────────────────────────────────────────────────────────────

    @Test
    fun `absolutize handles relative, root-relative and protocol relative urls`() {
        val index = "https://host/dir/sub/index.min.json"
        assertEquals("https://host/dir/sub/x.apk", CompatRepoParsers.absolutize(index, "x.apk"))
        assertEquals("https://host/top/x.apk", CompatRepoParsers.absolutize(index, "/top/x.apk"))
        assertEquals("https://cdn/x.apk", CompatRepoParsers.absolutize(index, "//cdn/x.apk"))
        assertEquals("http://other/x", CompatRepoParsers.absolutize(index, "http://other/x"))
        assertEquals("", CompatRepoParsers.absolutize(index, "  "))
    }

    @Test
    fun `sniff returns null for unrelated json`() {
        assertNull(CompatRepoParsers.sniff("""{"a":1,"b":[1,2]}"""))
        assertNull(CompatRepoParsers.sniff("not json at all", "https://host/x.txt"))
        assertNull(CompatRepoParsers.sniff(""))
    }

    @Test
    fun `sniff falls back to file name when content is ambiguous`() {
        val body = """[{"name":"weird","url":"https://x/y.json","id":"q"}]"""
        assertEquals(CompatEcosystem.LNREADER, CompatRepoParsers.sniff(body, "https://host/plugins.min.json"))
        assertEquals(CompatEcosystem.MANGAYOMI, CompatRepoParsers.sniff(body, "https://host/novel_index.json"))
    }

    private fun List<CompatRepoEntry>.keySet(): Set<String> = mapTo(HashSet()) { it.key }

    @Test
    fun `duplicate keys collapse to one entry`() {
        val duplicated = """
[
  { "bookSourceName": "Dup", "bookSourceUrl": "https://dup.example", "searchUrl": "/s/{{key}}", "ruleSearch": { "bookList": ".a" } },
  { "bookSourceName": "Dup again", "bookSourceUrl": "https://dup.example", "searchUrl": "/s2/{{key}}", "ruleSearch": { "bookList": ".b" } }
]
        """.trimIndent()
        val parsed = CompatRepoParsers.parseLegado(repoId, "https://host/bookSource.json", duplicated)
        assertEquals(1, parsed.entries.size)
        assertEquals(setOf("https://dup.example"), parsed.entries.keySet())
    }
}
