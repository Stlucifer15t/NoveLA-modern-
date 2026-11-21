package my.noveldokusha.extension_compat.legado

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatException
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.SourceInterface
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.nio.charset.Charsets

/**
 * Загрузчик Legado bookSource: payload — JSON одного bookSource или массива.
 * Один record может содержать массив — создаётся по одному источнику на bookSource.
 */
@Singleton
class LegadoExtensionLoader @Inject constructor(
    private val networkClient: NetworkClient,
) : CompatExtensionLoader {

    override val ecosystem = CompatEcosystem.LEGADO

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        val text = payload.toString(Charsets.UTF_8)
        val sources = parseBookSources(text)
        if (sources.isEmpty()) {
            throw CompatException("No bookSource found in payload for ${record.id}", operation = "load")
        }
        // record описывает установку; id источника — из bookSourceUrl/имени
        return sources.mapIndexed { index, bs ->
            val srcId = if (sources.size == 1) record.id
            else "${record.id}#${bs.bookSourceUrl.ifBlank { bs.bookSourceName }#$index}"
            LegadoSourceAdapter(
                bookSource = bs,
                networkClient = networkClient,
                id = srcId,
            )
        }
    }

    fun parseBookSources(text: String): List<LegadoBookSource> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        return when (trimmed.first()) {
            '[' -> try {
                json.decodeFromString(ListSerializer(LegadoBookSource.serializer()), trimmed)
            } catch (e: Exception) {
                // Массив с мусором/объектами-обёртками — пробуем поэлементно
                parseLooseArray(trimmed)
            }
            '{' -> listOf(
                try {
                    json.decodeFromString(LegadoBookSource.serializer(), trimmed)
                } catch (e: Exception) {
                    // Возможно, это обёртка {"data":[...]} / {"bookSources":[...]}
                    parseLooseObject(trimmed) ?: throw CompatException(
                        "Invalid bookSource JSON: ${e.message}", operation = "load",
                    )
                },
            )
            else -> emptyList()
        }
    }

    private fun parseLooseArray(text: String): List<LegadoBookSource> {
        return try {
            val arr = json.parseToJsonElement(text)
            (arr as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { el ->
                    try {
                        json.decodeFromJsonElement(LegadoBookSource.serializer(), el)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLooseObject(text: String): LegadoBookSource? {
        return try {
            val obj = json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
                ?: return null
            for (key in listOf("bookSources", "data", "sources")) {
                val inner = obj[key] as? kotlinx.serialization.json.JsonArray ?: continue
                val parsed = inner.mapNotNull {
                    try {
                        json.decodeFromJsonElement(LegadoBookSource.serializer(), it)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (parsed.isNotEmpty()) return parsed.first()
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
