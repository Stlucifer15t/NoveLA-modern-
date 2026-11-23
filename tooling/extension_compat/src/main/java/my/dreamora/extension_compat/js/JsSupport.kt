package my.dreamora.extension_compat.js

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** A JS string literal for [value]. A JSON string literal is also a valid JS string literal. */
internal fun jsString(value: String): String = JsonPrimitive(value).toString()

internal fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

internal fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonElement.asObjectOrEmpty(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())

internal fun JsonElement.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

/**
 * Epoch seconds from a plugin's date: "2024-01-02", an ISO instant, an offset date-time,
 * or a number of milliseconds (values above 10^10 are read as milliseconds).
 */
internal fun parseEpochSeconds(raw: String?): Long? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    text.toLongOrNull()?.let { number ->
        return if (number > 10_000_000_000L) number / 1000 else number
    }
    runCatching { return OffsetDateTime.parse(text).toEpochSecond() }
    runCatching { return Instant.parse(text).epochSecond }
    runCatching { return LocalDate.parse(text.take(10)).atStartOfDay(ZoneOffset.UTC).toEpochSecond() }
    return null
}

/**
 * Absolute URL for a path a plugin returned. Plugins hand back paths relative to their
 * site (`fiction/123`, `/novel/9`) or full URLs.
 */
internal fun absoluteUrl(base: String, link: String): String {
    val trimmed = link.trim()
    if (trimmed.isEmpty()) return base
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    val baseUri = runCatching { URI(base) }.getOrNull()
    if (trimmed.startsWith("//")) {
        return (baseUri?.scheme ?: "https") + ":" + trimmed
    }
    if (trimmed.startsWith("/")) {
        val origin = baseUri?.let { "${it.scheme}://${it.authority}" } ?: base.trimEnd('/')
        return origin + trimmed
    }
    return base.trimEnd('/') + "/" + trimmed
}

/** Thread-safe LRU map with a fixed capacity. */
internal class BoundedCache<K : Any, V : Any>(private val capacity: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > capacity
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }
}
