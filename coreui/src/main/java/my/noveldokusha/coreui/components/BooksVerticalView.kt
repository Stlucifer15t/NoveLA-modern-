package my.noveldokusha.coreui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.composableActions.ListGridLoadWatcher
import my.noveldokusha.coreui.states.IteratorState
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.ScrollbarDefaults
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.network.NetworkException

@Composable
fun BooksVerticalView(
    list: List<BookMetadata>,
    state: LazyGridState,
    error: Exception?,
    loadState: IteratorState,
    layoutMode: ListLayoutMode,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit = {},
    onCopyError: (String) -> Unit = {},
    onWebViewOpen: (() -> Unit)? = null,
    cells: GridCells = GridCells.Adaptive(112.dp),
    innerPadding: PaddingValues = PaddingValues(),
    coverOverrides: Map<String, String> = emptyMap(),
    translatedTitles: Map<String, String> = emptyMap(),
    libraryBadges: LibraryBadgeMaps = LibraryBadgeMaps.Empty,
    onBookVisible: ((BookMetadata) -> Unit)? = null,
    coverReferer: String? = null,
) {
    val colors = NovelaTheme.colors
    val layoutDirection = LocalLayoutDirection.current

    val columns by remember(layoutMode, cells) {
        derivedStateOf {
            when (layoutMode) {
                ListLayoutMode.VerticalList -> GridCells.Fixed(1)
                ListLayoutMode.VerticalGrid -> cells
            }
        }
    }

    ListGridLoadWatcher(
        listState = state,
        loadState = loadState,
        onLoadNext = onLoadNext
    )

    LazyVerticalGridScrollbar(
        state = state,
        settings = ScrollbarDefaults.thumb(isScrolling = state.isScrollInProgress),
    ) {
        LazyVerticalGrid(
            columns = columns,
            state = state,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection) + NovelaSpacing.gridHorizontal,
                end = innerPadding.calculateEndPadding(layoutDirection) + NovelaSpacing.gridHorizontal,
                top = innerPadding.calculateTopPadding() + NovelaSpacing.sm,
                bottom = innerPadding.calculateBottomPadding() + 140.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.gridGutter),
            verticalArrangement = Arrangement.spacedBy(
                if (layoutMode == ListLayoutMode.VerticalGrid) 14.dp else 10.dp
            ),
        ) {
            items(
                items = list,
                key = { it.url }
            ) { item ->
                val effectiveCover = coverOverrides[item.url] ?: item.coverImageUrl
                val displayTitle = translatedTitles[item.url]?.takeIf { t -> t.isNotBlank() } ?: item.title
                val badgeState = libraryBadges.getLibraryBadgeState(item.url, item.title)

                if (onBookVisible != null) {
                    LaunchedEffect(item.url) { onBookVisible(item) }
                }

                val coverModel = rememberResolvedBookImagePath(
                    bookUrl = item.url,
                    imagePath = effectiveCover,
                    referer = coverReferer,
                )

                when (layoutMode) {
                    ListLayoutMode.VerticalGrid -> {
                        BookImageButtonView(
                            title = displayTitle,
                            coverImageModel = coverModel,
                            onClick = { onBookClicked(item) },
                            onLongClick = { onBookLongClicked(item) },
                            topStartBadge = if (badgeState != LibraryBadgeState.None) {
                                { InLibraryBadge(state = badgeState) }
                            } else null,
                            topEndBadge = item.rating.takeIf { r -> r.isNotBlank() }?.let { rating ->
                                { BookRatingBadge(rating = rating) }
                            },
                        )
                    }
                    ListLayoutMode.VerticalList -> {
                        NovelaBookRow(
                            title = displayTitle,
                            coverImageModel = coverModel,
                            rating = item.rating.takeIf { r -> r.isNotBlank() },
                            subtitle = item.description.takeIf { d -> d.isNotBlank() },
                            libraryBadgeState = badgeState,
                            onClick = { onBookClicked(item) },
                            onLongClick = { onBookLongClicked(item) },
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                ) {
                    when (loadState) {
                        IteratorState.LOADING -> CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 2.5.dp,
                            color = colors.accent,
                        )
                        IteratorState.CONSUMED -> {
                            when {
                                error != null -> {
                                    // Error block rendered below
                                }
                                list.isEmpty() -> {
                                    NovelaEmptyState(
                                        title = stringResource(R.string.no_results_found),
                                        icon = Icons.Outlined.SearchOff,
                                    )
                                }
                                else -> {
                                    Surface(
                                        shape = RoundedCornerShape(NovelaRadius.pill),
                                        color = colors.surfaceSubtle,
                                        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_more_results),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.textMuted,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }

            if (error != null) {
                if (onWebViewOpen != null && (error as? NetworkException)?.code in listOf(403, 503)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.card),
                            color = colors.accentSoft,
                            border = BorderStroke(NovelaSize.hairline, colors.accent.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                NovelaPrimaryButton(
                                    text = stringResource(R.string.Pass_Cloudflare_check_in_WebView),
                                    onClick = onWebViewOpen,
                                )
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorView(
                        error = error.message ?: error.toString(),
                        onReload = onReload,
                        onCopyError = onCopyError
                    )
                }
            }
        }
    }
}
