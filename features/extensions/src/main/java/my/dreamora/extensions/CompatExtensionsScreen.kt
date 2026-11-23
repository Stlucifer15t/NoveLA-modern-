package my.dreamora.extensions

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Extension
import my.dreamora.coreui.components.DreamoraEmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.res.stringResource
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraType
import my.dreamora.strings.R as StringsR
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.dreamora.coreui.components.ImageView
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.extension_compat.core.CompatEcosystem
import my.dreamora.extension_compat.core.CompatInstallIds
import my.dreamora.extension_compat.core.CompatInstallRecord
import my.dreamora.extension_compat.core.CompatRepoEntry
import my.dreamora.extension_compat.core.CompatRepository
import my.dreamora.extension_compat.core.CompatRepositoryManager
import my.dreamora.extension_compat.core.CompatRuntime
import my.dreamora.extension_compat.core.CompatRuntimeState
import my.dreamora.coreui.R as CoreUiR

/**
 * Third-party repository manager — a screen of its own, not a dialog.
 *
 * Manager of third-party repositories: subscriptions (Legado JSON, Mihon/keiyoushi,
 * LNReader, Mangayomi, CloudStream), the catalog of the selected subscription, installs.
 *
 * Full screen on purpose. A Mihon index carries a thousand sources and a Legado one carries
 * a few hundred, each row needing a name, a format, a state and an action; in a dialog that
 * fits about five rows at a time and scrolls inside a second scroller, so the list is read
 * through a keyhole. As a screen it gets the whole window, a real app bar, and one list.
 */
