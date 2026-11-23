package my.dreamora.extension_compat.mangayomi

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import my.dreamora.extension_compat.core.CompatEcosystem
import my.dreamora.extension_compat.core.CompatException
import my.dreamora.extension_compat.core.CompatExtensionLoader
import my.dreamora.extension_compat.core.CompatInstallRecord
import my.dreamora.extension_compat.core.CompatStorage
import my.dreamora.extension_compat.js.FileJsStorage
import my.dreamora.extension_compat.js.JsHostScripts
import my.dreamora.extension_compat.js.JsHttp
import my.dreamora.extension_compat.js.JsPluginRuntime
import my.dreamora.extension_compat.js.NetworkJsHttp
import my.dreamora.extension_compat.js.asObjectOrEmpty
import my.dreamora.extension_compat.js.bool
import my.dreamora.extension_compat.js.int
import my.dreamora.extension_compat.js.jsString
import my.dreamora.extension_compat.js.str
import my.dreamora.network.NetworkClient
import my.dreamora.scraper.SourceInterface
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mangayomi JavaScript sources (`class DefaultExtension extends MProvider`) run in QuickJS
 * with the Mangayomi host ([JsPluginRuntime] + `mangayomi-host.js`).
 *
 * Dart sources (`.dart` payloads) and video sources are not runnable in this build and
 * fail with a plain message, so the repository screen shows why instead of a generic error.
 */
@Singleton
class MangayomiExtensionLoader @Inject constructor(
    private val networkClient: NetworkClient,
    @ApplicationContext private val context: Context,
) : CompatExtensionLoader {

    override val ecosystem: CompatEcosystem = CompatEcosystem.MANGAYOMI

    private val http: JsHttp by lazy { NetworkJsHttp(networkClient) }
    private val scripts: JsHostScripts by lazy {
        JsHostScripts.load { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
    }
    private val loaded = ConcurrentHashMap<String, Loaded>()
    private val closer = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class Loaded(val key: String, val source: MangayomiSourceAdapter)

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        if (record.payloadPath.endsWith(".dart", ignoreCase = true)) {
            throw CompatException(DART_MESSAGE, null, "load")
        }
        if (record.contentType == "video") {
            throw CompatException(VIDEO_MESSAGE, null, "load")
        }
        val code = payload.toString(Charsets.UTF_8)
        if (!code.contains("MProvider")) {
            throw CompatException(DART_MESSAGE, null, "load")
        }
        val key = "${code.length}:${code.hashCode()}"
        loaded[record.id]?.takeIf { it.key == key }?.let { return listOf(it.source) }

        val storage = FileJsStorage(storageFile(record.id))
        val runtime = JsPluginRuntime.create(http, storage, scripts, scripts.mangayomi, "mangayomi")
        val meta = try {
            readMeta(runtime, code)
        } catch (e: Exception) {
            runtime.close()
            throw CompatException("Mangayomi source failed to load: ${e.message}", e, "load")
        }
        val contentType = if (meta.isManga) "manga" else ""
        val adapter = MangayomiSourceAdapter(
            id = record.id,
            name = meta.name?.takeIf { it.isNotBlank() } ?: record.name,
            baseUrl = meta.baseUrl?.takeIf { it.isNotBlank() } ?: record.baseUrl,
            contentType = contentType,
            runtime = runtime,
        )
        loaded.put(record.id, Loaded(key, adapter))?.let { previous ->
            closer.launch { previous.source.close() }
        }
        return listOf(adapter)
    }

    override fun clearCache() {
        val all = loaded.values.toList()
        loaded.clear()
        closer.launch { all.forEach { it.source.close() } }
    }

    private fun storageFile(recordId: String): File {
        val safe = recordId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(context.filesDir, "${CompatStorage.DIR_NAME}/js-storage/$safe.json")
    }

    private data class Meta(
        val name: String?,
        val baseUrl: String?,
        val isManga: Boolean,
    )

    /** Evaluates the source and reads its declared metadata. Throws on a broken source. */
    private suspend fun readMeta(runtime: JsPluginRuntime, code: String): Meta {
        val raw = runtime.evaluate("__mgLoad(${jsString(code)})", "source.js")
            ?: throw IllegalStateException("the source returned no metadata")
        val obj = Json.parseToJsonElement(raw).asObjectOrEmpty()
        return Meta(
            name = obj.str("name"),
            baseUrl = obj.str("baseUrl"),
            isManga = obj.int("itemType") == 0 || obj.bool("isManga") == true,
        )
    }

    companion object {
        const val DART_MESSAGE =
            "This Mangayomi source is written in Dart. Dart sources can't run in this app yet, so it is listed but not available."
        const val VIDEO_MESSAGE =
            "This Mangayomi source is for video. Video sources can't run in this app yet."
    }
}
