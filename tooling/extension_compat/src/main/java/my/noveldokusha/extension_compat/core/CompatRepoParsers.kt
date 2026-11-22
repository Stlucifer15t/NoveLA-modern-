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
 * Разбор индекс-файлов чужих репозиториев в единый список [CompatRepoEntry].
 *
 * Форматы выверены по живым репозиториям (2026-11):
 *
 * | Экосистема | Индекс | Форма |
 * |---|---|---|
 * | Mihon/keiyoushi (v2) | `repo.json` → `index.json` | `{name, signingKey, extensionList:{extensions:[…]}}` |
 * | Mihon (v1, legacy) | `index.min.json` | `[{name,pkg,apk,code,version,nsfw,sources:[…]}]` |
 * | Legado | `bookSource.json` / `.txt` | массив bookSource'ов, массив URL, или построчный список URL |
 * | LNReader | `.dist/plugins.min.json` | `[{id,name,site,lang,version,url,iconUrl}]` |
 * | Mangayomi | `index.json`, `novel_index.json` | `[{name,id,baseUrl,sourceCodeUrl,itemType,sourceCodeLanguage,…}]` |
 * | CloudStream | `repo.json` | `{name, pluginLists:[{name,url}]}` → вложенные списки `.cs3` |
 *
 * Здесь нет сети и нет Android: чистый текст → модели, поэтому каждый формат
 * закрыт юнит-тестом `CompatRepoParsersTest`.
 */
object CompatRepoParsers {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Сколько записей максимум отдаём из TREE-репозитория. */
    const val TREE_ENTRY_LIMIT = 400

    /** Один bookSource-файл не бывает гигантским; больше — это подборка или мусор. */
    private const val MAX_TREE_FILE_BYTES = 4_000_000L

    /** Кандидаты индекс-файлов, в порядке предпочтения (v2 раньше v1!). */
    val INDEX_FILE_NAMES = listOf(
        "repo.json",          // Mihon v2-указатель / CloudStream / Mangayomi
        "index.json",         // Mihon v2 (полный), Mangayomi
        "novel_index.json",   // Mangayomi — новеллы
        "manga_index.json",
        "index.min.json",     // Mihon v1 / CloudStream
        "plugins.min.json",   // LNReader
        "plugins.json",
        "bookSource.json",    // Legado-подборки
        "bookSourceNew.json",
        "booksource.json",
        "sources.json",
        "source.json",
        "sources.txt",
        "bookSource.txt",
        "index.txt",
    )

    /** Файлы, которые точно не источники (мусор TREE-скана). */
    private val TREE_SKIP = listOf(
        "package.json", "package-lock", "tsconfig", "renovate", "crowdin",
        "i18n", "locales", "/res/", "/build/", "/gradle", ".github/", ".gitlab",
        "node_modules", "readme", "changelog", "license", "icon",
    )

    /** Ключи, под которыми экспорт Legado прячет массив bookSource'ов. */
    private val LEGADO_WRAPPER_KEYS = listOf("bookSources", "data", "sources", "list")

    data class ParsedIndex(
        val ecosystem: CompatEcosystem,
        val repoName: String = "",
        val signerFingerprint: String = "",
        val entries: List<CompatRepoEntry> = emptyList(),
        /** CloudStream: репозиторий только указывает на вложенные списки плагинов. */
        val nestedIndexUrls: List<String> = emptyList(),
    )

    // ── Определение формата ────────────────────────────────────────────────

