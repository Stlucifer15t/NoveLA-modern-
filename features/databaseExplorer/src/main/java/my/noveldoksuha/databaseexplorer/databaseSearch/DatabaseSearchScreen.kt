package my.noveldokusha.databaseexplorer.databaseSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.coreui.components.BooksVerticalView
import my.noveldokusha.coreui.components.CollapsibleDivider
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.TopAppBarSearch
import my.noveldokusha.coreui.states.PagedListIteratorState
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.databaseexplorer.R
import my.noveldokusha.feature.local_database.BookMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseSearchScreen(
    state: DatabaseSearchScreenState,
    onSearchCatalogSubmit: () -> Unit,
    onGenresFiltersSubmit: () -> Unit,
    onSearchInputSubmit: () -> Unit,
    onSearchInputChange: (String) -> Unit,
    onSearchModeChange: (SearchMode) -> Unit,
    onBookClicked: (BookMetadata) -> Unit,
    onBookLongClicked: (BookMetadata) -> Unit,
    onPressBack: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val showFiltersBottomSheet = rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null,
    )

    val searchPlaceholderText = when (state.searchMode.value) {
        SearchMode.BookTitle, SearchMode.Catalog -> stringResource(id = R.string.search_by_title)
        SearchMode.BookGenres -> ""
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBarSearch(
                focusRequester = focusRequester,
                searchTextInput = state.searchTextInput.value,
                onSearchTextChange = onSearchInputChange,
                onTextDone = {
                    onSearchModeChange(SearchMode.BookTitle)
                    onSearchInputSubmit()
                },
                onClose = onPressBack,
                placeholderText = searchPlaceholderText,
                scrollBehavior = scrollBehavior,
                inputEnabled = state.searchMode.value != SearchMode.BookGenres,
                labelText = stringResource(id = state.databaseNameStrId.value),
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = NovelaSpacing.lg, vertical = 8.dp),
                ) {
                    NovelaFilterChip(
                        label = stringResource(R.string.filter_catalog),
                        selected = state.searchMode.value == SearchMode.Catalog,
                        leadingIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                        onClick = {
                            onSearchModeChange(SearchMode.Catalog)
                            focusRequester.freeFocus()
                            onSearchCatalogSubmit()
                        },
                    )
                    NovelaFilterChip(
                        label = stringResource(R.string.filter_title),
                        selected = state.searchMode.value == SearchMode.BookTitle,
                        leadingIcon = Icons.Filled.Title,
                        onClick = {
                            onSearchModeChange(SearchMode.BookTitle)
                            focusRequester.requestFocus()
                        },
                    )
                    NovelaFilterChip(
                        label = stringResource(R.string.filter_genres),
                        selected = state.searchMode.value == SearchMode.BookGenres,
                        leadingIcon = Icons.Filled.FilterList,
                        onClick = {
                            onSearchModeChange(SearchMode.BookGenres)
                            showFiltersBottomSheet.value = true
                        },
                    )
                }
                CollapsibleDivider(scrollBehavior.state)
                BooksVerticalView(
                    list = state.fetchIterator.list,
                    state = rememberLazyGridState(),
                    error = state.fetchIterator.error,
                    loadState = state.fetchIterator.state,
                    layoutMode = state.listLayoutMode.value,
                    onLoadNext = state.fetchIterator::fetchNext,
                    onBookClicked = onBookClicked,
                    onBookLongClicked = onBookLongClicked,
                )
            }
        },
    )

    LaunchedEffect(state.searchMode.value) {
        showFiltersBottomSheet.value = state.searchMode.value == SearchMode.BookGenres
    }

    if (showFiltersBottomSheet.value) {
        val sheetState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showFiltersBottomSheet.value = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
            containerColor = colors.surfaceFloating,
        ) {
            DatabaseSearchBottomSheet(
                genresList = state.genresList,
                onGenresFiltersSubmit = {
                    onGenresFiltersSubmit()
                    coroutineScope.launch {
                        sheetState.partialExpand()
                    }
                },
            )
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {
    val genresList = remember { mutableStateListOf<GenreItem>() }
    val list = remember {
        mutableStateListOf(
            BookMetadata(title = "title1", url = "url1"),
            BookMetadata(title = "title2", url = "url2"),
            BookMetadata(title = "title3", url = "url3"),
        )
    }
    val scope = rememberCoroutineScope()
    val fetchIterator = PagedListIteratorState(list = list, coroutineScope = scope) {
        Response.Success(PagedList(listOf(), 0, true))
    }
    val state = remember {
        DatabaseSearchScreenState(
            databaseNameStrId = mutableIntStateOf(R.string.database_name_baka_updates),
            searchMode = mutableStateOf(SearchMode.BookGenres),
            genresList = genresList,
            searchTextInput = mutableStateOf(""),
            fetchIterator = fetchIterator,
            listLayoutMode = mutableStateOf(ListLayoutMode.VerticalGrid),
        )
    }

    InternalTheme {
        DatabaseSearchScreen(
            state = state,
            onSearchModeChange = { },
            onSearchInputChange = { },
            onSearchCatalogSubmit = { },
            onSearchInputSubmit = { },
            onBookClicked = { },
            onBookLongClicked = { },
            onPressBack = { },
            onGenresFiltersSubmit = { },
        )
    }
}
