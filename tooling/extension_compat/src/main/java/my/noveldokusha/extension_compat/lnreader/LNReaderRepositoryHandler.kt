package my.noveldokusha.extension_compat.lnreader

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URL

/**
 * Handles LNReader plugin repositories.
 *
 * Repository format: plugins.min.json manifest with plugin metadata and code URLs.
 */
class LNReaderRepositoryHandler @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
    private val storage: CompatStorage,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class PluginManifest(
        val plugins: List<PluginEntry>,
    )

    data class PluginEntry(
        val id: String,
        val name: String,
        val version: String,
        val site: String,
        val icon: String,
        val codeUrl: String, // URL to the compiled .js file
        val languages: List<String>? = null,
        val tags: List<String>? = null,
    )

    /**
     * Fetch and parse a repository manifest, returning available plugins.
     */
    suspend fun fetchRepository(repoUrl: String): Result<List<RepoPluginInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(repoUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val manifest = json.decodeFromString<PluginManifest>(body)

                val plugins = manifest.plugins.map { entry ->
                    RepoPluginInfo(
                        id = entry.id,
                        name = entry.name,
                        version = entry.version,
                        site = entry.site,
                        iconUrl = entry.icon,
                        codeUrl = entry.codeUrl,
                        languages = entry.languages ?: listOf("en"),
                        repoUrl = repoUrl,
                    )
                }
                Result.success(plugins)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch LNReader repository: $repoUrl")
                Result.failure(e)
            }
        }
    }

    /**
     * Download a plugin's JavaScript code and create an install record.
     */
    suspend fun downloadAndInstall(plugin: RepoPluginInfo): Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(plugin.codeUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val jsCode = response.body?.string() ?: return@withContext Result.failure(Exception("Empty plugin code"))

                // Create install record
                val record = CompatInstallRecord(
                    id = plugin.id,
                    ecosystem = CompatEcosystem.LNREADER,
                    name = plugin.name,
                    version = plugin.version,
                    versionCode = parseVersionCode(plugin.version),
                    payloadPath = "${plugin.id}.js",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    metadata = json.encodeToString(plugin),
                )

                // Write JS payload
                storage.writePayload(CompatEcosystem.LNREADER, record.payloadPath, jsCode.toByteArray(Charsets.UTF_8))
                storage.upsert(record)

                Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download LNReader plugin: ${plugin.id}")
                Result.failure(e)
            }
        }
    }

    /**
     * Check for updates to installed plugins.
     */
    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoPluginInfo> {
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.LNREADER }
            .groupBy { it.metadata?.let { json.decodeFromString<RepoPluginInfo>(it)?.repoUrl } ?: "" }

        val updates = mutableListOf<RepoPluginInfo>()
        for ((repoUrl, records) in repoGroups) {
            if (repoUrl.isBlank()) continue
            fetchRepository(repoUrl).onSuccess { repoPlugins ->
                val installedMap = records.associateBy { it.id }
                for (repoPlugin in repoPlugins) {
                    installedMap[repoPlugin.id]?.let { installed ->
                        if (compareVersions(repoPlugin.version, installed.version) > 0) {
                            updates.add(repoPlugin)
                        }
                    }
                }
            }
        }
        return updates
    }

    private fun parseVersionCode(version: String): Long {
        return try {
            val parts = version.split(".").map { it.toLongOrNull() ?: 0 }
            var code = 0L
            for (i in parts.indices) {
                code = code * 1000 + parts[i]
            }
            code
        } catch (e: Exception) {
            0
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }

    data class RepoPluginInfo(
        val id: String,
        val name: String,
        val version: String,
        val site: String,
        val iconUrl: String,
        val codeUrl: String,
        val languages: List<String>,
        val repoUrl: String,
    ) {
        fun toCompatInstallRecord(): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.LNREADER,
                name = name,
                version = version,
                versionCode = parseVersionCode(version),
                payloadPath = "$id.js",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                metadata = json.encodeToString(this),
            )
        }
    }
}