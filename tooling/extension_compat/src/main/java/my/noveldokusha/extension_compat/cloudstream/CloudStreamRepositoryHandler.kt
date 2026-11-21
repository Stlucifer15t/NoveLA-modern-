package my.noveldokusha.extension_compat.cloudstream

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Handles CloudStream plugin repositories.
 *
 * Repository format: JSON index with plugin metadata and download URLs.
 */
class CloudStreamRepositoryHandler @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
    private val storage: CompatStorage,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class RepositoryIndex(
        val plugins: List<PluginEntry>,
    )

    data class PluginEntry(
        val name: String,
        val pluginClassName: String,
        val version: Int,
        val downloadUrl: String, // URL to the .jar or .cs3 file
        val requiresResources: Boolean = false,
        val description: String? = null,
        val iconUrl: String? = null,
        val author: String? = null,
        val category: String? = null, // anime, movie, tv, live
    )

    /**
     * Fetch and parse a repository index, returning available plugins.
     */
    suspend fun fetchRepository(repoUrl: String): Result<List<RepoPluginInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(repoUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val index = json.decodeFromString<RepositoryIndex>(body)

                val plugins = index.plugins.map { entry ->
                    RepoPluginInfo(
                        id = entry.pluginClassName, // Use class name as ID
                        name = entry.name,
                        version = entry.version.toString(),
                        versionCode = entry.version.toLong(),
                        downloadUrl = entry.downloadUrl,
                        requiresResources = entry.requiresResources,
                        description = entry.description,
                        iconUrl = entry.iconUrl,
                        author = entry.author,
                        category = entry.category,
                        repoUrl = repoUrl,
                    )
                }
                Result.success(plugins)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch CloudStream repository: $repoUrl")
                Result.failure(e)
            }
        }
    }

    /**
     * Download a plugin and create an install record.
     */
    suspend fun downloadAndInstall(plugin: RepoPluginInfo): Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(plugin.downloadUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val pluginBytes = response.body?.bytes() ?: return@withContext Result.failure(Exception("Empty plugin"))

                // Create install record
                val record = CompatInstallRecord(
                    id = plugin.id,
                    ecosystem = CompatEcosystem.CLOUDSTREAM,
                    name = plugin.name,
                    version = plugin.version,
                    versionCode = plugin.versionCode,
                    payloadPath = "${plugin.id}.jar",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    extra = json.toJsonElement(plugin).jsonObject,
                )

                // Write plugin payload
                storage.writePayload(CompatEcosystem.CLOUDSTREAM, record.payloadPath, pluginBytes)
                storage.upsert(record)

                Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download CloudStream plugin: ${plugin.id}")
                Result.failure(e)
            }
        }
    }

    /**
     * Check for updates to installed plugins.
     */
    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoPluginInfo> {
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.CLOUDSTREAM }
            .groupBy { it.extra?.get("repoUrl")?.contentOrNull ?: "" }

        val updates = mutableListOf<RepoPluginInfo>()
        for ((repoUrl, records) in repoGroups) {
            if (repoUrl.isBlank()) continue
            val result = fetchRepository(repoUrl)
            if (result.isSuccess) {
                val repoPlugins = result.getOrThrow()
                val installedMap = records.associateBy { it.id }
                for (repoPlugin in repoPlugins) {
                    installedMap[repoPlugin.id]?.let { installed ->
                        if (repoPlugin.versionCode > installed.versionCode) {
                            updates.add(repoPlugin)
                        }
                    }
                }
            }
        }
        return updates
    }

    data class RepoPluginInfo(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Long,
        val downloadUrl: String,
        val requiresResources: Boolean,
        val description: String?,
        val iconUrl: String?,
        val author: String?,
        val category: String?,
        val repoUrl: String,
    ) {
        fun toCompatInstallRecord(): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.CLOUDSTREAM,
                name = name,
                version = version,
                versionCode = versionCode,
                payloadPath = "$id.jar",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                extra = json.toJsonElement(this).jsonObject,
            )
        }
    }
}