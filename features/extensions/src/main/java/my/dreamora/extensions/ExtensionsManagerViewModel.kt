package my.dreamora.extensions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import my.dreamora.core.ExtensionManager
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.core.appPreferences.ExtensionInfoCached
import my.dreamora.network.NetworkClient
import my.dreamora.scraper.LuaSourceLoader
import my.dreamora.scraper.LuaSourceProvider
import my.dreamora.data.ScraperRepository
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import my.dreamora.core.getLanguageDisplayName
import my.dreamora.text_translator.domain.TranslationManager
import javax.inject.Inject

@HiltViewModel
class ExtensionsManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extensionManager: ExtensionManager,
    private val httpClient: NetworkClient,
    private val appPreferences: AppPreferences,
    private val scraperRepository: ScraperRepository,
private val luaSourceLoader: LuaSourceLoader,          // ← downloads the .lua file
    private val luaSourceProvider: LuaSourceProvider,
    private val translationManager: TranslationManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ExtensionsScreenState())
    val state: StateFlow<ExtensionsScreenState> = _state.asStateFlow()

// ponytail: a reactive flow for Compose so that the translator icon
// in CatalogList updates when the per-plugin toggle flips.
    val pluginEnabledMap: StateFlow<Map<String, Boolean>> =
        appPreferences.TRANSLATION_PLUGIN_ENABLED_MAP.flow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), appPreferences.TRANSLATION_PLUGIN_ENABLED_MAP.value)

/** Serializes import/save/reset of local Lua scripts so that concurrent
*  operations don't run reload() and file writes at the same time. */
    private val localMutex = Mutex()

    private var cachedAvailableExtensions: List<ExtensionInfo>? = null
    private var lastFetchTime: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L

    init {
        _state.update {
            it.copy(
                selectedLanguages = appPreferences.EXTENSIONS_LANGUAGES_FILTER.value,
                repositoryUrl = appPreferences.EXTENSIONS_REPOSITORY_URL.value
            )
        }

// Keep the installed extension list in sync reactively (Lua + compat)
        viewModelScope.launch {
            extensionManager.getInstalledExtensionsFlow().collectLatest { extensions ->
                _state.update { it.copy(extensions = extensions) }
                updateAvailableExtensionsStatus()
            }
        }

        // Compat layer not yet implemented - skip sync

// Load the cache and (when needed) fresh network data — on a background thread
        viewModelScope.launch {
loadCachedExtensions()        // sets cachedAvailableExtensions + lastFetchTime
            loadAllAvailableExtensions()
        }
    }

    fun onEvent(event: ExtensionsScreenEvent) = when (event) {
        is ExtensionsScreenEvent.OnExtensionToggle       -> toggleExtension(event.extensionId, event.enabled)
        is ExtensionsScreenEvent.OnExtensionUninstall    -> uninstallExtension(event.extensionId)
        is ExtensionsScreenEvent.OnExtensionConfigure    -> openTranslationSettings(event.extensionId)
        ExtensionsScreenEvent.OnRefresh                  -> refreshAll()
        ExtensionsScreenEvent.OnShowRepositoryDialog     -> _state.update { it.copy(showRepositoryDialog = true) }
        ExtensionsScreenEvent.OnHideRepositoryDialog     -> _state.update { it.copy(showRepositoryDialog = false) }
        is ExtensionsScreenEvent.OnUpdateRepositoryUrl   -> updateRepositoryUrl(event.url)
        is ExtensionsScreenEvent.OnLanguageFilterToggle  -> toggleLanguageFilter(event.languageCode)
        is ExtensionsScreenEvent.OnLanguageFilterClear   -> clearLanguageFilter(event.languageCode)
        is ExtensionsScreenEvent.OnContentTypeFilterToggle -> toggleContentTypeFilter(event.contentType)

        ExtensionsScreenEvent.OnBackPressed              -> Unit
        is ExtensionsScreenEvent.OnExtensionInstall      -> installExtension(event.extensionId)
        ExtensionsScreenEvent.OnUpdateAll                -> updateAllExtensions()
        is ExtensionsScreenEvent.OnExtensionUninstallById -> uninstallExtensionById(event.extensionId)
        is ExtensionsScreenEvent.OnEditLuaClick           -> openLuaEditor(event.extensionId)
        ExtensionsScreenEvent.OnLuaEditorDismiss          -> closeLuaEditor()
        is ExtensionsScreenEvent.OnLuaEditorChange        -> updateLuaEditorText(event.code)
        ExtensionsScreenEvent.OnLuaEditorSave             -> saveLuaEditor()
        is ExtensionsScreenEvent.OnResetLuaClick          -> resetLuaExtension(event.extensionId)
        ExtensionsScreenEvent.OnTranslationSettingsDismiss -> closeTranslationSettings()
    }



