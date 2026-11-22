package my.noveldokusha.extension_compat.core

/**
 * Detects an extension's ecosystem from the repository/file URL and, if needed,
 * from the content. Detection runs BEFORE download: indexes by URL and extension;
 * content is used for single files (bookSource JSON, a JS plugin).
 *
 * Rules (order matters — most specific first):
 *
 * 1. Repository indexes:
 *    - URL contains `index.min.json` + `apk`/`sources` in the expected shape
 *      → TACHIYOMI (keiyoushi) or ANIYOMI (package `...animeextension...`)
 *    - `plugins.min.json` (array of {id,name,site,lang,version,url,iconUrl})
 *      → LNREADER
 *    - `index.json`/`novel_index.json` with baseUrl/typeSource/itemType fields
 *      → MANGAYOMI
 *    - `repo.json` with {name, pluginLists[]} or `plugins.json` with plugins[]
 *      → CLOUDSTREAM
 * 2. Single files, by content:
 *    - JSON with bookSourceName/ruleSearch/ruleBookInfo → LEGADO
 *    - JS with `mangayomiSources` / `extends MProvider` → MANGAYOMI
 *    - JS with `popularNovels`/`parseNovel`/`exports.default` → LNREADER
 *    - .cs3 (zip: classes.dex + manifest.json) → CLOUDSTREAM
 *    - .apk with tachiyomi.extension metadata → TACHIYOMI/ANIYOMI (by package name)
 * 3. YAML → the existing Lua path (NOT compat; never reaches this detector).
 */
object FormatDetector {

    sealed interface Detection {
        data class Detected(val ecosystem: CompatEcosystem) : Detection
        data object Unknown : Detection
        /** The format needs the user to clarify (e.g. a .apk without metadata). */
        data class Ambiguous(val candidates: List<CompatEcosystem>) : Detection
    }

    /** Detection by URL (a repository index or a direct file link). */
    fun detectFromUrl(url: String): Detection {
        val lower = url.lowercase()
        val path = lower.substringBefore('?').substringBefore('#')

        // ── Repository indexes ─────────────────────────────────────────────────────
        if (path.endsWith("index.min.json")) {
            // An Aniyomi index contains eu.kanade.tachiyomi.animeextension.* packages;
            // without the content it is indistinguishable from keiyoushi — both index
            // shapes are compatible, the final call happens on load, by content.
            return Detection.Detected(CompatEcosystem.TACHIYOMI)
        }
        if (path.endsWith("plugins.min.json") || path.endsWith("plugins.json") && path.contains("lnreader")) {
            return Detection.Detected(CompatEcosystem.LNREADER)
        }
        if (path.endsWith("novel_index.json") || path.endsWith("index.json") && path.contains("mangayomi")) {
            return Detection.Detected(CompatEcosystem.MANGAYOMI)
        }
        if (path.endsWith("repo.json")) {
            return Detection.Detected(CompatEcosystem.CLOUDSTREAM)
        }

        // ── Single files, by extension ──────────────────────────────────────────────
        when {
            path.endsWith(".cs3") -> return Detection.Detected(CompatEcosystem.CLOUDSTREAM)
            path.endsWith(".apk") -> return Detection.Ambiguous(
                listOf(CompatEcosystem.TACHIYOMI, CompatEcosystem.ANIYOMI)
            )
            path.endsWith(".lua") -> return Detection.Unknown // Lua is outside the compat layer
            path.endsWith(".json") -> {
                // bookSource or an index — decided by content
                return Detection.Ambiguous(
                    listOf(CompatEcosystem.LEGADO, CompatEcosystem.MANGAYOMI)
                )
            }
            path.endsWith(".js") -> {
                return Detection.Ambiguous(
                    listOf(CompatEcosystem.LNREADER, CompatEcosystem.MANGAYOMI)
                )
            }
        }
        return Detection.Unknown
    }

    /**
     * Detection by file content (the first bytes/lines).
     * @param fileName original file name (may be null when text is pasted in).
     * @param head the first ~4 KB of content.
     */
    fun detectFromContent(fileName: String?, head: String): Detection {
        val name = fileName?.lowercase() ?: ""
        val content = head.trimStart('\uFEFF', ' ', '\n', '\r', '\t')

        // .cs3 is a zip, binary — match by name
        if (name.endsWith(".cs3")) return Detection.Detected(CompatEcosystem.CLOUDSTREAM)

        // JSON content
        if (content.startsWith("{") || content.startsWith("[")) {
            // Legado bookSource
            if (content.contains("\"bookSourceName\"") || content.contains("\"ruleSearch\"") ||
                content.contains("\"ruleBookInfo\"") || content.contains("\"ruleToc\"")
            ) {
                return Detection.Detected(CompatEcosystem.LEGADO)
            }
            // An array of bookSources is Legado too
            if (content.contains("\"bookSourceUrl\"")) {
                return Detection.Detected(CompatEcosystem.LEGADO)
            }
            // LNReader index: an array of objects with id/site/lang/version/url
            if (name.endsWith("plugins.min.json") ||
                (content.contains("\"site\"") && content.contains("\"iconUrl\"") && content.contains("\"lang\""))
            ) {
                return Detection.Detected(CompatEcosystem.LNREADER)
            }
            // Mangayomi index: baseUrl + typeSource/itemType
            if (content.contains("\"typeSource\"") || content.contains("\"itemType\"") ||
                content.contains("\"isManga\"")
            ) {
                return Detection.Detected(CompatEcosystem.MANGAYOMI)
            }
            // CloudStream repo: pluginLists
            if (content.contains("\"pluginLists\"")) {
                return Detection.Detected(CompatEcosystem.CLOUDSTREAM)
            }
            // Tachiyomi index: apk + sources
            if (content.contains("\"apk\"") && content.contains("\"sources\"")) {
                return Detection.Detected(CompatEcosystem.TACHIYOMI)
            }
            // A short bookSource without the keys above — try Legado by structure
            if (content.contains("\"bookSourceUrl\"") || content.contains("\"bookSourceType\"")) {
                return Detection.Detected(CompatEcosystem.LEGADO)
            }
            return Detection.Unknown
        }

        // JS content
        if (content.startsWith("const mangayomiSources") || content.startsWith("var mangayomiSources") ||
            content.startsWith("let mangayomiSources") || content.contains("mangayomiSources =") ||
            content.contains("extends MProvider")
        ) {
            return Detection.Detected(CompatEcosystem.MANGAYOMI)
        }
        if (content.contains("popularNovels") || content.contains("parseNovel") ||
            content.contains("parseChapter") || content.contains("searchNovels")
        ) {
            return Detection.Detected(CompatEcosystem.LNREADER)
        }
        // exports.default is shared by both, but LNReader plugins usually
        // declare a class ... extends and export an instance
        if (content.contains("exports.default") || content.contains("module.exports")) {
            return Detection.Ambiguous(listOf(CompatEcosystem.LNREADER, CompatEcosystem.MANGAYOMI))
        }

        return Detection.Unknown
    }

    /**
     * Narrows TACHIYOMI vs ANIYOMI using the index (extension package name).
     * @param packageName e.g. `eu.kanade.tachiyomi.animeextension.en.foo`.
     */
    fun refineByPackageName(ecosystem: CompatEcosystem, packageName: String): CompatEcosystem {
        if (ecosystem != CompatEcosystem.TACHIYOMI) return ecosystem
        return if (packageName.contains("animeextension")) CompatEcosystem.ANIYOMI else CompatEcosystem.TACHIYOMI
    }
}
