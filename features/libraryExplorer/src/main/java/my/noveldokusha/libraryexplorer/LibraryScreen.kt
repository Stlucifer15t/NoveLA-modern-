package my.noveldokusha.libraryexplorer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.core.appPreferences.TernaryState
import my.noveldokusha.coreui.components.AnimatedTransition
import my.noveldokusha.coreui.components.NovelaScreenHeader
import my.noveldokusha.coreui.components.NovelaTopProgress
import my.noveldokusha.coreui.components.ToolbarMode
import my.noveldokusha.coreui.components.TopAppBarSearch
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.feature.local_database.BookWithContext
import my.noveldokusha.navigation.NavigationRouteViewModel
import my.noveldokusha.tooling.epub_importer.onDoImportFile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LibraryScreen(
    navigationRouteViewModel: NavigationRouteViewModel = viewModel(),
) {
    val libraryModel: LibraryViewModel = viewModel()
    val pageViewModel: LibraryPageViewModel = viewModel()
    val colors = NovelaTheme.colors

    val uiState by libraryModel.uiState.collectAsStateWithLifecycle()
    val selectedGenres by pageViewModel.selectedGenres.collectAsStateWithLifecycle()
    val selectedSources by pageViewModel.selectedSources.collectAsStateWithLifecycle()
    val selectedContentType by pageViewModel.selectedContentType.collectAsStateWithLifecycle()
    val filteredBooks by pageViewModel.filteredList

    val hasActiveFilter = uiState.readFilter != TernaryState.Inactive ||
        selectedGenres.isNotEmpty() ||
        selectedSources.isNotEmpty() ||
        selectedContentType != ContentTypeFilter.ALL

    val context = LocalContext.current
    val showDropdownMenu = remember { mutableStateOf(false) }
    val lastClickTime = remember { mutableStateOf(0L) }
    val importFileAction = onDoImportFile()

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showMoveToCategoryDialog by remember { mutableStateOf(false) }

    val handleBookClick = remember(context, uiState.isSelectionMode) {
        { book: BookWithContext ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime.value >= 200L) {
                lastClickTime.value = currentTime
                if (uiState.isSelectionMode) {
                    libraryModel.toggleBookSelection(book.book.url)
                } else {
                    navigationRouteViewModel.chapters(
                        context = context,
                        bookMetadata = BookMetadata(
                            title = book.book.title,
                            url = book.book.url,
                        ),
                    ).let(context::startActivity)
                }
            }
        }
    }

    val handleBookLongClick = remember(uiState.isSelectionMode) {
        { book: BookWithContext ->
            if (!uiState.isSelectionMode) {
                libraryModel.setBookActionsSheetBook(book.book)
            } else {
                libraryModel.toggleBookSelection(book.book.url)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (uiState.isSelectionMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = NovelaSpacing.md, vertical = NovelaSpacing.xs),
                ) {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.card),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                color = colors.accentSoft,
                            ) {
                                Text(
                                    text = stringResource(R.string.selected_count, libraryModel.selectedBooks.size),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    val currentBooks = pageViewModel.filteredList.value
                                    libraryModel.selectAllBooks(currentBooks)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelectAll,
                                    contentDescription = stringResource(R.string.select_all),
                                    tint = colors.textPrimary,
                                )
                            }
                            IconButton(
                                onClick = { showMoveToCategoryDialog = true },
                                enabled = libraryModel.selectedBooks.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DriveFileRenameOutline,
                                    contentDescription = stringResource(R.string.move_to),
                                    tint = if (libraryModel.selectedBooks.isNotEmpty()) colors.accent else colors.textMuted,
                                )
                            }
                            IconButton(
                                onClick = { libraryModel.fixSelectedBooks() },
                                enabled = libraryModel.selectedBooks.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Build,
                                    contentDescription = stringResource(R.string.book_fix),
                                    tint = if (libraryModel.selectedBooks.isNotEmpty()) colors.warning else colors.textMuted,
                                )
                            }
                            IconButton(
                                onClick = { libraryModel.deleteSelectedBooks() },
                                enabled = libraryModel.selectedBooks.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = if (libraryModel.selectedBooks.isNotEmpty()) colors.danger else colors.textMuted,
                                )
                            }
                            IconButton(onClick = { libraryModel.toggleSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = colors.textPrimary,
                                )
                            }
                        }
                    }
                    if (uiState.isFixingBooks) {
                        LinearProgressIndicator(
                            progress = {
                                if (uiState.fixTotal > 0) {
                                    uiState.fixProgress.toFloat() / uiState.fixTotal.toFloat()
                                } else {
                                    0f
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            color = colors.warning,
                            trackColor = colors.surfaceSubtle,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    AnimatedTransition(targetState = uiState.toolbarMode) { target ->
                        when (target) {
                            ToolbarMode.MAIN -> Column {
                                val headerSubtitle = when {
                                    pageViewModel.isUpdating -> stringResource(R.string.updating_library_notice)
                                    pageViewModel.isLibraryLoaded -> "${filteredBooks.size} · ${stringResource(R.string.reading)}"
                                    else -> stringResource(R.string.reading)
                                }
                                NovelaScreenHeader(
                                    title = stringResource(R.string.title_library),
                                    subtitle = headerSubtitle,
                                    actions = {
                                        if (pageViewModel.isUpdating) {
                                            IconButton(onClick = { pageViewModel.cancelLibraryUpdates() }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = stringResource(R.string.cancel),
                                                    tint = colors.danger,
                                                )
                                            }
                                        }
                                        IconButton(onClick = { libraryModel.setToolbarMode(ToolbarMode.SEARCH) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = stringResource(R.string.search_for_title),
                                                tint = colors.textPrimary,
                                            )
                                        }
                                        Box {
                                            IconButton(onClick = { libraryModel.setShowBottomSheet(true) }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Tune,
                                                    contentDescription = stringResource(R.string.filter),
                                                    tint = if (hasActiveFilter) colors.accent else colors.textPrimary,
                                                )
                                            }
                                            if (hasActiveFilter) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-9).dp, y = 10.dp)
                                                        .size(7.dp)
                                                        .background(colors.accent, CircleShape)
                                                )
                                            }
                                        }
                                        IconButton(onClick = { libraryModel.toggleShowCategories() }) {
                                            Icon(
                                                imageVector = if (uiState.showCategories) {
                                                    Icons.Rounded.FolderOpen
                                                } else {
                                                    Icons.Outlined.Folder
                                                },
                                                contentDescription = stringResource(R.string.categories),
                                                tint = if (uiState.showCategories) colors.accent else colors.textPrimary,
                                            )
                                        }
                                        Box {
                                            IconButton(onClick = { showDropdownMenu.value = true }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = stringResource(R.string.options_panel),
                                                    tint = colors.textPrimary,
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showDropdownMenu.value,
                                                onDismissRequest = { showDropdownMenu.value = false },
                                            ) {
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.Checklist,
                                                            stringResource(R.string.select_books),
                                                        )
                                                    },
                                                    text = { Text(stringResource(R.string.select_books)) },
                                                    onClick = {
                                                        showDropdownMenu.value = false
                                                        libraryModel.toggleSelectionMode()
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.FileOpen,
                                                            stringResource(id = R.string.import_epub),
                                                        )
                                                    },
                                                    text = { Text(stringResource(id = R.string.import_epub)) },
                                                    onClick = {
                                                        showDropdownMenu.value = false
                                                        importFileAction()
                                                    },
                                                )
                                            }
                                        }
                                    },
                                )
                                NovelaTopProgress(visible = pageViewModel.isUpdating)
                            }
                            ToolbarMode.SEARCH -> TopAppBarSearch(
                                focusRequester = remember { FocusRequester() },
                                searchTextInput = pageViewModel.searchQuery,
                                onClose = {
                                    libraryModel.setToolbarMode(ToolbarMode.MAIN)
                                    pageViewModel.updateSearchQuery("")
                                },
                                onTextDone = { },
                                placeholderText = stringResource(R.string.search_here),
                                onSearchTextChange = { pageViewModel.updateSearchQuery(it) },
                            )
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            LibraryScreenBody(
                innerPadding = innerPadding,
                topAppBarState = null,
                onBookClick = handleBookClick,
                onBookLongClick = handleBookLongClick,
                gridColumns = uiState.gridColumns,
                sourceStripPosition = uiState.sourceStripPosition,
                selectedBooks = libraryModel.selectedBooks,
                isSelectionMode = uiState.isSelectionMode,
                pendingRemoval = libraryModel.pendingRemoval.keys.toSet(),
                showCategories = uiState.showCategories,
                customCategories = uiState.customCategories,
                onCreateCategory = { showCreateCategoryDialog = true },
                onDeleteCategory = { libraryModel.removeCategory(it) },
                onImportEpub = importFileAction,
            )
        },
    )

    uiState.bookActionsSheetBook?.let { book ->
        BookActionsBottomSheet(
            book = book,
            onDismiss = { libraryModel.setBookActionsSheetBook(null) },
            onDeleteNovel = { libraryModel.deleteBook(book.url) },
            onCategorySelected = { libraryModel.updateBookCategory(book.url, it) },
            categories = libraryModel.getCategories(),
            onMarkAllChaptersRead = { libraryModel.markAllChaptersAsRead(book.url) },
            onMarkAllChaptersUnread = { libraryModel.markAllChaptersAsUnread(book.url) },
            onMigrate = {
                navigationRouteViewModel.novelMigration(
                    context = context,
                    bookUrl = book.url,
                    bookTitle = book.title,
                ).let(context::startActivity)
            },
        )
    }

    LibraryBottomSheet(
        visible = uiState.showBottomSheet,
        onDismiss = { libraryModel.setShowBottomSheet(false) },
    )

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onCreate = { name ->
                libraryModel.addCategory(name)
                showCreateCategoryDialog = false
            },
        )
    }

    if (showMoveToCategoryDialog) {
        MoveToCategoryDialog(
            categories = libraryModel.getCategories(),
            onDismiss = { showMoveToCategoryDialog = false },
            onCategorySelected = { category ->
                libraryModel.moveBooksToCategory(category)
                showMoveToCategoryDialog = false
            },
            onCreateCategory = {
                showMoveToCategoryDialog = false
                showCreateCategoryDialog = true
            },
        )
    }
}
