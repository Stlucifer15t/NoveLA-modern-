package my.noveldokusha.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SettingsBackup(
    onBackupData: () -> Unit = {},
    onRestoreData: () -> Unit = {},
    // Auto backup
    autoBackupEnabled: Boolean = false,
    onAutoBackupEnabledChange: (Boolean) -> Unit = {},
    autoBackupDirectoryUri: String = "",
    autoBackupDirectoryDisplayName: String = "",
    onAutoBackupSelectDirectory: () -> Unit = {},
    autoBackupMaxCount: Int = 5,
    onAutoBackupMaxCountChange: (Int) -> Unit = {},
    autoBackupIntervalMinutes: Long = 60L,
    onAutoBackupIntervalMinutesChange: (Long) -> Unit = {},
    autoBackupIncludeImages: Boolean = false,
    onAutoBackupIncludeImagesChange: (Boolean) -> Unit = {},
    autoBackupIncludeSettings: Boolean = true,
    onAutoBackupIncludeSettingsChange: (Boolean) -> Unit = {},
    autoBackupIncludePlugins: Boolean = true,
    onAutoBackupIncludePluginsChange: (Boolean) -> Unit = {},
    autoBackupLastTimestamp: Long = 0L,
) {
    val colors = NovelaTheme.colors
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = colors.onAccent,
        checkedTrackColor = colors.accent,
        uncheckedThumbColor = colors.textMuted,
        uncheckedTrackColor = colors.surfaceMuted,
    )

    Column(verticalArrangement = Arrangement.spacedBy(NovelaSpacing.lg)) {
        // ── Manual Backup Section ──
        NovelaSettingsGroupCard(title = stringResource(id = R.string.backup)) {
            NovelaSettingsTile(
                title = stringResource(R.string.backup_data),
                subtitle = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_saving_location),
                icon = Icons.Outlined.Save,
                showChevron = true,
                showDivider = true,
                onClick = onBackupData,
            )
            NovelaSettingsTile(
                title = stringResource(R.string.restore_data),
                subtitle = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_file),
                icon = Icons.Outlined.SettingsBackupRestore,
                showChevron = true,
                onClick = onRestoreData,
            )
        }

        // ── Auto Backup Section ──
        NovelaSettingsGroupCard(title = stringResource(id = R.string.auto_backup)) {
            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_switch),
                subtitle = stringResource(id = R.string.auto_backup_description),
                icon = Icons.Outlined.Backup,
                showDivider = true,
                onClick = { onAutoBackupEnabledChange(!autoBackupEnabled) },
                trailing = {
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = onAutoBackupEnabledChange,
                        colors = switchColors,
                    )
                },
            )

            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_directory),
                subtitle = when {
                    autoBackupDirectoryUri.isNotEmpty() && autoBackupDirectoryDisplayName.isNotEmpty() ->
                        autoBackupDirectoryDisplayName
                    autoBackupDirectoryUri.isNotEmpty() ->
                        stringResource(R.string.auto_backup_directory_selected)
                    else ->
                        stringResource(R.string.auto_backup_select_directory)
                },
                icon = Icons.Outlined.Storage,
                showChevron = true,
                showDivider = true,
                onClick = onAutoBackupSelectDirectory,
            )

            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_max_count),
                subtitle = stringResource(R.string.auto_backup_max_count_value, autoBackupMaxCount),
                icon = Icons.Outlined.Schedule,
                showDivider = true,
                onClick = {
                    val options = listOf(1, 3, 5, 10, 20, 50)
                    val nextIndex = (options.indexOf(autoBackupMaxCount) + 1) % options.size
                    onAutoBackupMaxCountChange(options[nextIndex])
                },
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accentSoft,
                    ) {
                        Text(
                            text = autoBackupMaxCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                },
            )

            val formattedInterval = formatIntervalMinutes(autoBackupIntervalMinutes)
            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_interval),
                subtitle = formattedInterval,
                icon = Icons.Outlined.Schedule,
                showDivider = true,
                onClick = {
                    val options = listOf(60L, 120L, 360L, 720L, 1440L, 4320L, 10080L)
                    val nextIndex = (options.indexOf(autoBackupIntervalMinutes) + 1) % options.size
                    onAutoBackupIntervalMinutesChange(options[nextIndex])
                },
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accentSoft,
                    ) {
                        Text(
                            text = formattedInterval,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                },
            )

            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_last_time),
                subtitle = if (autoBackupLastTimestamp > 0) {
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(autoBackupLastTimestamp))
                } else {
                    stringResource(R.string.auto_backup_never)
                },
                icon = Icons.Outlined.Schedule,
                showDivider = true,
            )

            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_include_images),
                subtitle = stringResource(id = R.string.auto_backup_include_images_description),
                icon = Icons.Outlined.Image,
                showDivider = true,
                onClick = { onAutoBackupIncludeImagesChange(!autoBackupIncludeImages) },
                trailing = {
                    Switch(
                        checked = autoBackupIncludeImages,
                        onCheckedChange = onAutoBackupIncludeImagesChange,
                        colors = switchColors,
                    )
                },
            )

            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_include_settings),
                subtitle = stringResource(id = R.string.auto_backup_include_settings_description),
                icon = Icons.Outlined.Settings,
                showDivider = true,
                onClick = { onAutoBackupIncludeSettingsChange(!autoBackupIncludeSettings) },
                trailing = {
                    Switch(
                        checked = autoBackupIncludeSettings,
                        onCheckedChange = onAutoBackupIncludeSettingsChange,
                        colors = switchColors,
                    )
                },
            )

            NovelaSettingsTile(
                title = stringResource(R.string.auto_backup_include_plugins),
                subtitle = stringResource(id = R.string.auto_backup_include_plugins_description),
                icon = Icons.Outlined.Extension,
                onClick = { onAutoBackupIncludePluginsChange(!autoBackupIncludePlugins) },
                trailing = {
                    Switch(
                        checked = autoBackupIncludePlugins,
                        onCheckedChange = onAutoBackupIncludePluginsChange,
                        colors = switchColors,
                    )
                },
            )
        }
    }
}

private fun formatIntervalMinutes(minutes: Long): String {
    return when {
        minutes < 60 -> "${minutes} min"
        minutes < 1440 -> "${minutes / 60} h"
        minutes < 10080 -> "${minutes / 1440} days"
        else -> {
            val weeks = minutes / 10080
            if (weeks == 1L) "1 week" else "$weeks weeks"
        }
    }
}
