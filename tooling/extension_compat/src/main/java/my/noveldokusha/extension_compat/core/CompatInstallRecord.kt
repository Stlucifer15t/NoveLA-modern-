package my.noveldokusha.extension_compat.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Запись об установленном compat-расширении. Хранится в registry.json
 * в filesDir/compat/. Описывает ОДИН установленный артефакт (файл/пакет),
 * который может порождать один или несколько источников.
 */
@Serializable
data class CompatInstallRecord(
    /** Уникальный id: "<ecosystem>:<источник id или имя файла>". */
    val id: String,
    val ecosystem: CompatEcosystem,
    /** Отображаемое имя (из манифеста/индекса/JSON). */
    val name: String,
    val version: String = "",
    /** ISO 639-1 код языка или特殊ные значения ("zh", "en", "MULTILANGUAGE"...). */
    val language: String = "",
    /** Базовый URL источника, если известен (для matching'а URL книг). */
    val baseUrl: String = "",
    /** Тип контента: "" (новелла), "manga", "video". */
    val contentType: String = "",
    /** Относительный путь файла внутри compat storage. */
    val payloadPath: String,
    /** URL, с которого установлен (для обновлений). */
    val sourceUrl: String = "",
    /** Иконка URL, если есть. */
    val iconUrl: String = "",
    /** Включён ли источник. */
    val enabled: Boolean = true,
    /** SHA-256 подписи APK (Tachiyomi/Aniyomi) — пиннинг при обновлении. */
    val signerSha256: String? = null,
    /** Версия code/number для проверки обновлений и downgrade. */
    val versionCode: Long = 0,
    /** Метаданные репозитория (nsfw, группы, pkg name...) как есть. */
    val extra: JsonObject? = null,
    /** Момент установки (unix millis). */
    val installedAt: Long = 0,
)

/**
 * Описание загруженного источника для UI (список в ExtensionsScreen).
 */
data class CompatSourceInfo(
    val record: CompatInstallRecord,
    /** Количество источников, созданных из record (Tachiyomi SourceFactory может давать несколько). */
    val loadedSources: Int,
    /** Ошибка загрузки, если источник не поднялся. */
    val loadError: String? = null,
    /** Объявленные возможности. */
    val capabilities: Set<CompatCapability> = emptySet(),
)
