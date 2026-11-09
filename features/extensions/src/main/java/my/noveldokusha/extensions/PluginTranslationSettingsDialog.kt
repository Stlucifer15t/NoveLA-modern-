package my.noveldokusha.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.appPreferences.TranslationLangPair
import my.noveldokusha.core.appPreferences.isComplete
import my.noveldokusha.coreui.components.LanguageButton
import my.noveldokusha.coreui.components.LanguageSearchDialog
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.strings.R as StringsR

@Composable
fun PluginTranslationSettingsDialog(
    extensionId: String,
    viewModel: ExtensionsManagerViewModel,
    onDismiss: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val models = viewModel.translationModels

    val state by viewModel.state.collectAsStateWithLifecycle()
    val extension = state.extensions.find { it.id == extensionId }

    val enabled = remember(extensionId) { mutableStateOf(viewModel.translationEnabled(extensionId)) }
    val pair = remember(extensionId) { mutableStateOf(viewModel.translationPair(extensionId)) }
    val provider = remember(extensionId) { mutableStateOf(viewModel.translationProvider(extensionId) ?: "GOOGLE_PA") }
    val scope = remember(extensionId) { mutableStateOf(viewModel.translationScope(extensionId)) }
    val prompt = remember(extensionId) { mutableStateOf(viewModel.translationPrompt(extensionId).orEmpty()) }

    val hideLibrary = remember(extensionId) { mutableStateOf(viewModel.translationHideLibrary(extensionId)) }
    val hideHistory = remember(extensionId) { mutableStateOf(viewModel.translationHideHistory(extensionId)) }
    val hideCatalog = remember(extensionId) { mutableStateOf(viewModel.translationHideCatalog(extensionId)) }
    val hideSearch = remember(extensionId) { mutableStateOf(viewModel.translationHideSearch(extensionId)) }

    val favoriteLanguages = remember {
        mutableStateListOf<String>().apply { addAll(viewModel.favoriteLanguages()) }
    }
    val recentPairs = remember {
        mutableStateListOf<TranslationLangPair>().apply { addAll(viewModel.recentTranslationPairs()) }
    }
    val refreshFavorite = remember(viewModel) {
        {
            favoriteLanguages.clear()
            favoriteLanguages.addAll(viewModel.favoriteLanguages())
        }
    }
    val refreshRecent = remember(viewModel) {
        {
            recentPairs.clear()
            recentPairs.addAll(viewModel.recentTranslationPairs())
        }
    }

    val safeProvider = if (PROVIDERS.any { it.first == provider.value }) provider.value else "GOOGLE_PA"

    var showPromptEditor by remember { mutableStateOf(false) }
    var showSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }

    val sourceModel = models.firstOrNull { it.language == pair.value.source }
    val targetModel = models.firstOrNull { it.language == pair.value.target }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
        title = {
            Text(
                text = extension?.name?.let { "Translation — $it" }
                    ?: stringResource(StringsR.string.plugin_translation_settings_title),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.md),
                    color = colors.surfaceSubtle,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(StringsR.string.plugin_translation_enabled),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = enabled.value,
                            onCheckedChange = {
                                enabled.value = it
                                viewModel.setTranslationEnabled(extensionId, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAccent,
                                checkedTrackColor = colors.accent,
                            ),
                        )
                    }
                }

                PROVIDERS.forEach { (key, labelRes) ->
                    val selected = safeProvider == key
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                provider.value = key
                                viewModel.setTranslationProvider(extensionId, key)
                            }
                            .padding(vertical = 2.dp),
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) colors.accent else colors.textPrimary,
                        )
                    }
                }

                HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LanguageButton(
                        label = sourceModel?.displayName
                            ?: stringResource(StringsR.string.language_source_empty_text),
                        active = sourceModel != null,
                        onClick = { showSourceDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    LanguageButton(
                        label = targetModel?.displayName
                            ?: stringResource(StringsR.string.language_target_empty_text),
                        active = targetModel != null,
                        onClick = { showTargetDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (showSourceDialog) {
                    LanguageSearchDialog(
                        languages = models,
                        selected = sourceModel,
                        onSelect = { model ->
                            val newPair = pair.value.copy(source = model?.language ?: "")
                            pair.value = newPair
                            viewModel.setTranslationPair(extensionId, newPair.source, newPair.target)
                            if (newPair.isComplete) {
                                viewModel.recordRecentTranslationPair(newPair.source, newPair.target)
                                refreshRecent()
                            }
                            showSourceDialog = false
                        },
                        onDismiss = { showSourceDialog = false },
                        favoriteLanguages = favoriteLanguages,
                        onToggleFavorite = {
                            viewModel.toggleFavoriteLanguage(it)
                            refreshFavorite()
                        },
                        recentPairs = recentPairs,
                        onApplyRecentPair = { s, t ->
                            viewModel.recordRecentTranslationPair(s, t)
                            val newPair = pair.value.copy(source = s, target = t)
                            pair.value = newPair
                            viewModel.setTranslationPair(extensionId, s, t)
                            refreshRecent()
                            showSourceDialog = false
                        },
                        onRemovePair = {
                            viewModel.removeRecentTranslationPair(it)
                            refreshRecent()
                        },
                    )
                }

                if (showTargetDialog) {
                    LanguageSearchDialog(
                        languages = models,
                        selected = targetModel,
                        onSelect = { model ->
                            val newPair = pair.value.copy(target = model?.language ?: "")
                            pair.value = newPair
                            viewModel.setTranslationPair(extensionId, newPair.source, newPair.target)
                            if (newPair.isComplete) {
                                viewModel.recordRecentTranslationPair(newPair.source, newPair.target)
                                refreshRecent()
                            }
                            showTargetDialog = false
                        },
                        onDismiss = { showTargetDialog = false },
                        favoriteLanguages = favoriteLanguages,
                        onToggleFavorite = {
                            viewModel.toggleFavoriteLanguage(it)
                            refreshFavorite()
                        },
                        recentPairs = recentPairs,
                        onApplyRecentPair = { s, t ->
                            viewModel.recordRecentTranslationPair(s, t)
                            val newPair = pair.value.copy(source = s, target = t)
                            pair.value = newPair
                            viewModel.setTranslationPair(extensionId, s, t)
                            refreshRecent()
                            showTargetDialog = false
                        },
                        onRemovePair = {
                            viewModel.removeRecentTranslationPair(it)
                            refreshRecent()
                        },
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = {
                            val swapped = TranslationLangPair(
                                source = pair.value.target,
                                target = pair.value.source,
                            )
                            pair.value = swapped
                            viewModel.setTranslationPair(extensionId, swapped.source, swapped.target)
                        },
                        enabled = pair.value.isComplete,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(StringsR.string.plugin_translation_swap_languages),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(StringsR.string.plugin_translation_swap_languages))
                    }
                }

                if (!pair.value.isComplete) {
                    val globalSource = viewModel.globalTranslationSource
                    val globalTarget = viewModel.globalTranslationTarget
                    if (globalSource.isNotBlank() && globalTarget.isNotBlank()) {
                        Text(
                            text = "$globalSource → $globalTarget",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }

                HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)

                Text(
                    text = stringResource(StringsR.string.plugin_translation_scope_standard),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                ScopeOption(
                    selected = scope.value == AppPreferences.TRANSLATION_SCOPE_STANDARD,
                    label = stringResource(StringsR.string.plugin_translation_scope_standard),
                    description = stringResource(StringsR.string.plugin_translation_scope_standard_description),
                    onClick = {
                        scope.value = AppPreferences.TRANSLATION_SCOPE_STANDARD
                        viewModel.setTranslationScope(extensionId, AppPreferences.TRANSLATION_SCOPE_STANDARD)
                    },
                )
                ScopeOption(
                    selected = scope.value == AppPreferences.TRANSLATION_SCOPE_FULL,
                    label = stringResource(StringsR.string.plugin_translation_scope_full),
                    description = stringResource(StringsR.string.plugin_translation_scope_full_description),
                    onClick = {
                        scope.value = AppPreferences.TRANSLATION_SCOPE_FULL
                        viewModel.setTranslationScope(extensionId, AppPreferences.TRANSLATION_SCOPE_FULL)
                    },
                )

                if (scope.value == AppPreferences.TRANSLATION_SCOPE_FULL) {
                    HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                    Text(
                        text = stringResource(StringsR.string.plugin_translation_hide_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        NovelaFilterChip(
                            label = stringResource(StringsR.string.plugin_translation_hide_library),
                            selected = hideLibrary.value,
                            onClick = {
                                viewModel.setTranslationHideLibrary(extensionId, !hideLibrary.value)
                                hideLibrary.value = !hideLibrary.value
                            },
                        )
                        NovelaFilterChip(
                            label = stringResource(StringsR.string.plugin_translation_hide_history),
                            selected = hideHistory.value,
                            onClick = {
                                viewModel.setTranslationHideHistory(extensionId, !hideHistory.value)
                                hideHistory.value = !hideHistory.value
                            },
                        )
                        NovelaFilterChip(
                            label = stringResource(StringsR.string.plugin_translation_hide_catalog),
                            selected = hideCatalog.value,
                            onClick = {
                                viewModel.setTranslationHideCatalog(extensionId, !hideCatalog.value)
                                hideCatalog.value = !hideCatalog.value
                            },
                        )
                        NovelaFilterChip(
                            label = stringResource(StringsR.string.plugin_translation_hide_search),
                            selected = hideSearch.value,
                            onClick = {
                                viewModel.setTranslationHideSearch(extensionId, !hideSearch.value)
                                hideSearch.value = !hideSearch.value
                            },
                        )
                    }
                }

                if (safeProvider == "GEMINI" || safeProvider == "OPENAI") {
                    HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPromptEditor = !showPromptEditor }
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(StringsR.string.plugin_translation_prompt_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (showPromptEditor) "▾" else "▸",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.accent,
                        )
                    }
                    AnimatedVisibility(visible = showPromptEditor) {
                        var promptText by remember(prompt.value) { mutableStateOf(prompt.value) }
                        Column {
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(NovelaRadius.md),
                                placeholder = {
                                    Text(
                                        stringResource(StringsR.string.plugin_translation_prompt_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    cursorColor = colors.accent,
                                ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.setTranslationPrompt(extensionId, promptText)
                                        prompt.value = promptText
                                        showPromptEditor = false
                                    },
                                ) {
                                    Text(
                                        text = stringResource(StringsR.string.save),
                                        color = colors.accent,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            NovelaPrimaryButton(
                text = stringResource(StringsR.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

private val PROVIDERS = listOf(
    "GOOGLE_PA" to StringsR.string.provider_google_pa,
    "GOOGLE_FREE" to StringsR.string.provider_google_free,
    "GEMINI" to StringsR.string.provider_gemini,
    "OPENAI" to StringsR.string.provider_openai,
)

@Composable
private fun ScopeOption(
    selected: Boolean,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.accent else colors.textPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}
