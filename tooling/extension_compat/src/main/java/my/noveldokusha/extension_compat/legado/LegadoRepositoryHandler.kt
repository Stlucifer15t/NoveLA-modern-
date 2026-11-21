package my.noveldokusha.extension_compat.legado

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
import java.nio.charset.Charsets

class LegadoRepositoryHandler @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
    private val storage: CompatStorage,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class RepositoryIndex(
        val sources: List<LegadoBookSource>,
    )

    data class LegadoBookSource(
        val bookSourceName: String = "",
        val bookSourceUrl: String = "",
        val bookSourceType: Int = 0,
        val bookSourceComment: String = "",
    )

    suspend fun fetchRepository(repoUrl: String): my.noveldokusha.extension_compat.core.Result<List<RepoSourceInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(repoUrl)
                if (!response.isSuccessful) {
                    return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("Empty response"))
                val sources = parseRepositoryIndex(body, repoUrl)
                my.noveldokusha.extension_compat.core.Result.success(sources)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Legado repository: $repoUrl")
                my.noveldokusha.extension_compat.core.Result.failure(e)
            }
        }
    }

    private fun parseRepositoryIndex(body: String, repoUrl: String): List<RepoSourceInfo> {
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) {
            val bookSources = json.decodeFromString<List<LegadoBookSource>>(trimmed)
            return bookSources.mapIndexed { index, bs ->
                RepoSourceInfo(
                    id = bs.bookSourceUrl.ifBlank { bs.bookSourceName }.ifBlank { "source_$index" },
                    name = bs.bookSourceName,
                    version = "1.0.0",
                    versionCode = 1L,
                    downloadUrl = repoUrl,
                    iconUrl = null,
                    author = null,
                    description = bs.bookSourceComment,
                    language = "en",
                    contentType = when (bs.bookSourceType) {
                        2 -> "manga"
                        4 -> "video"
                        else -> "novel"
                    },
                    repoUrl = repoUrl,
                )
            }
        }
        return emptyList()
    }

    suspend fun downloadAndInstall(source: RepoSourceInfo): my.noveldokusha.extension_compat.core.Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(source.downloadUrl)
                if (!response.isSuccessful) {
                    return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext my.noveldokusha.extension_compat.core.Result.failure(Exception("Empty response"))
                val bookSource = parseSingleBookSource(body, source.id)
                val jsonPayload = json.encodeToString(bookSource)

                val record = CompatInstallRecord(
                    id = source.id,
                    ecosystem = CompatEcosystem.LEGADO,
                    name = source.name,
                    version = source.version,
                    versionCode = source.versionCode,
                    payloadPath = "${source.id}.json",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    extra = (json.encodeToJsonElement(bookSource) as JsonObject),
                )

                storage.writePayload(CompatEcosystem.LEGADO, record.payloadPath, jsonPayload.toByteArray(Charsets.UTF_8))
                storage.upsert(record)

                my.noveldokusha.extension_compat.core.Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download Legado source: ${source.id}")
                my.noveldokusha.extension_compat.core.Result.failure(e)
            }
        }
    }

    private fun parseSingleBookSource(body: String, targetId: String): LegadoBookSource {
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) {
            val bookSources = json.decodeFromString<List<LegadoBookSource>>(trimmed)
            return bookSources.firstOrNull { bs ->
                bs.bookSourceUrl == targetId || bs.bookSourceName == targetId
            } ?: bookSources.first()
        }
        if (trimmed.startsWith("{")) {
            return json.decodeFromString(trimmed)
        }
        throw Exception("Invalid bookSource format")
    }

    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoSourceInfo> {
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.LEGADO }
            .groupBy { it.extra?.get("repoUrl")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull } ?: "" }

        val updates = mutableListOf<RepoSourceInfo>()
        for ((repoUrl, records) in repoGroups) {
            if (repoUrl.isBlank()) continue
            val result = fetchRepository(repoUrl)
            if (result.isSuccess) {
                val repoSources = result.getOrThrow()
                val installedMap = records.associateBy { it.id }
                for (repoSource in repoSources) {
                    installedMap[repoSource.id]?.let { installed ->
                        if (repoSource.versionCode > installed.versionCode) {
                            updates.add(repoSource)
                        }
                    }
                }
            }
        }
        return updates
    }

    data class RepoSourceInfo(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Long,
        val downloadUrl: String,
        val iconUrl: String?,
        val author: String?,
        val description: String?,
        val language: String,
        val contentType: String,
        val repoUrl: String,
    ) {
        fun toCompatInstallRecord(jsonInstance: Json): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.LEGADO,
                name = name,
                version = version,
                versionCode = versionCode,
                payloadPath = "$id.js",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                extra = (jsonInstance.encodeToJsonElement(this) as JsonObject),
            )
        }
    }
}