package my.noveldokusha.databaseexplorer.databaseSearch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldokusha.coreui.states.PagedListIteratorState
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.feature.local_database.BookMetadata

internal data class DatabaseSearchScreenState(
    val databaseNameStrId: State<Int>,
    val searchMode: MutableState<SearchMode>,
    val searchTextInput: MutableState<String>,
    val genresList: SnapshotStateList<GenreItem>,
    val listLayoutMode: MutableState<ListLayoutMode>,
    // Число колонок из BOOKS_GRID_COLUMNS — то же значение, что у библиотеки
    // и каталога, поэтому настройка «Раскладка» меняет все три экрана сразу.
    val gridColumns: MutableState<Int>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
)