package my.noveldokusha.scraper

import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.utils.refererFor
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.scraper.sources.LocalSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scraper @Inject constructor(
    networkClient: NetworkClient,
    localSources: Set<@JvmSuppressWildcards LocalSource>,
    @Suppress("UNUSED_PARAMETER") appPreferences: AppPreferences,
    // Интерфейс вместо LuaSourceLoader — нет зависимости от Android Context
    private val luaSourceProvider: LuaSourceProvider,
    // Compat-источники других экосистем (Tachiyomi/Legado/LNReader/...).
    // Дефолт — пустой провайдер, чтобы тесты и DI без binding'а не падали.
    private val compatSourceProvider: CompatSourceProvider = EmptyCompatSourceProvider,
) {
    val databasesList = setOf(
        NovelUpdates(networkClient),
        BakaUpdates(networkClient)
    )

    val localSourcesList: Set<SourceInterface> = localSources

    private val scraperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _luaSources = MutableStateFlow<Set<SourceInterface>>(emptySet())
    val luaSources: StateFlow<Set<SourceInterface>> = _luaSources.asStateFlow()

    private val _compatSources = MutableStateFlow<Set<SourceInterface>>(emptySet())
    val compatSources: StateFlow<Set<SourceInterface>> = _compatSources.asStateFlow()

    init {
        scraperScope.launch {
            luaSourceProvider.sourcesFlow.collect { sources ->
                _luaSources.value = sources.toSet()
                Timber.d("Lua sources updated: ${sources.size}")
            }
        }
        scraperScope.launch {
            compatSourceProvider.sourcesFlow.collect { sources ->
                _compatSources.value = sources.toSet()
                Timber.d("Compat sources updated: ${sources.size}")
            }
        }
    }

    suspend fun awaitLoaded() {
        luaSourceProvider.awaitLoaded()
        compatSourceProvider.awaitLoaded()
    }

    fun clearCache() {
        luaSourceProvider.clearCache()
        compatSourceProvider.clearCache()
    }

    /** Все источники включая CachedSource заглушки (для UI) */
    val sourcesList: Set<SourceInterface>
        get() = localSourcesList + _luaSources.value + _compatSources.value

    /** Только загруженные реальные Lua + compat источники (для data-операций) */
    val loadedSourcesList: Set<SourceInterface>
        get() = localSourcesList +
            luaSourceProvider.loadedSourcesFlow.value.toSet() +
            compatSourceProvider.loadedSourcesFlow.value.toSet()

    val sourcesCatalogListFlow: kotlinx.coroutines.flow.Flow<List<SourceInterface.Catalog>> =
        kotlinx.coroutines.flow.combine(_luaSources, _compatSources) { lua, compat ->
            (localSourcesList + lua + compat).filterIsInstance<SourceInterface.Catalog>()
        }

    val sourcesLanguagesListFlow: kotlinx.coroutines.flow.Flow<List<my.noveldokusha.core.LanguageCode>> =
        sourcesCatalogListFlow.map { catalogs ->
            catalogs.mapNotNull { it.language }.distinct()
        }

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean {
        val a = if (endsWith("/")) this else "$this/"
        val b = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return a.startsWith(b)
    }

    fun getCompatibleSource(url: String): SourceInterface? =
        loadedSourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    /** Referer для картинок книги/каталога: глобал плагина, иначе хост страницы. */
    fun coverReferer(pageUrl: String): String {
        val custom = getCompatibleSource(pageUrl)?.referer?.trim().orEmpty()
        if (custom.isNotEmpty()) return custom
        return refererFor(pageUrl)
    }

    /** Источник книги, если он умеет отдавать отзывы (проверка наличия функции — в hasBookReviews). */
    fun getCompatibleSourceReviews(url: String): SourceInterface.Reviews? =
        loadedSourcesList.filterIsInstance<SourceInterface.Reviews>()
            .find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleSourceCatalog(url: String): SourceInterface.Catalog? =
        loadedSourcesList.filterIsInstance<SourceInterface.Catalog>()
            .find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleDatabase(url: String): DatabaseInterface? =
        databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun isUrlSupported(url: String) = sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) } != null

    fun getSourceId(url: String): String? = sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }?.id
}
