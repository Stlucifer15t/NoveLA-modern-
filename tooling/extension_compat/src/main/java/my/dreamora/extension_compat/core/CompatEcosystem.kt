package my.dreamora.extension_compat.core

/**
 * Extension ecosystems supported by the compat layer.
 * Every ecosystem has its own storage format, runtime and API surface,
 * which an adapter translates into [my.dreamora.scraper.SourceInterface].
 */
enum class CompatEcosystem(
    /** Stable identifier for storage/DI/UI. */
    val id: String,
    /** Human-readable name for UI and diagnostics. */
    val displayName: String,
    /** File extensions typical for this ecosystem. */
    val fileExtensions: List<String>,
) {
    TACHIYOMI(
        id = "tachiyomi",
        displayName = "Mihon/Tachiyomi",
        fileExtensions = listOf("apk"),
    ),
    ANIYOMI(
        id = "aniyomi",
        displayName = "Aniyomi",
        fileExtensions = listOf("apk"),
    ),
    LEGADO(
        id = "legado",
        displayName = "Legado",
        fileExtensions = listOf("json"),
    ),
    LNREADER(
        id = "lnreader",
        displayName = "LNReader",
        fileExtensions = listOf("js"),
    ),
    MANGAYOMI(
        id = "mangayomi",
        displayName = "Mangayomi",
        fileExtensions = listOf("js"),
    ),
    CLOUDSTREAM(
        id = "cloudstream",
        displayName = "CloudStream",
        fileExtensions = listOf("cs3"),
    ),
    ;

    companion object {
        fun fromId(id: String): CompatEcosystem? = entries.find { it.id == id }
    }
}

/**
 * Explicit source capabilities. A source declares only what it really supports —
 * a manga source is not obliged to implement the video API and vice versa
 * (see SourceInterface.getVideoList: null = "not a video source").
 */
enum class CompatCapability {
    /** "Popular" catalog. */
    POPULAR,
    /** "Latest" catalog. */
    LATEST,
    /** Search. */
    SEARCH,
    /** Catalog filters. */
    FILTERS,
    /** Book/manga details. */
    DETAILS,
    /** Chapter list. */
    CHAPTERS,
    /** Chapter text (novel). */
    CHAPTER_TEXT,
    /** Page list (manga). */
    PAGES,
    /** Video streams (episodes). */
    VIDEO,
    /** User-facing source preferences. */
    PREFERENCES,
}