// ── Loading available extensions from the repository ────────────────────────

    private fun loadAllAvailableExtensions(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedAvailableExtensions != null && now - lastFetchTime < CACHE_DURATION_MS) {
            applyCache()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
// An empty repository URL is not an error: with only locally installed plugins
// we stay offline and do not set state.error.
            val repoUrl = _state.value.repositoryUrl.trim()
            if (repoUrl.isEmpty()) {
                _state.update { it.copy(isLoading = false, error = null) }
                return@launch
            }
            try {
                val responseBody = withContext(Dispatchers.IO) {
                    val response = if (forceRefresh) {
                        httpClient.getWithHeaders(repoUrl, mapOf("Cache-Control" to "no-cache"))
                    } else {
                        httpClient.get(repoUrl)
                    }
                    response.body.string()
                }
                val yaml     = Yaml()
                @Suppress("UNCHECKED_CAST")
                val repoIndex = withContext(Dispatchers.Default) {
                    yaml.loadAs(responseBody, Map::class.java)
                } as Map<String, Any>

                @Suppress("UNCHECKED_CAST")
                val languages = repoIndex["languages"] as Map<String, Map<String, Any>>
                val allExt = mutableListOf<ExtensionInfo>()

                languages.forEach { (langCode, langInfo) ->
                    try {
                        val langUrl = langInfo["url"] as String
                        val langBody = withContext(Dispatchers.IO) {
                            httpClient.get(langUrl).body.string()
                        }
                        @Suppress("UNCHECKED_CAST")
                        val langData = withContext(Dispatchers.Default) {
                            yaml.loadAs(langBody, Map::class.java)
                        } as Map<String, Any>

                        @Suppress("UNCHECKED_CAST")
                        val sources = langData["sources"] as List<Map<String, Any>>
                        sources.forEach { src ->
                            val id = src["id"] as String
                            val installedVer = getInstalledVersion(id)
                            val remoteVer = src["version"] as? String ?: "1.0.0"
                            Timber.d("Processing extension: $id, installed: $installedVer, remote: $remoteVer")
                            allExt.add(
                                ExtensionInfo(
                                    id               = id,
                                    name             = src["name"] as? String ?: "Unknown",
                                    description      = src.get("description") as? String ?: "",
                                    author           = src.get("author") as? String ?: "",
                                    version          = installedVer ?: remoteVer,
                                    remoteVersion    = remoteVer,
                                    codeUrl          = src["url"] as String,
                                    iconUrl          = src["icon"] as String,
                                    language         = langCode,
                                    isInstalled      = installedVer != null,
                                    isEnabled        = isEnabled(id),
                                    isUpdateAvailable = isUpdateAvailable(remoteVer, installedVer),
                                    contentType      = src["content_type"] as? String ?: ""
                                )
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to load language $langCode")
                    }
                }

                cachedAvailableExtensions = allExt
                lastFetchTime = now

// Persist to the cache so the next start loads instantly
                saveCachedExtensions(allExt)

                val langList = allExt.flatMap { ext ->
                    ext.language.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { code -> code to ext }
                }
                .groupBy { it.first }
                .map { (code, list) ->
                    ExtensionLanguage(code, getLanguageDisplayName(code), list.size)
                }
                .sortedBy { it.name }

                _state.update {
                    it.copy(
                        availableExtensions = allExt,
                        availableLanguages  = langList,
                        isLoading           = false,
                        error               = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load extensions repository")
                _state.update { it.copy(isLoading = false, error = context.getString(my.dreamora.strings.R.string.ext_failed_to_load, e.message)) }
            }
        }
    }

    private fun applyCache() {
        val cached = cachedAvailableExtensions ?: return
        _state.update { state ->
            state.copy(
                availableExtensions = cached.map { ext ->
                    val installedVer = getInstalledVersion(ext.id)
                    ext.copy(
                        isInstalled       = installedVer != null,
                        isEnabled         = isEnabled(ext.id),
                        version           = installedVer ?: ext.version,
                        isUpdateAvailable = isUpdateAvailable(ext.remoteVersion, installedVer)
                    )
                }
            )
        }
    }

    private suspend fun loadCachedExtensions() {
// Reading SharedPrefs and building the list (dozens of rows) leaves the main thread
// so Extensions screen init never delays the first frame of a cold start.
        val (extensions, langList) = withContext(Dispatchers.IO) {
            val cachedEntries = appPreferences.EXTENSIONS_AVAILABLE_CACHE.value
            if (cachedEntries.isEmpty()) return@withContext null

            val list = cachedEntries.map { cached ->
                ExtensionInfo(
                    id = cached.id,
                    name = cached.name,
                    description = cached.description,
                    author = cached.author,
                    version = cached.version,
                    remoteVersion = cached.remoteVersion,
                    codeUrl = cached.codeUrl,
                    iconUrl = cached.iconUrl,
                    language = cached.language,
                    isInstalled = getInstalledVersion(cached.id) != null,
                    isEnabled = isEnabled(cached.id),
                    isUpdateAvailable = isUpdateAvailable(cached.remoteVersion, getInstalledVersion(cached.id)),
                    contentType = cached.contentType
                )
            }
            val langs = list.flatMap { ext ->
                ext.language.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { code -> code to ext }
            }
            .groupBy { it.first }
            .map { (code, group) ->
                ExtensionLanguage(code, getLanguageDisplayName(code), group.size)
            }
            .sortedBy { it.name }
            list to langs
        } ?: return

        _state.update {
            it.copy(
                availableExtensions = extensions,
                availableLanguages = langList
            )
        }
        Timber.d("Loaded ${extensions.size} extensions from cache")
        cachedAvailableExtensions = extensions
        lastFetchTime = System.currentTimeMillis()
    }

    private fun saveCachedExtensions(extensions: List<ExtensionInfo>) {
        val cached = extensions.map { ext ->
            ExtensionInfoCached(
                id = ext.id,
                name = ext.name,
                description = ext.description,
                author = ext.author,
                version = ext.version,
                remoteVersion = ext.remoteVersion,
                codeUrl = ext.codeUrl,
                iconUrl = ext.iconUrl,
                language = ext.language,
                contentType = ext.contentType
            )
        }
        appPreferences.EXTENSIONS_AVAILABLE_CACHE.value = cached
        Timber.d("Saved ${cached.size} extensions to cache")
    }

// ── Installation ────────────────────────────────────────────────────────────

    /**
* Install process:
* 1. Lua: download the .lua file to disk through LuaSourceLoader
* 2. Compat: install through CompatRepositoryManager
* 3. Save the record to the database through ExtensionManager
* 4. The scraper reloads reactively through the installed-extensions Flow
     */
    private fun installExtension(extensionId: String) {
        viewModelScope.launch {
            val extInfo = _state.value.availableExtensions.find { it.id == extensionId } ?: return@launch
            setInstalling(extensionId, true)
            try {
                // Check if this is a compat extension (has ecosystem metadata)
                val isCompat = extInfo.id.contains("#") || extInfo.id.startsWith("compat_")

                if (isCompat) {
                    // Compat layer not yet implemented - skip
                    Timber.d("Compat installation not yet implemented")
                } else {
                    // Lua extension installation
// Step 1: download the .lua file to disk
                    val downloaded = luaSourceLoader.downloadAndCacheScript(extensionId, extInfo.codeUrl)
                    if (!downloaded) {
                        _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.ext_failed_to_download, extInfo.name)) }
                        return@launch
                    }

// Step 2: write to the database with the NEW version
                    extensionManager.installExtensionFromInfo(
                        id       = extensionId,
                        name     = extInfo.name,
version  = extInfo.remoteVersion, // use the new version!
                        language = extInfo.language,
                        imageUrl = extInfo.iconUrl,
                        codeUrl  = extInfo.codeUrl
                    )
// Step 2b: store codeUrl and content_type in settings as JSON so that
// LuaSourceLoader knows where to re-download on the next start
                    val settingsJson = Gson().toJson(mapOf("codeUrl" to extInfo.codeUrl, "content_type" to extInfo.contentType))
                    extensionManager.updateExtensionSettings(extensionId, settingsJson)

                    Timber.d("Installed extension: ${extInfo.name}")
                }
// Step 3: the scraper refreshes reactively via getInstalledExtensionsFlow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to install ${extInfo.name}")
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.ext_failed_to_install, extInfo.name, e.message)) }
// If installation failed, delete the downloaded file
                luaSourceLoader.removeScript(extensionId)
            } finally {
                setInstalling(extensionId, false)
            }
        }
    }

    private fun updateAllExtensions() {
        viewModelScope.launch {
            val extensionsToUpdate = _state.value.availableExtensions.filter { it.isUpdateAvailable }
            extensionsToUpdate.forEach { ext ->
                installExtension(ext.id)
            }
        }
    }

