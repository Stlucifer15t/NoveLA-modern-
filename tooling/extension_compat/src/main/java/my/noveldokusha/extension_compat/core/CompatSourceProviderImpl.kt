package my.noveldokusha.extension_compat.core

import my.noveldokusha.scraper.CompatSourceProvider
import my.noveldokusha.scraper.SourceInterface
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loader of one ecosystem: turns an installed record into sources.
 * Implementations live in the ecosystem packages (legado/lnreader/...).
 * DI: multibinding `@IntoSet CompatExtensionLoader`.
 */
interface CompatExtensionLoader {
    val ecosystem: CompatEcosystem

    /**
     * Build sources from the record's payload.
     * @throws CompatException on a fatal load error (surfaced in diagnostics).
     */
    suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface>

    /** Clear the runtime cache (dex cache, JS bytecode...). */
    fun clearCache() {}
}

/**
 * The loader registry, injected as its own type.
 *
 * A bare `Set<CompatExtensionLoader>` injection point could not be satisfied by this
 * module — neither through `@Binds @IntoSet` nor through `@Multibinds` +
 * `@Provides @IntoSet`: `:app:hiltJavaCompileDebug` kept reporting
 * `MissingBinding: java.util.Set<? extends CompatExtensionLoader>`. Wrapping the list in
 * a class gives Dagger a plain type, and it is also what unit tests construct directly.
 */
class CompatLoaders(val loaders: List<CompatExtensionLoader>)

/**
 * [CompatSourceProvider] implementation for compat ecosystems.
 *
 * Lifecycle:
 *  1. init → reads the registry from [CompatStorage] and runs reload()
 *  2. reload(): every enabled record is handed to its ecosystem's loader
 *     under [CompatTimeouts.LOAD_MS]; failures are isolated —
 *     a failing source does not take the others down, it goes to [CompatDiagnostics]
 *  3. sourcesFlow — every successfully loaded source (no UI placeholders:
 *     the install list comes from storage.records, the flow holds real sources)
 *  4. awaitLoaded() — the first reload has finished
 */
