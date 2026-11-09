package my.noveldokusha.catalogexplorer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.noveldokusha.coreui.components.AddByUrlDialog
import my.noveldokusha.coreui.components.ChipOption
import my.noveldokusha.coreui.components.LanguageFilterChips
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaScreenHeader
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.extensions.ExtensionsManagerViewModel
import my.noveldokusha.extensions.ExtensionsScreen
import my.noveldokusha.extensions.ExtensionsScreenEvent
import my.noveldokusha.extensions.PluginTranslationSettingsDialog
import my.noveldokusha.navigation.NavigationRouteViewModel
import my.noveldokusha.strings.R
import my.noveldokusha.strings.R as StringsR
import my.noveldokusha.tooling.novel_migration.ui.MigrationTabContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogExplorerScreen(
    navigationRouteViewModel: NavigationRouteViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val colors = NovelaTheme.colors
    val viewModel: CatalogExplorerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableLanguages by viewModel.availableLanguages.collectAsStateWithLifecycle()

    val extensionsViewModel = hiltViewModel<ExtensionsManagerViewModel>()
    val extensionsState by extensionsViewModel.state.collectAsStateWithLifecycle()
    val pluginEnabledMap by extensionsViewModel.pluginEnabledMap.collectAsStateWithLifecycle()

    val translationSettingsExtensionIds = remember(extensionsState.extensions) {
        extensionsState.extensions
            .filter { it.installed }
            .flatMap { ext -> listOf(ext.id to ext.id, "lua_${ext.id}" to ext.id) }
            .toMap()
    }

    val updatesCount = remember(extensionsState.availableExtensions) {
        extensionsState.availableExtensions.count { it.isInstalled && it.isUpdateAvailable }
    }

    val context = LocalContext.current
    var extensionsChipsVisible by rememberSaveable { mutableStateOf(false) }

    val importLuaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { input ->
            val code = input.bufferedReader().readText()
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "local.lua"
            extensionsViewModel.importLuaFromText(fileName, code)
        }
    }

    val onTranslationSettingsClick: (String) -> Unit = remember {
        { extensionId: String ->
            extensionsViewModel.onEvent(ExtensionsScreenEvent.OnExtensionConfigure(extensionId))
        }
    }

    val onDatabaseClick = remember(context) {
        { database: my.noveldokusha.scraper.DatabaseInterface ->
            navigationRouteViewModel.databaseSearch(
                context,
                databaseBaseUrl = database.baseUrl,
            ).let(context::startActivity)
        }
    }

    val onSourceClick = remember(context) {
        { source: my.noveldokusha.scraper.SourceInterface ->
            navigationRouteViewModel.sourceCatalog(
                context,
                sourceBaseUrl = source.baseUrl,
            ).let(context::startActivity)
        }
    }

    val onGlobalSearchClick = remember(context) {
        {
            navigationRouteViewModel.globalSearch(
                context,
                text = "",
            ).let(context::startActivity)
        }
    }

    val onMigrationSourceClick = remember(context) {
        { source: my.noveldokusha.scraper.SourceInterface.Catalog ->
            navigationRouteViewModel.massMigration(
                context,
                sourceBaseUrl = source.baseUrl,
            ).let(context::startActivity)
        }
    }

    val onUnknownSourceClick = remember(context) {
        {
            navigationRouteViewModel.massMigration(
                context,
                sourceBaseUrl = null,
            ).let(context::startActivity)
        }
    }

    val headerSubtitle = when (uiState.selectedTabIndex) {
        0 -> stringResource(R.string.title_browse)
        1 -> stringResource(R.string.title_extensions)
        else -> stringResource(R.string.migration_tab)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                NovelaScreenHeader(
                    title = stringResource(R.string.title_finder),
                    subtitle = headerSubtitle,
                    actions = {
                        when (uiState.selectedTabIndex) {
                            0 -> BrowseTabActions(
                                hasLanguageFilter = uiState.selectedLanguages.isNotEmpty(),
                                onAddByUrlClick = { viewModel.setShowAddByUrlDialog(true) },
                                onGlobalSearchClick = onGlobalSearchClick,
                                onToggleLanguageChips = viewModel::toggleLanguageChips,
                            )
                            1 -> ExtensionsTabActions(
                                hasLanguageFilter = extensionsState.selectedLanguages.isNotEmpty(),
                                onRefresh = { extensionsViewModel.onEvent(ExtensionsScreenEvent.OnRefresh) },
                                onShowRepositoryDialog = { extensionsViewModel.onEvent(ExtensionsScreenEvent.OnShowRepositoryDialog) },
                                onToggleLanguageChips = { extensionsChipsVisible = !extensionsChipsVisible },
                                onImportLua = {
                                    importLuaLauncher.launch(
                                        arrayOf("text/*", "application/octet-stream", "application/x-lua"),
                                    )
                                },
                            )
                            2 -> MigrationTabActions(
                                onHistoryClick = {
                                    navigationRouteViewModel.migrationHistory(context).let(context::startActivity)
                                },
                                onGlobalSearchClick = onGlobalSearchClick,
                            )
                        }
                    },
                )

                // Segmented WebNovel Pill Switcher (Browse · Extensions · Migrate)
                val tabTitles = listOf(
                    R.string.title_browse to Icons.Outlined.Explore,
                    R.string.title_extensions to Icons.Outlined.Extension,
                    R.string.migration_tab to Icons.AutoMirrored.Outlined.AltRoute,
                )
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.surfaceSubtle,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovelaSpacing.lg, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tabTitles.forEachIndexed { index, (titleRes, icon) ->
                            val isSelected = uiState.selectedTabIndex == index
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) colors.accent else Color.Transparent,
                                animationSpec = tween(NovelaMotion.STANDARD, easing = NovelaMotion.StandardEasing),
                                label = "finderTabBg$index",
                            )
                            val fgColor = if (isSelected) colors.onAccent else colors.textSecondary
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(NovelaRadius.pill))
                                    .background(bg)
                                    .clickable { viewModel.setTabIndex(index) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = fgColor,
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = stringResource(titleRes),
                                    modifier = Modifier.weight(1f, fill = false),
                                    color = fgColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (index == 1 && updatesCount > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = if (isSelected) colors.onAccent else colors.accent,
                                                shape = CircleShape,
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Global Search Pill Bar + Filters on Browse tab
                AnimatedVisibility(visible = uiState.selectedTabIndex == 0) {
                    Column {
                        Surface(
                            onClick = onGlobalSearchClick,
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceCard,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = NovelaSpacing.lg, vertical = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(19.dp),
                                )
                                Text(
                                    text = stringResource(R.string.global_search),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        LanguageFilterChips(
                            selected = uiState.selectedLanguages,
                            all = availableLanguages.map { ChipOption(id = it.code, label = it.name) },
                            onToggle = viewModel::toggleSourceLanguage,
                            onClearAll = viewModel::clearLanguageFilter,
                            visible = uiState.showLanguageChips,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = NovelaSpacing.lg, vertical = 6.dp),
                        ) {
                            val contentTypeOptions = listOf(
                                "" to stringResource(StringsR.string.all_categories),
                                "novel" to stringResource(StringsR.string.content_type_novel),
                                "manga" to stringResource(StringsR.string.content_type_manga),
                                "video" to stringResource(StringsR.string.content_type_video),
                            )
                            contentTypeOptions.forEach { (value, label) ->
                                NovelaFilterChip(
                                    label = label,
                                    selected = uiState.selectedContentType == value,
                                    onClick = { viewModel.toggleContentType(value) },
                                )
                            }
                        }
                    }
                }

                if (uiState.selectedTabIndex == 1) {
                    LanguageFilterChips(
                        selected = extensionsState.selectedLanguages,
                        all = extensionsState.availableLanguages.map {
                            ChipOption(id = it.code, label = it.name, count = it.count)
                        },
                        onToggle = { code ->
                            extensionsViewModel.onEvent(ExtensionsScreenEvent.OnLanguageFilterToggle(code))
                        },
                        onClearAll = {
                            extensionsViewModel.onEvent(ExtensionsScreenEvent.OnLanguageFilterClear(null))
                        },
                        visible = extensionsChipsVisible,
                    )
                }

                HorizontalDivider(
                    thickness = NovelaSize.hairline,
                    color = colors.borderSubtle,
                )
            }
        },
        content = { innerPadding ->
            when (uiState.selectedTabIndex) {
                0 -> {
                    val filteredSources = remember(
                        uiState.sourcesList,
                        uiState.selectedLanguages,
                        uiState.selectedContentType,
                    ) {
                        uiState.sourcesList.filter { item ->
                            val langOk = uiState.selectedLanguages.isEmpty() ||
                                item.catalog.isLocalSource ||
                                item.catalog.languageTag in uiState.selectedLanguages
                            val typeOk = uiState.selectedContentType.isEmpty() ||
                                item.catalog.contentType == uiState.selectedContentType ||
                                (uiState.selectedContentType == "novel" && item.catalog.contentType.isEmpty())
                            langOk && typeOk
                        }
                    }
                    CatalogList(
                        innerPadding = innerPadding,
                        databasesList = uiState.databaseList,
                        sourcesList = filteredSources,
                        onDatabaseClick = onDatabaseClick,
                        onSourceClick = onSourceClick,
                        onSourceSetPinned = viewModel::onSourceSetPinned,
                        translationSettingsExtensionIds = translationSettingsExtensionIds,
                        onTranslationSettingsClick = onTranslationSettingsClick,
                        pluginEnabledMap = pluginEnabledMap,
                    )
                }
                1 -> {
                    ExtensionsScreen(
                        innerPadding = innerPadding,
                        onBackPressed = null,
                        showExtensionsLanguageFilter = false,
                        onExtensionsLanguageFilterDismiss = { },
                        onRefresh = { },
                        onImportLua = {
                            importLuaLauncher.launch(
                                arrayOf("text/*", "application/octet-stream", "application/x-lua"),
                            )
                        },
                    )
                }
                2 -> {
                    MigrationTabContent(
                        innerPadding = innerPadding,
                        onSourceClick = onMigrationSourceClick,
                        onUnknownSourceClick = onUnknownSourceClick,
                        onHistoryClick = {
                            navigationRouteViewModel.migrationHistory(context).let(context::startActivity)
                        },
                        onGlobalSearchClick = onGlobalSearchClick,
                    )
                }
            }
        },
    )

    extensionsState.translationSettingsExtensionId?.let { extensionId ->
        PluginTranslationSettingsDialog(
            extensionId = extensionId,
            viewModel = extensionsViewModel,
            onDismiss = { extensionsViewModel.onEvent(ExtensionsScreenEvent.OnTranslationSettingsDismiss) },
        )
    }

    if (uiState.showAddByUrlDialog) {
        AddByUrlDialog(
            onDismiss = { viewModel.setShowAddByUrlDialog(false) },
            onConfirm = { urls ->
                viewModel.addNovelsByUrls(urls)
                viewModel.setShowAddByUrlDialog(false)
            },
            isUrlSupported = viewModel.scraperRepository.scraper::isUrlSupported,
        )
    }
}

