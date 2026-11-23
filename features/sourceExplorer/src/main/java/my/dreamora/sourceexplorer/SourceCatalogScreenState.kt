package my.dreamora.sourceexplorer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.dreamora.coreui.components.ToolbarMode
import my.dreamora.coreui.states.PagedListIteratorState
import my.dreamora.core.appPreferences.ListLayoutMode
import my.dreamora.core.appPreferences.SortOrder
import my.dreamora.core.appPreferences.SourceStripPosition
import my.dreamora.feature.local_database.BookMetadata
import my.dreamora.scraper.ActiveFilters
import my.dreamora.scraper.LuaFilter

internal data class SourceCatalogScreenState(
    val sourceCatalogNameStrId: State<Int>,
    val sourceCatalogName: State<String?>,
    val sourceContentType: String,
    val searchTextInput: MutableState<String>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
    val toolbarMode: MutableState<ToolbarMode>,
    val listLayoutMode: MutableState<ListLayoutMode>,
    val sortOrder: MutableState<SortOrder>,

    // Размер сетки — общий preference (2..6), дефолт 3
    val gridColumns: MutableState<Int>,

    // Стиль карточки: OnCover / BelowCover / InfoPanel
    val sourceStripPosition: State<SourceStripPosition>,

    // Фильтры — показывать кнопку только если источник реализует FilterableCatalog
    val hasFilters: Boolean,
    val filterList: State<List<LuaFilter>>,
    val activeFilters: MutableState<ActiveFilters>,
    val isFilterSheetOpen: MutableState<Boolean>,

    // Переводы названий книг (url -> translatedTitle), читаются в item-контенте
    val translatedTitles: Map<String, String>,
)
