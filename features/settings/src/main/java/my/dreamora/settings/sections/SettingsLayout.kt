package my.dreamora.settings.sections

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.core.appPreferences.ListLayoutMode
import my.dreamora.settings.LayoutPreferences
import my.dreamora.settings.R
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumChip
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumSectionTitle
import my.dreamora.settings.ui.PremiumSettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.textPadding
import androidx.compose.ui.text.font.FontWeight

/**
 * How the shelves are laid out.
 *
 * This screen writes the same preferences the tabs read: `FOR_YOU_*` for the For You tab and
 * `BOOKS_*` for the library, the plugin catalog and the source search.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsLayout(layout: LayoutPreferences) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.ViewModule,
            title = stringResource(R.string.settings_group_layout),
            summary = stringResource(R.string.settings_group_layout_hint),
            accent = MaterialTheme.colorScheme.primary,
        )

        PremiumSectionTitle(text = stringResource(R.string.layout_display_mode))

        PremiumSettingsRow(
            modifier = Modifier.textPadding().padding(top = DreamoraSpacing.sm),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.layout_for_you),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ListLayoutMode.entries.forEach { mode ->
                            PremiumChip(
                                selected = mode == layout.forYouLayoutMode.value,
                                onClick = { layout.forYouLayoutMode.value = it },
                                label = {
                                    Text(
                                        text = if (mode == ListLayoutMode.VerticalGrid) {
                                            stringResource(R.string.layout_display_mode_grid)
                                        } else {
                                            stringResource(R.string.layout_display_mode_list)
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    )
                                },
                            )
                        }
                    }
                }
            },
        )

        PremiumSettingsRow(
            modifier = Modifier.textPadding(),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.layout_browse_search),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ListLayoutMode.entries.forEach { mode ->
                            PremiumChip(
                                selected = mode == layout.booksLayoutMode.value,
                                onClick = { layout.booksLayoutMode.value = it },
                                label = {
                                    Text(
                                        text = if (mode == ListLayoutMode.VerticalGrid) {
                                            stringResource(R.string.layout_display_mode_grid)
                                        } else {
                                            stringResource(R.string.layout_display_mode_list)
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    )
                                },
                            )
                        }
                    }
                }
            },
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        PremiumSectionTitle(text = stringResource(R.string.layout_grid_columns))

        PremiumSettingsRow(
            modifier = Modifier.textPadding().padding(top = DreamoraSpacing.sm),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.layout_for_you),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        choicesForYou.forEach { columns ->
                            PremiumChip(
                                selected = columns == layout.forYouGridColumns.value,
                                onClick = { layout.setForYouGridColumns(columns) },
                                label = {
                                    Text(
                                        text = if (columns == LayoutPreferences.AUTO_COLUMNS) {
                                            stringResource(R.string.layout_columns_auto)
                                        } else {
                                            columns.toString()
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    )
                                },
                            )
                        }
                    }
                }
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.layout_for_you_columns_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        PremiumSettingsRow(
            modifier = Modifier.textPadding(),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.layout_library_shared),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (LayoutPreferences.MIN_GRID_COLUMNS..LayoutPreferences.MAX_GRID_COLUMNS).toList().forEach { columns ->
                            PremiumChip(
                                selected = columns == layout.booksGridColumns.value,
                                onClick = { layout.setBooksGridColumns(columns) },
                                label = {
                                    Text(
                                        text = columns.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    )
                                },
                            )
                        }
                    }
                }
            },
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        PremiumSettingsRow(
            modifier = Modifier.textPadding(),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.layout_grey_read),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.layout_grey_read_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            trailingContent = {
                ExpressiveSwitch(
                    checked = layout.greyOutReadChapters.value,
                    onCheckedChange = { layout.greyOutReadChapters.value = it },
                )
            },
            onClick = { layout.greyOutReadChapters.value = !layout.greyOutReadChapters.value },
        )
    }
}

private val choicesForYou = listOf(LayoutPreferences.Companion.AUTO_COLUMNS, 2, 3, 4, 5, 6)
