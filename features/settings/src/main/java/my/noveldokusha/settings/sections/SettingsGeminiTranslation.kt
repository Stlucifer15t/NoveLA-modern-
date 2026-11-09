package my.noveldokusha.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsGeminiTranslation(
    translationProvider: String,
    translationGlobalMode: Boolean,
    onTranslationGlobalModeChange: (Boolean) -> Unit,
    hasGlobalPair: Boolean,
    geminiApiKey: String,
    geminiModel: String,
    googlePaApiKeys: String,
    openAiBaseUrl: String,
    openAiApiKeys: String,
    openAiModel: String,
    activeSystemPrompt: String,
    promptPresets: List<Pair<String, String>>,
    promptUseEnglishLocale: Boolean,
    onTranslationProviderChange: (String) -> Unit,
    onGeminiApiKeyChange: (String) -> Unit,
    onGeminiModelChange: (String) -> Unit,
    onGooglePaApiKeysChange: (String) -> Unit,
    onOpenAiBaseUrlChange: (String) -> Unit,
    onOpenAiApiKeysChange: (String) -> Unit,
    onOpenAiModelChange: (String) -> Unit,
    onActiveSystemPromptChange: (String) -> Unit,
    onPromptUseEnglishLocaleChange: (Boolean) -> Unit,
    onSavePreset: (name: String, prompt: String) -> Unit,
    onDeletePreset: (name: String) -> Unit,
    llmBatchSize: Int,
    llmMaxOutputTokens: Int,
    onLlmBatchSizeChange: (Int) -> Unit,
    onLlmMaxOutputTokensChange: (Int) -> Unit,
    novelPromptCount: Int,
    onNovelPromptsClick: () -> Unit,
) {
    val colors = NovelaTheme.colors

    val predefinedGeminiModels = listOf(
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite",
        "gemini-2.0-flash-exp",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-1.0-pro",
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.borderSubtle,
        cursorColor = colors.accent,
    )

    var apiKeyText by rememberSaveable { mutableStateOf(geminiApiKey) }
    var modelText by rememberSaveable { mutableStateOf(geminiModel) }
    var paKeysText by rememberSaveable { mutableStateOf(googlePaApiKeys) }

    val focusManager = LocalFocusManager.current

    NovelaSettingsGroupCard(title = stringResource(R.string.translation_services)) {
        // ── Global / Per-novel Translation Mode ──
        NovelaSettingsTile(
            title = stringResource(R.string.translation_mode_title),
            subtitle = stringResource(R.string.translation_mode_settings_description),
            icon = Icons.Outlined.Public,
            showDivider = true,
            onClick = if (hasGlobalPair) {
                { onTranslationGlobalModeChange(!translationGlobalMode) }
            } else {
                null
            },
            trailing = {
                Switch(
                    checked = translationGlobalMode,
                    onCheckedChange = onTranslationGlobalModeChange,
                    enabled = hasGlobalPair,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceMuted,
                    ),
                )
            },
        )

        // ── Provider Picker ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProviderRadioOption(
                label = stringResource(R.string.provider_google_pa),
                description = stringResource(R.string.provider_google_pa_description),
                selected = translationProvider == "GOOGLE_PA",
                onClick = { onTranslationProviderChange("GOOGLE_PA") },
            )
            ProviderRadioOption(
                label = stringResource(R.string.provider_google_free),
                description = stringResource(R.string.provider_google_free_description),
                selected = translationProvider == "GOOGLE_FREE",
                onClick = { onTranslationProviderChange("GOOGLE_FREE") },
            )
            ProviderRadioOption(
                label = stringResource(R.string.provider_gemini),
                description = stringResource(R.string.provider_gemini_description),
                selected = translationProvider == "GEMINI",
                onClick = { onTranslationProviderChange("GEMINI") },
            )
            ProviderRadioOption(
                label = stringResource(R.string.provider_openai),
                description = stringResource(R.string.provider_openai_description),
                selected = translationProvider == "OPENAI",
                onClick = { onTranslationProviderChange("OPENAI") },
            )
        }

        // ── Google PA Config ──
        AnimatedVisibility(
            visible = translationProvider == "GOOGLE_PA",
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Key, null, tint = colors.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.google_pa_api_keys),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = paKeysText,
                    onValueChange = {
                        paKeysText = it
                        onGooglePaApiKeysChange(it)
                    },
                    label = { Text(stringResource(R.string.enter_google_pa_api_keys)) },
                    placeholder = { Text(stringResource(R.string.google_pa_api_keys_placeholder)) },
                    shape = RoundedCornerShape(NovelaRadius.md),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    colors = textFieldColors,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.google_pa_api_keys_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        // ── Gemini Config ──
        AnimatedVisibility(
            visible = translationProvider == "GEMINI",
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Key, null, tint = colors.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.gemini_api_key),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        onGeminiApiKeyChange(it)
                    },
                    label = { Text(stringResource(R.string.enter_gemini_api_keys)) },
                    placeholder = { Text(stringResource(R.string.gemini_api_keys_placeholder)) },
                    shape = RoundedCornerShape(NovelaRadius.md),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    colors = textFieldColors,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.get_free_api_key) + "\n" +
                        stringResource(R.string.api_key_tip) + "\n\n" +
                        stringResource(R.string.no_api_key_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.gemini_model),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelText,
                    onValueChange = {
                        modelText = it
                        onGeminiModelChange(it)
                    },
                    placeholder = { Text(predefinedGeminiModels.first()) },
                    label = { Text(stringResource(R.string.model_name)) },
                    shape = RoundedCornerShape(NovelaRadius.md),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = textFieldColors,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    predefinedGeminiModels.forEach { preset ->
                        FilterChip(
                            selected = modelText == preset,
                            onClick = {
                                modelText = preset
                                onGeminiModelChange(preset)
                                focusManager.clearFocus()
                            },
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentSoft,
                                selectedLabelColor = colors.accent,
                            ),
                            label = {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.default_gemini_model) + "\n" +
                        stringResource(R.string.model_examples),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }

        // ── OpenAI-compatible Config ──
        AnimatedVisibility(
            visible = translationProvider == "OPENAI",
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            SettingsOpenAITranslation(
                baseUrl = openAiBaseUrl,
                apiKeys = openAiApiKeys,
                model = openAiModel,
                onBaseUrlChange = onOpenAiBaseUrlChange,
                onApiKeysChange = onOpenAiApiKeysChange,
                onModelChange = onOpenAiModelChange,
            )
        }

        // ── Unified Prompt Manager (Gemini & OpenAI) ──
        AnimatedVisibility(
            visible = translationProvider == "GEMINI" || translationProvider == "OPENAI",
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                SettingsPromptManager(
                    activeSystemPrompt = activeSystemPrompt,
                    promptPresets = promptPresets,
                    promptUseEnglishLocale = promptUseEnglishLocale,
                    onActiveSystemPromptChange = onActiveSystemPromptChange,
                    onPromptUseEnglishLocaleChange = onPromptUseEnglishLocaleChange,
                    onSavePreset = onSavePreset,
                    onDeletePreset = onDeletePreset,
                )
                HorizontalDivider(
                    thickness = NovelaSize.hairline,
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                NovelaSettingsTile(
                    title = stringResource(R.string.novel_prompts_title),
                    subtitle = if (novelPromptCount > 0) {
                        stringResource(R.string.novel_prompts_count, novelPromptCount)
                    } else {
                        null
                    },
                    icon = Icons.Outlined.Psychology,
                    showChevron = true,
                    showDivider = true,
                    onClick = onNovelPromptsClick,
                )
                SettingsLlmBatchOptions(
                    batchSize = llmBatchSize,
                    maxOutputTokens = llmMaxOutputTokens,
                    onBatchSizeChange = onLlmBatchSizeChange,
                    onMaxTokensChange = onLlmMaxOutputTokensChange,
                )
            }
        }
    }
}

