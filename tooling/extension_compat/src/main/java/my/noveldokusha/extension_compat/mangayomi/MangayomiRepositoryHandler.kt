package my.noveldokusha.extension_compat.mangayomi

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Handles Mangayomi source repositories.
 *
 * Repository format: JSON index with source metadata and code URLs.
 */
class MangayomiRepositoryHandler @javax.inject.Inject constructor(
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
        val version: Int,
        val baseUrl: String,
        val apiUrl: String?,
        val lang: String,
        val iconUrl: String?,
        val codeUrl: String, // URL to the compiled .js file
        val isManga: Boolean = true,
        val isNsfw: Boolean = false,
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
                val index = json.decodeFromString<RepositoryIndex>(body)

                val sources = index.sources.map { entry ->
                    RepoSourceInfo(
                        id = entry.id,
                        name = entry.name,
                        version = entry.version.toString(),
                        versionCode = entry.version.toLong(),
                        baseUrl = entry.baseUrl,
                        apiUrl = entry.apiUrl ?: "",
                        lang = entry.lang,
                        iconUrl = entry.iconUrl,
                        codeUrl = entry.codeUrl,
                        isManga = entry.isManga,
                        isNsfw = entry.isNsfw,
                        repoUrl = repoUrl,
                    )
                }
                Result.success(sources)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Mangayomi repository: $repoUrl")
                Result.failure(e)
            }
        }
    }

    /**
     * Download a source's JavaScript code and create an install record.
     */
    suspend fun downloadAndInstall(source: RepoSourceInfo): Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(source.codeUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val jsCode = response.body?.string() ?: return@withContext Result.failure(Exception("Empty source code"))

                // Create install record
                val record = CompatInstallRecord(
                    id = source.id,
                    ecosystem = CompatEcosystem.MANGAYOMI,
                    name = source.name,
                    version = source.version,
                    versionCode = source.versionCode,
                    payloadPath = "${source.id}.js",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    metadata = json.encodeToString(source),
                )

                // Write JS payload
                storage.writePayload(CompatEcosystem.MANGAYOMI, record.payloadPath, jsCode.toByteArray(Charsets.UTF_8))
                storage.upsert(record)

                Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download Mangayomi source: ${source.id}")
                Result.failure(e)
            }
        }
    }

    /**
     * Check for updates to installed sources.
     */
    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoSourceInfo> {
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.MANGAYOMI }
            .groupBy { it.metadata?.let { json.decodeFromString<RepoSourceInfo>(it)?.repoUrl } ?: "" }

        val updates = mutableListOf<RepoSourceInfo>()
        for ((repoUrl, records) in repoGroups) {
            if (repoUrl.isBlank()) continue
            fetchRepository(repoUrl).onSuccess { repoSources ->
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
        val baseUrl: String,
        val apiUrl: String,
        val lang: String,
        val iconUrl: String?,
        val codeUrl: String,
        val isManga: Boolean,
        val isNsfw: Boolean,
        val repoUrl: String,
    ) {
        fun toCompatInstallRecord(): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.MANGAYOMI,
                name = name,
                version = version,
                versionCode = versionCode,
                payloadPath = "$id.js",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                metadata = json.encodeToString(this),
            )
        }
    }
}