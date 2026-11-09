package my.noveldokusha.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.settings.R

@Composable
fun SettingsRegexCleanup(
    onNavigateToRegexCleanup: () -> Unit,
) {
    NovelaSettingsGroupCard(title = stringResource(id = R.string.regex_cleanup)) {
        NovelaSettingsTile(
            title = stringResource(id = R.string.regex_cleanup_manage_rules),
            subtitle = stringResource(id = R.string.regex_cleanup_manage_description),
            icon = Icons.Outlined.FindReplace,
            showChevron = true,
            onClick = onNavigateToRegexCleanup,
        )
    }
}
