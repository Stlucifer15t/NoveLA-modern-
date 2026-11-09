package my.noveldokusha.settings.sections

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.settings.R
import my.noveldokusha.text_translator.BUILT_IN_PROMPTS

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
    val colors = NovelaTheme.colors
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.borderSubtle,
        cursorColor = colors.accent,
    )

    var promptText by remember(activeSystemPrompt) { mutableStateOf(activeSystemPrompt) }
    var showSavePresetDialog by rememberSaveable { mutableStateOf(false) }
    var presetNameInput by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = NovelaSize.hairline,
            color = colors.borderSubtle,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.llm_prompt_section_title).uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = colors.accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.llm_prompt_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = promptText,
                onValueChange = {
                    promptText = it
                    onActiveSystemPromptChange(it)
                },
                shape = RoundedCornerShape(NovelaRadius.md),
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 12,
                colors = textFieldColors,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.llm_prompt_placeholders_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )

            Spacer(Modifier.height(10.dp))

            val allPresets: List<Pair<String, String>> = buildList {
                addAll(BUILT_IN_PROMPTS)
                addAll(promptPresets)
            }
            val builtInNames = BUILT_IN_PROMPTS.map { it.first }.toSet()

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allPresets.forEach { (presetName, presetPrompt) ->
                    val isBuiltIn = presetName in builtInNames
                    val isSelected = promptText == presetPrompt
                    var showChipMenu by remember { mutableStateOf(false) }

                    if (isBuiltIn) {
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                promptText = presetPrompt
                                onActiveSystemPromptChange(presetPrompt)
                            },
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentSoft,
                                selectedLabelColor = colors.accent,
                            ),
                            label = { Text(presetName) },
                        )
                    } else {
                        Column {
                            FilterChip(
                                selected = isSelected,
                                onClick = { showChipMenu = true },
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentSoft,
                                    selectedLabelColor = colors.accent,
                                ),
                                label = { Text(presetName) },
                            )
                            DropdownMenu(
                                expanded = showChipMenu,
                                onDismissRequest = { showChipMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.apply)) },
                                    onClick = {
                                        promptText = presetPrompt
                                        onActiveSystemPromptChange(presetPrompt)
                                        showChipMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.delete),
                                            color = colors.danger,
                                        )
                                    },
                                    onClick = {
                                        onDeletePreset(presetName)
                                        showChipMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    presetNameInput = ""
                    showSavePresetDialog = true
                },
                shape = RoundedCornerShape(NovelaRadius.pill),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.accent,
                ),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.llm_save_preset),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        NovelaSettingsTile(
            title = stringResource(R.string.llm_prompt_use_english_locale),
            subtitle = stringResource(R.string.llm_prompt_use_english_locale_tip),
            icon = Icons.Outlined.Translate,
            onClick = { onPromptUseEnglishLocaleChange(!promptUseEnglishLocale) },
            trailing = {
                Switch(
                    checked = promptUseEnglishLocale,
                    onCheckedChange = onPromptUseEnglishLocaleChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceMuted,
                    ),
                )
            },
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(NovelaRadius.cardLarge),
            title = {
                Text(
                    text = stringResource(R.string.llm_save_preset),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text(stringResource(R.string.llm_preset_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(NovelaRadius.md),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = presetNameInput.trim()
                        if (name.isNotEmpty()) {
                            onSavePreset(name, promptText)
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetNameInput.trim().isNotEmpty(),
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
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = colors.textSecondary,
                    )
                }
            },
        )
    }
}