@Composable
fun DreamoraCompatRepositoriesScreen(
    onBack: () -> Unit,
    viewModel: CompatExtensionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.surface)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = DreamoraSpacing.lg, top = DreamoraSpacing.sm, bottom = DreamoraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(StringsR.string.action_back))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(StringsR.string.compat_repos_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.onSurface,
                )
                // Before anything is subscribed the header explains what to do; after that
                // the same line reports what is there.
                if (state.repositories.isEmpty() && state.installed.isEmpty()) {
                    Text(
                        text = stringResource(StringsR.string.compat_subtitle),
                        style = DreamoraType.bookMeta,
                        color = palette.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            StringsR.string.compat_repos_subtitle,
                            state.repositories.size,
                            state.installed.size,
                        ),
                        style = DreamoraType.bookMeta,
                        color = palette.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
            contentPadding = PaddingValues(
                start = DreamoraSpacing.lg,
                end = DreamoraSpacing.lg,
                top = DreamoraSpacing.md,
                bottom = DreamoraSpacing.huge,
            ),
        ) {
            item(key = "compat_add") { AddRepoBlock(state = state, viewModel = viewModel) }

            if (state.repositories.isNotEmpty()) {
                item(key = "compat_repos_label") {
                    SectionLabel(stringResource(StringsR.string.compat_repos_section, state.repositories.size))
                }
                items(items = state.repositories, key = { "repo_" + it.id }) { repo ->
                    RepositoryRow(
                        repo = repo,
                        selected = repo.id == state.selectedRepoId,
                        busy = repo.id in state.busyRepos,
                        installedHere = state.installed.count { it.repositoryId == repo.id },
                        viewModel = viewModel,
                    )
                }
                item(key = "compat_divider") { HorizontalDivider(modifier = Modifier.padding(vertical = DreamoraSpacing.xs)) }
                // Catalog rows are items of THIS list on purpose: a Mihon index can hold a
                // thousand sources, and composing them all inside one block is what makes a
                // scrolling list feel like it hangs.
                item(key = "compat_toolbar") { CatalogToolbar(state = state, viewModel = viewModel) }
                items(
                    items = state.entries,
                    key = { "entry_" + it.repoId + "_" + it.key },
                    contentType = { "catalog-row" },
                ) { entry ->
                    CatalogRow(
                        entry = entry,
                        installState = state.installStates[entry.key]
                            ?: CompatRepositoryManager.InstallState.NOT_INSTALLED,
                        busy = state.isRefreshing,
                        viewModel = viewModel,
                    )
                }
                // An empty catalog used to be a blank gap; say why it is empty.
                if (state.entries.isEmpty()) {
                    item(key = "compat_no_matches") {
                        Text(
                            text = stringResource(StringsR.string.compat_no_matches),
                            style = DreamoraType.bookMeta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = DreamoraSpacing.md),
                        )
                    }
                }
            }

            if (state.visibleInstalled.isNotEmpty()) {
                item(key = "compat_installed_label") {
                    SectionLabel(stringResource(StringsR.string.compat_installed_section, state.visibleInstalled.size))
                }
                items(items = state.visibleInstalled, key = { "installed_" + it.id }) { record ->
                    InstalledRow(
                        record = record,
                        loadError = state.loadInfos.firstOrNull { it.record.id == record.id }?.loadError,
                        viewModel = viewModel,
                    )
                }
            }

            if (state.repositories.isEmpty() && state.installed.isEmpty()) {
                item(key = "compat_empty") {
                    DreamoraEmptyState(
                        icon = Icons.Outlined.Extension,
                        title = stringResource(StringsR.string.compat_empty_title),
                        message = stringResource(StringsR.string.compat_empty_message),
                        actionLabel = stringResource(StringsR.string.compat_add_repo),
                        onAction = { viewModel.onEvent(CompatExtensionsEvent.OnShowAddRepo) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRepoBlock(
    state: CompatExtensionsState,
    viewModel: CompatExtensionsViewModel,
) {
    if (!state.showAddRepo) {
        FilledTonalButton(
            onClick = { viewModel.onEvent(CompatExtensionsEvent.OnShowAddRepo) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(StringsR.string.compat_add_repo), fontSize = 13.sp)
        }
        state.feedback?.let { FeedbackLine(it) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = state.addRepoUrl,
            onValueChange = { viewModel.onEvent(CompatExtensionsEvent.OnAddRepoUrlChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://github.com/keiyoushi/extensions", fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnAddRepo(state.addRepoUrl)) },
                enabled = state.addRepoUrl.isNotBlank() && !state.isWorking,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(6.dp))
                Text(stringResource(StringsR.string.compat_detect_add), fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnHideAddRepo) },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(StringsR.string.cancel), fontSize = 12.sp)
            }
        }
        state.feedback?.let { FeedbackLine(it) }
        state.error?.let {
            FeedbackLine(it, error = true)
        }
    }
}

@Composable
private fun FeedbackLine(text: String, error: Boolean = false) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RepositoryRow(
    repo: CompatRepository,
    selected: Boolean,
    busy: Boolean,
    installedHere: Int,
    viewModel: CompatExtensionsViewModel,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) colorAccent() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = repo.name.ifBlank { repo.url },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val unknownFormat = stringResource(StringsR.string.compat_unknown_format)
                val sourcesStat = if (installedHere > 0) {
                    stringResource(StringsR.string.compat_sources_installed_stat, repo.entryCount, installedHere)
                } else {
                    stringResource(StringsR.string.compat_sources_stat, repo.entryCount)
                }
                val treeNote = if (repo.kind.name == "TREE") {
                    " · " + stringResource(StringsR.string.compat_file_list)
                } else {
                    ""
                }
                Text(
                    text = (repo.ecosystem?.displayName ?: unknownFormat) + " · " + sourcesStat + treeNote,
                    style = DreamoraType.bookMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { viewModel.onEvent(CompatExtensionsEvent.OnRefreshRepo(repo.id)) }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(StringsR.string.compat_refresh), modifier = Modifier.size(18.dp))
                }
            }
            Switch(
                checked = repo.enabled,
                onCheckedChange = { viewModel.onEvent(CompatExtensionsEvent.OnToggleRepo(repo.id, it)) },
            )
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(StringsR.string.compat_remove),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        repo.lastError?.let {
            Text(
                text = it,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (confirmDelete) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(StringsR.string.compat_remove_confirm), fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        viewModel.onEvent(CompatExtensionsEvent.OnRemoveRepo(repo.id, true))
                        confirmDelete = false
                    },
                ) { Text(stringResource(StringsR.string.compat_yes), fontSize = 11.sp) }
                TextButton(
                    onClick = {
                        viewModel.onEvent(CompatExtensionsEvent.OnRemoveRepo(repo.id, false))
                        confirmDelete = false
                    },
                ) { Text(stringResource(StringsR.string.compat_no), fontSize = 11.sp) }
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", fontSize = 11.sp) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = { viewModel.onEvent(CompatExtensionsEvent.OnSelectRepo(repo.id)) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = if (selected) 10.dp else 8.dp,
                        end = 8.dp,
                    ),
                ) {
                    Text(
                        text = if (selected) stringResource(StringsR.string.compat_showing_catalog)
                        else stringResource(StringsR.string.compat_show_catalog),
                        fontSize = 11.sp,
                        color = if (selected) colorAccent() else MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(
                    onClick = { viewModel.onEvent(CompatExtensionsEvent.OnInstallAll(repo.id)) },
                    enabled = repo.entryCount > 0,
                ) { Text(stringResource(StringsR.string.compat_install_all), fontSize = 11.sp) }
                TextButton(
                    onClick = { viewModel.onEvent(CompatExtensionsEvent.OnUpdateAll(repo.id)) },
                ) { Text(stringResource(StringsR.string.compat_check_updates), fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun CatalogToolbar(
    state: CompatExtensionsState,
    viewModel: CompatExtensionsViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(CompatExtensionsEvent.OnSearchQuery(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(StringsR.string.compat_filter_hint), fontSize = 11.sp) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(CompatExtensionsEvent.OnSearchQuery("")) }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = state.filterEcosystem == null,
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnFilterEcosystem(null)) },
                label = { Text(stringResource(StringsR.string.compat_filter_all), fontSize = 11.sp) },
            )
            CompatEcosystem.entries.forEach { ecosystem ->
                FilterChip(
                    selected = state.filterEcosystem == ecosystem,
                    onClick = {
                        viewModel.onEvent(
                            CompatExtensionsEvent.OnFilterEcosystem(
                                if (state.filterEcosystem == ecosystem) null else ecosystem
                            )
                        )
                    },
                    label = { Text(ecosystem.displayName, fontSize = 11.sp, maxLines = 1) },
                )
            }
            FilterChip(
                selected = state.showAdult,
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnShowAdult(!state.showAdult)) },
                label = { Text(stringResource(StringsR.string.compat_show_adult), fontSize = 11.sp, maxLines = 1) },
            )
        }
        Text(
            text = if (state.truncated) {
                stringResource(StringsR.string.compat_catalog_truncated, state.entries.size, state.totalCount)
            } else {
                stringResource(StringsR.string.compat_sources_stat, state.entries.size)
            },
            style = DreamoraType.bookMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.hiddenAdult > 0) {
            Text(
                text = stringResource(StringsR.string.compat_adult_hidden, state.hiddenAdult),
                style = DreamoraType.bookMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Small uppercase label used between blocks of the dialog. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun CatalogRow(
    entry: CompatRepoEntry,
    installState: CompatRepositoryManager.InstallState,
    busy: Boolean,
    viewModel: CompatExtensionsViewModel,
) {
    val runtime = CompatRuntime.of(entry.ecosystem)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val icon = entry.iconUrl
            if (!icon.isNullOrBlank()) {
                ImageView(
                    imageModel = icon,
                    modifier = Modifier.fillMaxSize(),
                    error = CoreUiR.drawable.default_icon,
                    placeholder = CoreUiR.drawable.default_icon,
                )
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(entry.ecosystem.displayName)
                    if (entry.language.isNotBlank()) {
                        append(" · ")
                        append(entry.language)
                    }
                    if (entry.contentType.isNotBlank()) {
                        append(" · ")
                        append(entry.contentType)
                    }
                    if (entry.version.isNotBlank()) {
                        append(" · ")
                        append(entry.version)
                    }
                    if (entry.nsfw) append(" · 18+")
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (installState) {
            CompatRepositoryManager.InstallState.NOT_INSTALLED ->
                SmallAction(stringResource(StringsR.string.compat_install), enabled = !busy) {
                    viewModel.onEvent(CompatExtensionsEvent.OnInstall(entry))
                }

            CompatRepositoryManager.InstallState.UPDATE_AVAILABLE ->
                SmallAction(stringResource(StringsR.string.compat_update), enabled = !busy) {
                    viewModel.onEvent(CompatExtensionsEvent.OnInstall(entry))
                }

// INSTALLED / DISABLED: a record exists — show whether it actually executes
            else -> {
                if (runtime.state == CompatRuntimeState.READY) {
                    Text(
                        text = stringResource(StringsR.string.compat_state_installed),
                        style = DreamoraType.bookMeta,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = stringResource(StringsR.string.compat_state_stored),
                        style = DreamoraType.bookMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        viewModel.onEvent(
                            CompatExtensionsEvent.OnUninstall(
                                CompatInstallIds.recordId(entry.ecosystem, entry.key)
                            )
                        )
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(StringsR.string.compat_uninstall),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** Compact action button used in a catalog row. */
@Composable
private fun SmallAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, fontSize = 11.sp)
    }
}

@Composable
private fun InstalledRow(
    record: CompatInstallRecord,
    loadError: String?,
    viewModel: CompatExtensionsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(record.ecosystem.displayName)
                        if (record.version.isNotBlank()) {
                            append(" · v")
                            append(record.version)
                        }
                        if (record.baseUrl.isNotBlank()) {
                            append(" · ")
                            append(record.baseUrl.removePrefix("https://").removePrefix("http://"))
                        }
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = record.enabled,
                onCheckedChange = {
                    viewModel.onEvent(CompatExtensionsEvent.OnToggleSource(record.id, it))
                },
            )
            IconButton(
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnUninstall(record.id)) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(StringsR.string.compat_uninstall),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        // A stored source whose ecosystem has no runtime in this build says so in plain words;
        // the loader's English diagnostic is only for sources that do run.
        val status = if (!CompatRuntime.isRunnable(record.ecosystem)) {
            stringResource(StringsR.string.compat_stored_not_runnable, record.ecosystem.displayName)
        } else {
            loadError
        }
        status?.let {
            Text(
                text = it,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
* Entry row for the compat repository manager on the Extensions screen.
* It keeps the user informed: how many subscriptions exist and how many of them
* actually produced sources — because "installed" and "working" mean different
* things per ecosystem (see [CompatRuntime]).
 */
@Composable
fun CompatReposSummaryRow(
    repos: Int,
    installed: Int,
    refreshable: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(colorAccent().copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = colorAccent(),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(StringsR.string.compat_repos_title),
                    fontSize = 13.sp,
                )
                Text(
                    text = if (repos == 0 && installed == 0) {
                        stringResource(StringsR.string.compat_summary_empty)
                    } else {
                        stringResource(StringsR.string.compat_summary_counts, repos, installed)
                    },
                    style = DreamoraType.bookMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (refreshable) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
