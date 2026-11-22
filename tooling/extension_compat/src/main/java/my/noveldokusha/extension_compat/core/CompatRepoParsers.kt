package my.noveldokusha.extension_compat.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Parsing third-party repository index files into one [CompatRepoEntry] list.
 *
 * Formats verified against live repositories (2026-11):
 *
 * | Ecosystem | Index | Shape |
 * |---|---|---|
 * | Mihon/keiyoushi (v2) | `repo.json` → `index.json` | `{name, signingKey, extensionList:{extensions:[…]}}` |
 * | Mihon (v1, legacy) | `index.min.json` | `[{name,pkg,apk,code,version,nsfw,sources:[…]}]` |
 * | Legado | `bookSource.json` / `.txt` | array of bookSources, array of URLs, or a line-per-URL list |
 * | LNReader | `.dist/plugins.min.json` | `[{id,name,site,lang,version,url,iconUrl}]` |
 * | Mangayomi | `index.json`, `novel_index.json` | `[{name,id,baseUrl,sourceCodeUrl,itemType,sourceCodeLanguage,…}]` |
 * | CloudStream | `repo.json` | `{name, pluginLists:[{name,url}]}` → nested `.cs3` lists |
 *
 * No network and no Android here: plain text in, models out — which is why every
 * format is covered by the `CompatRepoParsersTest` unit test.
 */
object CompatRepoParsers {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Maximum number of entries a TREE repository may yield. */
    const val TREE_ENTRY_LIMIT = 400

    /** A single bookSource file is never huge; bigger means a collection or junk. */
    private const val MAX_TREE_FILE_BYTES = 4_000_000L

    /** Index file candidates, in preference order (v2 before v1!). */
    val INDEX_FILE_NAMES = listOf(
        "repo.json",          // Mihon v2 pointer / CloudStream / Mangayomi
        "index.json",         // Mihon v2 (full), Mangayomi
        "novel_index.json",   // Mangayomi — novels
        "manga_index.json",
        "index.min.json",     // Mihon v1 / CloudStream
        "plugins.min.json",   // LNReader
        "plugins.json",
        "bookSource.json",    // Legado collections
        "bookSourceNew.json",
        "booksource.json",
        "sources.json",
        "source.json",
        "sources.txt",
        "bookSource.txt",
        "index.txt",
    )

    /** Files that are certainly not sources (noise picked up by a TREE scan). */
    private val TREE_SKIP = listOf(
        "package.json", "package-lock", "tsconfig", "renovate", "crowdin",
        "i18n", "locales", "/res/", "/build/", "/gradle", ".github/", ".gitlab",
        "node_modules", "readme", "changelog", "license", "icon",
    )

    /** Keys under which a Legado export hides the bookSource array. */
    private val LEGADO_WRAPPER_KEYS = listOf("bookSources", "data", "sources", "list")

    data class ParsedIndex(
        val ecosystem: CompatEcosystem,
        val repoName: String = "",
        val signerFingerprint: String = "",
        val entries: List<CompatRepoEntry> = emptyList(),
        /** CloudStream: the repository only points at nested plugin lists. */
        val nestedIndexUrls: List<String> = emptyList(),
    )

    // ── Format detection ───────────────────────────────────────────────────────

    /**
     * Format from content; the order matters — most specific markers first.
     * null = matches no known index, so the caller tries the next candidate
     * from [INDEX_FILE_NAMES].
     */
    fun sniff(text: String, url: String = ""): CompatEcosystem? {
        val head = text.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        if (head.isBlank()) return null

        if (!head.startsWith("{") && !head.startsWith("[")) {
            // Not JSON: could be a "one URL per line" subscription (Legado .txt)
            val firstLine = head.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            return if (firstLine != null && firstLine.startsWith("http")) CompatEcosystem.LEGADO else null
        }

        if (head.contains("\"extensionList\"") || head.contains("\"index_v2\"")) {
            return CompatEcosystem.TACHIYOMI
        }
        if (head.contains("\"bookSourceName\"") || head.contains("\"bookSourceUrl\"") ||
            head.contains("\"bookSourceType\"") || head.contains("\"ruleBookInfo\"") ||
            head.contains("\"ruleSearch\"") || head.contains("\"ruleToc\"")
        ) {
            return CompatEcosystem.LEGADO
        }
        if (head.contains("\"pluginLists\"")) return CompatEcosystem.CLOUDSTREAM
        if (head.contains("\"sourceCodeUrl\"") || head.contains("\"typeSource\"") ||
            head.contains("\"appMinVerReq\"")
        ) {
            return CompatEcosystem.MANGAYOMI
        }
        if (head.contains("\"pkg\"") && head.contains("\"apk\"")) return CompatEcosystem.TACHIYOMI
        if (head.contains("\"site\"") && head.contains("\"url\"")) return CompatEcosystem.LNREADER

        // Content is inconclusive — last resort: the file name
        return when {
            url.endsWith("plugins.min.json") || url.endsWith("plugins.json") -> CompatEcosystem.LNREADER
            url.endsWith("novel_index.json") || url.contains("mangayomi") -> CompatEcosystem.MANGAYOMI
            url.contains("booksource", ignoreCase = true) -> CompatEcosystem.LEGADO
            else -> null
        }
    }

