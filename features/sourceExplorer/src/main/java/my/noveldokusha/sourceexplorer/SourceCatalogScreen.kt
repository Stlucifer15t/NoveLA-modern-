package my.noveldokusha.sourceexplorer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.utils.actionCopyToClipboard
import my.noveldokusha.coreui.components.AnimatedTransition
import my.noveldokusha.coreui.components.BooksVerticalView
import my.noveldokusha.coreui.components.InLibraryBadge
import my.noveldokusha.coreui.components.LibraryBadgeMaps
import my.noveldokusha.coreui.components.LibraryBadgeState
import my.noveldokusha.coreui.components.ToolbarMode
import my.noveldokusha.coreui.components.TopAppBarSearch
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.scraper.ActiveFilters
import my.noveldokusha.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun SourceCatalogScreen(
    viewModel: SourceCatalogViewModel,
    state: SourceCatalogScreenState,
    onSearchTextInputChange: (String) -> Unit,
    onSearchTextInputSubmit: (String) -> Unit,
    onSearchCatalogSubmit: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onListLayoutModeChange: (my.noveldokusha.core.appPreferences.ListLayoutMode) -> Unit,
    onToolbarModeChange: (ToolbarMode) -> Unit,
    onOpenSourceWebPage: () -> Unit,
    onBookClicked: (BookMetadata) -> Unit,
    onBookLongClicked: (BookMetadata) -> Unit,
    onPressBack: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onApplyFilters: (ActiveFilters) -> Unit,
    getLibraryBadge: (String, String) -> LibraryBadgeState? = { _, _ -> null },
    libraryBadgeData: State<LibraryBadgeMaps> = mutableStateOf(LibraryBadgeMaps()),
) {
    val colors = NovelaTheme.colors
    val context by rememberUpdatedState(newValue = LocalContext.current)
    val focusRequester = remember { FocusRequester() }
    val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)
    val hasActiveFilters = !state.activeFilters.value.isEmpty

    // Reactive dependency: reading libraryBadgeData.value triggers recomposition when library changes.
    libraryBadgeData.value

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    AnimatedTransition(targetState = state.toolbarMode.value) { target ->
                        when (target) {
                            ToolbarMode.MAIN -> TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                                ),
                                title = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        val title = state.sourceCatalogName.value
                                            ?: if (state.sourceCatalogNameStrId.value != 0) {
                                                stringResource(id = state.sourceCatalogNameStrId.value)
                                            } else {
                                                ""
                                            }
                                        val contentTypeLabel = when (state.sourceContentType) {
                                            "manga" -> stringResource(StringsR.string.content_type_manga)
                                            "video" -> stringResource(StringsR.string.content_type_video)
                                            else -> stringResource(StringsR.string.content_type_novel)
                                        }
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(NovelaRadius.pill),
                                                color = colors.accentSoft,
                                            ) {
                                                Text(
                                                    text = contentTypeLabel,
                                                    style = NovelaType.badge,
                                                    color = colors.accent,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp),
                                                )
                                            }
                                            Text(
                                                text = stringResource(R.string.catalog),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onPressBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { onOpenSourceWebPage() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Public,
                                            contentDescription = stringResource(R.string.open_the_web_view),
                                            tint = colors.textPrimary,
                                        )
                                    }
                                    IconButton(onClick = { onToolbarModeChange(ToolbarMode.SEARCH) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = stringResource(R.string.search_for_title),
                                            tint = colors.textPrimary,
                                        )
                                    }
                                    if (state.hasFilters) {
                                        IconButton(onClick = onOpenFilterSheet) {
                                            BadgedBox(
                                                badge = {
                                                    if (hasActiveFilters) {
                                                        Badge(containerColor = colors.accent)
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.FilterList,
                                                    contentDescription = stringResource(R.string.filter),
                                                    tint = if (hasActiveFilters) colors.accent else colors.textPrimary,
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                            ToolbarMode.SEARCH -> TopAppBarSearch(
                                focusRequester = focusRequester,
                                searchTextInput = state.searchTextInput.value,
                                onClose = {
                                    focusManager.clearFocus()
                                    onToolbarModeChange(ToolbarMode.MAIN)
                                    onSearchCatalogSubmit()
                                },
                                onTextDone = {
                                    onSearchTextInputChange(it)
                                    onSearchTextInputSubmit(it)
                                },
                                placeholderText = stringResource(R.string.search_by_title),
                                onSearchTextChange = onSearchTextInputChange,
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = NovelaSize.hairline,
                        color = colors.borderSubtle,
                    )
                }
            },
            content = { innerPadding ->
                BooksVerticalView(
                    layoutMode = state.listLayoutMode.value,
                    gridColumns = state.gridColumns.value,
                    sourceStripPosition = state.sourceStripPosition.value,
                    list = state.fetchIterator.list,
                    state = rememberLazyGridState(),
                    error = state.fetchIterator.error,
                    loadState = state.fetchIterator.state,
                    onLoadNext = state.fetchIterator::fetchNext,
                    onBookClicked = onBookClicked,
                    onBookLongClicked = onBookLongClicked,
                    onReload = state.fetchIterator::reloadFailedLastLoad,
                    onCopyError = context::actionCopyToClipboard,
                    onWebViewOpen = onOpenSourceWebPage,
                    innerPadding = innerPadding,
                    translatedTitles = state.translatedTitles,
                    coverOverrides = viewModel.covers,
                    referer = viewModel.coverReferer,
                    topLeftBadge = { bookMeta ->
                        val badge = getLibraryBadge(bookMeta.url, bookMeta.title)
                        if (badge != null) {
                            InLibraryBadge(
                                inSameSource = badge.inSameSource,
                                sourceCount = badge.sourceCount,
                            )
                        }
                    },
                )
            },
        )

        if (state.isFilterSheetOpen.value && state.hasFilters) {
            FilterSheetWrapper(
                viewModel = viewModel,
                onDismiss = { state.isFilterSheetOpen.value = false },
            )
        }
    }
}
