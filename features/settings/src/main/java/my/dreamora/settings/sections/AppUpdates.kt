package my.dreamora.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.DoubleArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.debouncedClickable
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import my.dreamora.settings.SettingsScreenState
import my.dreamora.settings.views.NewAppUpdateDialog
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionHeader
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun AppUpdates(
    state: SettingsScreenState.UpdateApp,
    onCheckForUpdatesManual: () -> Unit
) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.AutoMode,
            title = stringResource(R.string.app_updates),
            summary = stringResource(R.string.app_updates_summary),
            accent = colorAccent(),
        )
        SettingsRow(
            modifier = Modifier.textPadding().clickable {
                state.appUpdateCheckerEnabled.value = !state.appUpdateCheckerEnabled.value
            },
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.automatically_check_for_app_updates),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.automatically_check_for_app_updates_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(DreamoraSpacing.md))
                    ExpressiveSwitch(
                        checked = state.appUpdateCheckerEnabled.value,
                        onCheckedChange = {
                            state.appUpdateCheckerEnabled.value = !state.appUpdateCheckerEnabled.value
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
            headlineContent = {
                Text(text = stringResource(R.string.check_for_app_updates_now))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.DoubleArrow,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                AnimatedVisibility(visible = state.checkingForNewVersion.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.textPadding().debouncedClickable { onCheckForUpdatesManual() }
        )
        NewAppUpdateDialog(
            updateApp = state
        )
    }
}
