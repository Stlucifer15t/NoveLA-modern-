package my.dreamora.extension_compat.lnreader

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
import my.dreamora.extension_compat.js.bool
import my.dreamora.extension_compat.js.jsString
import my.dreamora.extension_compat.js.asObjectOrEmpty
import my.dreamora.extension_compat.js.obj
import my.dreamora.extension_compat.js.str
import my.dreamora.network.NetworkClient
import my.dreamora.scraper.SourceInterface
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LNReader plugins: one `.js` file per source, CommonJS, run in QuickJS with the
 * LNReader host ([JsPluginRuntime] + `lnreader-host.js`).
 *
 * A loaded runtime is kept per record and reused by later reloads while the payload is
 * unchanged, because the registry is reloaded on every install and toggle.
 */
@Singleton
class LnReaderExtensionLoader @Inject constructor(
    private val networkClient: NetworkClient,
    @ApplicationContext private val context: Context,
) : CompatExtensionLoader {

    override val ecosystem: CompatEcosystem = CompatEcosystem.LNREADER

    private val http: JsHttp by lazy { NetworkJsHttp(networkClient) }
    private val scripts: JsHostScripts by lazy {
        JsHostScripts.load { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
    }
    private val loaded = ConcurrentHashMap<String, Loaded>()
    private val closer = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class Loaded(val key: String, val source: LnReaderSourceAdapter)

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        val code = payload.toString(Charsets.UTF_8)
        val key = "${code.length}:${code.hashCode()}"
        loaded[record.id]?.takeIf { it.key == key }?.let { return listOf(it.source) }

        val storage = FileJsStorage(storageFile(record.id))
        val runtime = JsPluginRuntime.create(http, storage, scripts, scripts.lnReader, "lnreader")
        val meta = try {
            readMeta(runtime, code)
        } catch (e: Exception) {
            runtime.close()
            throw CompatException("LNReader plugin failed to load: ${e.message}", e, "load")
        }
        val adapter = LnReaderSourceAdapter(
            id = record.id,
            name = meta.name?.takeIf { it.isNotBlank() } ?: record.name,
            baseUrl = meta.site?.takeIf { it.isNotBlank() } ?: record.baseUrl,
            runtime = runtime,
            filters = meta.filters,
            hasSearch = meta.hasSearch,
            hasPages = meta.hasPages,
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
        val site: String?,
        val filters: JsonObject?,
        val hasSearch: Boolean,
        val hasPages: Boolean,
    )

    /** Evaluates the plugin and reads the fields the app needs. Throws on a broken plugin. */
    private suspend fun readMeta(runtime: JsPluginRuntime, code: String): Meta {
        val raw = runtime.evaluate("__lnLoad(${jsString(code)})", "plugin.js")
            ?: throw IllegalStateException("the plugin returned no metadata")
        val obj = Json.parseToJsonElement(raw).asObjectOrEmpty()
        return Meta(
            name = obj.str("name"),
            site = obj.str("site"),
            filters = obj.obj("filters"),
            hasSearch = obj.bool("hasSearch") ?: false,
            hasPages = obj.bool("hasPages") ?: false,
        )
    }
}