// ── Removal ──────────────────────────────────────────────────────────────────

    private fun uninstallExtension(extensionId: String) = uninstallExtensionById(extensionId)

    private fun uninstallExtensionById(extensionId: String) {
        viewModelScope.launch {
            try {
                    val isCompat = false // Compat layer not yet implemented
                    if (isCompat) {
                        // Uninstall from compat layer
                    } else {
                    extensionManager.uninstallExtension(extensionId)
                    luaSourceLoader.removeScript(extensionId)
                }
// The scraper refreshes reactively
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to uninstall $extensionId")
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.ext_failed_to_uninstall)) }
            }
        }
    }

// ── Local import / Lua editor ───────────────────────────────────────────────

    private fun luaField(code: String, key: String, fallback: String = ""): String {
        val regex = Regex("""(?m)^\s*$key\s*=\s*["']([^"']+)["']""")
        return regex.find(code)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { fallback }
    }

    /** Reads `key` as either a quoted string ("en,es") or a Lua table ({"en","es"}).
     * Returns comma-separated codes (e.g. "en,es"). */
    private fun luaLanguages(code: String, key: String, fallback: String = "en"): String {
        // Try Lua table format: key = {"en", "es"}
        val tableRegex = Regex("""(?m)^\s*$key\s*=\s*\{\s*["']([^"']+)["']""")
        val first = tableRegex.find(code)?.groupValues?.getOrNull(1)?.trim()
        if (first.isNotBlank()) {
            // Collect all quoted strings in the table
            val all = Regex("""["']([^"']+)["']""").findAll(code.substringAfter(tableRegex.find(code)!!.value))
                .map { it.groupValues[1] }
                .filter { it.isNotBlank() }
            if (all.isNotEmpty()) return all.joinToString(",")
        }
        // Fall back to quoted string format: key = "en,es"
        return luaField(code, key, fallback)
    }

    private suspend fun settingsMap(extensionId: String): Map<String, Any>? {
        val raw = extensionManager.getExtensionSettings(extensionId) ?: return null
        if (raw.isBlank() || raw == "{}") return null
        return try {
            @Suppress("UNCHECKED_CAST")
            if (raw.startsWith("{")) {
                Gson().fromJson(raw, Map::class.java) as? Map<String, Any>
            } else {
// Migration from the old YAML format to JSON
                val legacy = Yaml().loadAs(raw, Map::class.java) as? Map<String, Any>
                if (legacy != null) {
                    val json = Gson().toJson(legacy)
                    extensionManager.updateExtensionSettings(extensionId, json)
                }
                legacy
            }
        } catch (e: Exception) {
            Timber.w(e, "Bad settings for $extensionId: $raw")
            null
        }
    }

    private suspend fun isLocalExtension(extensionId: String): Boolean =
        settingsMap(extensionId)?.get("sourceType")?.toString() == "local"

    private suspend fun getCodeUrl(extensionId: String): String? =
        settingsMap(extensionId)?.get("codeUrl")?.toString()

