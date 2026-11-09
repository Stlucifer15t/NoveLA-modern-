package my.noveldokusha.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.settings.R

@Composable
internal fun SettingsSupport(
    onExportLogs: () -> Unit,
) {
    NovelaSettingsGroupCard(title = stringResource(R.string.support)) {
        NovelaSettingsTile(
            title = stringResource(R.string.export_logs),
            subtitle = stringResource(R.string.export_logs_description),
            icon = Icons.Outlined.BugReport,
            showChevron = true,
            onClick = onExportLogs,
        )
    }
}
