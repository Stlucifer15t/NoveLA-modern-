package my.noveldokusha.extension_compat.core

/**
 * Определение экосистемы расширения по URL репозитория/файла и, если нужно,
 * по содержимому. Детекция работает ДО скачивания: по индексам и расширению;
 * содержимое используется для одиночных файлов (bookSource JSON, JS-плагин).
 *
 * Правила (порядок важен — от более специфичных к общим):
 *
 * 1. Индексы репозиториев:
 *    - URL содержит `index.min.json` + `apk`/`sources` в ожидаемом формате
 *      → TACHIYOMI (keiyoushi) или ANIYOMI (aniyomi, пакет `...animeextension...`)
 *    - `plugins.min.json` (массив {id,name,site,lang,version,url,iconUrl})
 *      → LNREADER
 *    - `index.json`/`novel_index.json` с полями baseUrl/typeSource/itemType
 *      → MANGAYOMI
 *    - `repo.json` с {name, pluginLists[]} или `plugins.json` с plugins[]
 *      → CLOUDSTREAM
 * 2. Одиночные файлы по содержимому:
 *    - JSON с bookSourceName/ruleSearch/ruleBookInfo → LEGADO
 *    - JS с `mangayomiSources` / `extends MProvider` → MANGAYOMI
 *    - JS с `popularNovels`/`parseNovel`/`exports.default` → LNREADER
 *    - .cs3 (zip: classes.dex + manifest.json) → CLOUDSTREAM
 *    - .apk с meta tachiyomi.extension → TACHIYOMI/ANIYOMI (по имени пакета)
 * 3. YAML → существующий Lua-путь (НЕ compat; сюда не попадает).
 */
object FormatDetector {

    sealed interface Detection {
        data class Detected(val ecosystem: CompatEcosystem) : Detection
        data object Unknown : Detection
        /** Формат требует уточнения у пользователя (например, .apk без метаданных). */
        data class Ambiguous(val candidates: List<CompatEcosystem>) : Detection
    }

    /** Детекция по URL (индекс репозитория или прямая ссылка на файл). */
    fun detectFromUrl(url: String): Detection {
        val lower = url.lowercase()
        val path = lower.substringBefore('?').substringBefore('#')

        // ── Индексы репозиториев ────────────────────────────────────────────
        if (path.endsWith("index.min.json")) {
            // Aniyomi index содержит пакеты eu.kanade.tachiyomi.animeextension.*;
            // без контента не отличим от keiyoushi — обе индекс-формы совместимы,
            // окончательный выбор по содержимому при загрузке.
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

        // ── Одиночные файлы по расширению ───────────────────────────────────
        when {
            path.endsWith(".cs3") -> return Detection.Detected(CompatEcosystem.CLOUDSTREAM)
            path.endsWith(".apk") -> return Detection.Ambiguous(
                listOf(CompatEcosystem.TACHIYOMI, CompatEcosystem.ANIYOMI)
            )
            path.endsWith(".lua") -> return Detection.Unknown // Lua — вне compat-слоя
            path.endsWith(".json") -> {
                // bookSource или index — уточняется по содержимому
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
     * Детекция по содержимому файла (первые байты/строки).
     * @param fileName исходное имя файла (может быть null при вставке текста).
     * @param head первые ~4 КБ содержимого.
     */
    fun detectFromContent(fileName: String?, head: String): Detection {
        val name = fileName?.lowercase() ?: ""
        val content = head.trimStart('\uFEFF', ' ', '\n', '\r', '\t')

        // .cs3 — zip, бинарный; по имени
        if (name.endsWith(".cs3")) return Detection.Detected(CompatEcosystem.CLOUDSTREAM)

        // JSON-контент
        if (content.startsWith("{") || content.startsWith("[")) {
            // Legado bookSource
            if (content.contains("\"bookSourceName\"") || content.contains("\"ruleSearch\"") ||
                content.contains("\"ruleBookInfo\"") || content.contains("\"ruleToc\"")
            ) {
                return Detection.Detected(CompatEcosystem.LEGADO)
            }
            // Массив bookSource'ов тоже Legado
            if (content.contains("\"bookSourceUrl\"")) {
                return Detection.Detected(CompatEcosystem.LEGADO)
            }
            // LNReader index: массив объектов с id/site/lang/version/url
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
            // Короткий bookSource без типовых ключей выше — пробуем Legado по структуре
            if (content.contains("\"bookSourceUrl\"") || content.contains("\"bookSourceType\"")) {
                return Detection.Detected(CompatEcosystem.LEGADO)
            }
            return Detection.Unknown
        }

        // JS-контент
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
        // exports.default — общий для обоих, но LNReader-плагины чаще всего
        // объявляют class ... extends и экспортируют instance
        if (content.contains("exports.default") || content.contains("module.exports")) {
            return Detection.Ambiguous(listOf(CompatEcosystem.LNREADER, CompatEcosystem.MANGAYOMI))
        }

        return Detection.Unknown
    }

    /**
     * Уточнение TACHIYOMI vs ANIYOMI по индексу (пакет расширения).
     * @param packageName например `eu.kanade.tachiyomi.animeextension.en.foo`.
     */
    fun refineByPackageName(ecosystem: CompatEcosystem, packageName: String): CompatEcosystem {
        if (ecosystem != CompatEcosystem.TACHIYOMI) return ecosystem
        return if (packageName.contains("animeextension")) CompatEcosystem.ANIYOMI else CompatEcosystem.TACHIYOMI
    }
}
