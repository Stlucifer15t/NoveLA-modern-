package my.noveldokusha.coreui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.Response
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.domain.CloudfareVerificationBypassFailedException
import my.noveldokusha.core.domain.WebViewCookieManagerInitializationFailedException
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.composableActions.ListGridLoadWatcher
import my.noveldokusha.coreui.states.IteratorState
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.feature.local_database.BookMetadata

@Composable
fun BooksVerticalView(
    list: List<BookMetadata>,
    state: LazyGridState,
    error: Response.Error?,
    loadState: IteratorState,
    layoutMode: ListLayoutMode,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit = {},
    onCopyError: (String) -> Unit = {},
    onWebViewOpen: () -> Unit = {},
    gridColumns: Int = 3,
    sourceStripPosition: SourceStripPosition = SourceStripPosition.BelowCover,
    innerPadding: PaddingValues = PaddingValues(),
    referer: String? = null,
    translatedTitles: Map<String, String> = emptyMap(),
    coverOverrides: Map<String, String> = emptyMap(),
    topLeftBadge: (@Composable (BookMetadata) -> Unit)? = null,
) {
    val colors = NovelaTheme.colors

    val columns by remember(layoutMode, gridColumns) {
        derivedStateOf {
            when (layoutMode) {
                ListLayoutMode.VerticalList -> GridCells.Fixed(1)
                ListLayoutMode.VerticalGrid -> GridCells.Fixed(gridColumns.coerceIn(2, 6))
            }
        }
    }

    ListGridLoadWatcher(
        listState = state,
        loadState = loadState,
        onLoadNext = onLoadNext,
    )

    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding),
        contentPadding = PaddingValues(
            start = NovelaSpacing.gridHorizontal,
            end = NovelaSpacing.gridHorizontal,
            top = NovelaSpacing.sm,
            bottom = 148.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.gridGutter),
        verticalArrangement = Arrangement.spacedBy(
            if (layoutMode == ListLayoutMode.VerticalGrid) 14.dp else 10.dp
        ),
    ) {
        items(
            items = list,
            key = { it.url },
            contentType = { if (layoutMode == ListLayoutMode.VerticalList) 1 else 2 },
        ) { item ->
            val title = translatedTitles[item.url]?.takeIf { t -> t.isNotBlank() } ?: item.title
            val coverModel = rememberResolvedBookImagePath(
                bookUrl = item.url,
                imagePath = coverOverrides[item.url] ?: item.coverImageUrl,
            )

            when (layoutMode) {
                ListLayoutMode.VerticalList -> {
                    NovelaBookRow(
                        title = title,
                        coverImageModel = coverModel,
                        rating = item.rating.takeIf { r -> r.isNotBlank() },
                        subtitle = item.description.takeIf { d -> d.isNotBlank() },
                        referer = referer,
                        topLeftBadge = topLeftBadge?.let { badge -> { badge(item) } },
                        onClick = { onBookClicked(item) },
                        onLongClick = { onBookLongClicked(item) },
                    )
                }
                ListLayoutMode.VerticalGrid -> {
                    BookImageButtonView(
                        title = title,
                        coverImageModel = coverModel,
                        onClick = { onBookClicked(item) },
                        onLongClick = { onBookLongClicked(item) },
                        topRightBadge = item.rating.takeIf { r -> r.isNotBlank() }?.let { rating ->
                            { BookRatingBadge(rating = rating) }
                        },
                        topLeftBadge = topLeftBadge?.let { badge -> { badge(item) } },
                        sourceStripPosition = sourceStripPosition,
                        referer = referer,
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
            ) {
                when (loadState) {
                    IteratorState.LOADING -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            color = colors.textMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IteratorState.CONSUMED -> {
                        if (list.isEmpty() && error == null) {
                            NovelaEmptyState(
                                title = stringResource(R.string.no_results_found),
                                icon = Icons.Outlined.SearchOff,
                            )
                        } else if (list.isNotEmpty()) {
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
                    else -> Unit
                }
            }
        }

        when (error?.exception) {
            is WebViewCookieManagerInitializationFailedException -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ClickableOption(
                        title = stringResource(R.string.cloudfare_firewall_detected),
                        subtitle = stringResource(R.string.please_open_web_view_and_try_verify_then_reload),
                        onClick = onWebViewOpen,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            is CloudfareVerificationBypassFailedException -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ClickableOption(
                        title = stringResource(R.string.cloudfare_firewall_failed_bypass),
                        subtitle = stringResource(R.string.tried_but_failed_to_bypass_cloudfare_firewall),
                        onClick = onWebViewOpen,
                        modifier = Modifier.padding(4.dp),
                        isButton = true,
                    )
                }
            }
            null -> Unit
        }

        if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorView(
                    error = error.message,
                    onReload = onReload,
                    onCopyError = onCopyError,
                )
            }
        }
    }
}
