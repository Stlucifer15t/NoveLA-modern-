package my.noveldokusha.extension_compat.tachiyomi

import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.net.URL

/**
 * Handles Tachiyomi/Mihon/Aniyomi extension repositories.
 *
 * Repository format: JSON index with languages as keys, each containing
 * a list of sources with metadata and download URLs.
 */
class TachiyomiRepositoryHandler @javax.inject.Inject constructor(
    private val networkClient: NetworkClient,
    private val storage: CompatStorage,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class RepositoryIndex(
        val languages: Map<String, LanguageEntry>,
    )

    data class LanguageEntry(
        val name: String,
        val url: String,
        val sources: List<SourceEntry>?,
    )

    data class SourceEntry(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Int,
        val url: String, // APK download URL
        val icon: String?,
        val author: String?,
        val description: String?,
        val contentWarning: Int?, // 0=Safe, 1=Mixed, 2=NSFW
        val lang: String,
        val sourceUrl: String?, // Base URL of the source
    )

    /**
     * Fetch and parse a repository index, returning available extensions.
     */
    suspend fun fetchRepository(repoUrl: String): Result<List<RepoExtensionInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(repoUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val index = json.decodeFromString<RepositoryIndex>(body)

                val extensions = mutableListOf<RepoExtensionInfo>()
                index.languages.forEach { (langCode, langEntry) ->
                    langEntry.sources?.forEach { source ->
                        extensions.add(RepoExtensionInfo(
                            id = source.id,
                            name = source.name,
                            version = source.version,
                            versionCode = source.versionCode.toLong(),
                            downloadUrl = source.url,
                            iconUrl = source.icon,
                            author = source.author,
                            description = source.description,
                            contentWarning = source.contentWarning ?: 0,
                            language = langCode,
                            sourceUrl = source.sourceUrl,
                            repoUrl = repoUrl,
                        ))
                    }
                }
                Result.success(extensions)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Tachiyomi repository: $repoUrl")
                Result.failure(e)
            }
        }
    }

    /**
     * Download an extension APK and create an install record.
     */
    suspend fun downloadAndInstall(extension: RepoExtensionInfo): Result<CompatInstallRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = networkClient.get(extension.downloadUrl)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val apkBytes = response.body?.bytes() ?: return@withContext Result.failure(Exception("Empty APK"))

                // Create install record
                val record = CompatInstallRecord(
                    id = extension.id,
                    ecosystem = CompatEcosystem.TACHIYOMI,
                    name = extension.name,
                    version = extension.version,
                    versionCode = extension.versionCode,
                    payloadPath = "${extension.id}.apk",
                    enabled = true,
                    installedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    metadata = json.encodeToString(extension),
                )

                // Write APK payload
                storage.writePayload(CompatEcosystem.TACHIYOMI, record.payloadPath, apkBytes)
                storage.upsert(record)

                Result.success(record)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download extension: ${extension.id}")
                Result.failure(e)
            }
        }
    }

    /**
     * Check for updates to installed extensions.
     */
    suspend fun checkUpdates(installedRecords: List<CompatInstallRecord>): List<RepoExtensionInfo> {
        // Group by repo URL
        val repoGroups = installedRecords
            .filter { it.ecosystem == CompatEcosystem.TACHIYOMI }
            .groupBy { it.metadata?.let { json.decodeFromString<RepoExtensionInfo>(it)?.repoUrl } ?: "" }

        val updates = mutableListOf<RepoExtensionInfo>()
        for ((repoUrl, records) in repoGroups) {
            if (repoUrl.isBlank()) continue
            fetchRepository(repoUrl).onSuccess { repoExtensions ->
                val installedMap = records.associateBy { it.id }
                for (repoExt in repoExtensions) {
                    installedMap[repoExt.id]?.let { installed ->
                        if (repoExt.versionCode > installed.versionCode) {
                            updates.add(repoExt)
                        }
                    }
                }
            }
        }
        return updates
    }

    data class RepoExtensionInfo(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Long,
        val downloadUrl: String,
        val iconUrl: String?,
        val author: String?,
        val description: String?,
        val contentWarning: Int,
        val language: String,
        val sourceUrl: String?,
        val repoUrl: String,
    ) {
        fun toCompatInstallRecord(): CompatInstallRecord {
            return CompatInstallRecord(
                id = id,
                ecosystem = CompatEcosystem.TACHIYOMI,
                name = name,
                version = version,
                versionCode = versionCode,
                payloadPath = "$id.apk",
                enabled = true,
                installedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                metadata = json.encodeToString(this),
            )
        }
    }
}