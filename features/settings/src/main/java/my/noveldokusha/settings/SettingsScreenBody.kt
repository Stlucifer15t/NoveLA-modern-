package my.noveldokusha.settings

import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.saveable.rememberSaveable
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.ui.SettingsGroupCard
import my.noveldokusha.settings.ui.SettingsSearchField
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons

import my.noveldokusha.coreui.theme.AppTheme
import my.noveldokusha.coreui.theme.DarkMode
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.core.appPreferences.AppLanguage
import my.noveldokusha.core.appPreferences.AppLanguageProvider
import my.noveldokusha.core.appPreferences.NovelPromptData
import my.noveldokusha.settings.sections.AppUpdates
import my.noveldokusha.settings.sections.LibraryAutoUpdate
import my.noveldokusha.settings.sections.SettingsBackup
import my.noveldokusha.settings.sections.SettingsData
import my.noveldokusha.settings.sections.SettingsGeminiTranslation
import my.noveldokusha.settings.sections.SettingsLanguage
import my.noveldokusha.settings.sections.SettingsNovelPromptsDialog
import my.noveldokusha.settings.sections.SettingsNetwork
import my.noveldokusha.settings.sections.SettingsTheme
import my.noveldokusha.settings.sections.SettingsRegexCleanup
import my.noveldokusha.settings.sections.SettingsSupport
import my.noveldokusha.settings.sections.SettingsAbout

