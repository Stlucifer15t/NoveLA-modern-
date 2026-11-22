package my.noveldokusha.features.foryou

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import my.noveldokusha.core.ExtensionManager
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.extension_compat.core.CompatRepositoryManager
import my.noveldokusha.scraper.LuaSourceLoader
import com.google.gson.Gson
import javax.inject.Inject

/** Where the one-shot WebNovel provisioning stands, so the tab can say so in plain words. */
enum class DefaultSourceState { NotNeeded, Working, Installed, AlreadyThere, NotFound, Failed }

data class DefaultSourceStatus(
    val state: DefaultSourceState = DefaultSourceState.NotNeeded,
    val name: String = "",
    val detail: String = "",
)

/**
 * Binds a default source for For You on the first run: if nothing named WebNovel is installed
 * yet, look for it where this app already knows how to get sources — the cached list from the
 * repository in Settings (Lua plugin), then the third-party repositories (Legado JSON,
 * Mihon, …) — and install it with those same paths.
 *
 * What it will not do: invent a repository URL, or retry forever. One attempt per install
 * ([AppPreferences.FOR_YOU_DEFAULT_SOURCE_DONE]), because a first run with no network must not
 * spend a request on every launch, and a silent second chance is how a screen ends up
 * "installing…" forever. The status it lands on is shown in the tab, so a miss is visible and
 * explainable rather than an empty grid.
 */
class ForYouSourceProvisioner @Inject constructor(
    private val appPreferences: AppPreferences,
    private val extensionManager: ExtensionManager,
    private val luaSourceLoader: LuaSourceLoader,
    private val compatManager: CompatRepositoryManager,
    private val engine: ForYouEngine,
) {
    private val _status = MutableStateFlow(DefaultSourceStatus())
    val status: StateFlow<DefaultSourceStatus> = _status.asStateFlow()

    private var started = false

    suspend fun ensureOnce(sources: List<my.noveldokusha.scraper.SourceInterface.Catalog>) {
        if (started) return
        started = true
        val hint = ForYouEngine.DEFAULT_HINT
        val already = sources.firstOrNull { hint in engine.sourceName(it).lowercase() }
        if (already != null) {
            _status.value = DefaultSourceStatus(DefaultSourceState.AlreadyThere, engine.sourceName(already))
            return
        }
        if (appPreferences.FOR_YOU_DEFAULT_SOURCE_DONE.value) {
            _status.value = DefaultSourceStatus(DefaultSourceState.NotNeeded)
            return
        }
        // Claim the attempt before the work, not after: a process killed mid-download must
        // not turn every later launch into a fresh attempt.
        appPreferences.FOR_YOU_DEFAULT_SOURCE_DONE.value = true
        _status.value = DefaultSourceStatus(DefaultSourceState.Working)

        // 1. The Lua repository configured in Settings. Only the cached index is consulted:
        //    refreshing it belongs to the Extensions screen, and a first run must not fire a
        //    second fetch of the same list from behind the user.
        val lua = appPreferences.EXTENSIONS_AVAILABLE_CACHE.value
            .firstOrNull { hint in it.name.lowercase() && it.codeUrl.isNotBlank() }
        if (lua != null) {
            val outcome = runCatching {
                if (!luaSourceLoader.downloadAndCacheScript(lua.id, lua.codeUrl)) {
                    error("download failed")
                }
                extensionManager.installExtensionFromInfo(
                    id = lua.id,
                    name = lua.name,
                    version = lua.version,
                    language = lua.language,
                    imageUrl = lua.iconUrl.ifBlank { null },
                    codeUrl = lua.codeUrl,
                )
                extensionManager.updateExtensionSettings(
                    lua.id,
                    Gson().toJson(
                        mapOf("codeUrl" to lua.codeUrl, "content_type" to lua.contentType)
                    ),
                )
            }
            _status.value = outcome.fold(
                onSuccess = { DefaultSourceStatus(DefaultSourceState.Installed, lua.name) },
                onFailure = {
                    DefaultSourceStatus(
                        state = DefaultSourceState.Failed,
                        name = lua.name,
                        detail = it.message.orEmpty(),
                    )
                },
            )
            return
        }

        // 2. The third-party repositories, if any are subscribed.
        for (repo in compatManager.repositories.value.filter { it.enabled }) {
            val entries = runCatching { compatManager.entriesOf(repo.id) }.getOrDefault(emptyList())
            val hit = entries.firstOrNull { entry ->
                hint in entry.name.lowercase() || hint in entry.key.lowercase()
            } ?: continue
            if (compatManager.installState(hit) == CompatRepositoryManager.InstallState.INSTALLED) {
                _status.value = DefaultSourceStatus(DefaultSourceState.AlreadyThere, hit.name)
                return
            }
            val result = runCatching { compatManager.install(hit) }
            val outcome = result.getOrNull()
            _status.value = when {
                outcome == null -> DefaultSourceStatus(
                    state = DefaultSourceState.Failed,
                    name = hit.name,
                    detail = result.exceptionOrNull()?.message.orEmpty(),
                )

                !outcome.ok -> DefaultSourceStatus(DefaultSourceState.Failed, hit.name, outcome.message)
                else -> DefaultSourceStatus(DefaultSourceState.Installed, hit.name, outcome.message)
            }
            return
        }

        _status.value = DefaultSourceStatus(
            state = DefaultSourceState.NotFound,
            detail = if (compatManager.repositories.value.none { it.enabled }) {
                NO_REPO
            } else {
                NOT_IN_REPO
            },
        )
    }

    private companion object {
        const val NO_REPO = "no repositories subscribed"
        const val NOT_IN_REPO = "not present in the cached index or any subscribed repository"
    }
}
