package my.noveldokusha.extension_compat.cloudstream

import android.content.Context
import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatException
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.SourceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Loads CloudStream plugins and creates SourceInterface adapters.
 */
class CloudStreamExtensionLoader @javax.inject.Inject constructor(
    private val context: Context,
    private val networkClient: NetworkClient,
) : CompatExtensionLoader {

    override val ecosystem = CompatEcosystem.CLOUDSTREAM

    private val runtime = CloudStreamRuntime()

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        return withContext(Dispatchers.IO) {
            // Payload contains the plugin JAR/.cs3 file bytes
            val pluginFile = writePluginToCache(record, payload)
            val loadResult = runtime.loadPlugin(context, record.id, pluginFile)

            if (loadResult.isFailure) {
                throw CompatException("Failed to load CloudStream plugin: ${loadResult.exceptionOrNull()?.message}", operation = "load", ecosystem = ecosystem)
            }

            val pluginInstance = loadResult.getOrThrow()
            val mainApis = runtime.getMainApis(record.id)

            // Create adapters for each MainAPI (each represents a source)
            return mainApis.mapIndexed { index, mainApi ->
                CloudStreamSourceAdapter(
                    pluginInstance = pluginInstance,
                    runtime = runtime,
                    networkClient = networkClient,
                    extensionId = record.id,
                    apiIndex = index,
                    mainApi = mainApi,
                )
            }
        }
    }

    private fun writePluginToCache(record: CompatInstallRecord, payload: ByteArray): File {
        val cacheDir = File(context.cacheDir, "cloudstream_plugins")
        cacheDir.mkdirs()
        val pluginFile = File(cacheDir, "${record.id}.jar")
        pluginFile.writeBytes(payload)
        return pluginFile
    }

    override fun clearCache() {
        runtime.close()
    }
}