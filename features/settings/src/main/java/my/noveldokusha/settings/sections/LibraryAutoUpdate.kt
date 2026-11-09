package my.noveldokusha.settings.sections

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.domain.AppVersion
import my.noveldokusha.core.domain.RemoteAppVersion
import my.noveldokusha.coreui.components.NovelaIconBadge
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R
import my.noveldokusha.settings.SettingsScreenState
import my.noveldokusha.settings.views.NewAppUpdateDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryAutoUpdate(
    state: SettingsScreenState.LibraryAutoUpdate,
) {
    val colors = NovelaTheme.colors

    NovelaSettingsGroupCard(title = "Library updates") {
        NovelaSettingsTile(
            title = stringResource(R.string.automatically_check_for_library_updates),
            icon = Icons.Outlined.AutoMode,
            showDivider = true,
            onClick = { state.autoUpdateEnabled.value = !state.autoUpdateEnabled.value },
            trailing = {
                Switch(
                    checked = state.autoUpdateEnabled.value,
                    onCheckedChange = { state.autoUpdateEnabled.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceMuted,
                    ),
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovelaIconBadge(icon = Icons.Outlined.Timer)
            Spacer(Modifier.width(12.dp))
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                libraryUpdateTimes.forEach { item ->
                    val selected = item.intervalHours == state.autoUpdateIntervalHours.value
                    Surface(
                        onClick = { state.autoUpdateIntervalHours.value = item.intervalHours },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = if (selected) colors.accentSoft else colors.surfaceMuted,
                        border = BorderStroke(
                            NovelaSize.hairline,
                            if (selected) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle,
                        ),
                    ) {
                        Text(
                            text = stringResource(id = item.nameRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) colors.accent else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

internal val libraryUpdateTimes = listOf(
    UpdateIntervalTimes(intervalHours = 6, nameRes = R.string.library_update_interval_6_h),
    UpdateIntervalTimes(intervalHours = 12, nameRes = R.string.library_update_interval_12_h),
    UpdateIntervalTimes(intervalHours = 24, nameRes = R.string.library_update_interval_1_day),
    UpdateIntervalTimes(intervalHours = 24 * 2, nameRes = R.string.library_update_interval_2_days),
)

internal data class UpdateIntervalTimes(
    val intervalHours: Int,
    @StringRes val nameRes: Int,
)

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        NewAppUpdateDialog(
            updateApp = SettingsScreenState.UpdateApp(
                currentAppVersion = "1.2.3",
                appUpdateCheckerEnabled = remember { mutableStateOf(true) },
                showNewVersionDialog = remember {
                    mutableStateOf(
                        RemoteAppVersion(
                            sourceUrl = "url",
                            version = AppVersion(1, 4, 5),
                        )
                    )
                },
                checkingForNewVersion = remember { mutableStateOf(true) },
            )
        )
    }
}