@Composable
private fun ProviderRadioOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
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
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsLlmBatchOptions(
    batchSize: Int,
    maxOutputTokens: Int,
    onBatchSizeChange: (Int) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
) {
    val colors = NovelaTheme.colors
    val focusManager = LocalFocusManager.current
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.borderSubtle,
        cursorColor = colors.accent,
    )

    var batchText by rememberSaveable(batchSize) { mutableStateOf(batchSize.toString()) }
    var tokensText by rememberSaveable(maxOutputTokens) { mutableStateOf(maxOutputTokens.toString()) }

    val batchPresets = listOf(20, 40, 60, 80, 100, 120)
    val tokensPresets = listOf(0, 1024, 2048, 4096, 8192, 16384, 32768)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Batch size ──
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.llm_batch_size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = batchText,
                onValueChange = { raw ->
                    batchText = raw
                    raw.toIntOrNull()?.coerceAtLeast(1)?.let(onBatchSizeChange)
                },
                label = { Text(stringResource(R.string.llm_batch_size_label)) },
                shape = RoundedCornerShape(NovelaRadius.md),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = textFieldColors,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                batchPresets.forEach { preset ->
                    FilterChip(
                        selected = batchText == preset.toString(),
                        onClick = {
                            batchText = preset.toString()
                            onBatchSizeChange(preset)
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentSoft,
                            selectedLabelColor = colors.accent,
                        ),
                        label = { Text(preset.toString(), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.llm_batch_size_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }

        // ── Max output tokens ──
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.llm_max_output_tokens),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tokensText,
                onValueChange = { raw ->
                    tokensText = raw
                    raw.toIntOrNull()?.coerceAtLeast(0)?.let(onMaxTokensChange)
                },
                label = { Text(stringResource(R.string.llm_max_output_tokens_label)) },
                shape = RoundedCornerShape(NovelaRadius.md),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = textFieldColors,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tokensPresets.forEach { preset ->
                    val label = if (preset == 0) stringResource(R.string.llm_tokens_auto) else preset.toString()
                    FilterChip(
                        selected = tokensText == preset.toString(),
                        onClick = {
                            tokensText = preset.toString()
                            onMaxTokensChange(preset)
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentSoft,
                            selectedLabelColor = colors.accent,
                        ),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.llm_max_output_tokens_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}
