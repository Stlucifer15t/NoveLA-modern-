package my.noveldokusha.extension_compat.core

import my.noveldokusha.extension_compat.cloudstream.CloudStreamRepositoryHandler
import my.noveldokusha.extension_compat.legado.LegadoRepositoryHandler
import my.noveldokusha.extension_compat.lnreader.LNReaderRepositoryHandler
import my.noveldokusha.extension_compat.mangayomi.MangayomiRepositoryHandler
import my.noveldokusha.extension_compat.tachiyomi.TachiyomiRepositoryHandler
import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified repository manager for all extension ecosystems.
 * Handles repository registration, fetching, and extension installation.
 */
@Singleton
class CompatRepositoryManager @Inject constructor(
    private val networkClient: NetworkClient,
    private val tachiyomiHandler: TachiyomiRepositoryHandler,
    private val legadoHandler: LegadoRepositoryHandler,
    private val lnReaderHandler: LNReaderRepositoryHandler,
    private val mangayomiHandler: MangayomiRepositoryHandler,
    private val cloudStreamHandler: CloudStreamRepositoryHandler,
) {

    private val _repositories = MutableStateFlow<List<RepoInfo>>(emptyList())
    val repositories: StateFlow<List<RepoInfo>> = _repositories.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    data class RepoInfo(
        val id: String,
        val url: String,
        val ecosystem: CompatEcosystem,
        val name: String,
        val enabled: Boolean = true,
        val lastSynced: Long = 0,
        val extensionCount: Int = 0,
        val error: String? = null,
    )

    sealed class SyncState {
        object Idle : SyncState()
        data class Syncing(val ecosystem: CompatEcosystem) : SyncState()
        data class Success(val count: Int) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    /**
     * Add a new repository.
     */
    suspend fun addRepository(repo: RepoInfo) {
        val current = _repositories.value
        if (current.any { it.id == repo.id }) {
            // Update existing
            _repositories.value = current.map { if (it.id == repo.id) repo else it }
        } else {
            _repositories.value = current + repo
        }
        // Trigger initial sync
        syncRepository(repo)
    }

    /**
     * Remove a repository.
     */
    suspend fun removeRepository(repoId: String) {
        _repositories.value = _repositories.value.filter { it.id != repoId }
    }

    /**
     * Toggle repository enabled state.
     */
    suspend fun setRepositoryEnabled(repoId: String, enabled: Boolean) {
        _repositories.value = _repositories.value.map { repo ->
            if (repo.id == repoId) repo.copy(enabled = enabled) else repo
        }
        if (enabled) {
            _repositories.value.find { it.id == repoId }?.let { syncRepository(it) }
        }
    }

    /**
     * Sync a specific repository.
     */
    suspend fun syncRepository(repo: RepoInfo) {
        if (!repo.enabled) return

        _syncState.value = SyncState.Syncing(repo.ecosystem)

        val result = when (repo.ecosystem) {
            CompatEcosystem.TACHIYOMI -> tachiyomiHandler.fetchRepository(repo.url)
            CompatEcosystem.LEGADO -> legadoHandler.fetchRepository(repo.url)
            CompatEcosystem.LNREADER -> lnReaderHandler.fetchRepository(repo.url)
            CompatEcosystem.MANGAYOMI -> mangayomiHandler.fetchRepository(repo.url)
            CompatEcosystem.CLOUDSTREAM -> cloudStreamHandler.fetchRepository(repo.url)
            else -> Result.failure(Exception("Unsupported ecosystem: ${repo.ecosystem}"))
        }

        result.fold(
            onSuccess = { extensions ->
                _repositories.value = _repositories.value.map { r ->
                    if (r.id == repo.id) r.copy(
                        lastSynced = System.currentTimeMillis(),
                        extensionCount = extensions.size,
                        error = null,
                    ) else r
                }
                _syncState.value = SyncState.Success(extensions.size)
                Timber.d("Synced ${repo.ecosystem.displayName} repo: ${extensions.size} extensions")
            },
            onFailure = { e ->
                _repositories.value = _repositories.value.map { r ->
                    if (r.id == repo.id) r.copy(error = e.message) else r
                }
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                Timber.e(e, "Failed to sync repo: ${repo.id}")
            }
        )
    }

    /**
     * Sync all enabled repositories.
     */
    suspend fun syncAll() {
        _repositories.value.filter { it.enabled }.forEach { syncRepository(it) }
    }

    /**
     * Install an extension from a repository.
     */
    suspend fun installExtension(repoId: String, extensionId: String): Result<CompatInstallRecord> {
        val repo = _repositories.value.find { it.id == repoId } ?: return Result.failure(Exception("Repository not found"))

        return when (repo.ecosystem) {
            CompatEcosystem.TACHIYOMI -> {
                tachiyomiHandler.fetchRepository(repo.url).flatMap { extensions ->
                    extensions.find { it.id == extensionId }?.let { tachiyomiHandler.downloadAndInstall(it) }
                        ?: Result.failure(Exception("Extension not found in repo"))
                }
            }
            CompatEcosystem.LEGADO -> {
                legadoHandler.fetchRepository(repo.url).flatMap { extensions ->
                    extensions.find { it.id == extensionId }?.let { legadoHandler.downloadAndInstall(it) }
                        ?: Result.failure(Exception("Extension not found in repo"))
                }
            }
            CompatEcosystem.LNREADER -> {
                lnReaderHandler.fetchRepository(repo.url).flatMap { extensions ->
                    extensions.find { it.id == extensionId }?.let { lnReaderHandler.downloadAndInstall(it) }
                        ?: Result.failure(Exception("Extension not found in repo"))
                }
            }
            CompatEcosystem.MANGAYOMI -> {
                mangayomiHandler.fetchRepository(repo.url).flatMap { extensions ->
                    extensions.find { it.id == extensionId }?.let { mangayomiHandler.downloadAndInstall(it) }
                        ?: Result.failure(Exception("Extension not found in repo"))
                }
            }
            CompatEcosystem.CLOUDSTREAM -> {
                cloudStreamHandler.fetchRepository(repo.url).flatMap { extensions ->
                    extensions.find { it.id == extensionId }?.let { cloudStreamHandler.downloadAndInstall(it) }
                        ?: Result.failure(Exception("Extension not found in repo"))
                }
            }
            else -> Result.failure(Exception("Unsupported ecosystem"))
        }
    }

    /**
     * Check for updates across all repositories.
     */
    suspend fun checkAllUpdates(installedRecords: List<CompatInstallRecord>): Map<CompatEcosystem, List<Any>> {
        val updates = mutableMapOf<CompatEcosystem, List<Any>>()

        for (repo in _repositories.value.filter { it.enabled }) {
            val ecosystemUpdates = when (repo.ecosystem) {
                CompatEcosystem.TACHIYOMI -> tachiyomiHandler.checkUpdates(installedRecords.filter { it.ecosystem == CompatEcosystem.TACHIYOMI })
                CompatEcosystem.LEGADO -> legadoHandler.checkUpdates(installedRecords.filter { it.ecosystem == CompatEcosystem.LEGADO })
                CompatEcosystem.LNREADER -> lnReaderHandler.checkUpdates(installedRecords.filter { it.ecosystem == CompatEcosystem.LNREADER })
                CompatEcosystem.MANGAYOMI -> mangayomiHandler.checkUpdates(installedRecords.filter { it.ecosystem == CompatEcosystem.MANGAYOMI })
                CompatEcosystem.CLOUDSTREAM -> cloudStreamHandler.checkUpdates(installedRecords.filter { it.ecosystem == CompatEcosystem.CLOUDSTREAM })
                else -> emptyList()
            }
            if (ecosystemUpdates.isNotEmpty()) {
                updates[repo.ecosystem] = ecosystemUpdates
            }
        }

        return updates
    }

    /**
     * Detect ecosystem from repository URL or content.
     */
    companion object {
        fun detectEcosystem(url: String): CompatEcosystem? {
            val lower = url.lowercase()
            return when {
                lower.contains("tachiyomi") || lower.contains("mihon") || lower.contains("keiyoushi") || lower.contains("mhrepo") -> CompatEcosystem.TACHIYOMI
                lower.contains("legado") || lower.contains("readera") -> CompatEcosystem.LEGADO
                lower.contains("lnreader") -> CompatEcosystem.LNREADER
                lower.contains("mangayomi") -> CompatEcosystem.MANGAYOMI
                lower.contains("cloudstream") -> CompatEcosystem.CLOUDSTREAM
                else -> null
            }
        }
    }
}