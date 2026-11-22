package my.noveldokusha.settings.sections

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.coreui.components.SlimListItem
import my.noveldokusha.coreui.theme.colorAccent
import my.noveldokusha.coreui.theme.textPadding
import my.noveldokusha.settings.LayoutPreferences
import my.noveldokusha.settings.R
import my.noveldokusha.settings.ui.ExpressiveSwitch

/**
 * How the shelves are laid out.
 *
 * This screen writes the same preferences the tabs read: `FOR_YOU_*` for the For You tab and
 * `BOOKS_*` for the library, the plugin catalog and the source search (those three have always
 * shared one column count, and they still do — what was missing was a door to it that was not
 * buried in the library's own sort sheet). Nothing here is a preview of a setting: the row shows
 * the stored value, and the tab changes while you look at it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsLayout(layout: LayoutPreferences) {
    Column {
        Text(
            text = stringResource(R.string.settings_group_layout),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = colorAccent(),
        )

        LayoutLabel(Icons.Outlined.ViewModule, R.string.layout_display_mode)
        ModeRow(
            title = stringResource(R.string.layout_for_you),
            selected = layout.forYouLayoutMode.value,
            onSelect = { layout.forYouLayoutMode.value = it },
        )
        // The library is a grid by design (its own sort sheet has no list mode), so this row
        // names the two screens the preference actually drives.
        ModeRow(
            title = stringResource(R.string.layout_browse_search),
            selected = layout.booksLayoutMode.value,
            onSelect = { layout.booksLayoutMode.value = it },
        )

        LayoutLabel(Icons.Outlined.GridView, R.string.layout_grid_columns)
        ColumnsRow(
            title = stringResource(R.string.layout_for_you),
            selected = layout.forYouGridColumns.value,
            choices = LayoutPreferences.GRID_COLUMN_CHOICES,
            onSelect = layout::setForYouGridColumns,
        )
        Text(
            text = stringResource(R.string.layout_for_you_columns_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .textPadding()
                .padding(bottom = 4.dp),
        )
        ColumnsRow(
            title = stringResource(R.string.layout_library_shared),
            selected = layout.booksGridColumns.value,
            choices = (LayoutPreferences.MIN_GRID_COLUMNS..LayoutPreferences.MAX_GRID_COLUMNS).toList(),
            onSelect = layout::setBooksGridColumns,
        )

        SlimListItem(
            modifier = Modifier.textPadding(),
            headlineContent = { Text(stringResource(R.string.layout_grey_read)) },
            supportingContent = {
                Text(
                    text = stringResource(R.string.layout_grey_read_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                ExpressiveSwitch(
                    checked = layout.greyOutReadChapters.value,
                    onCheckedChange = { layout.greyOutReadChapters.value = it },
                )
            },
        )
    }
}

@Composable
private fun LayoutLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, @StringRes text: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .textPadding()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeRow(
    title: String,
    selected: ListLayoutMode,
    onSelect: (ListLayoutMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .textPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ListLayoutMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    label = {
                        Text(
                            stringResource(
                                if (mode == ListLayoutMode.VerticalGrid) R.string.layout_display_mode_grid
                                else R.string.layout_display_mode_list
                            )
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnsRow(
    title: String,
    selected: Int,
    choices: List<Int>,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .textPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { columns ->
                FilterChip(
                    selected = columns == selected,
                    onClick = { onSelect(columns) },
                    label = {
                        Text(
                            if (columns == LayoutPreferences.AUTO_COLUMNS) {
                                stringResource(R.string.layout_columns_auto)
                            } else {
                                columns.toString()
                            }
                        )
                    }
                )
            }
        }
    }
}
