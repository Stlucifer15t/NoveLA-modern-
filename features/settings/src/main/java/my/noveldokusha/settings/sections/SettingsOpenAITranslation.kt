package my.noveldokusha.settings.sections

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R

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
    val colors = NovelaTheme.colors
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
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.borderSubtle,
        cursorColor = colors.accent,
    )

    var baseUrlText by rememberSaveable { mutableStateOf(baseUrl) }
    var apiKeysText by rememberSaveable { mutableStateOf(apiKeys) }
    var modelText by rememberSaveable { mutableStateOf(model) }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)

        // ── Base URL ──
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Link, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.openai_base_url),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrlText,
                onValueChange = {
                    baseUrlText = it
                    onBaseUrlChange(it)
                },
                label = { Text(stringResource(R.string.openai_base_url_label)) },
                placeholder = { Text("https://api.openai.com") },
                shape = RoundedCornerShape(NovelaRadius.md),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = textFieldColors,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.openai_base_url_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }

        // ── API Keys ──
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.openai_api_keys),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKeysText,
                onValueChange = {
                    apiKeysText = it
                    onApiKeysChange(it)
                },
                label = { Text(stringResource(R.string.openai_api_keys_label)) },
                placeholder = { Text(stringResource(R.string.openai_api_keys_placeholder)) },
                shape = RoundedCornerShape(NovelaRadius.md),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                colors = textFieldColors,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.openai_api_keys_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }

        // ── Model ──
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.openai_model),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = modelText,
                onValueChange = {
                    modelText = it
                    onModelChange(it)
                },
                label = { Text(stringResource(R.string.openai_model_label)) },
                placeholder = { Text(predefinedModels.first()) },
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
                predefinedModels.forEach { preset ->
                    FilterChip(
                        selected = modelText == preset,
                        onClick = {
                            modelText = preset
                            onModelChange(preset)
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
                text = stringResource(R.string.openai_model_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}
