package my.dreamora.extensions

import my.dreamora.core.appPreferences.AppPreferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.dreamora.extension_compat.core.CompatEcosystem
import my.dreamora.extension_compat.core.CompatInstallIds
import my.dreamora.extension_compat.core.CompatInstallRecord
import my.dreamora.extension_compat.core.CompatRepoEntry
import my.dreamora.extension_compat.core.CompatRepository
import my.dreamora.extension_compat.core.CompatRepositoryManager
import my.dreamora.extension_compat.core.CompatRuntime
import my.dreamora.extension_compat.core.CompatSourceInfo
import my.dreamora.extension_compat.core.CompatSourceProviderImpl
import timber.log.Timber
import javax.inject.Inject

/**
* Third-party extension repositories: Legado JSON, Mihon/keiyoushi, LNReader,
* Mangayomi, CloudStream — subscriptions, source catalog and installation.
 *
* Separate from [ExtensionsManagerViewModel], which owns the Lua plugins. The state
* is derived entirely from the compat layer's flows (repositories, catalog, install
* records, load statuses) — there is no manual "refresh the list" anywhere:
* installing and uninstalling arrives from [CompatStorage] on its own.
 */
@HiltViewModel
class CompatExtensionsViewModel @Inject constructor(
    private val manager: CompatRepositoryManager,
    provider: CompatSourceProviderImpl,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val local = MutableStateFlow(CompatExtensionsLocal())

    init {
// Catalogs live in manager memory but are cached on disk — load them up so that
// the list is not empty after a restart, before the first refresh.
        viewModelScope.launch { manager.primeFromCache() }
    }

/** Catalog + subscriptions: read from the disk cache when memory is empty. */
    private val repoSnapshot = combine(
        manager.repositories,
        manager.entries,
    ) { repos, entries -> repos to entries }

    private val runtimeSnapshot = combine(
        manager.installedRecords,
        manager.busy,
        provider.infos,
    ) { records, busy, infos -> CompatRuntimeSnapshot(records, busy, infos) }

    val state: StateFlow<CompatExtensionsState> = combine(
        repoSnapshot,
        runtimeSnapshot,
        local,
        appPreferences.EXTENSIONS_SHOW_ADULT.flow(),
    ) { (repos, entriesByRepo), runtime, view, showAdult ->
        val selectedId = view.filterRepoId ?: repos.firstOrNull { it.enabled }?.id ?: repos.firstOrNull()?.id
        val all = if (entriesByRepo.isEmpty()) {
            repos.map { it.id to emptyList<CompatRepoEntry>() }.toMap()
        } else {
            entriesByRepo
        }
        // A language filter asks "where are the Legado sources?", so it searches every subscription.
        // Without one, the list shows the repository the reader selected.
        val pool = if (view.filterEcosystem != null) {
            all.values.flatten()
        } else {
            selectedId?.let { all[it] } ?: all.values.flatten()
        }
        val byEcosystem = pool.filter { view.filterEcosystem == null || it.ecosystem == view.filterEcosystem }
        val hiddenAdult = if (showAdult) 0 else byEcosystem.count { it.nsfw }
        val query = view.searchQuery.trim().lowercase()
        val filtered = byEcosystem
            .filter { showAdult || !it.nsfw }
            .filter { query.isEmpty() || it.matches(query) }
            .sortedWith(
                compareByDescending<CompatRepoEntry> { CompatRuntime.isRunnable(it.ecosystem) }
                    .thenBy { it.name.lowercase() }
            )
        val visibleInstalled = runtime.records.filter { record ->
            (showAdult || !record.nsfw) && (view.filterEcosystem == null || record.ecosystem == view.filterEcosystem)
        }

        val installedById = runtime.records.associateBy { it.id }
        CompatExtensionsState(
            repositories = repos,
            busyRepos = runtime.busy,
            installed = runtime.records,
            visibleInstalled = visibleInstalled,
            loadInfos = runtime.infos,
            selectedRepoId = selectedId,
            entries = filtered.take(MAX_VISIBLE_ENTRIES),
            truncated = filtered.size > MAX_VISIBLE_ENTRIES,
            totalCount = pool.size,
            hiddenAdult = hiddenAdult,
            showAdult = showAdult,
            installStates = filtered.associate { entry ->
                entry.key to installStateOf(entry, installedById)
            },
            searchQuery = view.searchQuery,
            filterEcosystem = view.filterEcosystem,
            showAddRepo = view.showAddRepo,
            addRepoUrl = view.addRepoUrl,
            feedback = view.feedback,
            isWorking = view.isWorking,
            error = view.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompatExtensionsState())

    private fun installStateOf(
        entry: CompatRepoEntry,
        installedById: Map<String, CompatInstallRecord>,
    ): CompatRepositoryManager.InstallState {
        val record = installedById[CompatInstallIds.recordId(entry.ecosystem, entry.key)]
            ?: return CompatRepositoryManager.InstallState.NOT_INSTALLED
        return when {
            !record.enabled -> CompatRepositoryManager.InstallState.DISABLED
            entry.versionCode > record.versionCode ||
                (entry.versionCode == record.versionCode &&
                    entry.version.isNotBlank() && entry.version != record.version) ->
                CompatRepositoryManager.InstallState.UPDATE_AVAILABLE

            else -> CompatRepositoryManager.InstallState.INSTALLED
        }
    }

    private fun CompatRepoEntry.matches(query: String): Boolean =
        name.lowercase().contains(query) ||
            baseUrl.lowercase().contains(query) ||
            key.lowercase().contains(query) ||
            language.lowercase().contains(query)

// ── Events ────────────────────────────────────────────────────────────────────

    fun onEvent(event: CompatExtensionsEvent) {
        when (event) {
            CompatExtensionsEvent.OnShowAddRepo ->
                local.update { it.copy(showAddRepo = true, feedback = null) }

            CompatExtensionsEvent.OnHideAddRepo ->
                local.update { it.copy(showAddRepo = false, addRepoUrl = "", feedback = null) }

            is CompatExtensionsEvent.OnAddRepoUrlChange ->
                local.update { it.copy(addRepoUrl = event.url) }

            is CompatExtensionsEvent.OnAddRepo -> addRepository(event.url)
            is CompatExtensionsEvent.OnRefreshRepo -> refresh(event.repoId)
            is CompatExtensionsEvent.OnRemoveRepo -> remove(event.repoId, event.removeInstalled)
            is CompatExtensionsEvent.OnToggleRepo ->
                manager.setRepositoryEnabled(event.repoId, event.enabled)

            is CompatExtensionsEvent.OnSelectRepo -> {
                local.update { it.copy(filterRepoId = event.repoId) }
                val id = event.repoId
                if (id != null) {
                    viewModelScope.launch { manager.entriesOf(id) }
                }
            }

            is CompatExtensionsEvent.OnFilterEcosystem ->
                local.update { it.copy(filterEcosystem = event.ecosystem) }

            is CompatExtensionsEvent.OnShowAdult -> {
                appPreferences.EXTENSIONS_SHOW_ADULT.value = event.show
            }

            is CompatExtensionsEvent.OnSearchQuery ->
                local.update { it.copy(searchQuery = event.query) }

            is CompatExtensionsEvent.OnInstall -> install(event.entry)
            is CompatExtensionsEvent.OnInstallAll -> installAll(event.repoId)
            is CompatExtensionsEvent.OnUpdateAll -> updateAll(event.repoId)
            is CompatExtensionsEvent.OnUninstall -> manager.uninstall(event.recordId)
            is CompatExtensionsEvent.OnToggleSource ->
                manager.setSourceEnabled(event.recordId, event.enabled)

            CompatExtensionsEvent.OnDismissError -> local.update { it.copy(error = null) }
        }
    }

    private fun addRepository(rawUrl: String) {
        val url = RepoUrlHint.normalize(rawUrl)
        if (url.isEmpty()) return
        local.update { it.copy(isWorking = true, feedback = "Reading index…", error = null) }
        viewModelScope.launch {
            runCatching { manager.add(url) }
                .onSuccess { outcome ->
                    local.update {
                        it.copy(
                            isWorking = false,
                            feedback = outcome.message.ifBlank {
                                if (outcome.ok) "Added" else "Could not read this repository"
                            },
                            addRepoUrl = if (outcome.ok) "" else url,
                            showAddRepo = !outcome.ok,
                            filterRepoId = outcome.repository?.id ?: it.filterRepoId,
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Compat: add repository failed")
                    local.update {
                        it.copy(
                            isWorking = false,
                            feedback = null,
                            error = e.message ?: e::class.java.simpleName,
                        )
                    }
                }
        }
    }

    private fun refresh(repoId: String) {
        viewModelScope.launch {
            runCatching { manager.refresh(repoId) }
                .onFailure { local.update { s -> s.copy(error = it.message) } }
        }
    }

    private fun remove(repoId: String, removeInstalled: Boolean) {
        viewModelScope.launch {
            runCatching { manager.remove(repoId, removeInstalled) }
                .onFailure { Timber.e(it, "Compat: remove repository failed") }
        }
    }

    private fun install(entry: CompatRepoEntry) {
        viewModelScope.launch {
            local.update { it.copy(isWorking = true) }
            val outcome = runCatching { manager.install(entry) }
            local.update { state ->
                state.copy(
                    isWorking = false,
                    feedback = outcome.getOrNull()?.message ?: outcome.exceptionOrNull()?.message,
                    error = outcome.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun installAll(repoId: String) {
        viewModelScope.launch {
            local.update { it.copy(isWorking = true, feedback = "Installing…") }
            val outcomes = runCatching { manager.installAll(repoId) }
            local.update { state ->
                val list = outcomes.getOrNull()
                state.copy(
                    isWorking = false,
                    feedback = when {
                        list == null -> "Install failed"
                        list.isEmpty() -> "Everything from this repository is already installed"
                        else -> {
                            val ok = list.count { it.ok }
                            val runnable = list.count { it.ok && it.runnable }
                            "Installed $ok of ${list.size}; $runnable usable in this build"
                        }
                    },
                    error = outcomes.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun updateAll(repoId: String) {
        viewModelScope.launch {
            local.update { it.copy(isWorking = true) }
            val updated = runCatching { manager.updateAll(repoId) }
            local.update {
                it.copy(
                    isWorking = false,
                    feedback = updated.getOrNull()?.let { n ->
                        if (n == 0) "All sources up to date" else "Updated $n source(s)"
                    },
                    error = updated.exceptionOrNull()?.message,
                )
            }
        }
    }

    private companion object {
/** The keiyoushi index returns ~1300 rows: cap the tail instead of rendering all. */
        const val MAX_VISIBLE_ENTRIES = 250
    }
}

private data class CompatRuntimeSnapshot(
    val records: List<CompatInstallRecord>,
    val busy: Set<String>,
    val infos: List<CompatSourceInfo>,
)

/** The user's local choices — filters and the add-repository dialog state. */
private data class CompatExtensionsLocal(
    val searchQuery: String = "",
    val filterEcosystem: CompatEcosystem? = null,
    val filterRepoId: String? = null,
    val showAddRepo: Boolean = false,
    val addRepoUrl: String = "",
    val feedback: String? = null,
    val error: String? = null,
    val isWorking: Boolean = false,
)

data class CompatExtensionsState(
    val repositories: List<CompatRepository> = emptyList(),
    val installed: List<CompatInstallRecord> = emptyList(),
    /** [installed] after the 18+ and ecosystem filters: what the list shows. */
    val visibleInstalled: List<CompatInstallRecord> = emptyList(),
    val loadInfos: List<CompatSourceInfo> = emptyList(),
    val busyRepos: Set<String> = emptySet(),
    val selectedRepoId: String? = null,
    val entries: List<CompatRepoEntry> = emptyList(),
    val installStates: Map<String, CompatRepositoryManager.InstallState> = emptyMap(),
    val totalCount: Int = 0,
    val truncated: Boolean = false,
    val searchQuery: String = "",
    val filterEcosystem: CompatEcosystem? = null,
    /** 18+ sources are listed only when the reader has turned them on. */
    val showAdult: Boolean = false,
    /** How many catalog entries the 18+ filter is hiding right now. */
    val hiddenAdult: Int = 0,
    val showAddRepo: Boolean = false,
    val addRepoUrl: String = "",
    val feedback: String? = null,
    val isWorking: Boolean = false,
    val error: String? = null,
) {
    val isRefreshing: Boolean get() = busyRepos.isNotEmpty() || isWorking

    fun runtimeBadgeOf(entry: CompatRepoEntry): String =
        CompatRuntime.of(entry.ecosystem).badge

    fun installedRecordOf(entry: CompatRepoEntry): CompatInstallRecord? =
        installed.firstOrNull { it.id == CompatInstallIds.recordId(entry.ecosystem, entry.key) }
}

sealed interface CompatExtensionsEvent {
    data object OnShowAddRepo : CompatExtensionsEvent
    data object OnHideAddRepo : CompatExtensionsEvent
    data object OnDismissError : CompatExtensionsEvent
    data class OnAddRepoUrlChange(val url: String) : CompatExtensionsEvent
    data class OnAddRepo(val url: String) : CompatExtensionsEvent
    data class OnRefreshRepo(val repoId: String) : CompatExtensionsEvent
    data class OnRemoveRepo(val repoId: String, val removeInstalled: Boolean) : CompatExtensionsEvent
    data class OnToggleRepo(val repoId: String, val enabled: Boolean) : CompatExtensionsEvent
    data class OnSelectRepo(val repoId: String?) : CompatExtensionsEvent
    data class OnFilterEcosystem(val ecosystem: CompatEcosystem?) : CompatExtensionsEvent
    data class OnShowAdult(val show: Boolean) : CompatExtensionsEvent
    data class OnSearchQuery(val query: String) : CompatExtensionsEvent
    data class OnInstall(val entry: CompatRepoEntry) : CompatExtensionsEvent
    data class OnInstallAll(val repoId: String) : CompatExtensionsEvent
    data class OnUpdateAll(val repoId: String) : CompatExtensionsEvent
    data class OnUninstall(val recordId: String) : CompatExtensionsEvent
    data class OnToggleSource(val recordId: String, val enabled: Boolean) : CompatExtensionsEvent
}

/**
* A link is pasted "as is" from the browser — with an anchor, a slash, no scheme.
* Strip what would break format detection; RepoUrlResolver handles the rest.
 */
private object RepoUrlHint {
    fun normalize(input: String): String {
        val trimmed = input.trim().substringBefore('#').trimEnd('/')
        if (trimmed.isEmpty() || !trimmed.contains('.')) return ""
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }
}
