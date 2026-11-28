package my.dreamora.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumSectionTitle
import my.dreamora.settings.ui.PremiumChip
import my.dreamora.settings.ui.PremiumSettingsRow
import androidx.compose.ui.text.font.FontWeight

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
    val predefinedGeminiModels = listOf(
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite",
        "gemini-2.0-flash-exp",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-1.0-pro"
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
        cursorColor          = MaterialTheme.colorScheme.onSurface
    )

    var apiKeyText by rememberSaveable { mutableStateOf(geminiApiKey) }
    var modelText  by rememberSaveable { mutableStateOf(geminiModel) }
    var paKeysText by rememberSaveable { mutableStateOf(googlePaApiKeys) }

    val focusManager = LocalFocusManager.current

    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.Translate,
            title = stringResource(R.string.translation_services),
            summary = stringResource(R.string.translation_services_summary),
            accent = colorAccent(),
        )

        // ── Translation mode: per-novel / global ──────────────────────────
        PremiumSettingsRow(
            modifier = Modifier.textPadding().padding(top = DreamoraSpacing.sm),
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = stringResource(R.string.translation_mode_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = stringResource(R.string.translation_mode_settings_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ExpressiveSwitch(
                        checked = translationGlobalMode,
                        onCheckedChange = onTranslationGlobalModeChange,
                        enabled = hasGlobalPair,
                    )
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp))

        // ── Provider picker ───────────────────────────────────────────────────
        PremiumSectionTitle(text = stringResource(R.string.translation_provider))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp)
        ) {
            ProviderRadioOption(
                label       = stringResource(R.string.provider_google_pa),
                description = stringResource(R.string.provider_google_pa_description),
                selected    = translationProvider == "GOOGLE_PA",
                onClick     = { onTranslationProviderChange("GOOGLE_PA") }
            )
            Spacer(Modifier.height(4.dp))
            ProviderRadioOption(
                label       = stringResource(R.string.provider_google_free),
                description = stringResource(R.string.provider_google_free_description),
                selected    = translationProvider == "GOOGLE_FREE",
                onClick     = { onTranslationProviderChange("GOOGLE_FREE") }
            )
            Spacer(Modifier.height(4.dp))
            ProviderRadioOption(
                label       = stringResource(R.string.provider_gemini),
                description = stringResource(R.string.provider_gemini_description),
                selected    = translationProvider == "GEMINI",
                onClick     = { onTranslationProviderChange("GEMINI") }
            )
            Spacer(Modifier.height(4.dp))
            ProviderRadioOption(
                label       = stringResource(R.string.provider_openai),
                description = stringResource(R.string.provider_openai_description),
                selected    = translationProvider == "OPENAI",
                onClick     = { onTranslationProviderChange("OPENAI") }
            )
        }

        // ── Google PA config ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = translationProvider == "GOOGLE_PA",
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp))
                ApiKeyRow(
                    title = stringResource(R.string.google_pa_api_keys),
                    icon = Icons.Outlined.Key,
                    value = paKeysText,
                    onValueChange = { paKeysText = it; onGooglePaApiKeysChange(it) },
                    label = stringResource(R.string.enter_google_pa_api_keys),
                    placeholder = stringResource(R.string.google_pa_api_keys_placeholder),
                    tip = stringResource(R.string.google_pa_api_keys_tip),
                    minLines = 2,
                    maxLines = 5,
                    textFieldColors = textFieldColors,
                )
            }
        }

        // ── Gemini config ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = translationProvider == "GEMINI",
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp))

                ApiKeyRow(
                    title = stringResource(R.string.gemini_api_key),
                    icon = Icons.Outlined.Key,
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it; onGeminiApiKeyChange(it) },
                    label = stringResource(R.string.enter_gemini_api_keys),
                    placeholder = stringResource(R.string.gemini_api_keys_placeholder),
                    tip = stringResource(R.string.get_free_api_key) + "\n" +
                            stringResource(R.string.api_key_tip) + "\n\n" +
                            stringResource(R.string.no_api_key_note),
                    minLines = 2,
                    maxLines = 5,
                    textFieldColors = textFieldColors,
                )

                // Model
                SettingsRow(
                    modifier = Modifier.textPadding(),
                    headlineContent = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text  = stringResource(R.string.gemini_model),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Spacer(Modifier.height(DreamoraSpacing.xs))

                            OutlinedTextField(
                                value         = modelText,
                                onValueChange = {
                                    modelText = it
                                    onGeminiModelChange(it)
                                },
                                placeholder   = { Text(predefinedGeminiModels.first()) },
                                label         = { Text(stringResource(R.string.model_name), color = MaterialTheme.colorScheme.onSurface) },
                                modifier      = Modifier.fillMaxWidth(),
                                singleLine    = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors        = textFieldColors
                            )

                            Spacer(Modifier.height(DreamoraSpacing.xs))

                            FlowRow(
                                modifier             = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement  = Arrangement.spacedBy(4.dp)
                            ) {
                                predefinedGeminiModels.forEach { preset ->
                                    PremiumChip(
                                        selected = modelText == preset,
                                        onClick  = {
                                            modelText = preset
                                            onGeminiModelChange(preset)
                                            focusManager.clearFocus()
                                        },
                                        label    = {
                                            Text(
                                                text  = preset,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                            )
                                        },
                                    )
                                }
                            }

                            Spacer(Modifier.height(DreamoraSpacing.xs))
                            Text(
                                text  = stringResource(R.string.default_gemini_model) + "\n" +
                                        stringResource(R.string.model_examples),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                SettingsPromptManager(
                    activeSystemPrompt             = activeSystemPrompt,
                    promptPresets                  = promptPresets,
                    promptUseEnglishLocale         = promptUseEnglishLocale,
                    onActiveSystemPromptChange     = onActiveSystemPromptChange,
                    onPromptUseEnglishLocaleChange = onPromptUseEnglishLocaleChange,
                    onSavePreset                   = onSavePreset,
                    onDeletePreset                 = onDeletePreset,
                )
            }
        }

        // ── OpenAI-compatible config ──────────────────────────────────────────
        AnimatedVisibility(
            visible = translationProvider == "OPENAI",
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column {
                SettingsOpenAITranslation(
                    baseUrl         = openAiBaseUrl,
                    apiKeys         = openAiApiKeys,
                    model           = openAiModel,
                    onBaseUrlChange = onOpenAiBaseUrlChange,
                    onApiKeysChange = onOpenAiApiKeysChange,
                    onModelChange   = onOpenAiModelChange,
                )
                SettingsPromptManager(
                    activeSystemPrompt             = activeSystemPrompt,
                    promptPresets                  = promptPresets,
                    promptUseEnglishLocale         = promptUseEnglishLocale,
                    onActiveSystemPromptChange     = onActiveSystemPromptChange,
                    onPromptUseEnglishLocaleChange = onPromptUseEnglishLocaleChange,
                    onSavePreset                   = onSavePreset,
                    onDeletePreset                 = onDeletePreset,
                )
            }
        }

        // ── Novel-specific prompts ────────────────────────────────────────────
        AnimatedVisibility(
            visible = translationProvider == "GEMINI" || translationProvider == "OPENAI",
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNovelPromptsClick() }
                        .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.md),
                ) {
                    Icon(
                        Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(DreamoraSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.novel_prompts_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (novelPromptCount > 0) {
                            Text(
                                text = stringResource(R.string.novel_prompts_count, novelPromptCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── LLM batch / token settings ────────────────────────────────────────
        AnimatedVisibility(
            visible = translationProvider == "GEMINI" || translationProvider == "OPENAI",
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            SettingsLlmBatchOptions(
                batchSize         = llmBatchSize,
                maxOutputTokens   = llmMaxOutputTokens,
                onBatchSizeChange = onLlmBatchSizeChange,
                onMaxTokensChange = onLlmMaxOutputTokensChange,
            )
        }
    }
}

@Composable
private fun ApiKeyRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    tip: String,
    minLines: Int,
    maxLines: Int,
    textFieldColors: OutlinedTextFieldDefaults.Colors,
) {
    var localValue by rememberSaveable { mutableStateOf(value) }
    SettingsRow(
        modifier = Modifier.textPadding(),
        headlineContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(DreamoraSpacing.xs))
                OutlinedTextField(
                    value         = localValue,
                    onValueChange = { localValue = it; onValueChange(it) },
                    label         = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                    placeholder   = { Text(placeholder) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = minLines,
                    maxLines      = maxLines,
                    colors        = textFieldColors
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}

@Composable
private fun ProviderRadioOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick  = null,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = colorAccent(),
                unselectedColor = MaterialTheme.colorScheme.onSurface,
            )
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label,       style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val focusManager = LocalFocusManager.current
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
        cursorColor          = MaterialTheme.colorScheme.onSurface
    )

    var batchText  by rememberSaveable(batchSize)       { mutableStateOf(batchSize.toString()) }
    var tokensText by rememberSaveable(maxOutputTokens) { mutableStateOf(maxOutputTokens.toString()) }

    val batchPresets  = listOf(20, 40, 60, 80, 100, 120)
    val tokensPresets = listOf(0, 1024, 2048, 4096, 8192, 16384, 32768)

    Column {
        HorizontalDivider(modifier = Modifier.padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp))

        NumericFieldRow(
            title = stringResource(R.string.llm_batch_size),
            tip = stringResource(R.string.llm_batch_size_tip),
            value = batchText,
            onValueChange = { raw ->
                batchText = raw
                raw.toIntOrNull()?.coerceAtLeast(1)?.let(onBatchSizeChange)
            },
            label = stringResource(R.string.llm_batch_size_label),
            presets = batchPresets.map { it.toString() },
            onPresetClick = { batchText = it; onBatchSizeChange(it.toInt()); focusManager.clearFocus() },
            textFieldColors = textFieldColors,
            focusManager = focusManager,
        )

        NumericFieldRow(
            title = stringResource(R.string.llm_max_output_tokens),
            tip = stringResource(R.string.llm_max_output_tokens_tip),
            value = tokensText,
            onValueChange = { raw ->
                tokensText = raw
                raw.toIntOrNull()?.coerceAtLeast(0)?.let(onMaxTokensChange)
            },
            label = stringResource(R.string.llm_max_output_tokens_label),
            presets = tokensPresets.map { if (it == 0) stringResource(R.string.llm_tokens_auto) else it.toString() },
            onPresetClick = { tokensText = it; onMaxTokensChange(if (it == stringResource(R.string.llm_tokens_auto)) 0 else it.toInt()); focusManager.clearFocus() },
            textFieldColors = textFieldColors,
            focusManager = focusManager,
        )
    }
}

@Composable
private fun NumericFieldRow(
    title: String,
    tip: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    presets: List<String>,
    onPresetClick: (String) -> Unit,
    textFieldColors: OutlinedTextFieldDefaults.Colors,
    focusManager: androidx.compose.ui.platform.LocalFocusManager,
) {
    SettingsRow(
        modifier = Modifier.textPadding(),
        headlineContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(DreamoraSpacing.xs))
                OutlinedTextField(
                    value         = value,
                    onValueChange = { raw ->
                        value = raw
                        onValueChange(raw)
                    },
                    label           = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors          = textFieldColors
                )
                Spacer(Modifier.height(DreamoraSpacing.xs))
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { preset ->
                        PremiumChip(
                            selected = value == preset,
                            onClick  = { onPresetClick(preset) },
                            label    = { Text(preset, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) }
                        )
                    }
                }
                Spacer(Modifier.height(DreamoraSpacing.xs))
                Text(
                    text  = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}