/** Unique local script id: prefix + base + 5 random characters.
*  The random suffix makes sure separate imports do not overwrite each other,
*  and the generated id is baked into the script once (see [withLocalId]), so a
*  re-import of the same file (edited included) stays idempotent. */
    private fun localStorageId(baseId: String): String {
        val safe = baseId.replace(Regex("""[^A-Za-z0-9_]"""), "_")
            .takeIf { it.isNotBlank() } ?: "local_source"
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val rand = (1..5).map { alphabet.random() }.joinToString("")
        return "local_${safe}_$rand"
    }

/** Puts id = [id] into the Lua code: replaces it or prepends it at the top. */
    private fun withLocalId(code: String, id: String): String {
        val idRegex = Regex("""(?m)^\s*id\s*=\s*["'][^"']*["']""")
        return if (idRegex.containsMatchIn(code)) {
            idRegex.replace(code) { "id = \"$id\"" }
        } else {
            "id = \"$id\"\n$code"
        }
    }

    fun importLuaFromText(fileName: String, code: String) = viewModelScope.launch {
        localMutex.withLock {
            if (code.isBlank()) {
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.lua_empty_error)) }
                return@withLock
            }
            if (luaSourceLoader.validateScript(code, fileName).isFailure) {
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.lua_compile_error)) }
                return@withLock
            }

            val fallbackId = fileName.substringBeforeLast('.').ifBlank { "local_source" }
            val declaredId = luaField(code, "id").trim()
            val baseId = declaredId.ifBlank { fallbackId }
