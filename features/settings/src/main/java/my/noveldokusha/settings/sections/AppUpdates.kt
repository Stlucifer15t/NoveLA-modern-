package my.noveldokusha.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R
import my.noveldokusha.settings.SettingsScreenState
import my.noveldokusha.settings.views.NewAppUpdateDialog

@Composable
internal fun AppUpdates(
    state: SettingsScreenState.UpdateApp,
    onCheckForUpdatesManual: () -> Unit,
) {
    val colors = NovelaTheme.colors

    NovelaSettingsGroupCard(title = stringResource(R.string.app_updates)) {
        NovelaSettingsTile(
            title = stringResource(R.string.automatically_check_for_app_updates),
            icon = Icons.Outlined.AutoMode,
            showDivider = true,
            onClick = { state.appUpdateCheckerEnabled.value = !state.appUpdateCheckerEnabled.value },
            trailing = {
                Switch(
                    checked = state.appUpdateCheckerEnabled.value,
                    onCheckedChange = { state.appUpdateCheckerEnabled.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceMuted,
                    ),
                )
            },
        )

        NovelaSettingsTile(
            title = stringResource(R.string.check_for_app_updates_now),
            icon = Icons.Outlined.SystemUpdateAlt,
            showChevron = !state.checkingForNewVersion.value,
            onClick = if (!state.checkingForNewVersion.value) onCheckForUpdatesManual else null,
            trailing = if (state.checkingForNewVersion.value) {
                {
                    AnimatedVisibility(visible = state.checkingForNewVersion.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent,
                        )
                    }
                }
            } else {
                null
            },
        )
    }

    NewAppUpdateDialog(updateApp = state)
}