    /**
     * Формат по содержимому; порядок важен — сначала самые специфичные маркеры.
     * null = не похоже ни на один известный индекс (тогда вызывающий пробует
     * следующий кандидат из [INDEX_FILE_NAMES]).
     */
    fun sniff(text: String, url: String = ""): CompatEcosystem? {
        val head = text.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        if (head.isBlank()) return null

        if (!head.startsWith("{") && !head.startsWith("[")) {
            // Не JSON: может быть подпиской «один URL в строке» (Legado .txt)
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

        // Содержимое невнятное — последнее средство: имя файла
        return when {
            url.endsWith("plugins.min.json") || url.endsWith("plugins.json") -> CompatEcosystem.LNREADER
            url.endsWith("novel_index.json") || url.contains("mangayomi") -> CompatEcosystem.MANGAYOMI
            url.contains("booksource", ignoreCase = true) -> CompatEcosystem.LEGADO
            else -> null
        }
    }

    /** Определить формат и разобрать. null — если формат не распознан. */
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
     * v2 (нынешний keiyoushi):
     * ```json
     * {"name":"Keiyoushi","signingKey":"9add…","extensionList":{"extensions":[
     *   {"name":"AchyManga","packageName":"eu.kanade…achymanga",
     *    "resources":{"apkUrl":"https://…/x.apk","iconUrl":"…"},
     *    "versionCode":"106001","versionName":"1.6.1","contentWarning":"CONTENT_WARNING_MIXED",
     *    "sources":[{"id":"…","name":"…","language":"es","homeUrl":"https://…"}]}]}}
     * ```
     * v1 (legacy): плоский массив, где `apk` — имя файла рядом с индексом.
     *
     * Одна запись = один APK: payload качается один раз, а несколько источников
     * внутри пакета загрузчик создаёт сам ([CompatExtensionLoader.load] возвращает
     * список). Чем пакет «многоязычнее», тем больше строк в его описании.
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
     * Mihon-репозиторий кладёт в `index.min.json` заглушку для устаревших
     * клиентов («Outdated App», «Update to Mihon 0.20.1+»). Это не источник.
     */
    private fun isOutdatedStub(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("outdated") || n.contains("update to") || n.contains("please update")
    }

    // ── Legado ───────────────────────────────────────────────────────────────

    /**
     * BookSource-подборка. Формы из живых репозиториев:
     *  - массив bookSource-объектов (`entr0pia/MyLegadoSource` → `bookSource.json`);
     *  - обёртка `{bookSources:[…]}` / `{data:[…]}` (экспорт приложения);
     *  - одиночный bookSource-объект;
     *  - массив URL-строк или построчный `.txt` — тогда payload качаем отдельно.
     *
     * Инлайновые объекты не перекодируем: при установке берем их исходный текст
     * из кэша индекса ([extractSourceJson]), чтобы не потерять поля, которых нет
     * в нашей модели.
     */
    fun parseLegado(repoId: String, indexUrl: String, text: String): ParsedIndex {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ParsedIndex(CompatEcosystem.LEGADO)
        val repoName = indexUrl.substringAfterLast('/').ifBlank { hostOf(indexUrl) }

        // a) построчный список URL
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

        // b) массив URL-строк
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
            // Без правил поиска источник нерабочий: Legado подставляет дефолты сам,
            // но мы угадывать не будем — такой источник только засоряет список.
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

    /** bookSource-объекты инлайном в индексе (массив, обёртка или одиночный объект). */
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
     * Исходный JSON одного bookSource из текста кэшированного индекса — по
     * [CompatRepoEntry.inlineKey] (bookSourceUrl, реже bookSourceName).
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

    /** Текст bookSource-объекта, если его bookSourceUrl/bookSourceName == [key]. */
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
     * `itemType`: 0 = манга, 1 = аниме, 2 = роман. `sourceCodeLanguage`: 0 = Dart, 1 = JS.
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
     * Вложенные списки отдаём как [ParsedIndex.nestedIndexUrls] — их догружает
     * [CompatRepositoryManager] (одна лишняя сетевая операция на репозиторий).
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

    /** Уже раскрытый список плагинов: `[{name,version,url,iconUrl,description,authors[]}]`. */
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

    // ── Mihon v2 `repo.json` (указатель, а не индекс) ────────────────────────

    /**
     * `repo.json` Mihon-репозитория второго поколения — это не список источников,
     * а указатель:
     * ```json
     * {"index_v2":"https://…/index.pb","meta":{"name":"Keiyoushi",
     *  "website":"https://keiyoushi.github.io","signingKeyFingerprint":"9add…"}}
     * ```
     * `index_v2` ссылается на protobuf, который мы не читаем, — но рядом всегда
     * лежит JSON-вариант того же индекса, на него и ставим первым кандидатом.
     */
    data class RepoPointer(
        val repoName: String,
        val signerFingerprint: String,
        /** Кандидаты индекса в порядке попытки (абсолютные или относительные). */
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
            // `repo.json` без указателя: репозиторий лежит рядом с index.json
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

    // ── TREE: репозиторий без индекса, источники — отдельные файлы ───────────

    data class TreeFile(val path: String, val size: Long = 0)

    /**
     * `jiwangyihao/source-j-legado` хранит каждый bookSource своим файлом
     * (`bilinovel.json`, `masiro.json`, …), индекс-файла там нет. Значит
     * источником файла является сам файл, а его payload качается по raw-URL.
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
        // Индекс мы уже искали отдельно — в дереве он не нужен
        return INDEX_FILE_NAMES.any { p.endsWith(it) }
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

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
     * Значение первого непустого поля. Строка, число и boolean приводятся к
     * строке: репозитории пишут `"version": 26` и `"version": "26"` в разных
     * индексах, а нам нужен только текст.
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

/** Помощь для `extra`: null/пустые значения в карту не попадают. */
internal fun meta(vararg pairs: Pair<String, String?>): Map<String, String> =
    pairs.mapNotNull { (k, v) -> if (v.isNullOrBlank()) null else k to v }.toMap()

/** Убрать дубликаты по ключу, сохранив порядок. */
internal fun <T> List<T>.distinctByKey(selector: (T) -> String): List<T> {
    val seen = HashSet<String>(size)
    val out = ArrayList<T>(size)
    for (item in this) {
        if (seen.add(selector(item))) out += item
    }
    return out
}

/**
 * Верхний JSON-массив → подстроки его элементов. Обход с учётом кавычек и
 * вложенности, чтобы вырезать один bookSource дословно.
 *
 * Для объекта-обёртки ищем первый массив под одним из ключей
 * `bookSources`/`data`/`sources`/`list`. null — если списка нет.
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
