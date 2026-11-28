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
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.R

@Composable
fun SettingsSources(showAdult: MutableState<Boolean>) {
    Column {
        PremiumSectionHeader(
            icon = androidx.compose.material.icons.Icons.Outlined.Extension,
            title = stringResource(my.dreamora.strings.R.string.settings_sources_adult_title),
            summary = stringResource(my.dreamora.strings.R.string.settings_sources_adult_hint),
            accent = MaterialTheme.colorScheme.error,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DreamoraSpacing.screenH)
                .padding(bottom = DreamoraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(my.dreamora.strings.R.string.settings_sources_adult_title),
                    style = MaterialTheme.typography.titleMedium,
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
}
