package my.noveldokusha.extensions

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
import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallIds
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatRepoEntry
import my.noveldokusha.extension_compat.core.CompatRepository
import my.noveldokusha.extension_compat.core.CompatRepositoryManager
import my.noveldokusha.extension_compat.core.CompatRuntime
import my.noveldokusha.extension_compat.core.CompatSourceInfo
import my.noveldokusha.extension_compat.core.CompatSourceProviderImpl
import timber.log.Timber
import javax.inject.Inject

/**
 * Сторонние репозитории расширений: Legado JSON, Mihon/keiyoushi, LNReader,
 * Mangayomi, CloudStream — подписки, каталог источников и установка.
 *
 * Отдельный от [ExtensionsManagerViewModel]: тот ведёт Lua-плагины. Состояние
 * полностью выводится из flow compat-слоя (репозитории, каталог, записи
 * установки, статусы загрузки), поэтому никаких ручных «обнови список» нет:
 * установка/удаление само прилетает из [CompatStorage].
 */
@HiltViewModel
class CompatExtensionsViewModel @Inject constructor(
    private val manager: CompatRepositoryManager,
    provider: CompatSourceProviderImpl,
) : ViewModel() {

    private val local = MutableStateFlow(CompatExtensionsLocal())

    init {
        // Каталоги живут в памяти менеджера, но кэшированы на диск — поднимаем их,
        // чтобы после перезапуска список не был пустым до первого refresh.
        viewModelScope.launch { manager.primeFromCache() }
    }

    /** Каталог + подписки: читаем из кэша диска, если память пуста. */
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
    ) { (repos, entriesByRepo), runtime, view ->
        val selectedId = view.filterRepoId ?: repos.firstOrNull { it.enabled }?.id ?: repos.firstOrNull()?.id
        val all = if (entriesByRepo.isEmpty()) {
            repos.map { it.id to emptyList<CompatRepoEntry>() }.toMap()
        } else {
            entriesByRepo
        }
        val pool = selectedId?.let { all[it] } ?: all.values.flatten()
        val query = view.searchQuery.trim().lowercase()
        val filtered = pool
            .filter { view.filterEcosystem == null || it.ecosystem == view.filterEcosystem }
            .filter { query.isEmpty() || it.matches(query) }
            .sortedWith(
                compareByDescending<CompatRepoEntry> { CompatRuntime.isRunnable(it.ecosystem) }
                    .thenBy { it.name.lowercase() }
            )

        val installedById = runtime.records.associateBy { it.id }
        CompatExtensionsState(
            repositories = repos,
            busyRepos = runtime.busy,
            installed = runtime.records,
            loadInfos = runtime.infos,
            selectedRepoId = selectedId,
            entries = filtered.take(MAX_VISIBLE_ENTRIES),
            truncated = filtered.size > MAX_VISIBLE_ENTRIES,
            totalCount = pool.size,
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

    // ── События ──────────────────────────────────────────────────────────────

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
        /** Каталог keiyoushi отдаёт ~1300 записей: режем хвост, а не рендерим всё. */
        const val MAX_VISIBLE_ENTRIES = 250
    }
}

private data class CompatRuntimeSnapshot(
    val records: List<CompatInstallRecord>,
    val busy: Set<String>,
    val infos: List<CompatSourceInfo>,
)

/** Локальный выбор пользователя — фильтры и состояние диалога добавления. */
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
    val loadInfos: List<CompatSourceInfo> = emptyList(),
    val busyRepos: Set<String> = emptySet(),
    val selectedRepoId: String? = null,
    val entries: List<CompatRepoEntry> = emptyList(),
    val installStates: Map<String, CompatRepositoryManager.InstallState> = emptyMap(),
    val totalCount: Int = 0,
    val truncated: Boolean = false,
    val searchQuery: String = "",
    val filterEcosystem: CompatEcosystem? = null,
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
    data class OnSearchQuery(val query: String) : CompatExtensionsEvent
    data class OnInstall(val entry: CompatRepoEntry) : CompatExtensionsEvent
    data class OnInstallAll(val repoId: String) : CompatExtensionsEvent
    data class OnUpdateAll(val repoId: String) : CompatExtensionsEvent
    data class OnUninstall(val recordId: String) : CompatExtensionsEvent
    data class OnToggleSource(val recordId: String, val enabled: Boolean) : CompatExtensionsEvent
}

/**
 * Ссылку вставляют «как есть» из браузера — с якорем, слеком и без схемы.
 * Чистим то, что ломает распознавание формата, остальное решает RepoUrlResolver.
 */
private object RepoUrlHint {
    fun normalize(input: String): String {
        val trimmed = input.trim().substringBefore('#').trimEnd('/')
        if (trimmed.isEmpty() || !trimmed.contains('.')) return ""
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }
}
