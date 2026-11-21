package my.noveldokusha.scraper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Провайдер совместимых (compat) источников из других экосистем:
 * Tachiyomi/Aniyomi APK, Legado bookSource JSON, LNReader JS-плагины,
 * Mangayomi JS, CloudStream .cs3.
 *
 * Зеркалит [LuaSourceProvider]: модуль scraper не зависит от Android Context
 * и от реализации хранения. Реализация находится в модуле extension_compat.
 */
interface CompatSourceProvider {

    /** Установленные compat-источники (включая заглушки для UI). */
    val sourcesFlow: Flow<List<SourceInterface>>

    /** Только успешно загруженные реальные источники (для data-операций). */
    val loadedSourcesFlow: StateFlow<List<SourceInterface>>

    /** Ждёт завершения первой загрузки. Возвращает мгновенно если уже загружены. */
    suspend fun awaitLoaded()

    /** Перезагрузить установленные compat-источники с диска. */
    suspend fun reload()

    /** Очистить кэш (для Tachiyomi — оптимизированный dex и т.п.). */
    fun clearCache()
}

/** Пустая реализация: используется когда compat-модуль не подключён (тесты, DI fallback). */
object EmptyCompatSourceProvider : CompatSourceProvider {
    override val sourcesFlow: kotlinx.coroutines.flow.Flow<List<SourceInterface>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
    override val loadedSourcesFlow: kotlinx.coroutines.flow.StateFlow<List<SourceInterface>> =
        kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override suspend fun awaitLoaded() {}
    override suspend fun reload() {}
    override fun clearCache() {}
}