// The id is already in our format (local_...) — reuse it (idempotent re-import).
// Otherwise generate a unique local_<base>_<rand> and bake it into the script.
            val storageId = if (declaredId.startsWith("local_")) declaredId
                            else localStorageId(baseId)
            val finalCode = if (declaredId.startsWith("local_")) code
                            else withLocalId(code, storageId)

            val name = luaField(finalCode, "name", baseId)
            val version = luaField(finalCode, "version", "1.0.0")
            val language = luaLanguages(finalCode, "language", "en")
            val icon = luaField(finalCode, "icon")
            val contentType = luaField(finalCode, "content_type", "novel")

            if (extensionManager.isExtensionInstalled(storageId)) {
// Idempotent re-import of the same file: update the script, no duplicate.
                luaSourceLoader.saveScript(storageId, finalCode)
                val settingsJson = Gson().toJson(mapOf("sourceType" to "local", "content_type" to contentType))
                extensionManager.updateExtensionSettings(storageId, settingsJson)
                luaSourceProvider.reload()
                return@withLock
            }

// Guards against two local copies with the same name.
            val nameClash = _state.value.extensions.find { it.name == name }
            if (nameClash != null && isLocalExtension(nameClash.id)) {
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.lua_duplicate_error, name)) }
                return@withLock
            }

            if (!luaSourceLoader.saveScript(storageId, finalCode)) {
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.lua_import_error)) }
                return@withLock
            }

            extensionManager.installExtensionFromInfo(
                id = storageId,
                name = name,
                version = version,
                language = language,
                imageUrl = icon.ifBlank { null },
                codeUrl = null
            )
            val localSettings = Gson().toJson(mapOf("sourceType" to "local", "content_type" to contentType))
            extensionManager.updateExtensionSettings(storageId, localSettings)
            luaSourceProvider.reload()
        }
    }

    fun openLuaEditor(extensionId: String) = viewModelScope.launch {
        val ext = _state.value.extensions.find { it.id == extensionId } ?: return@launch
        val code = luaSourceLoader.readScript(extensionId)
        if (code == null) {
            _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.lua_not_editable)) }
            return@launch
        }
        _state.update {
            it.copy(
                showLuaEditor = true,
                luaEditorExtensionId = extensionId,
                luaEditorTitle = ext.name,
                luaEditorCode = code,
                luaEditorError = null
            )
        }
    }

    fun updateLuaEditorText(code: String) {
        _state.update { it.copy(luaEditorCode = code, luaEditorError = null) }
    }

    fun closeLuaEditor() {
        _state.update {
            it.copy(
                showLuaEditor = false,
                luaEditorExtensionId = null,
                luaEditorTitle = "",
                luaEditorCode = "",
                luaEditorError = null
            )
        }
    }

    fun saveLuaEditor() = viewModelScope.launch {
        val id = _state.value.luaEditorExtensionId ?: return@launch
        val code = _state.value.luaEditorCode
        if (luaSourceLoader.validateScript(code, "$id.lua").isFailure) {
            _state.update { it.copy(luaEditorError = context.getString(my.dreamora.strings.R.string.lua_compile_error)) }
            return@launch
        }
        if (!luaSourceLoader.saveScript(id, code)) {
            _state.update { it.copy(luaEditorError = context.getString(my.dreamora.strings.R.string.lua_save_error)) }
            return@launch
        }

        val newName = luaField(code, "name", _state.value.luaEditorTitle)
            .takeIf { it.isNotBlank() } ?: _state.value.luaEditorTitle
        val newVersion = luaField(code, "version", "1.0.0")
        val newLanguage = luaField(code, "language", "en")
        val newIcon = luaField(code, "icon")
        val newContentType = luaField(code, "content_type", "novel")
        val oldCodeUrl = getCodeUrl(id)

        extensionManager.installExtensionFromInfo(
            id = id,
            name = newName,
            version = newVersion,
            language = newLanguage,
            imageUrl = newIcon.ifBlank { null },
            codeUrl = oldCodeUrl
        )
        val settingsMap = mutableMapOf<String, Any>()
        if (!oldCodeUrl.isNullOrBlank()) settingsMap["codeUrl"] = oldCodeUrl
        settingsMap["content_type"] = newContentType
        extensionManager.updateExtensionSettings(id, Gson().toJson(settingsMap))
        luaSourceProvider.reload()
        closeLuaEditor()
    }

    fun resetLuaExtension(extensionId: String) = viewModelScope.launch {
        localMutex.withLock {
            val codeUrl = getCodeUrl(extensionId)
            if (codeUrl.isNullOrBlank()) {
                luaSourceLoader.removeScript(extensionId)
                if (isLocalExtension(extensionId)) {
                    extensionManager.uninstallExtension(extensionId)
                }
                luaSourceProvider.reload()
                return@withLock
            }

            luaSourceLoader.removeScript(extensionId)
            if (!luaSourceLoader.downloadAndCacheScript(extensionId, codeUrl)) {
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.lua_restore_error)) }
                return@withLock
            }
            luaSourceProvider.reload()
        }
    }

