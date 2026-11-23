package my.dreamora.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.theme.DreamoraTheme
import my.dreamora.coreui.theme.DreamoraType
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.SettingsRowPadding

/**
 * Extension sources marked 18+ by their repository. Hidden from the extension catalog and the
 * installed list until the reader turns this on. The same switch is in the repository manager.
 */
@Composable
fun SettingsSources(showAdult: MutableState<Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(my.dreamora.strings.R.string.settings_sources_adult_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(my.dreamora.strings.R.string.settings_sources_adult_hint),
                style = DreamoraType.bookMeta,
                color = DreamoraTheme.colors.textMuted,
            )
        }
        ExpressiveSwitch(
            checked = showAdult.value,
            onCheckedChange = { showAdult.value = it },
        )
    }
}
