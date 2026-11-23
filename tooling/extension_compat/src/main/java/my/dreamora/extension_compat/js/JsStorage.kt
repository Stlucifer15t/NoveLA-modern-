package my.dreamora.extension_compat.js

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Key/value storage that one plugin sees as its own `storage` / `localStorage`
 * (LNReader) or preferences (Mangayomi). Values are JSON text.
 */
interface JsStorage {
    fun get(key: String): String?
    fun set(key: String, value: String?)
    fun clear()
}

/** Storage that lives only as long as the runtime. Used by tests. */
class MemoryJsStorage : JsStorage {
    private val values = mutableMapOf<String, String>()

    @Synchronized
    override fun get(key: String): String? = values[key]

    @Synchronized
    override fun set(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }

    @Synchronized
    override fun clear() {
        values.clear()
    }
}

/**
 * Storage persisted as one JSON object per plugin, so a plugin's settings survive an app
 * restart. Write failures are ignored: the value is still kept in memory for this session.
 */
class FileJsStorage(private val file: File) : JsStorage {
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    private val json = Json { ignoreUnknownKeys = true }
    private val values: MutableMap<String, String> by lazy { load() }

    @Synchronized
    override fun get(key: String): String? = values[key]

    @Synchronized
    override fun set(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
        persist()
    }

    @Synchronized
    override fun clear() {
        values.clear()
        persist()
    }

    private fun load(): MutableMap<String, String> = runCatching {
        if (file.exists()) json.decodeFromString(serializer, file.readText()).toMutableMap() else mutableMapOf()
    }.getOrDefault(mutableMapOf())

    private fun persist() {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, values))
        }
    }
}