// ── Plugin translation settings ─────────────────────────────────────────────

/** Opens the translation settings dialog for an installed plugin. */
    private fun openTranslationSettings(extensionId: String) {
        _state.update { it.copy(translationSettingsExtensionId = extensionId) }
    }

    private fun closeTranslationSettings() {
        _state.update { it.copy(translationSettingsExtensionId = null) }
    }

/** Language models for picking a translation pair (from TranslationManager). */
    val translationModels: List<my.dreamora.text_translator.domain.TranslationModelState>
        get() = translationManager.models

/** Global translation mode is active — plugin settings do not apply. */
    val translationGlobalMode: Boolean
        get() = appPreferences.TRANSLATION_GLOBAL_MODE.value

/** The global default pair (fallback preview when the plugin has none). */
    val globalTranslationSource: String
        get() = appPreferences.GLOBAL_TRANSLATION_PREFERRED_SOURCE.value

    val globalTranslationTarget: String
        get() = appPreferences.GLOBAL_TRANSLATION_PREFERRED_TARGET.value

// Read the plugin's current values (empty/default when unset).
    fun translationEnabled(extensionId: String): Boolean =
        appPreferences.translationEnabledForPlugin(extensionId)

    fun translationPair(extensionId: String): my.dreamora.core.appPreferences.TranslationLangPair =
        appPreferences.translationPairForPlugin(extensionId)

    fun translationProvider(extensionId: String): String? =
        appPreferences.translationProviderForPlugin(extensionId)

    fun translationScope(extensionId: String): String =
        appPreferences.translationScopeForPlugin(extensionId)

    fun translationPrompt(extensionId: String): String? =
        appPreferences.translationPromptForPlugin(extensionId)

