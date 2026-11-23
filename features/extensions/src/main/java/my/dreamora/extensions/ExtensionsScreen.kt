package my.dreamora.extensions

import my.dreamora.coreui.components.NovelaStatCard
import my.dreamora.coreui.components.NovelaCardGroupDivider
import my.dreamora.coreui.components.NovelaCardGroup
import my.dreamora.coreui.components.NovelaCircleIcon
import androidx.compose.foundation.shape.CircleShape
import my.dreamora.coreui.components.NovelaTileActionButton
import my.dreamora.coreui.components.NovelaTileIconButton
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.outlined.Extension
import my.dreamora.coreui.components.NovelaHeroHeader
import my.dreamora.coreui.components.NovelaHeroPill
import androidx.compose.material3.HorizontalDivider
import my.dreamora.coreui.components.NovelaIconChip
import my.dreamora.coreui.components.NovelaTileBadge
import my.dreamora.coreui.components.NovelaTileCard
import my.dreamora.coreui.components.novelaTileAccent
import my.dreamora.coreui.theme.NovelaSpacing
import my.dreamora.coreui.theme.NovelaTheme
import my.dreamora.coreui.theme.NovelaType
import my.dreamora.coreui.components.NovelaFilterChip
import my.dreamora.coreui.components.NovelaSectionHeader
import my.dreamora.coreui.components.NovelaTopProgress
import my.dreamora.coreui.theme.NovelaRadius
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
import androidx.compose.ui.platform.LocalContext
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
import my.dreamora.coreui.components.ImageView
import my.dreamora.core.Extension
import my.dreamora.coreui.components.MyButton
import my.dreamora.coreui.components.SlimListItem
import my.dreamora.extension_compat.core.CompatEcosystem
import java.util.Locale
import my.dreamora.core.getLanguageDisplayName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.dreamora.coreui.R
import my.dreamora.strings.R as StringsR

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
                val compatContext = LocalContext.current
                CompatReposSummaryRow(
                    repos = compatState.repositories.size,
                    installed = compatState.installed.size,
                    refreshable = compatState.isRefreshing,
                    // Its own screen, not a dialog: a Mihon or Legado index carries hundreds
                    // of rows and needs the whole window to be readable.
                    onClick = {
                        compatContext.startActivity(
                            CompatRepositoriesActivity.createIntent(compatContext)
                        )
                    },
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
                                Text(stringResource(my.dreamora.strings.R.string.retry))
                            }
                        }
                    }
                }
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val localExtensionDesc = stringResource(my.dreamora.strings.R.string.local_extension_description)
                val localExtensionAuthor = stringResource(my.dreamora.strings.R.string.extension_author_local)
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
                            text = stringResource(my.dreamora.strings.R.string.no_extensions_available),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(
                            onClick = onImportLua,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(my.dreamora.strings.R.string.import_lua))
                        }
                        FilledTonalButton(
                            onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnRefresh) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(my.dreamora.strings.R.string.refresh))
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

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(
                            top = NovelaSpacing.xs,
                            bottom = 240.dp,
                        ),
                    ) {
                        // The second mockup's structure: a greeting-style header, a row of
                        // counters, then labelled groups whose rows carry a circular tinted
                        // icon and a trailing action. Counters come from the same filtered
                        // lists the groups render, so they cannot disagree with what is shown.
                        item(contentType = "hero") {
                            val accent = MaterialTheme.colorScheme.primary
                            NovelaHeroHeader(
                                title = stringResource(my.dreamora.strings.R.string.title_extensions),
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
                            )
                        }

                        item(contentType = "stats") {
                            val scheme = MaterialTheme.colorScheme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = NovelaSpacing.screenH),
                                horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
                            ) {
                                NovelaStatCard(
                                    value = searchInstalledExtensions.size.toString(),
                                    label = stringResource(my.dreamora.strings.R.string.installed),
                                    accent = scheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                NovelaStatCard(
                                    value = searchAvailableExtensions.size.toString(),
                                    label = stringResource(my.dreamora.strings.R.string.available),
                                    accent = scheme.tertiary,
                                    modifier = Modifier.weight(1f),
                                )
                                NovelaStatCard(
                                    value = searchInstalledExtensions.count { it.isUpdateAvailable }.toString(),
                                    label = stringResource(my.dreamora.strings.R.string.update),
                                    accent = scheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (searchInstalledExtensions.isNotEmpty()) {
                            item(contentType = "header") {
                                NovelaSectionHeader(
                                    title = stringResource(my.dreamora.strings.R.string.installed),
                                    actionLabel = stringResource(StringsR.string.update_all)
                                        .takeIf { searchInstalledExtensions.any { it.isUpdateAvailable } },
                                    onActionClick = {
                                        viewModel.onEvent(ExtensionsScreenEvent.OnUpdateAll)
                                    },
                                )
                            }
                            item(contentType = "group_installed") {
                                NovelaCardGroup {
                                    searchInstalledExtensions.forEachIndexed { index, extension ->
                                        if (index > 0) {
                                            NovelaCardGroupDivider()
                                        }
                                        ExtensionRow(
                                            extension = extension,
                                            viewModel = viewModel,
                                        )
                                    }
                                }
                            }
                        }

                        if (searchAvailableExtensions.isNotEmpty()) {
                            item(contentType = "header") {
                                NovelaSectionHeader(
                                    title = stringResource(my.dreamora.strings.R.string.available),
                                )
                            }
                            item(contentType = "group_available") {
                                NovelaCardGroup {
                                    searchAvailableExtensions.forEachIndexed { index, extension ->
                                        if (index > 0) {
                                            NovelaCardGroupDivider()
                                        }
                                        ExtensionRow(
                                            extension = extension,
                                            viewModel = viewModel,
                                        )
                                    }
                                }
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
            Text(stringResource(StringsR.string.extensions_repo_url_title))
        },
        text = {
            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                label = { Text(stringResource(StringsR.string.extensions_repo_url_label)) },
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
                        text = stringResource(StringsR.string.cancel),
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
                        text = stringResource(StringsR.string.save),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}


/**
 * One extension as a tile: icon chip, name, two fact badges, and the install/update
 * actions on their own row. The card shape replaces the old two-line row; the events
 * it fires are the same install / update / edit / reset / uninstall set, so nothing was
 * dropped on the way (and no second copy of those handlers exists to drift).
 */
@Composable
private fun ExtensionRow(
    extension: ExtensionInfo,
    viewModel: ExtensionsManagerViewModel,
) {
    val accent = novelaTileAccent(extension.id)
    val updateAvailable = extension.isUpdateAvailable

    // A row inside a group, not a floating card: the group already carries the surface.
    // The icon is circular because the row is where you *act*, while the squares on the
    // Browse board mark things you *open*.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.md, vertical = NovelaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
    ) {
        NovelaCircleIcon(accent = accent) {
            ImageView(
                imageModel = extension.iconUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                error = my.dreamora.coreui.R.drawable.default_icon,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = extension.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = extension.description.ifBlank { extension.author },
                style = NovelaType.bookMeta,
                color = NovelaTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(NovelaSpacing.xxs))
            Row(horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs)) {
                NovelaTileBadge(text = extension.language.uppercase(), accent = accent)
                if (extension.isLocal) {
                    NovelaTileBadge(
                        text = stringResource(my.dreamora.strings.R.string.local),
                        accent = accent,
                    )
                }
                if (updateAvailable) {
                    NovelaTileBadge(
                        text = stringResource(my.dreamora.strings.R.string.update),
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        // The state machine the old row used, unchanged: installing / update / installed /
        // available. Only its shape moved from a trailing column to the row's end.
        when {
            extension.isInstalling -> NovelaTileActionButton(
                label = stringResource(my.dreamora.strings.R.string.installing),
                icon = { CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp) },
                accent = accent,
                enabled = false,
                onClick = {},
            )

            updateAvailable -> NovelaTileActionButton(
                label = stringResource(my.dreamora.strings.R.string.update),
                icon = { Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp)) },
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnExtensionInstall(extension.id)) },
            )

            extension.isInstalled -> Row(horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs)) {
                NovelaTileIconButton(
                    accent = accent,
                    contentDescription = stringResource(my.dreamora.strings.R.string.edit_lua),
                    onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnEditLuaClick(extension.id)) },
                ) { Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp)) }
                if (!extension.isLocal) {
                    NovelaTileIconButton(
                        accent = accent,
                        contentDescription = stringResource(my.dreamora.strings.R.string.reset),
                        onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnResetLuaClick(extension.id)) },
                    ) { Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp)) }
                }
                NovelaTileIconButton(
                    accent = MaterialTheme.colorScheme.error,
                    contentDescription = stringResource(my.dreamora.strings.R.string.uninstall),
                    onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnExtensionUninstallById(extension.id)) },
                ) { Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp)) }
            }

            else -> NovelaTileActionButton(
                label = stringResource(my.dreamora.strings.R.string.install),
                icon = { Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp)) },
                accent = accent,
                onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnExtensionInstall(extension.id)) },
            )
        }
    }
}