@Composable
internal fun SettingsScreenBody(
    state: SettingsScreenState,
    modifier: Modifier = Modifier,
    onRefreshSizes: () -> Unit,
    onAppThemeSelected: (AppTheme) -> Unit,
    onDarkModeSelected: (DarkMode) -> Unit,
    onRequestCleanDatabase: () -> Unit,
    onRequestCleanImageFolder: () -> Unit,
    onRequestCleanNovelCache: () -> Unit,
    onRequestCleanMangaCache: () -> Unit,
    onRequestCleanOfflineVideo: () -> Unit,
    onConfirmClean: () -> Unit,
    onDismissClean: () -> Unit,
    onMassAddDelayChange: (Long) -> Unit,
    onDownloadDelayChange: (Long) -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    onCheckForUpdatesManual: () -> Unit,
    onGeminiApiKeyChange: (String) -> Unit,
    onGeminiModelChange: (String) -> Unit,
    onTranslationProviderChange: (String) -> Unit,
    onTranslationGlobalModeChange: (Boolean) -> Unit,
    onGooglePaApiKeysChange: (String) -> Unit,
    onOpenAiBaseUrlChange: (String) -> Unit,
    onOpenAiApiKeysChange: (String) -> Unit,
    onOpenAiModelChange: (String) -> Unit,
    onActiveSystemPromptChange: (String) -> Unit,
    onPromptUseEnglishLocaleChange: (Boolean) -> Unit,
    onSavePreset: (name: String, prompt: String) -> Unit,
    onDeletePreset: (name: String) -> Unit,
    onLlmBatchSizeChange: (Int) -> Unit,
    onLlmMaxOutputTokensChange: (Int) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onNavigateToRegexCleanup: () -> Unit,
    onDeleteNovelPrompt: (String) -> Unit,
    // Auto Backup
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    onAutoBackupSelectDirectory: () -> Unit,
    onAutoBackupMaxCountChange: (Int) -> Unit,
    onAutoBackupIntervalMinutesChange: (Long) -> Unit,
    onAutoBackupIncludeImagesChange: (Boolean) -> Unit,
    onAutoBackupIncludeSettingsChange: (Boolean) -> Unit,
    onAutoBackupIncludePluginsChange: (Boolean) -> Unit,
    onExportLogs: () -> Unit = {},
) {
    // Refresh size displays every time the user navigates to this screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshSizes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val showNovelPromptsDialog = remember { mutableStateOf(false) }
    val searchQuery = rememberSaveable { mutableStateOf("") }

    // A keyword list per card is the whole coupling between the search box and
    // the sections: filtering happens at card granularity, never inside a section.
    fun matches(title: String, keywords: List<String>): Boolean {
        val q = searchQuery.value.trim().lowercase()
        if (q.isEmpty()) return true
        return title.lowercase().contains(q) || keywords.any { it.contains(q) || q.contains(it) }
    }

    val palette = MaterialTheme.colorScheme
    // One visibility decision per card, computed before the list: the keyword lists are
    // written once and reused by the empty-state check, so the filter cannot disagree
    // with what a card shows.
    val showAppearance = matches("Appearance", listOf("theme", "dark", "amoled", "language", "color", "font"))
    val showLibrary = matches("Library",
        listOf("library", "auto update", "update", "version", "check for updates", "nightly"))
    val showStorage = matches("Storage",
        listOf("storage", "cache", "database", "clean", "size", "images", "video", "manga"))
    val showBackup = matches("Backup",
        listOf("backup", "restore", "export", "import", "auto backup", "logs", "directory"))
    val showTranslation = matches("Translation",
        listOf("translate", "translation", "gemini", "openai", "prompt", "api key", "model", "regex", "cleanup", "batch"))
    val showNetwork = matches("Network",
        listOf("network", "user agent", "cloudflare", "timeout", "delay", "mass add", "download"))
    val showSupport = matches("Support", listOf("support", "about", "donate", "telegram", "github", "credits", "version"))
    val anyVisibleMatches = showAppearance || showLibrary || showStorage ||
        showBackup || showTranslation || showNetwork || showSupport

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = NovelaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
    ) {
        SettingsSearchField(
            query = searchQuery.value,
            onQueryChange = { searchQuery.value = it },
            onClear = { searchQuery.value = "" },
            suggestions = listOf("Theme", "Translation", "Backup", "Cloudflare"),
            modifier = Modifier.padding(bottom = NovelaSpacing.sm),
        )
        val currentLanguageObj = remember(state.currentLanguage.value) {
            AppLanguageProvider.fromCode(state.currentLanguage.value)
                ?: AppLanguageProvider.supportedLanguages.first()
        }

        // Appearance & language

        SettingsGroupCard(
            title = "Appearance & language",
            icon = Icons.Outlined.Palette,
            accent = palette.primary,
            summary = state.currentLanguage.value.uppercase(),
            visible = showAppearance,
        ) {
            SettingsTheme(
                currentAppTheme = state.currentAppTheme.value,
                currentDarkMode = state.currentDarkMode.value,
                onAppThemeChange = onAppThemeSelected,
                onDarkModeChange = onDarkModeSelected,
            )
            HorizontalDivider(color = NovelaTheme.colors.hairline)
            SettingsLanguage(
                currentLanguage = currentLanguageObj,
                onLanguageChange = onLanguageChange,
            )
        }

        // Library & app updates

        SettingsGroupCard(
            title = "Library & app updates",
            icon = Icons.Outlined.Update,
            accent = palette.tertiary,
            visible = showLibrary,
        ) {
            LibraryAutoUpdate(state = state.libraryAutoUpdate)
            HorizontalDivider(color = NovelaTheme.colors.hairline)
            AppUpdates(
                state = state.updateAppSetting,
                onCheckForUpdatesManual = onCheckForUpdatesManual
            )
        }

        // Storage & cache

        SettingsGroupCard(
            title = "Storage & cache",
            icon = Icons.Outlined.Storage,
            accent = palette.secondary,
            visible = showStorage,
        ) {
            SettingsData(
                databaseSize = state.databaseSize.value,
                imagesFolderSize = state.imageFolderSize.value,
                novelCacheSize = state.novelCacheSize.value,
                mangaCacheSize = state.mangaCacheSize.value,
                offlineVideoSize = state.offlineVideoSize.value,
                isCleaningDatabase = state.isCleaningDatabase.value,
                isCleaningImages = state.isCleaningImages.value,
                isCleaningNovelCache = state.isCleaningNovelCache.value,
                isCleaningMangaCache = state.isCleaningMangaCache.value,
                isCleaningOfflineVideo = state.isCleaningOfflineVideo.value,
                onRequestCleanDatabase = onRequestCleanDatabase,
                onRequestCleanImageFolder = onRequestCleanImageFolder,
                onRequestCleanNovelCache = onRequestCleanNovelCache,
                onRequestCleanMangaCache = onRequestCleanMangaCache,
                onRequestCleanOfflineVideo = onRequestCleanOfflineVideo,
            )
        }

        // Backup & restore

        SettingsGroupCard(
            title = "Backup & restore",
            icon = Icons.Outlined.SettingsBackupRestore,
            accent = palette.error,
            visible = showBackup,
        ) {
            SettingsBackup(
                onBackupData = onBackupData,
                onRestoreData = onRestoreData,
                autoBackupEnabled = state.autoBackupEnabled.value,
                onAutoBackupEnabledChange = onAutoBackupEnabledChange,
                autoBackupDirectoryUri = state.autoBackupDirectoryUri.value,
                autoBackupDirectoryDisplayName = state.autoBackupDirectoryDisplayName.value,
                onAutoBackupSelectDirectory = onAutoBackupSelectDirectory,
                autoBackupMaxCount = state.autoBackupMaxCount.value,
                onAutoBackupMaxCountChange = onAutoBackupMaxCountChange,
                autoBackupIntervalMinutes = state.autoBackupIntervalMinutes.value,
                onAutoBackupIntervalMinutesChange = onAutoBackupIntervalMinutesChange,
                autoBackupIncludeImages = state.autoBackupIncludeImages.value,
                onAutoBackupIncludeImagesChange = onAutoBackupIncludeImagesChange,
                autoBackupIncludeSettings = state.autoBackupIncludeSettings.value,
                onAutoBackupIncludeSettingsChange = onAutoBackupIncludeSettingsChange,
                autoBackupIncludePlugins = state.autoBackupIncludePlugins.value,
                onAutoBackupIncludePluginsChange = onAutoBackupIncludePluginsChange,
                autoBackupLastTimestamp = state.autoBackupLastTimestamp.value,
            )
        }

        // Translation & text

        SettingsGroupCard(
            title = "Translation & text",
            icon = Icons.Outlined.Translate,
            accent = palette.primary,
            visible = showTranslation,
        ) {
            SettingsGeminiTranslation(
                translationProvider            = state.translationProvider.value,
                onTranslationProviderChange    = onTranslationProviderChange,
                translationGlobalMode          = state.translationGlobalMode.value,
                onTranslationGlobalModeChange  = onTranslationGlobalModeChange,
                hasGlobalPair                  = state.hasGlobalPair.value,
                geminiApiKey                   = state.geminiApiKey.value,
                geminiModel                    = state.geminiModel.value,
                googlePaApiKeys                = state.googlePaApiKeys.value,
                openAiBaseUrl                  = state.openAiBaseUrl.value,
                openAiApiKeys                  = state.openAiApiKeys.value,
                openAiModel                    = state.openAiModel.value,
                activeSystemPrompt             = state.activeSystemPrompt.value,
                promptPresets                  = state.promptPresets.value,
                promptUseEnglishLocale         = state.promptUseEnglishLocale.value,
                onGeminiApiKeyChange           = onGeminiApiKeyChange,
                onGeminiModelChange            = onGeminiModelChange,
                onGooglePaApiKeysChange        = onGooglePaApiKeysChange,
                onOpenAiBaseUrlChange          = onOpenAiBaseUrlChange,
                onOpenAiApiKeysChange          = onOpenAiApiKeysChange,
                onOpenAiModelChange            = onOpenAiModelChange,
                onActiveSystemPromptChange     = onActiveSystemPromptChange,
                onPromptUseEnglishLocaleChange = onPromptUseEnglishLocaleChange,
                onSavePreset                   = onSavePreset,
                onDeletePreset                 = onDeletePreset,
                llmBatchSize                   = state.llmBatchSize.value,
                llmMaxOutputTokens             = state.llmMaxOutputTokens.value,
                onLlmBatchSizeChange           = onLlmBatchSizeChange,
                onLlmMaxOutputTokensChange     = onLlmMaxOutputTokensChange,
                novelPromptCount               = state.translationNovelPrompts.value.size,
                onNovelPromptsClick            = { showNovelPromptsDialog.value = true },
            )
            HorizontalDivider(color = NovelaTheme.colors.hairline)
            SettingsRegexCleanup(
                onNavigateToRegexCleanup = onNavigateToRegexCleanup
            )
        }

        // Network & scraping

        SettingsGroupCard(
            title = "Network & scraping",
            icon = Icons.Outlined.Public,
            accent = palette.tertiary,
            visible = showNetwork,
        ) {
            val context = LocalContext.current
            SettingsNetwork(
                context = context,
                scraperUserAgent = state.scraperUserAgent,
                cloudflareBypassEnabled = state.cloudflareBypassEnabled,
                cloudflareChallengeTimeoutSeconds = state.cloudflareChallengeTimeoutSeconds,
                massAddDelayMs = state.massAddDelayMs,
                onMassAddDelayChange = onMassAddDelayChange,
                downloadDelayMs = state.downloadDelayMs,
                onDownloadDelayChange = onDownloadDelayChange
            )
        }

        // Support & about

        SettingsGroupCard(
            title = "Support & about",
            icon = Icons.Outlined.FavoriteBorder,
            accent = palette.error,
            visible = showSupport,
        ) {
            SettingsSupport(
                onExportLogs = onExportLogs,
            )
            HorizontalDivider(color = NovelaTheme.colors.hairline)
            SettingsAbout(
                appVersion = state.appVersion,
            )
        }

        if (!anyVisibleMatches) {
            NovelaEmptyState(
                icon = Icons.Outlined.Search,
                title = "Nothing matches that",
                message = "Nothing here matches that. Try one of: theme, backup, translation, cache.",
                modifier = Modifier.padding(top = NovelaSpacing.xl),
            )
        }

        // The mascot stays, as a quiet closer under the cards.
        Text(
            text = "(°.°)",
            modifier = Modifier
                .padding(top = NovelaSpacing.lg)
                .fillMaxWidth(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = palette.primary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(NovelaSpacing.navBarClearance))
    }

    if (showNovelPromptsDialog.value) {
        SettingsNovelPromptsDialog(
            novelPrompts = state.translationNovelPrompts.value,
            onDeleteNovelPrompt = onDeleteNovelPrompt,
            onDismiss = { showNovelPromptsDialog.value = false },
        )
    }

    val confirmationType = state.cleanConfirmationType.value
    if (confirmationType != null) {
        val titleRes = when (confirmationType) {
            CleanConfirmationType.DATABASE -> R.string.clean_database
            CleanConfirmationType.IMAGES_FOLDER -> R.string.clean_images_folder
            CleanConfirmationType.NOVEL_CACHE -> R.string.clean_novel_cache
            CleanConfirmationType.MANGA_CACHE -> R.string.clean_manga_cache
            CleanConfirmationType.OFFLINE_VIDEO -> R.string.clean_offline_video
        }
        val textRes = when (confirmationType) {
            CleanConfirmationType.DATABASE -> R.string.clean_database_confirmation
            CleanConfirmationType.IMAGES_FOLDER -> R.string.clean_images_folder_confirmation
            CleanConfirmationType.NOVEL_CACHE -> R.string.clean_novel_cache_confirmation
            CleanConfirmationType.MANGA_CACHE -> R.string.clean_manga_cache_confirmation
            CleanConfirmationType.OFFLINE_VIDEO -> R.string.clean_offline_video_confirmation
        }
        AlertDialog(
            onDismissRequest = onDismissClean,
            title = {
                Text(
                    text = stringResource(id = titleRes),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(id = textRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = onConfirmClean,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissClean,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@PreviewThemes
@Composable
private fun Preview() {
    val isDark = isSystemInDarkTheme()
    InternalTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreenBody(
                state = SettingsScreenState(
                    currentAppTheme = remember { mutableStateOf(AppTheme.DEFAULT) },
                    currentDarkMode = remember { mutableStateOf(DarkMode.SYSTEM) },
                    currentLanguage = remember { mutableStateOf("en") },
                    databaseSize = remember { mutableStateOf("1 MB") },
                    imageFolderSize = remember { mutableStateOf("10 MB") },
                    isCleaningDatabase = remember { mutableStateOf(false) },
                    isCleaningImages = remember { mutableStateOf(false) },
                    novelCacheSize = remember { mutableStateOf("3 MB") },
                    mangaCacheSize = remember { mutableStateOf("2 MB") },
                    offlineVideoSize = remember { mutableStateOf("500 MB") },
                    isCleaningNovelCache = remember { mutableStateOf(false) },
                    isCleaningMangaCache = remember { mutableStateOf(false) },
                    isCleaningOfflineVideo = remember { mutableStateOf(false) },
                    updateAppSetting = SettingsScreenState.UpdateApp(
                        currentAppVersion = "1.0.0",
                        appUpdateCheckerEnabled = remember { mutableStateOf(true) },
                        showNewVersionDialog = remember { mutableStateOf(null) },
                        checkingForNewVersion = remember { mutableStateOf(true) },
                    ),
                    libraryAutoUpdate = SettingsScreenState.LibraryAutoUpdate(
                        autoUpdateEnabled = remember { mutableStateOf(true) },
                        autoUpdateIntervalHours = remember { mutableIntStateOf(24) },
                    ),
                    massAddDelayMs = remember { derivedStateOf { 2000L } },
                    downloadDelayMs = remember { derivedStateOf { 2000L } },
                    geminiApiKey = remember { derivedStateOf { "" } },
                    geminiModel = remember { derivedStateOf { "" } },
                    translationProvider = remember { mutableStateOf("GOOGLE_PA") },
                    translationGlobalMode = remember { mutableStateOf(false) },
                    hasGlobalPair = remember { derivedStateOf { false } },
                    googlePaApiKeys = remember { derivedStateOf { "" } },
                    scraperUserAgent = remember { mutableStateOf("") },
                    cloudflareBypassEnabled = remember { mutableStateOf(true) },
                    cloudflareChallengeTimeoutSeconds = remember { mutableStateOf(120) },
                    openAiBaseUrl = remember { derivedStateOf { "" } },
                    openAiApiKeys = remember { derivedStateOf { "" } },
                    openAiModel = remember { derivedStateOf { "gpt-4o-mini" } },
                    activeSystemPrompt = remember { derivedStateOf { "" } },
                    promptPresets = remember { derivedStateOf { emptyList<Pair<String, String>>() } },
                    promptUseEnglishLocale = remember { derivedStateOf { true } },
                    llmBatchSize = remember { derivedStateOf { 60 } },
                    llmMaxOutputTokens = remember { derivedStateOf { 0 } },
                    autoBackupEnabled = remember { mutableStateOf(false) },
                    autoBackupDirectoryUri = remember { derivedStateOf { "" } },
                    autoBackupDirectoryDisplayName = remember { mutableStateOf("") },
                    autoBackupMaxCount = remember { derivedStateOf { 5 } },
                    autoBackupIntervalMinutes = remember { derivedStateOf { 60L } },
                    autoBackupIncludeImages = remember { derivedStateOf { false } },
                    autoBackupIncludeSettings = remember { derivedStateOf { true } },
                    autoBackupIncludePlugins = remember { derivedStateOf { true } },
                    autoBackupLastTimestamp = remember { derivedStateOf { 0L } },
                    translationNovelPrompts = remember { derivedStateOf { emptyMap<String, NovelPromptData>() } },
                    cleanConfirmationType = remember { mutableStateOf(null) },
                ),
                onRefreshSizes = { },
                onRequestCleanDatabase = { },
                onRequestCleanImageFolder = { },
                onRequestCleanNovelCache = { },
                onRequestCleanMangaCache = { },
                onRequestCleanOfflineVideo = { },
                onConfirmClean = { },
                onDismissClean = { },
                onMassAddDelayChange = { },
                onDownloadDelayChange = { },
                onBackupData = { },
                onRestoreData = { },
                onCheckForUpdatesManual = { },
                onGeminiApiKeyChange = { },
                onGeminiModelChange = { },
                onTranslationProviderChange = { },
                onTranslationGlobalModeChange = { },
                onGooglePaApiKeysChange = { },
                onOpenAiBaseUrlChange = { },
                onOpenAiApiKeysChange = { },
                onOpenAiModelChange = { },
                onActiveSystemPromptChange = { },
                onPromptUseEnglishLocaleChange = { },
                onSavePreset = { _, _ -> },
                onDeletePreset = { },
                onLlmBatchSizeChange = { },
                onLlmMaxOutputTokensChange = { },
                    onLanguageChange = { },
                    onAppThemeSelected = { },
                    onDarkModeSelected = { },
                    onNavigateToRegexCleanup = { },
                    onAutoBackupEnabledChange = { },
                    onAutoBackupSelectDirectory = { },
                    onAutoBackupMaxCountChange = { },
                    onAutoBackupIntervalMinutesChange = { },
                    onAutoBackupIncludeImagesChange = { },
                    onAutoBackupIncludeSettingsChange = { },
                    onAutoBackupIncludePluginsChange = { },
                    onDeleteNovelPrompt = { },
            )
        }
    }
}