// Write: stored in AppPreferences at once (write+rebuild re-reads the dialog).
    fun setTranslationEnabled(extensionId: String, enabled: Boolean) {
        appPreferences.setTranslationEnabledForPlugin(extensionId, enabled)
    }

    fun setTranslationPair(extensionId: String, source: String, target: String) {
        appPreferences.setTranslationPairForPlugin(extensionId, source, target)
    }

    fun setTranslationProvider(extensionId: String, provider: String) {
        appPreferences.setTranslationProviderForPlugin(extensionId, provider)
    }

    fun setTranslationScope(extensionId: String, scope: String) {
        appPreferences.setTranslationScopeForPlugin(extensionId, scope)
    }

    fun setTranslationPrompt(extensionId: String, prompt: String) {
        appPreferences.setTranslationPromptForPlugin(extensionId, prompt)
    }

    // Hide translation toggles (per-plugin, only when scope==FULL)
    fun translationHideLibrary(extensionId: String): Boolean =
        appPreferences.TRANSLATION_PLUGIN_HIDE_LIBRARY.value[extensionId] == true

    fun translationHideHistory(extensionId: String): Boolean =
        appPreferences.TRANSLATION_PLUGIN_HIDE_HISTORY.value[extensionId] == true

    fun translationHideCatalog(extensionId: String): Boolean =
        appPreferences.TRANSLATION_PLUGIN_HIDE_CATALOG.value[extensionId] == true

    fun translationHideSearch(extensionId: String): Boolean =
        appPreferences.TRANSLATION_PLUGIN_HIDE_SEARCH.value[extensionId] == true

    fun setTranslationHideLibrary(extensionId: String, hide: Boolean) {
        appPreferences.setTranslationPluginHideMap(appPreferences.TRANSLATION_PLUGIN_HIDE_LIBRARY, extensionId, hide)
    }

    fun setTranslationHideHistory(extensionId: String, hide: Boolean) {
        appPreferences.setTranslationPluginHideMap(appPreferences.TRANSLATION_PLUGIN_HIDE_HISTORY, extensionId, hide)
    }

    fun setTranslationHideCatalog(extensionId: String, hide: Boolean) {
        appPreferences.setTranslationPluginHideMap(appPreferences.TRANSLATION_PLUGIN_HIDE_CATALOG, extensionId, hide)
    }

    fun setTranslationHideSearch(extensionId: String, hide: Boolean) {
        appPreferences.setTranslationPluginHideMap(appPreferences.TRANSLATION_PLUGIN_HIDE_SEARCH, extensionId, hide)
    }