    /** Detect the format and parse it. null when the format is unknown. */
    fun parse(repoId: String, indexUrl: String, text: String): ParsedIndex? {
        val ecosystem = sniff(text, indexUrl) ?: return null
        return parse(ecosystem, repoId, indexUrl, text)
    }

    fun parse(
        ecosystem: CompatEcosystem,
        repoId: String,
        indexUrl: String,
        text: String,
    ): ParsedIndex = when (ecosystem) {
        CompatEcosystem.TACHIYOMI, CompatEcosystem.ANIYOMI ->
            parseMihon(repoId, indexUrl, text, ecosystem)

        CompatEcosystem.LEGADO -> parseLegado(repoId, indexUrl, text)
        CompatEcosystem.LNREADER -> parseLNReader(repoId, indexUrl, text)
        CompatEcosystem.MANGAYOMI -> parseMangayomi(repoId, indexUrl, text)
        CompatEcosystem.CLOUDSTREAM -> parseCloudStream(repoId, indexUrl, text)
    }

    // ── Mihon / Tachiyomi / Aniyomi / keiyoushi ─────────────────────────────

    /**
     * v2 (current keiyoushi):
     * ```json
     * {"name":"Keiyoushi","signingKey":"9add…","extensionList":{"extensions":[
     *   {"name":"AchyManga","packageName":"eu.kanade…achymanga",
     *    "resources":{"apkUrl":"https://…/x.apk","iconUrl":"…"},
     *    "versionCode":"106001","versionName":"1.6.1","contentWarning":"CONTENT_WARNING_MIXED",
     *    "sources":[{"id":"…","name":"…","language":"es","homeUrl":"https://…"}]}]}}
     * ```
     * v1 (legacy): a flat array where `apk` is a file name next to the index.
     *
     * One row = one APK: the payload is downloaded once, and the several sources
     * inside the package are created by the loader itself ([CompatExtensionLoader.load]
     * returns a list — the more languages a package has, the more rows it produces.
     */
    fun parseMihon(
        repoId: String,
        indexUrl: String,
        text: String,
        declared: CompatEcosystem = CompatEcosystem.TACHIYOMI,
    ): ParsedIndex {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ParsedIndex(declared)

        if (trimmed.startsWith("{")) {
            val root = trimmed.parseObject() ?: return ParsedIndex(declared)
            val list = root["extensionList"].asObject().arrayOfObjects("extensions")
                .ifEmpty { root.arrayOfObjects("extensions") }
                .ifEmpty { root.arrayOfObjects("plugins") }
            val repoName = root.stringOr("name")
            val signing = root.stringOr("signingKey", "signingKeyFingerprint")
            val entries = list.mapNotNull { ext ->
                mihonV2Entry(ext, repoId, indexUrl, declared, signing)
            }
            return ParsedIndex(
                ecosystem = declared,
                repoName = repoName,
                signerFingerprint = signing,
                entries = entries.distinctByKey(CompatRepoEntry::key),
            )
        }

        val array = trimmed.parseArray() ?: return ParsedIndex(declared)
        val entries = array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val pkg = obj.stringOr("pkg", "packageName")
            val name = obj.stringOr("name")
            if (isOutdatedStub(name) || (pkg.isBlank() && name.isBlank())) return@mapNotNull null
            val sources = obj.arrayOfObjects("sources")
            val first = sources.firstOrNull()
            val apkUrl = absolutize(indexUrl, obj.stringOr("apk", "apkName"))
            val ecosystem = if (pkg.contains("animeextension")) CompatEcosystem.ANIYOMI else declared
            CompatRepoEntry(
                key = pkg.ifBlank { name },
                repoId = repoId,
                name = name.ifBlank { pkg.substringAfterLast('.') },
                ecosystem = ecosystem,
                version = obj.stringOr("version", "versionName"),
                versionCode = obj.longOf("code", "versionCode") ?: 0L,
                language = first.stringOr("lang", "language").ifBlank { obj.stringOr("lang") },
                baseUrl = first.stringOr("baseUrl", "homeUrl").trimEnd('/'),
                payloadUrl = apkUrl,
                iconUrl = obj.stringOr("icon").ifBlank { null },
                contentType = if (ecosystem == CompatEcosystem.ANIYOMI) "video" else "manga",
                nsfw = obj.longOf("nsfw") == 1L,
                description = sources.sourceNames(),
                payloadKind = CompatPayloadKind.APK,
                extra = meta("sourceCount" to if (sources.size > 1) sources.size.toString() else null),
            )
        }
        return ParsedIndex(
            ecosystem = declared,
            entries = entries.distinctByKey(CompatRepoEntry::key),
        )
    }

    private fun mihonV2Entry(
        ext: JsonObject,
        repoId: String,
        indexUrl: String,
        declared: CompatEcosystem,
        signing: String,
    ): CompatRepoEntry? {
        val pkg = ext.stringOr("packageName", "pkg")
        val name = ext.stringOr("name")
        if (pkg.isBlank() && name.isBlank()) return null
        if (isOutdatedStub(name)) return null
        val resources = ext["resources"].asObject()
        val apkRelative = resources.stringOr("apkUrl").ifBlank { ext.stringOr("apk", "apkName") }
        val apkUrl = absolutize(indexUrl, apkRelative)
        val sources = ext.arrayOfObjects("sources")
        val first = sources.firstOrNull()
        val warning = ext.stringOr("contentWarning")
        val ecosystem = if (pkg.contains("animeextension")) CompatEcosystem.ANIYOMI else declared
        return CompatRepoEntry(
            key = pkg.ifBlank { name },
            repoId = repoId,
            name = name.ifBlank { pkg.substringAfterLast('.') },
            ecosystem = ecosystem,
            version = ext.stringOr("versionName", "version"),
            versionCode = ext.longOf("versionCode", "code") ?: 0L,
            language = first.stringOr("language", "lang").ifBlank { ext.stringOr("lang") },
            baseUrl = first.stringOr("homeUrl", "baseUrl").trimEnd('/'),
            payloadUrl = apkUrl,
            iconUrl = resources.stringOr("iconUrl").ifBlank { null },
            contentType = if (ecosystem == CompatEcosystem.ANIYOMI) "video" else "manga",
            nsfw = warning.contains("NSFW", ignoreCase = true) || ext.longOf("nsfw") == 1L,
            description = sources.sourceNames(),
            payloadKind = CompatPayloadKind.fromUrlOrName(apkUrl.ifBlank { "package.apk" }),
            signerFingerprint = signing,
            size = resources.longOf("apkSize", "size") ?: 0L,
            extra = meta(
                "sourceCount" to if (sources.size > 1) sources.size.toString() else null,
                "contentWarning" to warning.ifBlank { null },
                "extensionLib" to resources.stringOr("extensionLib").ifBlank {
                    ext.stringOr("extensionLib", "libVersion").ifBlank { null }
                },
                "jarUrl" to resources.stringOr("jarUrl").ifBlank { null },
            ),
        )
    }

    /**
     * A Mihon repository puts a stub for outdated clients into `index.min.json`
     * ("Outdated App", "Update to Mihon 0.20.1+"). That is not a source.
     */
    private fun isOutdatedStub(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("outdated") || n.contains("update to") || n.contains("please update")
    }

    // ── Legado ───────────────────────────────────────────────────────────────

    /**
     * A bookSource collection. Shapes seen in live repositories:
     *  - an array of bookSource objects (`entr0pia/MyLegadoSource` → `bookSource.json`);
     *  - a wrapper `{bookSources:[…]}` / `{data:[…]}` (in-app export);
     *  - a single bookSource object;
     *  - an array of URL strings or a line-per-URL `.txt` — then the payload is fetched.
     *
     * Inline objects are never re-encoded: install takes their original text
     * out of the index cache ([extractSourceJson]) so that fields missing from
     * our model are not lost.
     */
    fun parseLegado(repoId: String, indexUrl: String, text: String): ParsedIndex {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ParsedIndex(CompatEcosystem.LEGADO)
        val repoName = indexUrl.substringAfterLast('/').ifBlank { hostOf(indexUrl) }

        // a) line-per-URL list
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            val urls = trimmed.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("http://") || it.startsWith("https://") }
                .distinct()
                .toList()
            return ParsedIndex(
                ecosystem = CompatEcosystem.LEGADO,
                repoName = repoName,
                entries = urls.mapNotNull { legadoUrlEntry(repoId, it) },
            )
        }

        val objects = inlineSources(trimmed)

        // b) array of URL strings
        if (objects.isEmpty()) {
            val urls = trimmed.parseArray()
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
                ?.filter { it.startsWith("http") }
                .orEmpty()
            if (urls.isNotEmpty()) {
                return ParsedIndex(
                    ecosystem = CompatEcosystem.LEGADO,
                    repoName = repoName,
                    entries = urls.distinct().mapNotNull { legadoUrlEntry(repoId, it) },
                )
            }
        }

        val entries = objects.mapIndexedNotNull { i, obj ->
            val bookUrl = obj.stringOr("bookSourceUrl").trim()
            val name = obj.stringOr("bookSourceName").trim()
            if (bookUrl.isBlank() && name.isBlank()) return@mapIndexedNotNull null
            // Without search rules a source is useless: Legado fills in defaults itself,
            // but we do not guess — such a source only clutters the list.
            val hasSearch = obj["ruleSearch"].asObject().stringOr("bookList").isNotBlank() ||
                obj.stringOr("searchUrl").isNotBlank() ||
                obj["ruleExplore"].asObject().stringOr("bookList").isNotBlank()
            if (!hasSearch) return@mapIndexedNotNull null
            val group = obj.stringOr("bookSourceGroup")
            val key = bookUrl.ifBlank { "$name#$i" }
            CompatRepoEntry(
                key = key,
                repoId = repoId,
                name = name.ifBlank { hostOf(bookUrl) },
                ecosystem = CompatEcosystem.LEGADO,
                baseUrl = bookUrl.substringBefore("{{").trimEnd('/'),
                inlineKey = key,
                contentType = when (obj.longOf("bookSourceType")) {
                    2L -> "manga"
                    4L -> "video"
                    1L -> "audio"
                    else -> ""
                },
                nsfw = isNsfwGroup(group),
                description = group.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim(),
                payloadKind = CompatPayloadKind.JSON,
                extra = meta(
                    "comment" to obj.stringOr("bookSourceComment").ifBlank { null }?.take(200),
                    "customOrder" to obj.stringOr("customOrder").ifBlank { null },
                    "lastUpdateTime" to obj.stringOr("lastUpdateTime").ifBlank { null },
                ),
            )
        }.distinctByKey(CompatRepoEntry::key)

        return ParsedIndex(
            ecosystem = CompatEcosystem.LEGADO,
            repoName = repoName,
            entries = entries,
        )
    }

    private fun legadoUrlEntry(repoId: String, url: String): CompatRepoEntry? {
        val host = url.removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (host.isBlank()) return null
        return CompatRepoEntry(
            key = url,
            repoId = repoId,
            name = url.substringAfterLast('/').substringBeforeLast('.').ifBlank { host },
            ecosystem = CompatEcosystem.LEGADO,
            baseUrl = "https://$host",
            payloadUrl = url,
            payloadKind = CompatPayloadKind.fromUrlOrName(url),
            description = host,
        )
    }

    /** bookSource objects inlined in the index (array, wrapper or single object). */
    private fun inlineSources(trimmed: String): List<JsonObject> {
        if (trimmed.startsWith("[")) return trimmed.parseArray().orEmpty().filterIsInstance<JsonObject>()
        val root = trimmed.parseObject() ?: return emptyList()
        for (key in LEGADO_WRAPPER_KEYS) {
            val arr = root.arrayOfObjects(key)
            if (arr.isNotEmpty()) return arr
        }
        return if (root.stringOr("bookSourceName").isNotBlank() ||
            root.stringOr("bookSourceUrl").isNotBlank()
        ) listOf(root) else emptyList()
    }

    private fun isNsfwGroup(group: String): Boolean {
        val g = group.lowercase()
        return listOf("18+", "r18", "nsfw", "成人", "里番", "禁漫").any { g.contains(it) }
    }

    /**
     * The original JSON of one bookSource, taken from the cached index text by
     * [CompatRepoEntry.inlineKey] (bookSourceUrl, less often bookSourceName).
     */
    fun extractSourceJson(indexText: String, key: String): String? {
        val trimmed = indexText.trim()
        if (trimmed.isEmpty() || key.isBlank()) return null
        val elements = topLevelArrayElements(trimmed)
            ?: return trimmed.takeIf { it.startsWith("{") }?.matchesLegadoKey(key)
        for (slice in elements) {
            if (slice.trim().matchesLegadoKey(key)) return slice.trim()
        }
        return null
    }

    /** Text of the bookSource object whose bookSourceUrl/bookSourceName == [key]. */
    private fun String.matchesLegadoKey(key: String): Boolean {
        val obj = runCatching { trim().parseObject() }.getOrNull() ?: return false
        return obj.stringOr("bookSourceUrl").trim() == key ||
            obj.stringOr("bookSourceName").trim() == key
    }

    // ── LNReader ─────────────────────────────────────────────────────────────

    /** `[{"id":"azora","name":"…","site":"https://…","lang":"en","version":"2.4.1","url":"…/x.js","iconUrl":"…"}]` */
    fun parseLNReader(repoId: String, indexUrl: String, text: String): ParsedIndex {
        val array = text.trim().parseArray() ?: return ParsedIndex(CompatEcosystem.LNREADER)
        val entries = array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = obj.stringOr("id")
            val name = obj.stringOr("name")
            if (id.isBlank() && name.isBlank()) return@mapNotNull null
            val codeUrl = obj.stringOr("url", "sourceUrl", "codeUrl")
            CompatRepoEntry(
                key = id.ifBlank { name },
                repoId = repoId,
                name = name.ifBlank { id },
                ecosystem = CompatEcosystem.LNREADER,
                version = obj.stringOr("version"),
                versionCode = obj.longOf("versionCode") ?: 0L,
                language = obj.stringOr("lang", "language"),
                baseUrl = obj.stringOr("site", "source").trimEnd('/'),
                payloadUrl = absolutize(indexUrl, codeUrl),
                iconUrl = obj.stringOr("iconUrl", "icon").ifBlank { null },
                contentType = if (obj.boolOr("isManga")) "manga" else "",
                nsfw = obj.boolOr("nsfw") || obj.boolOr("contentWarning"),
                description = obj.stringOr("description"),
                payloadKind = CompatPayloadKind.JS,
            )
        }.distinctByKey(CompatRepoEntry::key)
        return ParsedIndex(
            ecosystem = CompatEcosystem.LNREADER,
            repoName = indexUrl.substringAfterLast('/').ifBlank { hostOf(indexUrl) },
            entries = entries,
        )
    }

    // ── Mangayomi ────────────────────────────────────────────────────────────

    /**
     * `[{"name":"…","id":756554088,"baseUrl":"https://…","lang":"ar","typeSource":"single",
     *    "sourceCodeUrl":"https://…/x.js","itemType":2,"isManga":false,
     *    "sourceCodeLanguage":1,"version":"0.0.1","isNsfw":false,"appMinVerReq":"0.5.0"}]`
     *
     * `itemType`: 0 = manga, 1 = anime, 2 = novel. `sourceCodeLanguage`: 0 = Dart, 1 = JS.
     */
    fun parseMangayomi(repoId: String, indexUrl: String, text: String): ParsedIndex {
        val trimmed = text.trim()
        val array: List<JsonObject> = if (trimmed.startsWith("[")) {
            trimmed.parseArray().orEmpty().filterIsInstance<JsonObject>()
        } else {
            trimmed.parseObject().arrayOfObjects("sources")
        }
        val entries = array.mapNotNull { obj ->
            val name = obj.stringOr("name")
            val baseUrl = obj.stringOr("baseUrl").trimEnd('/')
            if (name.isBlank() && baseUrl.isBlank()) return@mapNotNull null
            val id = obj.stringOr("id")
            val codeUrl = obj.stringOr("sourceCodeUrl", "jsCodeUrl", "dartCodeUrl")
            val isDart = codeUrl.endsWith(".dart", ignoreCase = true) ||
                obj.stringOr("dartCodeUrl").isNotBlank()
            CompatRepoEntry(
                key = id.ifBlank { name.ifBlank { baseUrl } },
                repoId = repoId,
                name = name.ifBlank { hostOf(baseUrl) },
                ecosystem = CompatEcosystem.MANGAYOMI,
                version = obj.stringOr("version", "versionLast"),
                versionCode = obj.longOf("versionCode") ?: 0L,
                language = obj.stringOr("lang", "language"),
                baseUrl = baseUrl,
                payloadUrl = absolutize(indexUrl, codeUrl),
                iconUrl = obj.stringOr("iconUrl").ifBlank { null },
                contentType = when {
                    obj.longOf("itemType") == 1L -> "video"
                    obj.longOf("itemType") == 0L || obj.boolOr("isManga") -> "manga"
                    else -> ""
                },
                nsfw = obj.boolOr("isNsfw") || obj.boolOr("nsfw"),
                description = obj.stringOr("notes").ifBlank { obj.stringOr("typeSource") },
                payloadKind = if (isDart) CompatPayloadKind.DART else CompatPayloadKind.JS,
                extra = meta(
                    "appMinVerReq" to obj.stringOr("appMinVerReq").ifBlank { null },
                    "typeSource" to obj.stringOr("typeSource").ifBlank { null },
                    "cloudflare" to if (obj.boolOr("hasCloudflare")) "true" else null,
                ),
            )
        }.distinctByKey(CompatRepoEntry::key)

        return ParsedIndex(
            ecosystem = CompatEcosystem.MANGAYOMI,
            repoName = indexUrl.substringAfterLast('/').ifBlank { hostOf(indexUrl) },
            entries = entries,
        )
    }

    // ── CloudStream ──────────────────────────────────────────────────────────

    /**
     * `repo.json` = `{"name":"…","pluginLists":[{"name":"v1.0","url":"…/index.min.json"}]}`.
     * Nested lists are returned as [ParsedIndex.nestedIndexUrls] — the manager
     * [CompatRepositoryManager] loads them (one extra request per repository).
     */
    fun parseCloudStream(repoId: String, indexUrl: String, text: String): ParsedIndex {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            val root = trimmed.parseObject()
            val lists = root.arrayOfObjects("pluginLists")
            if (lists.isNotEmpty()) {
                return ParsedIndex(
                    ecosystem = CompatEcosystem.CLOUDSTREAM,
                    repoName = root.stringOr("name").ifBlank { hostOf(indexUrl) },
                    nestedIndexUrls = lists.map { absolutize(indexUrl, it.stringOr("url")) }
                        .filter { it.isNotBlank() }
                        .distinct(),
                )
            }
        }
        return ParsedIndex(
            ecosystem = CompatEcosystem.CLOUDSTREAM,
            repoName = hostOf(indexUrl),
            entries = parseCloudStreamList(repoId, indexUrl, text),
        )
    }

    /** An already expanded plugin list: `[{name,version,url,iconUrl,description,authors[]}]`. */
    fun parseCloudStreamList(repoId: String, indexUrl: String, text: String): List<CompatRepoEntry> {
        val trimmed = text.trim()
        val array: List<JsonElement> = if (trimmed.startsWith("[")) {
            trimmed.parseArray().orEmpty()
        } else {
            trimmed.parseObject().arrayOfObjects("plugins")
        }
        return array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = obj.stringOr("name")
            val url = absolutize(indexUrl, obj.stringOr("url", "apkUrl"))
            if (name.isBlank() && url.isBlank()) return@mapNotNull null
            val authors = obj["authors"] as? JsonArray
            CompatRepoEntry(
                key = obj.stringOr("pkg").ifBlank { url.ifBlank { name } },
                repoId = repoId,
                name = name.ifBlank { hostOf(url) },
                ecosystem = CompatEcosystem.CLOUDSTREAM,
                version = obj.stringOr("version", "versionName"),
                versionCode = obj.longOf("versionCode") ?: 0L,
                language = obj.stringOr("language", "lang"),
                payloadUrl = url,
                iconUrl = obj.stringOr("iconUrl").ifBlank { null },
                contentType = "video",
                nsfw = obj.boolOr("nsfw"),
                description = authors
                    ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    .orEmpty()
                    .joinToString(", ")
                    .ifBlank { obj.stringOr("description") },
                payloadKind = CompatPayloadKind.fromUrlOrName(url.ifBlank { "plugin.cs3" }),
            )
        }.distinctByKey(CompatRepoEntry::key)
    }

    // ── Mihon v2 `repo.json` (a pointer, not an index) ────────────────────────

    /**
     * The `repo.json` of a second-generation Mihon repository is not a source list,
     * it is a pointer:
     * ```json
     * {"index_v2":"https://…/index.pb","meta":{"name":"Keiyoushi",
     *  "website":"https://keiyoushi.github.io","signingKeyFingerprint":"9add…"}}
     * ```
     * `index_v2` points at protobuf, which we cannot read — but next to it there is
     * always a JSON variant of the same index, which is why it ranks first.
     */
    data class RepoPointer(
        val repoName: String,
        val signerFingerprint: String,
        /** Index candidates in attempt order (absolute or relative). */
        val indexUrls: List<String>,
    )

    fun parseRepoPointer(text: String): RepoPointer? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) return null
        val root = trimmed.parseObject() ?: return null
        val pointer = root.stringOr("index_v2", "indexUrl", "index")
        val meta = root["meta"].asObject()
        val urls = ArrayList<String>(2)
        if (pointer.isNotBlank()) {
            if (pointer.endsWith(".pb", ignoreCase = true)) {
                urls += pointer.dropLast(3) + ".json"
            }
            urls += pointer
        } else if (root.stringOr("name").isNotBlank() && meta != null) {
            // `repo.json` without a pointer: the repository sits next to index.json
            urls += "index.json"
            urls += "index.min.json"
        } else {
            return null
        }
        return RepoPointer(
            repoName = root.stringOr("name").ifBlank { meta.stringOr("name") },
            signerFingerprint = root.stringOr("signingKey")
                .ifBlank { meta.stringOr("signingKeyFingerprint", "signingKey") },
            indexUrls = urls,
        )
    }

    // ── TREE: a repository with no index, sources as separate files ────────────

    data class TreeFile(val path: String, val size: Long = 0)

    /**
     * `jiwangyihao/source-j-legado` keeps every bookSource in its own file
     * (`bilinovel.json`, `masiro.json`, …), there is no index file — so the file
     * itself is the source and its payload is downloaded from the raw URL.
     */
    fun parseTree(
        repoId: String,
        rawBase: String,
        files: List<TreeFile>,
        ecosystem: CompatEcosystem?,
    ): List<CompatRepoEntry> {
        val base = rawBase.trimEnd('/')
        return files.asSequence()
            .filter { it.size in 1..MAX_TREE_FILE_BYTES }
            .filter { !isTreeJunk(it.path) }
            .mapNotNull { file ->
                val kind = when (file.path.substringAfterLast('.', "").lowercase()) {
                    "json" -> CompatPayloadKind.JSON
                    "js" -> CompatPayloadKind.JS
                    "dart" -> CompatPayloadKind.DART
                    else -> return@mapNotNull null
                }
                val eco = ecosystem ?: when (kind) {
                    CompatPayloadKind.DART -> CompatEcosystem.MANGAYOMI
                    CompatPayloadKind.JS -> CompatEcosystem.LNREADER
                    else -> CompatEcosystem.LEGADO
                }
                CompatRepoEntry(
                    key = file.path,
                    repoId = repoId,
                    name = file.path.substringAfterLast('/')
                        .substringBeforeLast('.')
                        .replace('-', ' ')
                        .replace('_', ' ')
                        .ifBlank { file.path },
                    ecosystem = eco,
                    payloadUrl = "$base/${file.path.trimStart('/')}",
                    payloadKind = kind,
                    size = file.size,
                )
            }
            .sortedBy { it.key }
            .take(TREE_ENTRY_LIMIT)
            .toList()
    }

    private fun isTreeJunk(path: String): Boolean {
        val p = path.lowercase()
        if (TREE_SKIP.any { p.contains(it) }) return true
        // The index was already looked for separately — a tree scan skips it
        return INDEX_FILE_NAMES.any { p.endsWith(it) }
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    /** `https://host/dir/index.json` + `x.apk` → `https://host/dir/x.apk`. */
    fun absolutize(indexUrl: String, relative: String): String {
        val rel = relative.trim()
        if (rel.isEmpty()) return ""
        if (rel.startsWith("http://") || rel.startsWith("https://")) return rel
        if (rel.startsWith("//")) return "https:$rel"
        val scheme = indexUrl.substringBefore("://", "")
        val host = indexUrl.substringAfter("://", "").substringBefore('/')
        if (scheme.isEmpty() || host.isEmpty()) return rel
        if (rel.startsWith("/")) return "$scheme://$host$rel"
        val dir = indexUrl.substringBeforeLast('/', "")
        return if (dir.isBlank() || !dir.contains("://")) {
            "$scheme://$host/$rel"
        } else {
            "$dir/$rel"
        }
    }

    private fun List<JsonObject>.sourceNames(): String =
        mapNotNull { it.stringOr("name", "language", "lang").ifBlank { null } }
            .distinct()
            .joinToString(", ")
            .take(200)

    private fun hostOf(url: String): String =
        url.removePrefix("https://").removePrefix("http://").substringBefore('/')

    private fun String.parseObject(): JsonObject? =
        runCatching { json.parseToJsonElement(this) as? JsonObject }.getOrNull()

    private fun String.parseArray(): List<JsonElement>? =
        runCatching { (json.parseToJsonElement(this) as? JsonArray)?.toList() }.getOrNull()

    private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

    private fun JsonObject?.arrayOfObjects(key: String): List<JsonObject> =
        ((this?.get(key)) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    /**
     * Value of the first non-empty field. Strings, numbers and booleans are all
     * converted to text: repositories write `"version": 26` and `"version": "26"`
     * in different indexes, and we only need the text.
     */
    private fun JsonObject?.stringOr(vararg keys: String): String {
        val self = this ?: return ""
        for (k in keys) {
            val v = (self[k] as? JsonPrimitive)?.contentOrNull?.trim()
            if (!v.isNullOrEmpty()) return v
        }
        return ""
    }

    private fun JsonObject?.longOf(vararg keys: String): Long? {
        val self = this ?: return null
        for (k in keys) {
            val p = self[k] as? JsonPrimitive ?: continue
            p.longOrNull?.let { return it }
            p.contentOrNull?.trim()?.toLongOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject?.boolOr(key: String): Boolean {
        val p = this?.get(key) as? JsonPrimitive ?: return false
        p.booleanOrNull?.let { return it }
        return when (p.contentOrNull?.trim()?.lowercase()) {
            "true", "1", "yes" -> true
            else -> false
        }
    }
}

/** Helper for `extra`: null/empty values never enter the map. */
internal fun meta(vararg pairs: Pair<String, String?>): Map<String, String> =
    pairs.mapNotNull { (k, v) -> if (v.isNullOrBlank()) null else k to v }.toMap()

/** Drop duplicate keys, keeping the original order. */
internal fun <T> List<T>.distinctByKey(selector: (T) -> String): List<T> {
    val seen = HashSet<String>(size)
    val out = ArrayList<T>(size)
    for (item in this) {
        if (seen.add(selector(item))) out += item
    }
    return out
}

/**
 * Top-level JSON array → substrings of its elements. The scan respects quotes and
 * nesting so that one bookSource can be cut out verbatim.
 *
 * For a wrapper object we look for the first array under one of the
 * `bookSources`/`data`/`sources`/`list` keys. null when there is no array.
 */
internal fun topLevelArrayElements(text: String): List<String>? {
    val trimmed = text.trim()
    val openBracket = when {
        trimmed.startsWith("[") -> 0
        trimmed.startsWith("{") -> {
            val keyIdx = listOf("bookSources", "data", "sources", "list")
                .mapNotNull { trimmed.indexOf("\"$it\"") }
                .minOrNull()
                ?: return null
            trimmed.indexOf('[', keyIdx).takeIf { it >= 0 } ?: return null
        }

        else -> return null
    }
    return splitArrayElements(trimmed, openBracket)
}

private fun splitArrayElements(text: String, openBracket: Int): List<String>? {
    val out = ArrayList<String>()
    var depth = 0
    var inString = false
    var escaped = false
    var elemStart = -1
    var i = openBracket
    while (i < text.length) {
        val c = text[i]
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
            i++
            continue
        }
        if (depth >= 1 && elemStart < 0 && !c.isWhitespace() && c != ',' && c != ']' && c != '}') {
            elemStart = i
        }
        when (c) {
            '"' -> inString = true
            '[', '{' -> depth++
            ']', '}' -> {
                depth--
                if (depth <= 0) {
                    if (c == ']' && elemStart >= 0 && elemStart < i) out += text.substring(elemStart, i)
                    return out.ifEmpty { null }
                }
            }

            ',' -> if (depth == 1 && elemStart >= 0) {
                out += text.substring(elemStart, i)
                elemStart = -1
            }
        }
        i++
    }
    return out.ifEmpty { null }
}
