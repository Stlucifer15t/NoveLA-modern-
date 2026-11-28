package my.dreamora.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun SettingsSupport(
    onExportLogs: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.support),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.textPadding().padding(bottom = DreamoraSpacing.sm),
            color = MaterialTheme.colorScheme.onSurface,
        )
        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.export_logs),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.width(DreamoraSpacing.sm))
                    Text(
                        text = stringResource(R.string.export_logs_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.BugReport, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding().clickable { onExportLogs() }
        )
    }
}
