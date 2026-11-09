package my.noveldokusha.settings.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.appPreferences.AppLanguage
import my.noveldokusha.core.appPreferences.AppLanguageProvider
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R

@Composable
internal fun SettingsLanguage(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val colors = NovelaTheme.colors
    var showDialog by remember { mutableStateOf(false) }

    NovelaSettingsGroupCard(title = stringResource(id = R.string.language)) {
        NovelaSettingsTile(
            title = stringResource(id = R.string.language),
            subtitle = currentLanguage.getDisplayName(),
            icon = Icons.Outlined.Language,
            showChevron = true,
            onClick = { showDialog = true },
            trailing = {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accentSoft,
                ) {
                    Text(
                        text = currentLanguage.code.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            },
        )
    }

    if (showDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredLanguages = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                AppLanguageProvider.supportedLanguages
            } else {
                AppLanguageProvider.supportedLanguages.filter { lang ->
                    lang.getDisplayName().contains(searchQuery, ignoreCase = true) ||
                        lang.code.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(NovelaRadius.cardLarge),
            title = {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceMuted,
                            unfocusedContainerColor = colors.surfaceMuted,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        filteredLanguages.forEach { language ->
                            val isSelected = language == currentLanguage
                            Surface(
                                shape = RoundedCornerShape(NovelaRadius.md),
                                color = if (isSelected) colors.accentSoft else colors.surfaceElevated,
                                border = if (isSelected) BorderStroke(NovelaSize.hairline, colors.accent.copy(alpha = 0.45f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(NovelaRadius.md))
                                    .clickable {
                                        onLanguageChange(language)
                                        showDialog = false
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = language.getDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        color = if (isSelected) colors.accent else colors.textPrimary,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        text = stringResource(R.string.close),
                        color = colors.textSecondary,
                    )
                }
            },
        )
    }
}
