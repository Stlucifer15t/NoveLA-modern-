package my.noveldokusha.extensions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.theme.colorAccent
import my.noveldokusha.extension_compat.core.CompatEcosystem
import my.noveldokusha.extension_compat.core.CompatInstallIds
import my.noveldokusha.extension_compat.core.CompatInstallRecord
import my.noveldokusha.extension_compat.core.CompatRepoEntry
import my.noveldokusha.extension_compat.core.CompatRepository
import my.noveldokusha.extension_compat.core.CompatRepositoryManager
import my.noveldokusha.extension_compat.core.CompatRuntime
import my.noveldokusha.extension_compat.core.CompatRuntimeState
import my.noveldokusha.coreui.R as CoreUiR

/**
 * Менеджер сторонних репозиториев: подписки (Legado JSON, Mihon/keiyoushi, LNReader,
 * Mangayomi, CloudStream), каталог источников выбранной подписки и установка.
 *
 * Диалог, а не отдельный роут: экран «Расширения» уже показывает Lua-плагины,
 * и compat-слой — второй источник того же списка источников в Finder.
 */
@Composable
fun CompatExtensionsDialog(
    onDismiss: () -> Unit,
    viewModel: CompatExtensionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Third-party extension repos", fontSize = 16.sp)
                Text(
                    text = "Paste a repository URL — the format is detected (Legado bookSource JSON, " +
                        "Mihon/keiyoushi index, LNReader, Mangayomi, CloudStream).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AddRepoBlock(state = state, viewModel = viewModel)

                if (state.repositories.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Repositories",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.repositories.forEach { repo ->
                        RepositoryRow(
                            repo = repo,
                            selected = repo.id == state.selectedRepoId,
                            busy = repo.id in state.busyRepos,
                            installedHere = state.installed.count { it.repositoryId == repo.id },
                            viewModel = viewModel,
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    RepoCatalogBlock(state = state, viewModel = viewModel)
                }

                if (state.installed.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Installed compat sources (${state.installed.size})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.installed.forEach { record ->
                        InstalledRow(
                            record = record,
                            loadError = state.loadInfos.firstOrNull { it.record.id == record.id }?.loadError,
                            viewModel = viewModel,
                        )
                    }
                }

                if (state.repositories.isEmpty() && state.installed.isEmpty() && !state.showAddRepo) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No repositories yet. Add one, or import a bookSource.json file " +
                            "from a Legado-style repo.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
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
            Text("Add repository", fontSize = 13.sp)
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
                Text("Detect & add", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnHideAddRepo) },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Cancel", fontSize = 12.sp)
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
                    tint = if (selected) colorAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = repo.name.ifBlank { repo.url },
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(repo.ecosystem?.displayName ?: "unknown format")
                        append(" · ")
                        append(repo.entryCount)
                        append(" sources")
                        if (installedHere > 0) {
                            append(" · ")
                            append(installedHere)
                            append(" installed")
                        }
                        if (repo.kind.name == "TREE") append(" · file list")
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { viewModel.onEvent(CompatExtensionsEvent.OnRefreshRepo(repo.id)) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
            }
            Switch(
                checked = repo.enabled,
                onCheckedChange = { viewModel.onEvent(CompatExtensionsEvent.OnToggleRepo(repo.id, it)) },
            )
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
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
                Text("Remove sources too?", fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        viewModel.onEvent(CompatExtensionsEvent.OnRemoveRepo(repo.id, true))
                        confirmDelete = false
                    },
                ) { Text("Yes", fontSize = 11.sp) }
                TextButton(
                    onClick = {
                        viewModel.onEvent(CompatExtensionsEvent.OnRemoveRepo(repo.id, false))
                        confirmDelete = false
                    },
                ) { Text("No", fontSize = 11.sp) }
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
                        text = if (selected) "Showing catalog" else "Show catalog",
                        fontSize = 11.sp,
                        color = if (selected) colorAccent else MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(
                    onClick = { viewModel.onEvent(CompatExtensionsEvent.OnInstallAll(repo.id)) },
                    enabled = repo.entryCount > 0,
                ) { Text("Install all", fontSize = 11.sp) }
                TextButton(
                    onClick = { viewModel.onEvent(CompatExtensionsEvent.OnUpdateAll(repo.id)) },
                ) { Text("Check updates", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun RepoCatalogBlock(
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
                placeholder = { Text("Filter by name or domain", fontSize = 11.sp) },
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = state.filterEcosystem == null,
                onClick = { viewModel.onEvent(CompatExtensionsEvent.OnFilterEcosystem(null)) },
                label = { Text("All", fontSize = 11.sp) },
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
                    label = { Text(ecosystem.displayName, fontSize = 11.sp) },
                )
            }
        }
        Text(
            text = buildString {
                append(state.entries.size)
                if (state.truncated) {
                    append(" of ")
                    append(state.totalCount)
                    append(" sources — narrow the filter to see more")
                } else {
                    append(" sources")
                }
            },
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.entries.forEach { entry ->
            CatalogRow(
                entry = entry,
                installState = state.installStates[entry.key]
                    ?: CompatRepositoryManager.InstallState.NOT_INSTALLED,
                busy = state.isRefreshing,
                viewModel = viewModel,
            )
        }
    }
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
                fontSize = 12.sp,
                maxLines = 1,
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
                SmallAction("Install", enabled = !busy) {
                    viewModel.onEvent(CompatExtensionsEvent.OnInstall(entry))
                }

            CompatRepositoryManager.InstallState.UPDATE_AVAILABLE ->
                SmallAction("Update", enabled = !busy) {
                    viewModel.onEvent(CompatExtensionsEvent.OnInstall(entry))
                }

            // INSTALLED / DISABLED: запись есть — показываем, исполняется ли она
            else -> {
                if (runtime.state == CompatRuntimeState.READY) {
                    Text(
                        text = "installed",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "stored",
                        fontSize = 10.sp,
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
                        contentDescription = "Uninstall",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** Компактная кнопка действия в строке каталога. */
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
                    fontSize = 12.sp,
                    maxLines = 1,
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
                    contentDescription = "Uninstall",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        loadError?.let {
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
 * Строка-вход в менеджер compat-репозиториев на экране расширений.
 * Держит пользователя в курсе: сколько подписок и сколько из них реально
 * дали источники, — потому что «установлено» и «работает» для разных экосистем
 * означает разное (см. [CompatRuntime]).
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
            Icon(
                Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorAccent,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Third-party extension repos",
                    fontSize = 13.sp,
                )
                Text(
                    text = if (repos == 0 && installed == 0) {
                        "Legado JSON, Mihon/keiyoushi, LNReader, Mangayomi — add a repo URL"
                    } else {
                        buildString {
                            append(repos)
                            append(if (repos == 1) " repository" else " repositories")
                            append(" · ")
                            append(installed)
                            append(" source")
                            if (installed != 1) append("s")
                            append(" installed")
                        }
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (refreshable) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
