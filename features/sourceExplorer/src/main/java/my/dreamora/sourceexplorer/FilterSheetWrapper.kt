package my.dreamora.sourceexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import my.dreamora.core.appPreferences.FilterHistoryEntry
import my.dreamora.core.appPreferences.FilterPreset
import my.dreamora.scraper.ActiveFilters

/**
 * Bridges ViewModel state to FilterBottomSheet params.
 *
 * Absorbs new wiring so the call-site in SourceCatalogScreen stays minimal.
 */
@Composable
internal fun FilterSheetWrapper(
    viewModel: SourceCatalogViewModel,
    onDismiss: () -> Unit,
) {
    val filterList by viewModel.state.filterList
    val activeFilters by viewModel.state.activeFilters
    val presets by viewModel.presets
    val textHistory by viewModel.textHistory

    FilterBottomSheet(
        filterList = filterList,
        activeFilters = activeFilters,
        onApply = { filters ->
            viewModel.onApplyFilters(filters)
        },
        onDismiss = onDismiss,
        presets = presets,
        textHistory = textHistory,
        onPresetSave = viewModel::onPresetSave,
        onPresetLoad = viewModel::onPresetLoad,
        onPresetDelete = viewModel::onPresetDelete,
        onTextHistoryAdd = viewModel::onTextHistoryAdd,
        onTextHistoryRemove = viewModel::onTextHistoryRemove,
    )
}