// Bridge to favorite languages and recent pairs: delegated to AppPreferences.
    fun favoriteLanguages(): List<String> = appPreferences.favoriteLanguages()
    fun toggleFavoriteLanguage(code: String) = appPreferences.toggleFavoriteLanguage(code)
    fun recentTranslationPairs() = appPreferences.recentTranslationPairs()
    fun recordRecentTranslationPair(source: String, target: String) = appPreferences.recordRecentTranslationPair(source, target)
    fun removeRecentTranslationPair(pair: my.dreamora.core.appPreferences.TranslationLangPair) =
        appPreferences.removeRecentTranslationPair(pair)

// ── Enable / disable ────────────────────────────────────────────────────────

    private fun toggleExtension(extensionId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) extensionManager.enableExtension(extensionId)
                else         extensionManager.disableExtension(extensionId)
// The scraper refreshes reactively
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle extension $extensionId")
                _state.update { it.copy(error = context.getString(my.dreamora.strings.R.string.ext_failed_to_toggle)) }
            }
        }
    }

// ── Filters / repository ────────────────────────────────────────────────────

    private fun toggleLanguageFilter(code: String) {
        _state.update { state ->
            val updated = if (code in state.selectedLanguages) state.selectedLanguages - code
            else state.selectedLanguages + code
            state.copy(selectedLanguages = updated)
        }
        appPreferences.EXTENSIONS_LANGUAGES_FILTER.value = _state.value.selectedLanguages
    }

    private fun clearLanguageFilter(code: String?) {
        _state.update { it.copy(selectedLanguages = if (code == null) emptySet() else it.selectedLanguages - code) }
        appPreferences.EXTENSIONS_LANGUAGES_FILTER.value = _state.value.selectedLanguages
    }

    private fun toggleContentTypeFilter(contentType: String) {
        _state.update { state ->
            state.copy(selectedContentType = if (state.selectedContentType == contentType) "" else contentType)
        }
    }

    private fun refreshAll() = loadAllAvailableExtensions(forceRefresh = true)

    private fun updateRepositoryUrl(url: String) {
        viewModelScope.launch {
            appPreferences.EXTENSIONS_REPOSITORY_URL.value = url
            _state.update { it.copy(repositoryUrl = url, showRepositoryDialog = false) }
            refreshAll()
        }
    }

// ── Helpers ───────────────────────────────────────────────────────────────────

    private fun updateAvailableExtensionsStatus() {
        _state.update { state ->
            state.copy(
                availableExtensions = state.availableExtensions.map { ext ->
                    val installedVer = getInstalledVersion(ext.id)
                ext.copy(
                    isInstalled       = installedVer != null,
                    isEnabled         = isEnabled(ext.id),
                    version           = installedVer ?: ext.version,
                    isUpdateAvailable = isUpdateAvailable(ext.remoteVersion, installedVer)
                )
                }
            )
        }
    }

    private fun setInstalling(id: String, installing: Boolean) {
        _state.update { state ->
            state.copy(availableExtensions = state.availableExtensions.map {
                if (it.id == id) it.copy(isInstalling = installing) else it
            })
        }
    }

    private fun getInstalledVersion(id: String) =
        _state.value.extensions.find { it.id == id }?.version

    private fun isEnabled(id: String) =
        _state.value.extensions.find { it.id == id }?.enabled ?: false

    private fun isUpdateAvailable(available: String, installed: String?): Boolean {
        if (installed == null) return false
        return try {
            val a = available.split(".").map { it.toIntOrNull() ?: 0 }
            val b = installed.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val av = a.getOrElse(i) { 0 }
                val bv = b.getOrElse(i) { 0 }
                if (av > bv) return true
                if (av < bv) return false
            }
            false
        } catch (e: Exception) { false }
    }

}
