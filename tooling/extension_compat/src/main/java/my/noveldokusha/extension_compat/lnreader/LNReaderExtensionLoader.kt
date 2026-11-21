package my.noveldokusha.extension_compat.lnreader

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatException
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.SourceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.StandardCharsets

/**
 * Loads LNReader JavaScript plugins and creates SourceInterface adapters.
 */
class LNReaderExtensionLoader @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
) : CompatExtensionLoader {

    override val ecosystem = CompatEcosystem.LNREADER

    private val runtime = LNReaderRuntime()

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        return withContext(Dispatchers.IO) {
            val jsCode = String(payload, StandardCharsets.UTF_8)
            if (jsCode.isBlank()) {
                throw CompatException("Empty plugin code for ${record.id}", operation = "load", ecosystem = ecosystem)
            }

            // Load plugin in QuickJS runtime
            val loadResult = runtime.loadPlugin(record.id, jsCode)
            if (loadResult.isFailure) {
                throw CompatException("Failed to load LNReader plugin: ${loadResult.exceptionOrNull()?.message}", operation = "load", ecosystem = ecosystem)
            }

            val pluginInstance = loadResult.getOrThrow()
            val metadata = pluginInstance.metadata

            // Create adapter
            return listOf(
                LNReaderSourceAdapter(
                    pluginInstance = pluginInstance,
                    runtime = runtime,
                    networkClient = networkClient,
                    extensionId = record.id,
                    pluginMetadata = metadata,
                )
            )
        }
    }

    override fun clearCache() {
        runtime.close()
    }
}