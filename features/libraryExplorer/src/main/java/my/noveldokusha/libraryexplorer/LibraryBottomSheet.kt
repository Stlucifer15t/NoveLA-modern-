package my.noveldokusha.libraryexplorer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.core.appPreferences.LibrarySortOption
import my.noveldokusha.core.appPreferences.SortDirection
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.utils.toToggleableState
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.PosNegCheckbox
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LibraryBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    model: LibraryViewModel = viewModel(),
    pageModel: LibraryPageViewModel = viewModel(),
) {
    if (!visible) return

    val colors = NovelaTheme.colors
    val uiState by model.uiState.collectAsStateWithLifecycle()
    val availableGenres by pageModel.availableGenres
    val selectedGenres by pageModel.selectedGenres.collectAsStateWithLifecycle()
    val availableSources by pageModel.availableSources
    val selectedSources by pageModel.selectedSources.collectAsStateWithLifecycle()
    val selectedContentType by pageModel.selectedContentType.collectAsStateWithLifecycle()
    val gridColumns = uiState.gridColumns

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Sheet Header + Reset Pill ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovelaSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.filter),
                    modifier = Modifier.weight(1f),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    onClick = { pageModel.resetAllFilters() },
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accentSoft,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(id = R.string.reset_filters),
                            color = colors.accent,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // ── Display: Grid Columns & Card Style ───────────────────────────
            NovelaSettingsGroupCard(title = stringResource(id = R.string.grid_columns)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (col in 2..6) {
                            NovelaFilterChip(
                                label = col.toString(),
                                selected = gridColumns == col,
                                onClick = { model.setGridColumns(col) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)

                    Text(
                        text = stringResource(id = R.string.card_style),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        NovelaFilterChip(
                            label = stringResource(R.string.source_strip_on_cover),
                            selected = uiState.sourceStripPosition == SourceStripPosition.OnCover,
                            onClick = { model.setSourceStripPosition(SourceStripPosition.OnCover) },
                        )
                        NovelaFilterChip(
                            label = stringResource(R.string.source_strip_below_cover),
                            selected = uiState.sourceStripPosition == SourceStripPosition.BelowCover,
                            onClick = { model.setSourceStripPosition(SourceStripPosition.BelowCover) },
                        )
                        NovelaFilterChip(
                            label = stringResource(R.string.source_strip_info_panel),
                            selected = uiState.sourceStripPosition == SourceStripPosition.InfoPanel,
                            onClick = { model.setSourceStripPosition(SourceStripPosition.InfoPanel) },
                        )
                    }
                }
            }

            // ── Content Type Filter ──────────────────────────────────────────
            NovelaSettingsGroupCard(title = stringResource(id = R.string.type)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    ContentTypeFilter.entries.forEach { filter ->
                        NovelaFilterChip(
                            label = stringResource(filter.labelRes),
                            selected = selectedContentType == filter,
                            onClick = { pageModel.setContentTypeFilter(filter) },
                        )
                    }
                }
            }

            // ── Sort Options ─────────────────────────────────────────────────
            NovelaSettingsGroupCard(title = stringResource(id = R.string.sort)) {
                val sortOptions = listOf(
                    LibrarySortOption.TITLE to R.string.title,
                    LibrarySortOption.UNREAD_CHAPTERS to R.string.unread_chapters,
                    LibrarySortOption.LAST_READ to R.string.last_read,
                    LibrarySortOption.LAST_UPDATE to R.string.last_update,
                    LibrarySortOption.ADDED to R.string.date_added,
                )

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    sortOptions.forEachIndexed { index, (option, labelRes) ->
                        val isSelected = uiState.sortConfig.option == option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        model.sortConfigToggleDirection()
                                    } else {
                                        model.setSortOption(option)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        model.sortConfigToggleDirection()
                                    } else {
                                        model.setSortOption(option)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                            )
                            Text(
                                text = stringResource(id = labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.accentSoft,
                                    border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(NovelaRadius.pill))
                                        .clickable { model.sortConfigToggleDirection() },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.sortConfig.direction == SortDirection.ASC) {
                                                Icons.Rounded.ArrowUpward
                                            } else {
                                                Icons.Rounded.ArrowDownward
                                            },
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (uiState.sortConfig.direction == SortDirection.ASC) "ASC" else "DESC",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                        )
                                    }
                                }
                            }
                        }
                        if (index < sortOptions.lastIndex) {
                            HorizontalDivider(
                                thickness = NovelaSize.hairline,
                                color = colors.borderSubtle,
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                        }
                    }
                }
            }

            // ── Read Tri-State Filter ────────────────────────────────────────
            NovelaSettingsGroupCard(title = stringResource(id = R.string.filter)) {
                PosNegCheckbox(
                    text = stringResource(id = R.string.read),
                    state = uiState.readFilter.toToggleableState(),
                    onStateChange = { model.readFilterToggle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            // ── Genres Filter ────────────────────────────────────────────────
            if (availableGenres.isNotEmpty()) {
                NovelaSettingsGroupCard(title = stringResource(id = R.string.genres)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        NovelaFilterChip(
                            label = stringResource(R.string.all_genres),
                            selected = selectedGenres.isEmpty(),
                            onClick = { pageModel.clearGenreFilters() },
                        )
                        availableGenres.forEach { genre ->
                            NovelaFilterChip(
                                label = genre,
                                selected = genre in selectedGenres,
                                onClick = { pageModel.toggleGenreFilter(genre) },
                            )
                        }
                    }
                }
            }

            // ── Sources Filter ───────────────────────────────────────────────
            if (availableSources.size > 1) {
                NovelaSettingsGroupCard(title = stringResource(id = R.string.sources)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        NovelaFilterChip(
                            label = stringResource(R.string.all_sources),
                            selected = selectedSources.isEmpty(),
                            onClick = { pageModel.clearSourceFilters() },
                        )
                        availableSources.forEach { sourceName ->
                            NovelaFilterChip(
                                label = sourceName,
                                selected = sourceName in selectedSources,
                                onClick = { pageModel.toggleSourceFilter(sourceName) },
                            )
                        }
                    }
                }
            }
        }
    }
}
