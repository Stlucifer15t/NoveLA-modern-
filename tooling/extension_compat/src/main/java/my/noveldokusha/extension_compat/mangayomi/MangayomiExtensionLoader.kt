package my.noveldokusha.extension_compat.mangayomi

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
 * Loads Mangayomi JavaScript sources and creates SourceInterface adapters.
 */
class MangayomiExtensionLoader @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
) : CompatExtensionLoader {

    override val ecosystem = CompatEcosystem.MANGAYOMI

    private val runtime = MangayomiRuntime()

    override suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface> {
        return withContext(Dispatchers.IO) {
            val jsCode = String(payload, StandardCharsets.UTF_8)
            if (jsCode.isBlank()) {
                throw CompatException("Empty source code for ${record.id}", operation = "load", ecosystem = ecosystem)
            }

            // Load source in QuickJS runtime
            val loadResult = runtime.loadSource(record.id, jsCode)
            if (loadResult.isFailure) {
                throw CompatException("Failed to load Mangayomi source: ${loadResult.exceptionOrNull()?.message}", operation = "load", ecosystem = ecosystem)
            }

            val sourceInstance = loadResult.getOrThrow()
            val metadata = sourceInstance.metadata

            // Create adapter
            return listOf(
                MangayomiSourceAdapter(
                    sourceInstance = sourceInstance,
                    runtime = runtime,
                    networkClient = networkClient,
                    extensionId = record.id,
                    sourceMetadata = metadata,
                )
            )
        }
    }

    override fun clearCache() {
        runtime.close()
    }
}