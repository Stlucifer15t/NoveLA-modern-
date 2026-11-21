package my.noveldokusha.extension_compat.core

import my.noveldokusha.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatRepositoryManager @Inject constructor(
    private val networkClient: NetworkClient,
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

    suspend fun addRepository(repo: RepoInfo) {
        val current = _repositories.value
        if (current.any { it.id == repo.id }) {
            _repositories.value = current.map { if (it.id == repo.id) repo else it }
        } else {
            _repositories.value = current + repo
        }
        syncRepository(repo)
    }

    suspend fun removeRepository(repoId: String) {
        _repositories.value = _repositories.value.filter { it.id != repoId }
    }

    suspend fun setRepositoryEnabled(repoId: String, enabled: Boolean) {
        _repositories.value = _repositories.value.map { repo ->
            if (repo.id == repoId) repo.copy(enabled = enabled) else repo
        }
        if (enabled) {
            _repositories.value.find { it.id == repoId }?.let { syncRepository(it) }
        }
    }

    suspend fun syncRepository(repo: RepoInfo) {
        if (!repo.enabled) return
        _syncState.value = SyncState.Syncing(repo.ecosystem)
        Timber.d("Syncing ${repo.ecosystem.displayName} repo: ${repo.url}")
        _syncState.value = SyncState.Success(0)
    }

    suspend fun syncAll() {
        _repositories.value.filter { it.enabled }.forEach { syncRepository(it) }
    }
}