@Singleton
class CompatSourceProviderImpl @Inject constructor(
    private val storage: CompatStorage,
    private val diagnostics: CompatDiagnostics,
    private val loaderSet: CompatLoaders,
) : CompatSourceProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sourcesFlow = MutableStateFlow<List<SourceInterface>>(emptyList())
    override val sourcesFlow: StateFlow<List<SourceInterface>> = _sourcesFlow.asStateFlow()

    private val _loadedSourcesFlow = MutableStateFlow<List<SourceInterface>>(emptyList())
    override val loadedSourcesFlow: StateFlow<List<SourceInterface>> = _loadedSourcesFlow.asStateFlow()

    private val _infos = MutableStateFlow<List<CompatSourceInfo>>(emptyList())
    /** UI description of installed extensions (including load errors). */
    val infos: StateFlow<List<CompatSourceInfo>> = _infos.asStateFlow()

    private val loadedSignal = CompletableDeferred<Unit>()
    private val reloadMutex = Mutex()

    private val loadersByEcosystem: Map<CompatEcosystem, CompatExtensionLoader> =
        loaderSet.loaders.associateBy { it.ecosystem }

    init {
        scope.launch {
            reloadInternal()
            // Reload whenever the registry changes (install/uninstall/toggle)
            storage.records.collect {
                reloadInternal()
            }
        }
    }

    override suspend fun awaitLoaded() = loadedSignal.await()

    override suspend fun reload() {
        reloadMutex.withLock {
            doReload()
        }
    }

    override fun clearCache() {
        loaderSet.loaders.forEach { runCatching { it.clearCache() } }
    }

    private suspend fun reloadInternal() {
        reloadMutex.withLock {
            doReload()
        }
    }

    private suspend fun doReload() {
        val records = storage.records.value.filter { it.enabled }
        val loaded = mutableListOf<SourceInterface>()
        val infos = mutableListOf<CompatSourceInfo>()

        for (record in records) {
            val loader = loadersByEcosystem[record.ecosystem]
            if (loader == null) {
                infos += CompatSourceInfo(
                    record = record,
                    loadedSources = 0,
                    loadError = "No loader for ecosystem ${record.ecosystem.id}",
                )
                diagnostics.recordError(
                    sourceId = record.id,
                    ecosystem = record.ecosystem,
                    operation = "load",
                    error = CompatException("No loader for ${record.ecosystem.id}"),
                )
                continue
            }
            val payload = storage.readPayload(record)
            if (payload == null) {
                infos += CompatSourceInfo(record, 0, "Payload file missing")
                diagnostics.recordError(
                    sourceId = record.id,
                    ecosystem = record.ecosystem,
                    operation = "load",
                    error = CompatException("Payload missing for ${record.id}"),
                )
                continue
            }

            val result = withTimeoutOrNull(CompatTimeouts.LOAD_MS) {
                runCatching { loader.load(record, payload) }
            }
            when {
                result == null -> {
                    val e = CompatTimeoutException("load", CompatTimeouts.LOAD_MS)
                    infos += CompatSourceInfo(record, 0, e.message)
                    diagnostics.recordError(record.id, record.ecosystem, "load", e, timedOut = true)
                    Timber.w("Compat: load timeout for ${record.id}")
                }
                result.isFailure -> {
                    val e = result.exceptionOrNull() ?: Exception("unknown")
                    infos += CompatSourceInfo(record, 0, e.message ?: "load failed")
                    diagnostics.recordError(record.id, record.ecosystem, "load", e)
                    Timber.e(e, "Compat: load failed for ${record.id}")
                }
                else -> {
                    val sources = result.getOrThrow()
                    loaded += sources
                    infos += CompatSourceInfo(
                        record = record,
                        loadedSources = sources.size,
                        capabilities = capabilitiesOf(sources),
                    )
                    Timber.d("Compat: loaded ${sources.size} source(s) from ${record.id}")
                }
            }
        }

        _sourcesFlow.value = loaded
        _loadedSourcesFlow.value = loaded
        _infos.value = infos
        val okCount = infos.count { it.loadError == null }
        diagnostics.setReportLine(
            CompatEcosystem.TACHIYOMI, // report key — an aggregated line across all ecosystems
            "loaded ${loaded.size} sources from ${records.size} records ($okCount ok, ${records.size - okCount} failed)",
        )
        if (!loadedSignal.isCompleted) loadedSignal.complete(Unit)
    }

    private fun capabilitiesOf(sources: List<SourceInterface>): Set<CompatCapability> {
        val caps = mutableSetOf<CompatCapability>()
        for (s in sources) {
            if (s is SourceInterface.Catalog) {
                caps += CompatCapability.SEARCH
                caps += CompatCapability.DETAILS
                caps += CompatCapability.CHAPTERS
                if (s is SourceInterface.FilterableCatalog) caps += CompatCapability.FILTERS
                when (s.contentType) {
                    "video" -> caps += CompatCapability.VIDEO
                    "manga" -> caps += CompatCapability.PAGES
                    else -> caps += CompatCapability.CHAPTER_TEXT
                }
            }
            // getVideoList returns null by default — a source without video support
            // simply does not override it; the VIDEO capability follows contentType.
        }
        return caps
    }

    // ── Install management (called from the UI/ViewModel) ──────────────────────

    fun install(record: CompatInstallRecord, payload: ByteArray) {
        storage.writePayload(record.ecosystem, record.payloadPath, payload)
        storage.upsert(record.copy(installedAt = System.currentTimeMillis()))
    }

    fun uninstall(id: String) {
        storage.getRecord(id)?.let { storage.deletePayload(it) }
        storage.remove(id)
        diagnostics.clearErrors(id)
    }

    fun setEnabled(id: String, enabled: Boolean) = storage.setEnabled(id, enabled)

    /**
     * Installed record looked up by its repository index key — the UI uses it to decide
     * between "Installed / Update / Not installed" ([CompatRepositoryManager.installState]).
     */
    fun findRecord(ecosystem: CompatEcosystem, entryKey: String): CompatInstallRecord? =
        storage.records.value.firstOrNull {
            it.ecosystem == ecosystem && (it.entryKey == entryKey || it.id == "${ecosystem.id}:$entryKey")
        }

    fun updatePayload(id: String, payload: ByteArray, version: String, versionCode: Long) {
        val record = storage.getRecord(id) ?: return
        storage.writePayload(record.ecosystem, record.payloadPath, payload)
        storage.upsert(record.copy(version = version, versionCode = versionCode))
    }
}
