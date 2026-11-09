package my.noveldokusha.extensions

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import my.noveldokusha.core.getLanguageDisplayName
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.components.NovelaSecondaryButton
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.strings.R as StringsR

@Composable
fun ExtensionsScreen(
    innerPadding: PaddingValues,
    onBackPressed: (() -> Unit)? = null,
    showExtensionsLanguageFilter: Boolean = false,
    onExtensionsLanguageFilterDismiss: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    onImportLua: () -> Unit = {},
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
        onImportLua = onImportLua,
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
    onImportLua: () -> Unit = {},
) {
    val colors = NovelaTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = innerPadding.calculateTopPadding()),
    ) {
        when {
            state.isLoading && state.extensions.isEmpty() && state.availableExtensions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            else -> {
                if (state.error != null) {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.card),
                        color = colors.danger.copy(alpha = 0.10f),
                        border = BorderStroke(NovelaSize.hairline, colors.danger.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NovelaSpacing.lg, vertical = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.danger,
                            )
                            TextButton(onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnRefresh) }) {
                                Text(
                                    text = stringResource(StringsR.string.retry),
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.accentSoft,
                    )
                }

                val localExtensionDesc = stringResource(StringsR.string.local_extension_description)
                val localExtensionAuthor = stringResource(StringsR.string.extension_author_local)
                val filteredExtensions = state.availableExtensions
                    .filter { state.selectedLanguages.isEmpty() || it.language in state.selectedLanguages }
                    .filter {
                        state.selectedContentType.isEmpty() ||
                            it.contentType == state.selectedContentType ||
                            (state.selectedContentType == "novel" && it.contentType.isEmpty())
                    }

                val localInstalledExtensions = remember(
                    state.extensions,
                    state.availableExtensions,
                    state.selectedLanguages,
                    state.selectedContentType,
                ) {
                    val ids = filteredExtensions.map { it.id }.toSet()
                    state.extensions
                        .filter { it.id !in ids }
                        .filter { state.selectedLanguages.isEmpty() || it.language in state.selectedLanguages }
                        .filter {
                            state.selectedContentType.isEmpty() ||
                                it.contentType == state.selectedContentType ||
                                (state.selectedContentType == "novel" && it.contentType.isEmpty())
                        }
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
                                contentType = installed.contentType,
                            )
                        }
                }

                if (filteredExtensions.isEmpty() && localInstalledExtensions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NovelaSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NovelaEmptyState(
                            icon = Icons.Outlined.Extension,
                            title = stringResource(StringsR.string.no_extensions_available),
                            actionText = stringResource(StringsR.string.refresh),
                            onActionClick = { viewModel.onEvent(ExtensionsScreenEvent.OnRefresh) },
                        )
                        NovelaSecondaryButton(
                            text = stringResource(StringsR.string.import_lua),
                            leadingIcon = Icons.Default.Add,
                            onClick = onImportLua,
                        )
                    }
                } else {
                    var searchQuery by rememberSaveable { mutableStateOf("") }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NovelaSpacing.lg, vertical = 8.dp),
                        placeholder = {
                            Text(
                                text = stringResource(StringsR.string.search),
                                color = colors.textMuted,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.accent,
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceSubtle,
                            unfocusedContainerColor = colors.surfaceSubtle,
                            cursorColor = colors.accent,
                        ),
                        singleLine = true,
                    )

                    // Content type filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = NovelaSpacing.lg, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                selected = state.selectedContentType == value,
                                onClick = {
                                    viewModel.onEvent(ExtensionsScreenEvent.OnContentTypeFilterToggle(value))
                                },
                            )
                        }
                    }

                    val searchFilteredExtensions = if (searchQuery.isBlank()) {
                        filteredExtensions
                    } else {
                        filteredExtensions.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    val searchLocalInstalled = if (searchQuery.isBlank()) {
                        localInstalledExtensions
                    } else {
                        localInstalledExtensions.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    val searchInstalledExtensions = remember(searchFilteredExtensions, searchLocalInstalled) {
                        (searchFilteredExtensions.filter { it.isInstalled } + searchLocalInstalled)
                            .sortedBy { it.name.lowercase(Locale.ROOT) }
                    }

                    val searchAvailableExtensions = remember(searchFilteredExtensions) {
                        searchFilteredExtensions.filter { !it.isInstalled }
                            .sortedBy { it.name.lowercase(Locale.ROOT) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (searchInstalledExtensions.isNotEmpty()) {
                            item(contentType = "header") {
                                val hasUpdates = searchInstalledExtensions.any { it.isUpdateAvailable }
                                NovelaSectionHeader(
                                    title = stringResource(StringsR.string.installed),
                                    badgeText = searchInstalledExtensions.size.toString(),
                                    actionText = if (hasUpdates) stringResource(StringsR.string.update_all) else null,
                                    onActionClick = if (hasUpdates) {
                                        { viewModel.onEvent(ExtensionsScreenEvent.OnUpdateAll) }
                                    } else null,
                                )
                            }

                            items(
                                searchInstalledExtensions,
                                key = { extension -> "installed_${extension.id}" },
                                contentType = { "extension" },
                            ) { extension ->
                                ExtensionListItem(
                                    extension = extension,
                                    viewModel = viewModel,
                                )
                            }
                        }

                        if (searchAvailableExtensions.isNotEmpty()) {
                            item(contentType = "header") {
                                Spacer(Modifier.height(6.dp))
                                NovelaSectionHeader(
                                    title = stringResource(StringsR.string.available),
                                    badgeText = searchAvailableExtensions.size.toString(),
                                )
                            }

                            items(
                                searchAvailableExtensions,
                                key = { extension -> "available_${extension.id}" },
                                contentType = { "extension" },
                            ) { extension ->
                                ExtensionListItem(
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

    if (state.showRepositoryDialog) {
        RepositoryUrlDialog(
            state = state,
            viewModel = viewModel,
        )
    }

    if (state.showLuaEditor) {
        LuaEditorDialog(
            title = state.luaEditorTitle,
            code = state.luaEditorCode,
            error = state.luaEditorError,
            onCodeChange = { viewModel.onEvent(ExtensionsScreenEvent.OnLuaEditorChange(it)) },
            onDismiss = { viewModel.onEvent(ExtensionsScreenEvent.OnLuaEditorDismiss) },
            onSave = { viewModel.onEvent(ExtensionsScreenEvent.OnLuaEditorSave) },
        )
    }
}

@Composable
private fun RepositoryUrlDialog(
    state: ExtensionsScreenState,
    viewModel: ExtensionsManagerViewModel,
) {
    val colors = NovelaTheme.colors
    var tempUrl by remember { mutableStateOf(state.repositoryUrl) }

    AlertDialog(
        onDismissRequest = {
            viewModel.onEvent(ExtensionsScreenEvent.OnHideRepositoryDialog)
        },
        shape = RoundedCornerShape(NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
        title = {
            Text(
                text = stringResource(StringsR.string.repository_settings),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                label = { Text("Repository URL") },
                placeholder = { Text("https://raw.githubusercontent.com/.../index.yaml") },
                singleLine = true,
                shape = RoundedCornerShape(NovelaRadius.md),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderStrong,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textMuted,
                    cursorColor = colors.accent,
                ),
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NovelaSecondaryButton(
                    text = stringResource(StringsR.string.cancel),
                    onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnHideRepositoryDialog) },
                    modifier = Modifier.weight(1f),
                )
                NovelaPrimaryButton(
                    text = stringResource(StringsR.string.save),
                    onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnUpdateRepositoryUrl(tempUrl)) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun ContentTypeBadge(contentType: String) {
    val colors = NovelaTheme.colors
    val (textRes, bgColor, fgColor) = when (contentType) {
        "manga" -> Triple(StringsR.string.content_type_manga, colors.accentSoft, colors.accent)
        "video" -> Triple(StringsR.string.content_type_video, colors.goldStarSoft, colors.goldStar)
        else -> Triple(StringsR.string.content_type_novel, colors.surfaceSubtle, colors.textSecondary)
    }
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .background(bgColor, RoundedCornerShape(NovelaRadius.pill))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(textRes),
            style = NovelaType.badge,
            color = fgColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ExtensionListItem(
    extension: ExtensionInfo,
    viewModel: ExtensionsManagerViewModel,
) {
    val colors = NovelaTheme.colors

    Surface(
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(
            width = NovelaSize.hairline,
            color = if (extension.isUpdateAvailable) colors.accentLine else colors.borderSubtle,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.lg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceSubtle,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                modifier = Modifier.size(40.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (extension.iconUrl.isNotBlank()) {
                        val iconUrlWithCacheBuster = Uri.parse(extension.iconUrl)
                            .buildUpon()
                            .appendQueryParameter("v", extension.version)
                            .build()
                            .toString()
                        ImageView(
                            imageModel = iconUrlWithCacheBuster,
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp)),
                            error = R.drawable.default_icon,
                            placeholder = R.drawable.default_icon,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colors.textMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extension.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    ContentTypeBadge(contentType = extension.contentType)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = getLanguageDisplayName(extension.language),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    val versionText = if (extension.isUpdateAvailable) {
                        "v${extension.version} → v${extension.remoteVersion}"
                    } else {
                        "v${extension.version}"
                    }
                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (extension.isUpdateAvailable) FontWeight.Bold else FontWeight.Normal,
                        color = if (extension.isUpdateAvailable) colors.accent else colors.textMuted,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            when {
                extension.isInstalling -> {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.surfaceSubtle,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent,
                            )
                            Text(
                                text = stringResource(StringsR.string.installing),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
                extension.isUpdateAvailable -> {
                    Surface(
                        onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnExtensionInstall(extension.id)) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accent,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(StringsR.string.update),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onAccent,
                            )
                        }
                    }
                }
                extension.isInstalled -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompactCircleAction(
                            onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnEditLuaClick(extension.id)) },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(StringsR.string.edit_lua),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (!extension.isLocal) {
                            CompactCircleAction(
                                onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnResetLuaClick(extension.id)) },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(StringsR.string.reset),
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        CompactCircleAction(
                            onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnExtensionUninstallById(extension.id)) },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(StringsR.string.uninstall),
                                tint = colors.danger,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                else -> {
                    Surface(
                        onClick = { viewModel.onEvent(ExtensionsScreenEvent.OnExtensionInstall(extension.id)) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accentSoft,
                        border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(StringsR.string.install),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCircleAction(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surfaceSubtle,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = Modifier.size(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
