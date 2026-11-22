package my.noveldokusha.extensions

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.outlined.Extension
import my.noveldokusha.coreui.components.NovelaHeroHeader
import my.noveldokusha.coreui.components.NovelaHeroPill
import androidx.compose.material3.HorizontalDivider
import my.noveldokusha.coreui.components.NovelaIconChip
import my.noveldokusha.coreui.components.NovelaTileBadge
import my.noveldokusha.coreui.components.NovelaTileCard
import my.noveldokusha.coreui.components.novelaTileAccent
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.components.NovelaTopProgress
import my.noveldokusha.coreui.theme.NovelaRadius
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.core.Extension
import my.noveldokusha.coreui.components.MyButton
import my.noveldokusha.coreui.components.SlimListItem
import my.noveldokusha.extension_compat.core.CompatEcosystem
import java.util.Locale
import my.noveldokusha.core.getLanguageDisplayName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.noveldokusha.coreui.R
import my.noveldokusha.strings.R as StringsR

@Composable
fun ExtensionsScreen(
    innerPadding: PaddingValues,
    onBackPressed: (() -> Unit)? = null,
    showExtensionsLanguageFilter: Boolean = false,
    onExtensionsLanguageFilterDismiss: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    onImportLua: () -> Unit = {}
) {
    val viewModel = hiltViewModel<ExtensionsManagerViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    UnifiedExtensionsScreen(
        innerPadding = innerPadding,
        state = state,
        viewModel = viewModel,
        onBackPressed = onBackPressed,
        showExtensionsLanguageFilter = showExtensionsLanguageFilter,
        onExtensionsLanguageFilterDismiss = onExtensionsLanguageFilterDismiss,
        onRefresh = onRefresh,
        onImportLua = onImportLua
    )
}

