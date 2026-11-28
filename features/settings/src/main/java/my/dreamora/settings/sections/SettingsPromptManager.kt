package my.dreamora.settings.sections

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionTitle
import my.dreamora.settings.ui.PremiumChip
import my.dreamora.settings.ui.PremiumSettingsRow
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun SettingsPromptManager(
    activeSystemPrompt: String,
    promptPresets: List<Pair<String, String>>,
    promptUseEnglishLocale: Boolean,
    onActiveSystemPromptChange: (String) -> Unit,
    onPromptUseEnglishLocaleChange: (Boolean) -> Unit,
    onSavePreset: (name: String, prompt: String) -> Unit,
    onDeletePreset: (name: String) -> Unit,
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor          = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor    = MaterialTheme.colorScheme.onSurface,
        unfocusedLabelColor  = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    var promptText by remember(activeSystemPrompt) { mutableStateOf(activeSystemPrompt) }
    var showSavePresetDialog by rememberSaveable { mutableStateOf(false) }
    var presetNameInput by rememberSaveable { mutableStateOf("") }

    PremiumSectionTitle(text = stringResource(R.string.llm_prompt_section_title))

    SettingsRow(
        modifier = Modifier.textPadding().padding(top = DreamoraSpacing.xs),
        headlineContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DreamoraSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = stringResource(R.string.llm_prompt_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(DreamoraSpacing.xs))
                OutlinedTextField(
                    value         = promptText,
                    onValueChange = { promptText = it; onActiveSystemPromptChange(it) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 5,
                    maxLines      = 12,
                    colors        = textFieldColors,
                )
                Spacer(Modifier.height(DreamoraSpacing.xs))
                Text(
                    text  = stringResource(R.string.llm_prompt_placeholders_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(DreamoraSpacing.md))

                PremiumSectionTitle(text = stringResource(R.string.llm_presets))

                Row(
                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
                ) {
                    val allPresets: List<Pair<String, String>> = buildList {
                        addAll(my.dreamora.text_translator.BUILT_IN_PROMPTS)
                        addAll(promptPresets)
                    }
                    val builtInNames = my.dreamora.text_translator.BUILT_IN_PROMPTS.map { it.first }.toSet()

                    allPresets.forEach { (presetName, presetPrompt) ->
                        val isBuiltIn  = presetName in builtInNames
                        val isSelected = promptText == presetPrompt

                        var showChipMenu by remember { mutableStateOf(false) }

                        if (isBuiltIn) {
                            PremiumChip(
                                selected = isSelected,
                                onClick  = {
                                    promptText = presetPrompt
                                    onActiveSystemPromptChange(presetPrompt)
                                },
                                label    = { Text(presetName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) },
                            )
                        } else {
                            Column {
                                PremiumChip(
                                    selected = isSelected,
                                    onClick  = { showChipMenu = true },
                                    label    = { Text(presetName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) },
                                )
                                DropdownMenu(
                                    expanded        = showChipMenu,
                                    onDismissRequest = { showChipMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text    = { Text(stringResource(R.string.apply)) },
                                        onClick = {
                                            promptText = presetPrompt
                                            onActiveSystemPromptChange(presetPrompt)
                                            showChipMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text    = {
                                            Text(
                                                text  = stringResource(R.string.delete),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            onDeletePreset(presetName)
                                            showChipMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(DreamoraSpacing.md))

                OutlinedButton(
                    onClick  = {
                        presetNameInput = ""
                        showSavePresetDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border   = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(DreamoraSpacing.xs))
                    Text(
                        text  = stringResource(R.string.llm_save_preset),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    )

    SettingsRow(
        modifier = Modifier.textPadding(),
        headlineContent = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.Translate,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(DreamoraSpacing.sm))
                    Column {
                        Text(
                            text  = stringResource(R.string.llm_prompt_use_english_locale),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text  = stringResource(R.string.llm_prompt_use_english_locale_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(DreamoraSpacing.md))
                ExpressiveSwitch(
                    checked         = promptUseEnglishLocale,
                    onCheckedChange = onPromptUseEnglishLocaleChange,
                )
            }
        }
    )

    if (showSavePresetDialog) {
        PremiumSavePresetDialog(
            onDismissRequest = { showSavePresetDialog = false },
            presetNameInput = presetNameInput,
            onPresetNameChange = { presetNameInput = it },
            onSave = {
                val name = presetNameInput.trim()
                if (name.isNotEmpty()) {
                    onSavePreset(name, promptText)
                    showSavePresetDialog = false
                }
            },
            onCancel = { showSavePresetDialog = false },
        )
    }
}

@Composable
private fun PremiumSavePresetDialog(
    onDismissRequest: () -> Unit,
    presetNameInput: String,
    onPresetNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text  = stringResource(R.string.llm_save_preset),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        text = {
            OutlinedTextField(
                value         = presetNameInput,
                onValueChange = onPresetNameChange,
                label         = {
                    Text(
                        text  = stringResource(R.string.llm_preset_name),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                modifier   = Modifier.fillMaxWidth(),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor          = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor    = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick  = onSave,
                enabled  = presetNameInput.trim().isNotEmpty(),
                colors   = ButtonDefaults.textButtonColors(
                    contentColor         = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                colors  = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) { Text(stringResource(R.string.cancel)) }
        },
    )
}
