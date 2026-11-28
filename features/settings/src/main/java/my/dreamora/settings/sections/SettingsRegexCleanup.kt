package my.dreamora.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import my.dreamora.settings.RegexCleanupSettingsScreen
import my.dreamora.settings.RegexCleanupSettingsViewModel
import my.dreamora.settings.ui.PremiumSectionHeader
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SettingsRegexCleanup(
    onNavigateToRegexCleanup: () -> Unit
) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.FindReplace,
            title = stringResource(id = R.string.regex_cleanup),
            summary = stringResource(id = R.string.regex_cleanup_summary),
            accent = colorAccent(),
        )
        SettingsRow(
            modifier = Modifier
                .textPadding()
                .clickable { onNavigateToRegexCleanup() },
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.regex_cleanup_manage_rules),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.width(DreamoraSpacing.sm))
                    Text(
                        text = stringResource(id = R.string.regex_cleanup_manage_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.FindReplace,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}
