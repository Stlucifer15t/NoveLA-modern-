package my.noveldokusha.tooling.novel_migration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.components.NovelaSecondaryButton
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.strings.R
import my.noveldokusha.tooling.novel_migration.data.MigrationOptions

@Composable
fun MigrationOptionsDialog(
    initialOptions: MigrationOptions,
    onConfirm: (MigrationOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = NovelaTheme.colors
    var transferProgress by remember { mutableStateOf(initialOptions.transferProgress) }
    var transferBodies by remember { mutableStateOf(initialOptions.transferBodies) }
    var transferTranslations by remember { mutableStateOf(initialOptions.transferTranslations) }
    var deleteOldBook by remember { mutableStateOf(initialOptions.deleteOldBook) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
        title = {
            Text(
                text = stringResource(R.string.migration_title),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckboxRow(
                    label = stringResource(R.string.migration_reading_progress),
                    checked = transferProgress,
                    onCheckedChange = { transferProgress = it },
                )
                CheckboxRow(
                    label = stringResource(R.string.migration_downloaded_chapters),
                    checked = transferBodies,
                    onCheckedChange = { transferBodies = it },
                )
                CheckboxRow(
                    label = stringResource(R.string.migration_translations),
                    checked = transferTranslations,
                    onCheckedChange = { transferTranslations = it },
                )
                CheckboxRow(
                    label = stringResource(R.string.migration_delete_old_book),
                    checked = deleteOldBook,
                    isDanger = true,
                    onCheckedChange = { deleteOldBook = it },
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NovelaSecondaryButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                NovelaPrimaryButton(
                    text = stringResource(R.string.migration_migrate_all),
                    onClick = {
                        onConfirm(
                            MigrationOptions(
                                transferProgress = transferProgress,
                                transferBodies = transferBodies,
                                transferTranslations = transferTranslations,
                                deleteOldBook = deleteOldBook,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    isDanger: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = NovelaTheme.colors
    val activeColor = if (isDanger) colors.danger else colors.accent
    Surface(
        shape = RoundedCornerShape(NovelaRadius.md),
        color = if (checked) activeColor.copy(alpha = 0.10f) else colors.surfaceSubtle,
        border = BorderStroke(
            width = NovelaSize.hairline,
            color = if (checked) activeColor.copy(alpha = 0.40f) else colors.borderSubtle,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = activeColor,
                    checkmarkColor = colors.onAccent,
                ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (checked && isDanger) colors.danger else colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