@Composable
private fun BrowseTabActions(
    hasLanguageFilter: Boolean,
    onAddByUrlClick: () -> Unit,
    onGlobalSearchClick: () -> Unit,
    onToggleLanguageChips: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAddByUrlClick) {
            Icon(
                imageVector = Icons.Filled.AddLink,
                contentDescription = stringResource(R.string.add_by_url),
                tint = colors.textPrimary,
            )
        }
        IconButton(onClick = onGlobalSearchClick) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.search),
                tint = colors.textPrimary,
            )
        }
        IconButton(onClick = onToggleLanguageChips) {
            BadgedBox(
                badge = {
                    if (hasLanguageFilter) {
                        Badge(containerColor = colors.accent)
                    }
                },
            ) {
                Icon(
                    painter = painterResource(id = my.noveldokusha.coreui.R.drawable.ic_baseline_languages_24),
                    contentDescription = stringResource(R.string.languages),
                    modifier = Modifier.size(22.dp),
                    tint = if (hasLanguageFilter) colors.accent else colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun MigrationTabActions(
    onHistoryClick: () -> Unit,
    onGlobalSearchClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onHistoryClick) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = stringResource(R.string.migration_history),
                tint = colors.textPrimary,
            )
        }
        IconButton(onClick = onGlobalSearchClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = stringResource(R.string.search),
                tint = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun ExtensionsTabActions(
    hasLanguageFilter: Boolean,
    onRefresh: () -> Unit,
    onShowRepositoryDialog: () -> Unit,
    onToggleLanguageChips: () -> Unit,
    onImportLua: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onImportLua) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.import_lua),
                tint = colors.textPrimary,
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.refresh),
                tint = colors.textPrimary,
            )
        }
        IconButton(onClick = onShowRepositoryDialog) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.repository_settings),
                tint = colors.textPrimary,
            )
        }
        IconButton(onClick = onToggleLanguageChips) {
            BadgedBox(
                badge = {
                    if (hasLanguageFilter) {
                        Badge(containerColor = colors.accent)
                    }
                },
            ) {
                Icon(
                    painter = painterResource(id = my.noveldokusha.coreui.R.drawable.ic_baseline_languages_24),
                    contentDescription = stringResource(R.string.languages),
                    modifier = Modifier.size(22.dp),
                    tint = if (hasLanguageFilter) colors.accent else colors.textPrimary,
                )
            }
        }
    }
}
