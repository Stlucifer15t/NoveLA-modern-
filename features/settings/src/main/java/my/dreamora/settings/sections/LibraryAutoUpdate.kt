package my.dreamora.settings.sections

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.InternalTheme
import my.dreamora.coreui.theme.textPadding
import my.dreamora.core.domain.AppVersion
import my.dreamora.core.domain.RemoteAppVersion
import my.dreamora.settings.R
import my.dreamora.strings.R as StringsR
import androidx.compose.material.icons.outlined.NotificationsActive
import my.dreamora.settings.SettingsScreenState
import my.dreamora.settings.views.NewAppUpdateDialog
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumChip
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryAutoUpdate(
    state: SettingsScreenState.LibraryAutoUpdate,
) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.AutoMode,
            title = stringResource(StringsR.string.settings_library_updates),
            summary = stringResource(StringsR.string.settings_library_updates_summary),
            accent = colorAccent(),
        )
        SettingsRow(
            modifier = Modifier.textPadding().clickable {
                state.autoUpdateEnabled.value = !state.autoUpdateEnabled.value
            },
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.automatically_check_for_library_updates),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.automatically_check_for_library_updates_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(DreamoraSpacing.md))
                    ExpressiveSwitch(
                        checked = state.autoUpdateEnabled.value,
                        onCheckedChange = {
                            state.autoUpdateEnabled.value = !state.autoUpdateEnabled.value
                        }
                    )
                }
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.AutoMode,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        SettingsRow(
            modifier = Modifier.textPadding(),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.library_update_interval),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm)) {
                        libraryUpdateTimes.forEach {
                            PremiumChip(
                                selected = it.intervalHours == state.autoUpdateIntervalHours.value,
                                onClick = { state.autoUpdateIntervalHours.value = it.intervalHours },
                                label = { Text(text = stringResource(id = it.nameRes), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) },
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )

        SettingsRow(
            modifier = Modifier.textPadding().clickable { state.onSendTestNotification() },
            headlineContent = {
                Text(
                    text = stringResource(StringsR.string.library_test_notification),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                )
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.NotificationsActive,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
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
    @StringRes val nameRes: Int
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
                            version = AppVersion(1, 4, 5)
                        )
                    )
                },
                checkingForNewVersion = remember { mutableStateOf(true) }
            )
        )
    }
}
