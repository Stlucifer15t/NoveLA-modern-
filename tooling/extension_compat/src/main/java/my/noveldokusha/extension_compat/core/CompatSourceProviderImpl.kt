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
 * Загрузчик одной экосистемы: из установленного record'а создаёт источники.
 * Реализации — в пакетах экосистем (legado/lnreader/...).
 * DI: multibinding `@IntoSet CompatExtensionLoader`.
 */
interface CompatExtensionLoader {
    val ecosystem: CompatEcosystem

    /**
     * Создать источники из payload'а record'а.
     * @throws CompatException при фатальной ошибке загрузки (диагностика).
     */
    suspend fun load(record: CompatInstallRecord, payload: ByteArray): List<SourceInterface>

    /** Очистить кэш рантайма (dex cache, JS bytecode...). */
    fun clearCache() {}
}

/**
 * Реализация [CompatSourceProvider] для compat-экосистем.
 *
 * Жизненный цикл:
 *  1. init → читает registry из [CompatStorage], запускает reload()
 *  2. reload(): для каждой включённой записи вызывает loader её экосистемы
 *     под таймаутом [CompatTimeouts.LOAD_MS]; ошибки изолируются —
 *     упавший источник не роняет остальные, попадает в [CompatDiagnostics]
 *  3. sourcesFlow — все успешно загруженные источники (+ заглушки для UI? нет:
 *     UI список установок берётся из storage.records, а flow — только реальные)
 *  4. awaitLoaded() — первый reload завершён
 */
@Singleton
class CompatSourceProviderImpl @Inject constructor(
    private val storage: CompatStorage,
    private val diagnostics: CompatDiagnostics,
    @CompatLoaders private val loaders: Set<CompatExtensionLoader>,
) : CompatSourceProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sourcesFlow = MutableStateFlow<List<SourceInterface>>(emptyList())
    override val sourcesFlow: StateFlow<List<SourceInterface>> = _sourcesFlow.asStateFlow()

    private val _loadedSourcesFlow = MutableStateFlow<List<SourceInterface>>(emptyList())
    override val loadedSourcesFlow: StateFlow<List<SourceInterface>> = _loadedSourcesFlow.asStateFlow()

    private val _infos = MutableStateFlow<List<CompatSourceInfo>>(emptyList())
    /** UI-описание установленных расширений (включая ошибки загрузки). */
    val infos: StateFlow<List<CompatSourceInfo>> = _infos.asStateFlow()

    private val loadedSignal = CompletableDeferred<Unit>()
    private val reloadMutex = Mutex()

    private val loadersByEcosystem: Map<CompatEcosystem, CompatExtensionLoader> =
        loaders.associateBy { it.ecosystem }

    init {
        scope.launch {
            reloadInternal()
            // Автоперезагрузка при изменении registry (install/uninstall/toggle)
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
        loaders.forEach { runCatching { it.clearCache() } }
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
            CompatEcosystem.TACHIYOMI, // ключ отчёта — агрегированная строка всех экосистем
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
            // getVideoList возвращает null по умолчанию — источник без видео-поддержки
            // просто не переопределяет метод; capability VIDEO ставится по contentType.
        }
        return caps
    }

    // ── Управление установками (вызывается из UI/ViewModel) ──────────────────

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
     * Запись установки по ключу из индекса репозитория — по ней UI решает,
     * «Установлено / Обновить / Не установлено» ([CompatRepositoryManager.installState]).
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
