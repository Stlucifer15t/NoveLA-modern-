package my.dreamora.settings.sections

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.settings.R
import my.dreamora.settings.ui.PremiumChip
import my.dreamora.settings.ui.PremiumSettingsRow
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsOpenAITranslation(
    baseUrl: String,
    apiKeys: String,
    model: String,
    onBaseUrlChange: (String) -> Unit,
    onApiKeysChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
) {
    val predefinedModels = listOf(
        "gpt-4o-mini",
        "gpt-4o",
        "gpt-3.5-turbo",
        "mistral-small-latest",
        "mistral-large-latest",
        "meta-llama/llama-3.3-70b-instruct:free",
        "google/gemini-2.0-flash-exp:free",
        "deepseek/deepseek-chat",
        "qwen/qwen-2.5-72b-instruct",
        "microsoft/phi-4",
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.onSurface
    )

    var baseUrlText by rememberSaveable { mutableStateOf(baseUrl) }
    var apiKeysText by rememberSaveable { mutableStateOf(apiKeys) }
    var modelText by rememberSaveable { mutableStateOf(model) }

    val focusManager = LocalFocusManager.current

    Column {
        HorizontalDivider(modifier = Modifier.padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp))

        TextFieldRow(
            title = stringResource(R.string.openai_base_url),
            icon = Icons.Outlined.Link,
            value = baseUrlText,
            onValueChange = {
                baseUrlText = it
                onBaseUrlChange(it)
            },
            label = stringResource(R.string.openai_base_url_label),
            placeholder = "https://api.openai.com",
            tip = stringResource(R.string.openai_base_url_tip),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textFieldColors = textFieldColors,
        )

        TextFieldRow(
            title = stringResource(R.string.openai_api_keys),
            icon = Icons.Outlined.Key,
            value = apiKeysText,
            onValueChange = {
                apiKeysText = it
                onApiKeysChange(it)
            },
            label = stringResource(R.string.openai_api_keys_label),
            placeholder = stringResource(R.string.openai_api_keys_placeholder),
            tip = stringResource(R.string.openai_api_keys_tip),
            singleLine = false,
            minLines = 2,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textFieldColors = textFieldColors,
        )

        ModelRow(
            title = stringResource(R.string.openai_model),
            value = modelText,
            onValueChange = {
                modelText = it
                onModelChange(it)
            },
            label = stringResource(R.string.openai_model_label),
            placeholder = predefinedModels.first(),
            tip = stringResource(R.string.openai_model_tip),
            presets = predefinedModels,
            onPresetClick = {
                modelText = it
                onModelChange(it)
                focusManager.clearFocus()
            },
            textFieldColors = textFieldColors,
            focusManager = focusManager,
        )
    }
}

@Composable
private fun TextFieldRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    tip: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    textFieldColors: OutlinedTextFieldDefaults.Colors,
) {
    SettingsRow(
        modifier = Modifier.textPadding(),
        headlineContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(DreamoraSpacing.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(DreamoraSpacing.xs))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    colors = textFieldColors
                )
                Spacer(Modifier.height(DreamoraSpacing.xs))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}

@Composable
private fun ModelRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    tip: String,
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(DreamoraSpacing.xs))

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = textFieldColors
                )

                Spacer(Modifier.height(DreamoraSpacing.xs))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { preset ->
                        PremiumChip(
                            selected = value == preset,
                            onClick = { onPresetClick(preset) },
                            label = {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(DreamoraSpacing.xs))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}
