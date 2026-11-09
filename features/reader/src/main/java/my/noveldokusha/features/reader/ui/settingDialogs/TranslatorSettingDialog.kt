package my.noveldokusha.features.reader.ui.settingDialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.appPreferences.TranslationLangPair
import my.noveldokusha.core.appPreferences.TranslationSettingsResolver.ActiveTranslatorLevel
import my.noveldokusha.coreui.components.LanguageButton
import my.noveldokusha.coreui.components.LanguageSearchDialog
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.features.reader.features.LiveTranslationSettingData
import my.noveldokusha.reader.R

@Composable
internal fun TranslatorSettingDialog(
    state: LiveTranslationSettingData,
) {
    val colors = NovelaTheme.colors
    val scrollState = rememberScrollState()
    val isScrolling by remember { derivedStateOf { scrollState.isScrollInProgress } }

    Surface(
        shape = RoundedCornerShape(NovelaRadius.cardLarge),
        color = colors.surfaceElevated.copy(alpha = 0.97f),
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Toggle + Scope Mode ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Switch(
                    checked = state.enable.value,
                    enabled = !isScrolling,
                    onCheckedChange = { state.onEnable(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceMuted,
                    ),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.live_translation),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.provider_name, getProviderLabel(state.currentProvider.value)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.enable.value) {
                    IconButton(onClick = { state.onRedoTranslation() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.live_translation),
                            tint = colors.accent,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                ModeToggle(state = state)
            }

            // ── Active Translator Status Pill ──
            val level = state.activeTranslatorLevel.value
            val levelActive = level != ActiveTranslatorLevel.NONE
            Surface(
                shape = RoundedCornerShape(NovelaRadius.md),
                color = if (levelActive) colors.accentSoft else colors.surfaceMuted,
                border = BorderStroke(
                    NovelaSize.hairline,
                    if (levelActive) colors.accent.copy(alpha = 0.45f) else colors.borderSubtle,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Icon(
                        imageVector = when (level) {
                            ActiveTranslatorLevel.PER_NOVEL -> Icons.Outlined.AutoStories
                            ActiveTranslatorLevel.PLUGIN -> Icons.Outlined.Extension
                            ActiveTranslatorLevel.GLOBAL -> Icons.Outlined.Public
                            ActiveTranslatorLevel.NONE -> Icons.Outlined.Block
                        },
                        contentDescription = null,
                        tint = if (levelActive) colors.accent else colors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (level) {
                            ActiveTranslatorLevel.PER_NOVEL -> stringResource(
                                R.string.active_translator_label,
                                stringResource(R.string.translation_mode_per_novel),
                            )
                            ActiveTranslatorLevel.PLUGIN -> stringResource(
                                R.string.active_translator_label,
                                state.activePluginName.value ?: "",
                            )
                            ActiveTranslatorLevel.GLOBAL -> stringResource(
                                R.string.active_translator_label,
                                stringResource(R.string.translation_mode_global),
                            )
                            ActiveTranslatorLevel.NONE -> stringResource(R.string.translators_all_off)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (levelActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (levelActive) colors.accent else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!state.translationGlobalMode.value && (state.source.value == null || state.target.value == null)) {
                Text(
                    text = stringResource(R.string.translation_select_pair_to_enable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }

            // ── Provider Selection ──
            ProviderSelector(state = state, isScrolling = isScrolling)

            HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)

            // ── Language Selection ──
            LanguageSelector(
                state = state,
                onRemovePair = state.onRemovePair,
            )

            // ── Display Options ──
            DisplayOptionsSection(state = state, isScrolling = isScrolling)

            // ── Novel Prompt (LLM only) ──
            NovelPromptSection(state = state, isScrolling = isScrolling)
        }
    }
}

@Composable
private fun ModeToggle(state: LiveTranslationSettingData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeChip(
            selected = !state.translationGlobalMode.value,
            icon = Icons.Outlined.AutoStories,
            description = stringResource(R.string.translation_mode_per_novel),
            onClick = { state.onTranslationGlobalModeChange(false) },
        )
        ModeChip(
            selected = state.translationGlobalMode.value,
            icon = Icons.Outlined.Public,
            description = stringResource(R.string.translation_mode_global),
            onClick = { state.onTranslationGlobalModeChange(true) },
        )
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.pill),
        label = {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceMuted,
            selectedContainerColor = colors.accentSoft,
            labelColor = colors.textMuted,
            selectedLabelColor = colors.accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = colors.accent,
            selectedBorderWidth = 1.dp,
        ),
    )
}

private fun getProviderLabel(key: String): String = when (key) {
    "GOOGLE_PA" -> "Google (Enhanced)"
    "GOOGLE_FREE" -> "Google (Simple)"
    "GEMINI" -> "Gemini"
    "OPENAI" -> "OpenAI"
    else -> key
}

@Composable
private fun ProviderSelector(state: LiveTranslationSettingData, isScrolling: Boolean = false) {
    val colors = NovelaTheme.colors
    val providers = listOf(
        Triple("GOOGLE_PA", R.string.provider_google_pa, R.string.provider_google_pa_description),
        Triple("GOOGLE_FREE", R.string.provider_google_free, R.string.provider_google_free_description),
        Triple("GEMINI", R.string.provider_gemini, R.string.provider_gemini_description),
        Triple("OPENAI", R.string.provider_openai, R.string.provider_openai_description),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        providers.forEach { (key, labelRes, descRes) ->
            val selected = state.currentProvider.value == key
            Surface(
                shape = RoundedCornerShape(NovelaRadius.md),
                color = if (selected) colors.accentSoft else colors.surfaceMuted,
                border = BorderStroke(
                    NovelaSize.hairline,
                    if (selected) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NovelaRadius.md))
                    .clickable(role = Role.RadioButton, enabled = !isScrolling) {
                        state.onProviderChange(key)
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.accent,
                            unselectedColor = colors.textMuted,
                        ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) colors.accent else colors.textPrimary,
                        )
                        Text(
                            text = stringResource(descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    state: LiveTranslationSettingData,
    onRemovePair: (TranslationLangPair) -> Unit = {},
) {
    val colors = NovelaTheme.colors
    var showSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.language_selection),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LanguageButton(
                label = state.source.value?.displayName
                    ?: stringResource(R.string.language_source_empty_text),
                active = state.source.value != null,
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
                label = state.target.value?.displayName
                    ?: stringResource(R.string.language_target_empty_text),
                active = state.target.value != null,
                onClick = { showTargetDialog = true },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showSourceDialog) {
        LanguageSearchDialog(
            languages = state.listOfAvailableModels,
            selected = state.source.value,
            onSelect = { state.onSourceChange(it); showSourceDialog = false },
            onDismiss = { showSourceDialog = false },
            favoriteLanguages = state.favoriteLanguages,
            onToggleFavorite = state.onToggleFavorite,
            recentPairs = state.recentPairs,
            onApplyRecentPair = state.onApplyRecentPair,
            onRemovePair = onRemovePair,
        )
    }

    if (showTargetDialog) {
        LanguageSearchDialog(
            languages = state.listOfAvailableModels,
            selected = state.target.value,
            onSelect = { state.onTargetChange(it); showTargetDialog = false },
            onDismiss = { showTargetDialog = false },
            favoriteLanguages = state.favoriteLanguages,
            onToggleFavorite = state.onToggleFavorite,
            recentPairs = state.recentPairs,
            onApplyRecentPair = state.onApplyRecentPair,
            onRemovePair = onRemovePair,
        )
    }
}

@Composable
private fun DisplayOptionsSection(state: LiveTranslationSettingData, isScrolling: Boolean = false) {
    val colors = NovelaTheme.colors
    HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle, modifier = Modifier.padding(vertical = 4.dp))

    val enabled = state.parallelEnabled.value
    val order = state.parallelOrder.value

    val modeLabel = when {
        !enabled -> ""
        order == "TRANSLATION_FIRST" -> stringResource(R.string.parallel_order_translation_first)
        else -> stringResource(R.string.parallel_order_original_first)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.md))
            .clickable(enabled = !isScrolling) {
                when {
                    !enabled -> {
                        state.onParallelEnabledChange(true)
                        state.onParallelOrderChange("TRANSLATION_FIRST")
                    }
                    order == "TRANSLATION_FIRST" -> {
                        state.onParallelOrderChange("ORIGINAL_FIRST")
                    }
                    else -> {
                        state.onParallelEnabledChange(false)
                    }
                }
            }
            .padding(vertical = 6.dp, horizontal = 4.dp),
    ) {
        Icon(
            if (enabled) Icons.Filled.ViewColumn else Icons.Outlined.ViewColumn,
            contentDescription = stringResource(R.string.parallel_mode_title),
            tint = if (enabled) colors.accent else colors.textMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.parallel_mode_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.accent else colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.parallel_mode_description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        if (enabled) {
            Surface(
                shape = RoundedCornerShape(NovelaRadius.pill),
                color = colors.accentSoft,
            ) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun NovelPromptSection(state: LiveTranslationSettingData, isScrolling: Boolean = false) {
    val colors = NovelaTheme.colors
    val isLlmProvider = state.currentProvider.value in listOf("GEMINI", "OPENAI")

    HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle, modifier = Modifier.padding(vertical = 4.dp))

    val isActive = isLlmProvider && state.novelPrompt.value.isNotBlank()
    var showEditor by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.md))
            .then(
                if (isLlmProvider) Modifier.clickable(enabled = !isScrolling) { showEditor = !showEditor }
                else Modifier
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
    ) {
        Icon(
            Icons.Outlined.Psychology,
            contentDescription = null,
            tint = if (isActive) colors.accent else colors.textMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.novel_prompt_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) colors.accent else colors.textPrimary,
            )
            Text(
                text = if (isLlmProvider) stringResource(R.string.novel_prompt_description_reader)
                else stringResource(R.string.novel_prompt_llm_only),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        if (isActive) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = "(${state.novelPrompt.value.length} chars)",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
        }
        if (isLlmProvider) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }

    if (isLlmProvider) {
        AnimatedVisibility(visible = showEditor) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                var promptText by remember(state.novelPrompt.value) {
                    mutableStateOf(state.novelPrompt.value)
                }

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    shape = RoundedCornerShape(NovelaRadius.md),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = {
                        Text(
                            stringResource(R.string.novel_prompt_placeholder),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderSubtle,
                        cursorColor = colors.accent,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            state.onNovelPromptChange(promptText)
                            showEditor = false
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.novel_prompt_append_mode),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.novelPromptAppendMode.value,
                        onCheckedChange = { state.onNovelPromptAppendModeChange(it) },
                        enabled = !isScrolling,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surfaceMuted,
                        ),
                    )
                }
            }
        }
    }
}
