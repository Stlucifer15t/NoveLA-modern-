package my.dreamora.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.outlined.MenuBook
import my.dreamora.coreui.components.DreamoraGreetingHeader
import my.dreamora.coreui.components.DreamoraMenuCard
import my.dreamora.coreui.components.DreamoraMenuSectionLabel
import my.dreamora.coreui.components.DreamoraStatCard
import my.dreamora.coreui.components.DreamoraStreakPill
import my.dreamora.coreui.components.DreamoraTileIconButton
import my.dreamora.coreui.components.novelaGreeting
import my.dreamora.settings.sections.SettingsLayout
import my.dreamora.settings.sections.SettingsReading
import my.dreamora.settings.sections.SettingsStats
import my.dreamora.settings.ui.hint
import my.dreamora.settings.ui.label
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.saveable.rememberSaveable
import my.dreamora.coreui.components.DreamoraEmptyState
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraTheme
import my.dreamora.settings.ui.SettingsGroup
import my.dreamora.settings.ui.SettingsGroupCard
import my.dreamora.settings.ui.SettingsGroupHero
import my.dreamora.settings.ui.SettingsGroupSpec
import my.dreamora.settings.ui.SettingsSearchField
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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

import my.dreamora.coreui.theme.AppTheme
import my.dreamora.coreui.theme.DarkMode
import my.dreamora.coreui.theme.InternalTheme
import my.dreamora.coreui.theme.PreviewThemes
import my.dreamora.core.appPreferences.AppLanguage
import my.dreamora.core.appPreferences.AppLanguageProvider
import my.dreamora.core.appPreferences.NovelPromptData
import my.dreamora.settings.sections.AppUpdates
import my.dreamora.settings.sections.LibraryAutoUpdate
import my.dreamora.settings.sections.SettingsBackup
import my.dreamora.settings.sections.SettingsData
import my.dreamora.settings.sections.SettingsGeminiTranslation
import my.dreamora.settings.sections.SettingsLanguage
import my.dreamora.settings.sections.SettingsNovelPromptsDialog
import my.dreamora.settings.sections.SettingsNetwork
import my.dreamora.settings.sections.SettingsTheme
import my.dreamora.settings.sections.SettingsRegexCleanup
import my.dreamora.settings.sections.SettingsSources
import my.dreamora.settings.sections.SettingsSupport
import my.dreamora.settings.sections.SettingsAbout

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
    reading: ReadingPreferences? = null,
    layout: LayoutPreferences? = null,
    groups: Set<SettingsGroup>? = null,
    onOpenGroup: (SettingsGroup) -> Unit = {},
) {
    // null [groups] = the Settings screen itself, which lists every group as a
    // tappable box; a non-null set = one group screen, which expands that group only.
    val expanded = groups != null

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

    val palette = MaterialTheme.colorScheme
    val currentLanguageObj = remember(state.currentLanguage.value) {
        AppLanguageProvider.fromCode(state.currentLanguage.value)
            ?: AppLanguageProvider.supportedLanguages.first()
    }

    // One list, two renderings: the Settings screen shows these as boxes (expanded =
    // false, tap opens a group), a group screen shows the one requested entry with its
    // sections inside. Keeping the sections in this file means the two levels can never
    // drift apart. Keywords are written once, so the search filter and the empty state
    // cannot disagree — filtering happens per box, never inside a section.
    fun matches(spec: SettingsGroupSpec): Boolean {
        val q = searchQuery.value.trim().lowercase()
        if (expanded || q.isEmpty()) return true
        return spec.title.lowercase().contains(q) ||
            spec.summary.lowercase().contains(q) ||
            // a blank keyword would match every query (q.contains("") is always true)
            spec.keywords.any { it.isNotBlank() && (it.contains(q) || q.contains(it)) }
    }

    val specs = listOf(
        SettingsGroupSpec(
            group = SettingsGroup.Appearance,
            title = SettingsGroup.Appearance.label(),
            summary = SettingsGroup.Appearance.hint(),
            icon = Icons.Outlined.Palette,
            keywords = listOf("theme", "dark", "amoled", "language", "color", "font"),
            content = {
                SettingsTheme(
                    currentAppTheme = state.currentAppTheme.value,
                    currentDarkMode = state.currentDarkMode.value,
                    onAppThemeChange = onAppThemeSelected,
                    onDarkModeChange = onDarkModeSelected,
                )
                HorizontalDivider(color = DreamoraTheme.colors.hairline)
                SettingsLanguage(
                    currentLanguage = currentLanguageObj,
                    onLanguageChange = onLanguageChange,
                )
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Reading,
            title = SettingsGroup.Reading.label(),
            summary = SettingsGroup.Reading.hint(),
            icon = Icons.Outlined.MenuBook,
            keywords = listOf(
                "reading", "reader", "font", "size", "line height", "spacing", "justify",
                "hyphen", "screen on", "full screen", "tap", "select", "preload", "prefetch",
            ),
            content = {
                // Same preference states the reader panel writes — this screen is another
                // door into them, not a second copy.
                reading?.let { SettingsReading(it) }
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Layout,
            title = SettingsGroup.Layout.label(),
            summary = SettingsGroup.Layout.hint(),
            icon = Icons.Outlined.ViewModule,
            keywords = listOf(
                "layout", "grid", "list", "columns", "for you", "library",
                "read chapters", "grey",
            ),
            content = {
                // The tabs read these while the screen is open, so tapping "3" has already
                // changed the tab — there is nothing left to apply.
                layout?.let { SettingsLayout(it) }
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Stats,
            title = SettingsGroup.Stats.label(),
            summary = SettingsGroup.Stats.hint(),
            icon = Icons.Outlined.Insights,
            keywords = listOf("stats", "statistics", "streak", "chapters", "episodes", "reading"),
            content = {
                SettingsStats(stats = state.stats.value)
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Library,
            title = SettingsGroup.Library.label(),
            summary = SettingsGroup.Library.hint(),
            icon = Icons.Outlined.Update,
            keywords = listOf(
                "library", "auto update", "update", "version", "nightly",
                "18+", "adult", "nsfw", "mature", "sources", "extensions", "hide",
            ),
            content = {
                LibraryAutoUpdate(state = state.libraryAutoUpdate)
                HorizontalDivider(color = DreamoraTheme.colors.hairline)
                AppUpdates(
                    state = state.updateAppSetting,
                    onCheckForUpdatesManual = onCheckForUpdatesManual,
                )
                HorizontalDivider(color = DreamoraTheme.colors.hairline)
                SettingsSources(showAdult = state.extensionsShowAdult)
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Storage,
            title = SettingsGroup.Storage.label(),
            summary = SettingsGroup.Storage.hint(),
            icon = Icons.Outlined.Storage,
            keywords = listOf("storage", "cache", "database", "clean", "size", "images", "video", "manga"),
            content = {
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
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Backup,
            title = SettingsGroup.Backup.label(),
            summary = SettingsGroup.Backup.hint(),
            icon = Icons.Outlined.SettingsBackupRestore,
            keywords = listOf("backup", "restore", "export", "import", "logs", "directory"),
            content = {
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
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Translation,
            title = SettingsGroup.Translation.label(),
            summary = SettingsGroup.Translation.hint(),
            icon = Icons.Outlined.Translate,
            keywords = listOf(
                "translate", "translation", "gemini", "openai", "prompt", "api key",
                "model", "regex", "cleanup", "batch",
            ),
            content = {
                SettingsGeminiTranslation(
                    translationProvider = state.translationProvider.value,
                    onTranslationProviderChange = onTranslationProviderChange,
                    translationGlobalMode = state.translationGlobalMode.value,
                    onTranslationGlobalModeChange = onTranslationGlobalModeChange,
                    hasGlobalPair = state.hasGlobalPair.value,
                    geminiApiKey = state.geminiApiKey.value,
                    geminiModel = state.geminiModel.value,
                    googlePaApiKeys = state.googlePaApiKeys.value,
                    openAiBaseUrl = state.openAiBaseUrl.value,
                    openAiApiKeys = state.openAiApiKeys.value,
                    openAiModel = state.openAiModel.value,
                    activeSystemPrompt = state.activeSystemPrompt.value,
                    promptPresets = state.promptPresets.value,
                    promptUseEnglishLocale = state.promptUseEnglishLocale.value,
                    onGeminiApiKeyChange = onGeminiApiKeyChange,
                    onGeminiModelChange = onGeminiModelChange,
                    onGooglePaApiKeysChange = onGooglePaApiKeysChange,
                    onOpenAiBaseUrlChange = onOpenAiBaseUrlChange,
                    onOpenAiApiKeysChange = onOpenAiApiKeysChange,
                    onOpenAiModelChange = onOpenAiModelChange,
                    onActiveSystemPromptChange = onActiveSystemPromptChange,
                    onPromptUseEnglishLocaleChange = onPromptUseEnglishLocaleChange,
                    onSavePreset = onSavePreset,
                    onDeletePreset = onDeletePreset,
                    llmBatchSize = state.llmBatchSize.value,
                    llmMaxOutputTokens = state.llmMaxOutputTokens.value,
                    onLlmBatchSizeChange = onLlmBatchSizeChange,
                    onLlmMaxOutputTokensChange = onLlmMaxOutputTokensChange,
                    novelPromptCount = state.translationNovelPrompts.value.size,
                    onNovelPromptsClick = { showNovelPromptsDialog.value = true },
                )
                HorizontalDivider(color = DreamoraTheme.colors.hairline)
                SettingsRegexCleanup(onNavigateToRegexCleanup = onNavigateToRegexCleanup)
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Network,
            title = SettingsGroup.Network.label(),
            summary = SettingsGroup.Network.hint(),
            icon = Icons.Outlined.Public,
            keywords = listOf("network", "user agent", "cloudflare", "timeout", "delay", "mass add"),
            content = {
                val context = LocalContext.current
                SettingsNetwork(
                    context = context,
                    scraperUserAgent = state.scraperUserAgent,
                    cloudflareBypassEnabled = state.cloudflareBypassEnabled,
                    cloudflareChallengeTimeoutSeconds = state.cloudflareChallengeTimeoutSeconds,
                    massAddDelayMs = state.massAddDelayMs,
                    onMassAddDelayChange = onMassAddDelayChange,
                    downloadDelayMs = state.downloadDelayMs,
                    onDownloadDelayChange = onDownloadDelayChange,
                )
            },
        ),
        SettingsGroupSpec(
            group = SettingsGroup.Support,
            title = SettingsGroup.Support.label(),
            summary = SettingsGroup.Support.hint(),
            icon = Icons.Outlined.FavoriteBorder,
            keywords = listOf("support", "about", "donate", "telegram", "github", "credits", "version", "logs"),
            content = {
                SettingsSupport(onExportLogs = onExportLogs)
                HorizontalDivider(color = DreamoraTheme.colors.hairline)
                SettingsAbout(appVersion = state.appVersion)
            },
        ),
    )

    val visibleSpecs = remember(specs, searchQuery.value, expanded) {
        specs.filter { (groups == null || it.group in groups) && matches(it) }
    }

    if (expanded) {
        // A group screen: the one spec, open, with its sections inside.
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = DreamoraSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(DreamoraSpacing.xs),
        ) {
            visibleSpecs.forEach { spec ->
                SettingsGroupHero(
                    icon = spec.icon,
                    title = spec.title,
                    summary = spec.summary,
                )
                SettingsGroupCard(
                    title = spec.title,
                    icon = spec.icon,
                    summary = null,
                    expanded = true,
                    showHeader = false,
                ) {
                    spec.content()
                }
            }
            Spacer(modifier = Modifier.height(DreamoraSpacing.navBarClearance))
        }
        return
    }

    // The hub. A greeting instead of a title bar, three counters, then the groups as menu
    // cards and "About" alone at the bottom — the structure of the reference screen, with
    // the one rule that a number is only printed if the database can answer for it.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        val stats = state.stats.value
        DreamoraGreetingHeader(
            greeting = novelaGreeting(),
            // The version lives in About, not under the greeting.
            subtitle = null,
            accent = palette.primary,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
                ) {
                    if (stats.streakDays > 0) {
                        DreamoraStreakPill(days = stats.streakDays, accent = palette.tertiary)
                    }
                    DreamoraTileIconButton(
                        accent = if (searchOpen) palette.primary else palette.surfaceVariant,
                        contentDescription = stringResource(
                            my.dreamora.strings.R.string.action_search_settings
                        ),
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) searchQuery.value = ""
                        },
                    ) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DreamoraSpacing.screenH),
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
        ) {
            DreamoraStatCard(
                value = stats.libraryBooks.displayCount(),
                label = stringResource(my.dreamora.strings.R.string.stats_library),
                accent = palette.primary,
                modifier = Modifier.weight(1f),
            )
            DreamoraStatCard(
                value = stats.chaptersRead.displayCount(),
                label = stringResource(my.dreamora.strings.R.string.stats_chapters_read),
                accent = palette.tertiary,
                modifier = Modifier.weight(1f),
            )
            // The cache line arrives as "12 MB" from the size scan the screen already runs.
            DreamoraStatCard(
                value = state.novelCacheSize.value.ifBlank { "…" },
                label = stringResource(my.dreamora.strings.R.string.stats_cache),
                accent = palette.secondary,
                modifier = Modifier.weight(1f),
            )
        }

        if (searchOpen) {
            SettingsSearchField(
                query = searchQuery.value,
                onQueryChange = { searchQuery.value = it },
                onClear = { searchQuery.value = "" },
                suggestions = listOf(
                    SettingsGroup.Appearance.label(),
                    SettingsGroup.Reading.label(),
                    SettingsGroup.Translation.label(),
                    SettingsGroup.Backup.label(),
                ),
                modifier = Modifier.padding(
                    start = DreamoraSpacing.screenH,
                    end = DreamoraSpacing.screenH,
                    top = DreamoraSpacing.sm,
                ),
            )
        }

        val menuSpecs = visibleSpecs.filter { it.group != SettingsGroup.Support }
        val aboutSpec = visibleSpecs.firstOrNull { it.group == SettingsGroup.Support }
        if (menuSpecs.isNotEmpty()) {
            DreamoraMenuSectionLabel(stringResource(my.dreamora.strings.R.string.menu_section_menu))
            menuSpecs.forEach { spec ->
                DreamoraMenuCard(
                    icon = spec.icon,
                    title = spec.title,
                    subtitle = spec.summary,
                    onClick = { onOpenGroup(spec.group) },
                )
            }
        }
        if (aboutSpec != null) {
            DreamoraMenuSectionLabel(stringResource(my.dreamora.strings.R.string.menu_section_about))
            DreamoraMenuCard(
                icon = aboutSpec.icon,
                title = aboutSpec.title,
                subtitle = aboutSpec.summary,
                onClick = { onOpenGroup(aboutSpec.group) },
            )
        }
        if (visibleSpecs.isEmpty()) {
            DreamoraEmptyState(
                icon = Icons.Outlined.Search,
                title = stringResource(my.dreamora.strings.R.string.settings_nothing_matches_title),
                message = stringResource(my.dreamora.strings.R.string.settings_nothing_matches_message),
                modifier = Modifier.padding(top = DreamoraSpacing.xl),
            )
        }
        Spacer(modifier = Modifier.height(DreamoraSpacing.navBarClearance))
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
                        onSendTestNotification = {},
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
                    extensionsShowAdult = remember { mutableStateOf(false) },
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