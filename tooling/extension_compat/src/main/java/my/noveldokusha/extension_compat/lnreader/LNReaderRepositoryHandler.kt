package my.noveldokusha.extension_compat.lnreader

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber
import java.net.URL
import java.nio.charset.Charsets

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
        val codeUrl: String,
        val languages: List<String>? = null,
        val tags: List<String>? = null,
    )

    suspend fun fetchRepository(repoUrl: String): my.noveldokusha.extension_compat.core.Result<List<RepoPluginInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(repoUrl)
                if (!response.isSuccessful) {
                    return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("Empty response"))
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
                my.noveldokusha.extension_compat.core.Result.success(plugins)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch LNReader repository: $repoUrl")
                my.noveldokusha.extension_compat.core.Result.failure(e)
            }
        }
    }

    suspend fun downloadAndInstall(plugin: RepoPluginInfo): my.noveldokusha.extension_compat.core.Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(plugin.codeUrl)
                if (!response.isSuccessful) {
                    return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("HTTP ${response.code}"))
                }
                val jsCode = response.body?.string() ?: return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("Empty plugin code"))

                val record = CompatInstallRecord(
                    id = plugin.id,
                    ecosystem = CompatEcosystem.LNREADER,
                    name = plugin.name,
                    version = plugin.version,
                    versionCode = plugin.versionCode,
                    payloadPath = "${plugin.id}.js",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    extra = (json.encodeToJsonElement(plugin) as JsonObject),
                )

                storage.writePayload(CompatEcosystem.LNREADER, record.payloadPath, jsCode.toByteArray(Charsets.UTF_8))
                storage.upsert(record)

                my.noveldokusha.extension_compat.core.Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download LNReader plugin: ${plugin.id}")
                my.noveldokusha.extension_compat.core.Result.failure(e)
            }
        }
    }

    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoPluginInfo> {
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.LNREADER }
            .groupBy { it.extra?.get("repoUrl")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull } ?: "" }

        val updates = mutableListOf<RepoPluginInfo>()
        for ((repoUrl, records) in repoGroups) {
            if (repoUrl.isBlank()) continue
            val result = fetchRepository(repoUrl)
            if (result.isSuccess) {
                val repoPlugins = result.getOrThrow()
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
        fun toCompatInstallRecord(jsonInstance: Json): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.LNREADER,
                name = name,
                version = version,
                versionCode = parseVersionCode(version),
                payloadPath = "$id.js",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                extra = (jsonInstance.encodeToJsonElement(this) as JsonObject),
            )
        }
    }
}