@Composable
private fun UnifiedExtensionsScreen(
    innerPadding: PaddingValues,
    state: ExtensionsScreenState,
    viewModel: ExtensionsManagerViewModel,
    @Suppress("UNUSED_PARAMETER") onBackPressed: (() -> Unit)?,
    @Suppress("UNUSED_PARAMETER") showExtensionsLanguageFilter: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onExtensionsLanguageFilterDismiss: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onRefresh: (() -> Unit)? = null,
    onImportLua: () -> Unit = {}
) {
    var showCompatRepos by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        // Content area
        when {
            state.isLoading && state.extensions.isEmpty() && state.availableExtensions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> {
// Compat repositories (Legado JSON, Mihon/keiyoushi, LNReader, Mangayomi):
// a second supplier of the source list, next to the Lua plugins.
                val compatViewModel: CompatExtensionsViewModel = hiltViewModel()
                val compatState by compatViewModel.state.collectAsStateWithLifecycle()
                CompatReposSummaryRow(
                    repos = compatState.repositories.size,
                    installed = compatState.installed.size,
                    refreshable = compatState.isRefreshing,
                    onClick = { showCompatRepos = true },
                )
                if (state.error != null) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnRefresh) }) {
                                Text(stringResource(my.noveldokusha.strings.R.string.retry))
                            }
                        }
                    }
                }
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val localExtensionDesc = stringResource(my.noveldokusha.strings.R.string.local_extension_description)
                val localExtensionAuthor = stringResource(my.noveldokusha.strings.R.string.extension_author_local)
                val filteredExtensions = remember(
                    state.availableExtensions,
                    state.selectedLanguages,
                    state.selectedContentType,
                ) {
                    state.availableExtensions
                        .filter { state.selectedLanguages.isEmpty() || it.language in state.selectedLanguages }
                        .filter {
                            state.selectedContentType.isEmpty() ||
                                it.contentType == state.selectedContentType ||
                                (state.selectedContentType == "novel" && it.contentType.isEmpty())
                        }
                }

                val localInstalledExtensions = remember(state.extensions, state.availableExtensions, state.selectedLanguages, state.selectedContentType) {
                    val ids = filteredExtensions.map { it.id }.toSet()
                    state.extensions
                        .filter { it.id !in ids }
                        .filter { state.selectedLanguages.isEmpty() || it.language in state.selectedLanguages }
                        .filter { state.selectedContentType.isEmpty() || it.contentType == state.selectedContentType || (state.selectedContentType == "novel" && it.contentType.isEmpty()) }
                        .map { installed ->
                            ExtensionInfo(
                                id = installed.id,
                                name = installed.name,
                                description = localExtensionDesc,
                                author = localExtensionAuthor,
                                version = installed.version,
                                remoteVersion = installed.version,
                                codeUrl = "",
                                iconUrl = installed.iconUrl.orEmpty(),
                                language = installed.language,
                                isInstalled = true,
                                isEnabled = installed.enabled,
                                isLocal = true,
                                contentType = installed.contentType
                            )
                        }
                }

                val installedExtensions = remember(filteredExtensions, localInstalledExtensions) {
                    (filteredExtensions.filter { it.isInstalled } + localInstalledExtensions)
                        .sortedBy { it.name.lowercase(Locale.ROOT) }
                }

                val availableExtensions = remember(filteredExtensions) {
                    filteredExtensions.filter { !it.isInstalled }.sortedBy { it.name.lowercase(Locale.ROOT) }
                }

                if (filteredExtensions.isEmpty() && localInstalledExtensions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        Text(
                            text = stringResource(my.noveldokusha.strings.R.string.no_extensions_available),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(
                            onClick = onImportLua,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(my.noveldokusha.strings.R.string.import_lua))
                        }
                        FilledTonalButton(
                            onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnRefresh) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(my.noveldokusha.strings.R.string.refresh))
                        }
                        }
                    }
                } else {
                    var searchQuery by rememberSaveable { mutableStateOf("") }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(stringResource(StringsR.string.search)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(NovelaRadius.lg),
                    )

                    // Content type filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val contentTypeOptions = listOf(
                            "" to stringResource(StringsR.string.all_categories),
                            "manga" to stringResource(StringsR.string.content_type_manga),
                            "novel" to stringResource(StringsR.string.content_type_novel),
                            "video" to stringResource(StringsR.string.content_type_video),
                        )
                        contentTypeOptions.forEach { (value, label) ->
                            NovelaFilterChip(
                                label = label,
                                selected = state.selectedContentType == value,
                                onClick = {
                                    viewModel.onEvent(ExtensionsScreenEvent.OnContentTypeFilterToggle(value))
                                },
                            )
                        }
                    }

                    val searchFilteredExtensions = remember(filteredExtensions, searchQuery) {
                        if (searchQuery.isBlank()) {
                            filteredExtensions
                        } else {
                            filteredExtensions.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    val searchLocalInstalled = remember(localInstalledExtensions, searchQuery) {
                        if (searchQuery.isBlank()) {
                            localInstalledExtensions
                        } else {
                            localInstalledExtensions.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    val searchInstalledExtensions = remember(searchFilteredExtensions, searchLocalInstalled) {
                        (searchFilteredExtensions.filter { it.isInstalled } + searchLocalInstalled)
                            .sortedBy { it.name.lowercase(Locale.ROOT) }
                    }

                    val searchAvailableExtensions = remember(searchFilteredExtensions) {
                        searchFilteredExtensions.filter { !it.isInstalled }.sortedBy { it.name.lowercase(Locale.ROOT) }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
                        contentPadding = PaddingValues(
                            start = NovelaSpacing.screenH,
                            end = NovelaSpacing.screenH,
                            top = NovelaSpacing.xs,
                            bottom = 240.dp,
                        ),
                    ) {
                        // Two columns is the point: the mockups read as a board of cards,
                        // not a settings list. A short section spans both columns instead of
                        // leaving a lone half-width tile.
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                            val accent = MaterialTheme.colorScheme.primary
                            NovelaHeroHeader(
                                title = stringResource(my.noveldokusha.strings.R.string.title_extensions),
                                subtitle = stringResource(StringsR.string.extensions_hero_subtitle),
                                accent = accent,
                                icon = {
                                    Icon(
                                        Icons.Outlined.Extension,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = accent,
                                    )
                                },
                                pill = {
                                    NovelaHeroPill(
                                        value = state.extensions.size.toString(),
                                        label = stringResource(StringsR.string.installed_count_label),
                                        accent = accent,
                                    )
                                },
                            )
                        }

                        if (searchInstalledExtensions.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                                NovelaSectionHeader(
                                    title = stringResource(my.noveldokusha.strings.R.string.installed) +
                                        " · ${searchInstalledExtensions.size}",
                                    actionLabel = stringResource(StringsR.string.update_all)
                                        .takeIf { searchInstalledExtensions.any { it.isUpdateAvailable } },
                                    onActionClick = {
                                        viewModel.onEvent(ExtensionsScreenEvent.OnUpdateAll)
                                    },
                                )
                            }
                            items(
                                items = searchInstalledExtensions,
                                key = { extension -> "installed_${extension.id}" },
                                contentType = { "extension" },
                                span = {
                                    GridItemSpan(if (searchInstalledExtensions.size > 2) 1 else 2)
                                },
                            ) { extension ->
                                ExtensionTile(
                                    extension = extension,
                                    viewModel = viewModel
                                )
                            }
                        }

                        if (searchAvailableExtensions.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                                NovelaSectionHeader(
                                    title = stringResource(my.noveldokusha.strings.R.string.available) +
                                        " · ${searchAvailableExtensions.size}",
                                )
                            }
                            items(
                                items = searchAvailableExtensions,
                                key = { extension -> "available_${extension.id}" },
                                contentType = { "extension" },
                                span = {
                                    GridItemSpan(if (searchAvailableExtensions.size > 2) 1 else 2)
                                },
                            ) { extension ->
                                ExtensionTile(
                                    extension = extension,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Repository URL dialog
    if (state.showRepositoryDialog) {
        RepositoryUrlDialog(
            state = state,
            viewModel = viewModel
        )
    }

// Third-party extension repositories (JSON/JS/APK indexes)
    if (showCompatRepos) {
        CompatExtensionsDialog(onDismiss = { showCompatRepos = false })
    }

    if (state.showLuaEditor) {
        LuaEditorDialog(
            title = state.luaEditorTitle,
            code = state.luaEditorCode,
            error = state.luaEditorError,
            onCodeChange = { viewModel.onEvent(ExtensionsScreenEvent.OnLuaEditorChange(it)) },
            onDismiss = { viewModel.onEvent(ExtensionsScreenEvent.OnLuaEditorDismiss) },
            onSave = { viewModel.onEvent(ExtensionsScreenEvent.OnLuaEditorSave) }
        )
    }
}

@Composable
private fun RepositoryUrlDialog(
    state: ExtensionsScreenState,
    viewModel: ExtensionsManagerViewModel
) {
    var tempUrl by remember { mutableStateOf(state.repositoryUrl) }

    AlertDialog(
        onDismissRequest = {
            viewModel.onEvent(ExtensionsScreenEvent.OnHideRepositoryDialog)
        },
        title = {
            Text("Repository URL")
        },
        text = {
            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                label = { Text("Extensions Repository URL") },
                placeholder = { Text("https://raw.githubusercontent.com/.../index.json") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.onEvent(ExtensionsScreenEvent.OnHideRepositoryDialog)
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.onEvent(ExtensionsScreenEvent.OnUpdateRepositoryUrl(tempUrl))
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Save",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable

/**
 * One extension as a tile: icon chip, name, two fact badges, and the install/update
 * actions on their own row. The card shape replaces the old two-line row; the events
 * it fires are the same install / update / edit / reset / uninstall set, so nothing was
 * dropped on the way (and no second copy of those handlers exists to drift).
 */
@Composable
private fun ExtensionTile(
    extension: ExtensionInfo,
    viewModel: ExtensionsManagerViewModel,
) {
    val accent = novelaTileAccent(extension.id)
    val updateAvailable = extension.isUpdateAvailable
    NovelaTileCard(
        accent = accent,
        onClick = {
            // Opening a card is never destructive: it only edits the Lua source when
            // there is one to edit, which mirrors tapping the row in the old list.
            if (extension.isInstalled && extension.isLocal) {
                viewModel.onEvent(ExtensionsScreenEvent.OnEditLuaClick(extension.id))
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        ) {
            NovelaIconChip(accent = accent) {
                if (extension.iconUrl.isNotBlank()) {
                    val iconUrlWithCacheBuster = Uri.parse(extension.iconUrl)
                        .buildUpon()
                        .appendQueryParameter("v", extension.version)
                        .build()
                        .toString()
                    ImageView(
                        imageModel = iconUrlWithCacheBuster,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(NovelaRadius.cover)),
                        error = R.drawable.default_icon,
                        placeholder = R.drawable.default_icon,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accent,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = extension.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = getLanguageDisplayName(extension.language),
                    style = NovelaType.bookMeta,
                    color = NovelaTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovelaTileBadge(
                text = if (extension.isLocal) {
                    stringResource(my.noveldokusha.strings.R.string.local)
                } else {
                    "v" + (if (updateAvailable) extension.remoteVersion else extension.version)
                },
                accent = if (updateAvailable) MaterialTheme.colorScheme.tertiary else accent,
            )
            if (updateAvailable) {
                NovelaTileBadge(text = "↑ " + extension.version, accent = MaterialTheme.colorScheme.tertiary)
            }
        }
        HorizontalDivider(
            color = NovelaTheme.colors.hairline,
            modifier = Modifier.padding(vertical = NovelaSpacing.xxs),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                extension.isInstalling -> {
                    FilledTonalButton(onClick = {}, enabled = false) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(my.noveldokusha.strings.R.string.installing))
                    }
                }
                updateAvailable -> {
                    FilledTonalButton(
                        onClick = {
                            viewModel.onEvent(ExtensionsScreenEvent.OnExtensionInstall(extension.id))
                        },
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(my.noveldokusha.strings.R.string.update))
                    }
                }
                extension.isInstalled -> {
                    FilledTonalIconButton(
                        onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnEditLuaClick(extension.id)) }
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(my.noveldokusha.strings.R.string.edit_lua),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (!extension.isLocal) {
                        FilledTonalIconButton(
                            onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnResetLuaClick(extension.id)) }
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(my.noveldokusha.strings.R.string.reset),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.onEvent(ExtensionsScreenEvent.OnExtensionUninstallById(extension.id))
                        }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(my.noveldokusha.strings.R.string.uninstall),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = {
                            viewModel.onEvent(ExtensionsScreenEvent.OnExtensionInstall(extension.id))
                        },
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(my.noveldokusha.strings.R.string.install))
                    }
                }
            }
        }
    }
}

@Composable
