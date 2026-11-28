package my.dreamora.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun SettingsBackup(
    onBackupData: () -> Unit = {},
    onRestoreData: () -> Unit = {},
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
    Column {
        Text(
            text = stringResource(id = R.string.backup),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.textPadding().padding(bottom = DreamoraSpacing.sm),
            color = MaterialTheme.colorScheme.onSurface,
        )
        SettingsRow(
            headlineContent = {
                Text(text = stringResource(R.string.backup_data))
            },
            supportingContent = {
                Text(text = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_saving_location))
            },
            leadingContent = {
                Icon(Icons.Outlined.Save, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding().clickable { onBackupData() }
        )
        SettingsRow(
            headlineContent = {
                Text(text = stringResource(R.string.restore_data))
            },
            supportingContent = {
                Text(text = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_file))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.SettingsBackupRestore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.textPadding().clickable { onRestoreData() }
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        Text(
            text = stringResource(id = R.string.auto_backup),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_backup_switch),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.auto_backup_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = onAutoBackupEnabledChange
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Backup, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding(),
        )

        BackupRow(
            title = stringResource(R.string.auto_backup_directory),
            value = if (autoBackupDirectoryUri.isNotEmpty() && autoBackupDirectoryDisplayName.isNotEmpty())
                autoBackupDirectoryDisplayName
            else if (autoBackupDirectoryUri.isNotEmpty())
                stringResource(R.string.auto_backup_directory_selected)
            else
                stringResource(R.string.auto_backup_select_directory),
            icon = Icons.Outlined.Storage,
            onClick = onAutoBackupSelectDirectory,
        )

        BackupRow(
            title = stringResource(R.string.auto_backup_max_count),
            value = stringResource(R.string.auto_backup_max_count_value, autoBackupMaxCount),
            icon = Icons.Outlined.Schedule,
            onClick = {
                val options = listOf(1, 3, 5, 10, 20, 50)
                val nextIndex = (options.indexOf(autoBackupMaxCount) + 1) % options.size
                onAutoBackupMaxCountChange(options[nextIndex])
            },
        )

        BackupRow(
            title = stringResource(R.string.auto_backup_interval),
            value = formatIntervalMinutes(autoBackupIntervalMinutes),
            icon = Icons.Outlined.Schedule,
            onClick = {
                val options = listOf(60L, 120L, 360L, 720L, 1440L, 4320L, 10080L)
                val nextIndex = (options.indexOf(autoBackupIntervalMinutes) + 1) % options.size
                onAutoBackupIntervalMinutesChange(options[nextIndex])
            },
        )

        BackupRow(
            title = stringResource(R.string.auto_backup_last_time),
            value = if (autoBackupLastTimestamp > 0) {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(autoBackupLastTimestamp))
            } else {
                stringResource(R.string.auto_backup_never)
            },
            icon = Icons.Outlined.Schedule,
        )

        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_backup_include_images),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.auto_backup_include_images_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = autoBackupIncludeImages,
                        onCheckedChange = onAutoBackupIncludeImagesChange
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding(),
        )

        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_backup_include_settings),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.auto_backup_include_settings_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = autoBackupIncludeSettings,
                        onCheckedChange = onAutoBackupIncludeSettingsChange
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding(),
        )

        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_backup_include_plugins),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.auto_backup_include_plugins_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = autoBackupIncludePlugins,
                        onCheckedChange = onAutoBackupIncludePluginsChange
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Extension, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding(),
        )
    }
}

@Composable
private fun BackupRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
) {
    SettingsRow(
        modifier = Modifier.textPadding()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}

/** Format interval in minutes to human-readable string. */
private fun formatIntervalMinutes(minutes: Long): String {
    return when {
        minutes < 60 -> "${minutes} min"
        minutes < 1440 -> "${minutes / 60} h"
        minutes < 10080 -> "${minutes / 1440} days"
        else -> {
            val weeks = minutes / 10080
            if (weeks == 1L) "1 week"
            else "$weeks weeks"
        }
    }
}
