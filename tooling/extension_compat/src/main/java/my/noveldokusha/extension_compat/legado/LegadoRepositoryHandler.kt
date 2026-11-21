package my.noveldokusha.extension_compat.legado

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.FormatDetector
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Репозиторий Legado — это просто URL/файл с JSON bookSource'ов
 * (одиночный объект или массив). Нет отдельного индекс-формата.
 */
@Serializable
data class LegadoRepositoryEntry(
    val bookSourceName: String = "",
    val bookSourceUrl: String = "",
)

object LegadoRepositoryHandler {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun detect(url: String, head: String? = null): Boolean {
        val byUrl = FormatDetector.detectFromUrl(url)
        if (byUrl is FormatDetector.Detection.Detected && byUrl.ecosystem == CompatEcosystem.LEGADO) return true
        if (head != null) {
            val byContent = FormatDetector.detectFromContent(null, head)
            if (byContent is FormatDetector.Detection.Detected && byContent.ecosystem == CompatEcosystem.LEGADO) return true
        }
        return false
    }

    /**
     * Извлечь метаданные для record'а из payload'а (для UI: имя/язык/baseUrl).
     * Не парсит правила — только верхнеуровневые поля.
     */
    fun describe(payload: ByteArray): List<LegadoRepositoryEntry> {
        val text = payload.toString(Charsets.UTF_8).trim()
        return try {
            when (text.first()) {
                '[' -> json.decodeFromString(ListSerializer(LegadoRepositoryEntry.serializer()), text)
                '{' -> listOf(json.decodeFromString(LegadoRepositoryEntry.serializer(), text))
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun installRecordFor(
        entry: LegadoRepositoryEntry,
        payloadPath: String,
        sourceUrl: String,
    ): CompatInstallRecord {
        val base = entry.bookSourceUrl.ifBlank { entry.bookSourceName }
        return CompatInstallRecord(
            id = "legado:${base.ifBlank { payloadPath }}",
            ecosystem = CompatEcosystem.LEGADO,
            name = entry.bookSourceName.ifBlank { base },
            baseUrl = entry.bookSourceUrl,
            payloadPath = payloadPath,
            sourceUrl = sourceUrl,
        )
    }
}
