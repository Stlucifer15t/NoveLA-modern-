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

/**
 * Handles Legado bookSource repositories.
 *
 * Repository format: JSON array of bookSource objects or URLs to .json files.
 */
class LegadoRepositoryHandler @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
    private val storage: CompatStorage,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class RepositoryIndex(
        val sources: List<SourceEntry>,
    )

    data class SourceEntry(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Int,
        val downloadUrl: String, // URL to the bookSource.json file
        val iconUrl: String? = null,
        val author: String? = null,
        val description: String? = null,
        val language: String = "en",
        val contentType: String = "", // novel, manga, video
    )

    /**
     * Fetch and parse a repository index, returning available sources.
     */
    suspend fun fetchRepository(repoUrl: String): Result<List<RepoSourceInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(repoUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

                val sources = parseRepositoryIndex(body, repoUrl)
                Result.success(sources)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Legado repository: $repoUrl")
                Result.failure(e)
            }
        }
    }

    private fun parseRepositoryIndex(body: String, repoUrl: String): List<RepoSourceInfo> {
        // Try parsing as array of bookSources
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) {
            val bookSources = json.decodeFromString<List<LegadoBookSource>>(trimmed)
            return bookSources.mapIndexed { index, bs ->
                RepoSourceInfo(
                    id = bs.bookSourceUrl.ifBlank { bs.bookSourceName }.ifBlank { "source_$index" },
                    name = bs.bookSourceName,
                    version = "1.0.0",
                    versionCode = 1L,
                    downloadUrl = repoUrl, // The repo URL itself contains the bookSource
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

        // Try parsing as object with sources array
        if (trimmed.startsWith("{")) {
            val obj = json.parseToJsonElement(trimmed).jsonObject
            for (key in listOf("sources", "bookSources", "data")) {
                val array = obj[key] as? kotlinx.serialization.json.JsonArray ?: continue
                val bookSources = array.mapNotNull { el ->
                    try {
                        json.decodeFromJsonElement(LegadoBookSource.serializer(), el)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bookSources.isNotEmpty()) {
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
            }
        }

        return emptyList()
    }

    /**
     * Download a bookSource and create an install record.
     */
    suspend fun downloadAndInstall(source: RepoSourceInfo): Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                // For Legado, the repo URL IS the bookSource JSON
                val response = networkClient.get(source.downloadUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

                // Parse to get the specific bookSource if it's an array
                val bookSource = parseSingleBookSource(body, source.id)
                val jsonPayload = json.encodeToString(bookSource)

                // Create install record
                val record = CompatInstallRecord(
                    id = source.id,
                    ecosystem = CompatEcosystem.LEGADO,
                    name = source.name,
                    version = source.version,
                    versionCode = source.versionCode,
                    payloadPath = "${source.id}.json",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    extra = (json.encodeToJsonElement(source) as JsonObject),
                )

                // Write JSON payload
                storage.writePayload(CompatEcosystem.LEGADO, record.payloadPath, jsonPayload.toByteArray(Charsets.UTF_8))
                storage.upsert(record)

                Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download Legado source: ${source.id}")
                Result.failure(e)
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
            return json.decodeFromString<LegadoBookSource>(trimmed)
        }
        throw Exception("Invalid bookSource format")
    }

    /**
     * Check for updates to installed sources.
     */
    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoSourceInfo> {
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.LEGADO }
            .groupBy { it.extra?.get("repoUrl")?.contentOrNull ?: "" }

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
        fun toCompatInstallRecord(): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.LEGADO,
                name = name,
                version = version,
                versionCode = versionCode,
                payloadPath = "$id.json",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                extra = (json.encodeToJsonElement(this) as JsonObject),
            )
        }
    }
}