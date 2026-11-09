package my.noveldokusha.globalsourcesearch

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.Response
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.components.BookImageButtonView
import my.noveldokusha.coreui.components.InLibraryBadge
import my.noveldokusha.coreui.components.LibraryBadgeMaps
import my.noveldokusha.coreui.components.LibraryBadgeState
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.composableActions.ListLoadWatcher
import my.noveldokusha.coreui.states.IteratorState
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.data.CatalogItem
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.mappers.mapToBookMetadata
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.fixtures.fixturesCatalogList

@Composable
internal fun GlobalSourceSearchScreenBody(
    listSources: List<SourceResults>,
    contentPadding: PaddingValues,
    onBookClick: (book: BookMetadata) -> Unit,
    getLibraryBadge: (String, String) -> LibraryBadgeState? = { _, _ -> null },
    libraryBadgeData: State<LibraryBadgeMaps> = mutableStateOf(LibraryBadgeMaps()),
) {
    // Reactive dependency: reading libraryBadgeData.value triggers recomposition when library changes.
    libraryBadgeData.value
    val colors = NovelaTheme.colors

    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(top = 12.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(items = listSources, key = { it.source.catalog.id }) { entry ->
            val sourceTitle = entry.source.catalog.name
                ?: if (entry.source.catalog.nameStrId != 0) {
                    stringResource(id = entry.source.catalog.nameStrId)
                } else {
                    "Unknown"
                }
            val resultCount = entry.fetchIterator.list.size

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovelaSectionHeader(
                        title = sourceTitle,
                        badgeText = resultCount.takeIf { it > 0 }?.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    if (entry.fetchIterator.state == IteratorState.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = NovelaSpacing.lg)
                                .size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                SourceListView(
                    list = entry.fetchIterator.list,
                    loadState = entry.fetchIterator.state,
                    error = entry.fetchIterator.error?.message,
                    onBookClick = onBookClick,
                    onLoadNext = { entry.fetchIterator.fetchNext() },
                    getLibraryBadge = getLibraryBadge,
                )
            }
        }
    }
}

@Composable
private fun SourceListView(
    list: List<BookResult>,
    loadState: IteratorState,
    error: String?,
    onBookClick: (book: BookMetadata) -> Unit,
    onLoadNext: () -> Unit,
    modifier: Modifier = Modifier,
    getLibraryBadge: (String, String) -> LibraryBadgeState? = { _, _ -> null },
) {
    val colors = NovelaTheme.colors
    val state = rememberLazyListState()

    ListLoadWatcher(listState = state, loadState = loadState, onLoadNext = onLoadNext)

    LazyRow(
        state = state,
        contentPadding = PaddingValues(
            start = NovelaSpacing.lg,
            end = NovelaSpacing.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth(),
    ) {
        items(items = list, key = { it.url }) { book ->
            val badge = getLibraryBadge(book.url, book.title)
            BookImageButtonView(
                title = book.title,
                coverImageModel = rememberResolvedBookImagePath(
                    bookUrl = book.url,
                    imagePath = book.coverImageUrl,
                ),
                onClick = { onBookClick(book.mapToBookMetadata()) },
                onLongClick = { },
                modifier = Modifier.width(120.dp),
                topLeftBadge = if (badge != null) {
                    { InLibraryBadge(inSameSource = badge.inSameSource, sourceCount = badge.sourceCount) }
                } else null,
            )
        }

        item {
            fun Modifier.topPadding() = padding(top = (120 / 1.45f).dp - 8.dp)

            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                when (loadState) {
                    IteratorState.LOADING -> if (list.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceSubtle,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accent,
                                )
                                Text(
                                    text = "...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textMuted,
                                )
                            }
                        }
                    }

                    IteratorState.CONSUMED -> when {
                        error != null -> Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.danger.copy(alpha = 0.10f),
                            modifier = if (list.isEmpty()) Modifier else Modifier.topPadding(),
                        ) {
                            Text(
                                text = stringResource(R.string.error_loading),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.danger,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }

                        list.isEmpty() -> Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceSubtle,
                        ) {
                            Text(
                                text = stringResource(R.string.no_results_found),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }

                        else -> Text(
                            text = stringResource(R.string.no_more_results),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            modifier = Modifier.topPadding(),
                        )
                    }

                    IteratorState.IDLE -> Unit
                }
            }
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {
    val list = fixturesCatalogList().mapIndexed { index, source ->
        val (catalog, books) = CatalogItem(
            catalog = source,
            pinned = false,
        ) to (0..5).map {
            BookResult(
                title = "Book $it",
                url = "url_$it",
            )
        }

        val sr = SourceResults(catalog, "", rememberCoroutineScope())
        when (index) {
            0 -> sr.fetchIterator.state = IteratorState.CONSUMED
            1 -> {
                sr.fetchIterator.list.addAll(books.take(2))
                sr.fetchIterator.state = IteratorState.CONSUMED
            }
            2 -> sr.fetchIterator.error = Response.Error("Error here", Exception())
            else -> {
                sr.fetchIterator.list.addAll(books.take(2))
                sr.fetchIterator.state = IteratorState.CONSUMED
                sr.fetchIterator.error = Response.Error("Error here", Exception())
            }
        }
        sr
    }

    InternalTheme {
        GlobalSourceSearchScreenBody(
            listSources = list,
            onBookClick = {},
            contentPadding = PaddingValues(),
        )
    }
}
