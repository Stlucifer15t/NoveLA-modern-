package my.noveldokusha.extension_compat.core

/**
 * Экосистемы расширений, поддерживаемые compat-слоем.
 * Каждая экосистема имеет свой формат хранения, рантайм и набор API,
 * который адаптер переводит в [my.noveldokusha.scraper.SourceInterface].
 */
enum class CompatEcosystem(
    /** Стабильный идентификатор для storage/DI/UI. */
    val id: String,
    /** Человекочитаемое имя для UI и диагностики. */
    val displayName: String,
    /** MIME/расширения файлов, характерные для этой экосистемы. */
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
 * Явные возможности источника. Источник объявляет только то, что реально умеает —
 * manga-источник не обязан реализовывать видео-API и наоборот
 * (см. SourceInterface.getVideoList: null = «не видео-источник»).
 */
enum class CompatCapability {
    /** Каталог «популярное». */
    POPULAR,
    /** Каталог «последнее». */
    LATEST,
    /** Поиск. */
    SEARCH,
    /** Фильтры каталога. */
    FILTERS,
    /** Детали книги/манги. */
    DETAILS,
    /** Список глав. */
    CHAPTERS,
    /** Текст главы (новелла). */
    CHAPTER_TEXT,
    /** Список страниц (манга). */
    PAGES,
    /** Видеопотоки (эпизоды). */
    VIDEO,
    /** Пользовательские настройки источника. */
    PREFERENCES,
}
