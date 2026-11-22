package my.noveldokusha.features.foryou

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.core.appPreferences.ListLayoutMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.foryou.NovelaGenre
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaBookCard
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaTileIconButton
import my.noveldokusha.coreui.components.novelaGreeting
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.BookMetadata

/**
 * The For You tab: a header that says what it is reading, a "because you read" card backed by
 * the history table, the three content types, and a two-column grid with a real pager.
 *
 * Everything printed here is a fact the app can hold up: the bound source, how many sources of
 * this type are installed, what the shelf and history contain. Where the reference shows a
 * "34% match" badge, this shows the source name and the reader's own rating — there is no
 * model in this codebase that could produce a percentage worth printing, and a made-up one is
 * worse than none.
 */
@Composable
fun NovelaForYouScreen(
    state: ForYouUiState,
    appPreferences: AppPreferences,
    onOpenBook: (BookMetadata) -> Unit,
    onOpenExtensions: () -> Unit,
    onSetType: (ForYouContentType) -> Unit,
    onSetBoundSource: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    var showSourcePicker by remember { mutableStateOf(false) }
    var showGenres by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = NovelaSpacing.navBarClearance),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.screenH)
                .padding(top = NovelaSpacing.md, bottom = NovelaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_for_you_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = palette.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                // The reference design opens with a greeting rather than the word "For you" —
                // the tab in the navigation pill already says what this is, so the header is
                // free to be a person, not a label.
                Text(
                    text = novelaGreeting(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.onBackground,
                )
                Text(
                    text = stringResource(R.string.for_you_subtitle),
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            NovelaTileIconButton(
                contentDescription = stringResource(R.string.for_you_genres_title),
                onClick = { showGenres = true },
                accent = palette.surfaceVariant,
            ) {
                Icon(Icons.Outlined.Tune, null, modifier = Modifier.size(18.dp))
            }
            NovelaTileIconButton(
                contentDescription = stringResource(R.string.for_you_refresh),
                onClick = onRefresh,
                accent = if (state.loading) palette.primary else palette.surfaceVariant,
            ) {
                Icon(Icons.Outlined.Update, null, modifier = Modifier.size(18.dp))
            }
        }

        // All three tabs are always present. A tab that only appears once a source of that
        // type exists rearranges the screen under the reader's thumb.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.screenH)
                .padding(bottom = NovelaSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        ) {
            ForYouContentType.entries.forEach { type ->
                NovelaFilterChip(
                    label = stringResource(type.labelRes),
                    selected = type == state.type,
                    onClick = { onSetType(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // One line that says which source this tab is reading (or that it is asking all of
        // them), with the way to change it next to it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.screenH)
                .padding(bottom = NovelaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                ForYouStatusPill(state = state)
            }
            TextButton(
                onClick = { showSourcePicker = true },
                enabled = state.sourcesOfThisType > 0 || state.boundSourceId.isNotEmpty(),
            ) {
                Text(
                    text = stringResource(R.string.for_you_change),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        ForYouReadingCard(
            reading = state.reading,
            onOpenBook = onOpenBook,
            modifier = Modifier.padding(horizontal = NovelaSpacing.screenH),
        )

        // One scroll container for the rails and the pool. Two nested ones — a rail scroller
        // above a grid that scrolls — fight over the fling, and that fight is most of what
        // makes a screen feel sticky instead of smooth.
        val topRated = remember(state.books) { state.books.highestRated() }
        val views = remember(state.rails, topRated, state.books.size) {
            buildList {
                state.rails.forEach { add(it) }
                if (topRated.size >= 2) {
                    add(ForYouRail(genre = null, books = topRated))
                }
            }
        }
        var expanded by remember { mutableStateOf<ForYouRail?>(null) }

        when {
            state.books.isEmpty() && state.loading -> ForYouPlaceholders()

            state.empty == ForYouEmpty.NoSourcesOfType -> NovelaEmptyState(
                icon = Icons.Outlined.Extension,
                title = stringResource(R.string.for_you_no_sources_title),
                message = stringResource(
                    R.string.for_you_no_sources_message,
                    stringResource(state.type.labelRes),
                ),
                actionLabel = stringResource(R.string.for_you_open_extensions),
                onAction = onOpenExtensions,
                modifier = Modifier.padding(horizontal = NovelaSpacing.screenH),
            )

            state.books.isEmpty() -> NovelaEmptyState(
                icon = Icons.Outlined.Update,
                title = stringResource(R.string.for_you_no_answer_title),
                // The failure text is appended rather than hidden: "the tab is empty and I do
                // not know why" is the complaint this whole screen exists to answer.
                message = stringResource(
                    if (state.queriedSources > 0) R.string.for_you_no_answer_message
                    else R.string.for_you_no_answer_offline
                ) + state.error?.let { "\n\n" + it }.orEmpty(),
                actionLabel = stringResource(R.string.for_you_refresh),
                onAction = onRefresh,
                modifier = Modifier.padding(horizontal = NovelaSpacing.screenH),
            )

            else -> ForYouPool(
                views = views,
                state = state,
                onOpenBook = onOpenBook,
                onLoadMore = onLoadMore,
                onExpand = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    expanded?.let { rail ->
        ForYouExpandedRail(
            rail = rail,
            state = state,
            onOpenBook = onOpenBook,
            onDismiss = { expanded = null },
        )
    }

    if (showSourcePicker) {
        ForYouSourcePickerDialog(
            state = state,
            onPick = { id ->
                onSetBoundSource(id)
                showSourcePicker = false
            },
            onDismiss = { showSourcePicker = false },
        )
    }
    if (showGenres) {
        ForYouGenresDialog(
            appPreferences = appPreferences,
            onDismiss = { showGenres = false },
        )
    }
}

@Composable
private fun ForYouStatusPill(state: ForYouUiState) {
    val palette = MaterialTheme.colorScheme
    val bound = state.boundSourceName
    val text = when {
        state.provision.state == DefaultSourceState.Working ->
            stringResource(R.string.for_you_provision_working)

        bound != null ->
            stringResource(R.string.for_you_status_bound, bound)

        state.usingFallback && state.queriedSources > 0 ->
            stringResource(R.string.for_you_status_fanout, state.queriedSources)

        state.provision.state == DefaultSourceState.NotFound ->
            stringResource(R.string.for_you_provision_not_found)

        state.provision.state == DefaultSourceState.Failed ->
            stringResource(R.string.for_you_provision_failed, state.provision.detail)

        state.loading -> stringResource(R.string.for_you_loading)
        else -> stringResource(R.string.for_you_status_getting_started)
    }
    Surface(
        shape = NovelaRadius.chipShape,
        color = palette.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovelaSpacing.md, vertical = NovelaSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.loading || state.provision.state == DefaultSourceState.Working) {
                            palette.primary
                        } else {
                            palette.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
            )
            Text(
                text = text,
                style = NovelaType.bookMeta,
                color = palette.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The top card: the last book this device has an open chapter for, with progress taken straight
 * from the history table. With no history there is nothing to recommend from, so the card says
 * what will happen instead of showing a placeholder book.
 */
@Composable
private fun ForYouReadingCard(
    reading: ForYouReading?,
    onOpenBook: (BookMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = NovelaRadius.cardShape,
        color = palette.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovelaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        ) {
            if (reading == null) {
                Text(
                    text = stringResource(R.string.for_you_first_run_card),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant,
                )
                return@Surface
            }
            val cover = rememberResolvedBookImagePath(
                bookUrl = reading.bookUrl,
                imagePath = reading.coverUrl,
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(NovelaSize.COVER_ASPECT)
                    .clip(RoundedCornerShape(NovelaRadius.cover))
                    .clickable {
                        onOpenBook(
                            BookMetadata(
                                title = reading.title,
                                url = reading.bookUrl,
                                coverImageUrl = reading.coverUrl,
                            )
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                ImageView(
                    imageModel = cover,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onOpenBook(
                            BookMetadata(
                                title = reading.title,
                                url = reading.bookUrl,
                                coverImageUrl = reading.coverUrl,
                            )
                        )
                    },
            ) {
                Text(
                    text = stringResource(R.string.for_you_because_you_read),
                    style = NovelaType.bookMeta,
                    color = palette.primary,
                )
                Spacer(modifier = Modifier.height(NovelaSpacing.xxs))
                Text(
                    text = reading.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(NovelaSpacing.xxs))
                Text(
                    text = if (reading.chaptersTotal > 0) {
                        stringResource(
                            R.string.for_you_percent_read,
                            reading.percentRead,
                            reading.chaptersRead,
                            reading.chaptersTotal,
                        )
                    } else {
                        stringResource(R.string.for_you_not_started)
                    },
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ForYouBookCell(
    book: ForYouBook,
    onOpenBook: (BookMetadata) -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val cover = rememberResolvedBookImagePath(
        bookUrl = book.metadata.url,
        imagePath = book.metadata.coverImageUrl,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        NovelaBookCard(
            title = book.metadata.title,
            coverModel = cover,
            subtitle = buildString {
                val rating = book.metadata.rating
                if (rating.isNotBlank()) {
                    append('★').append(' ').append(rating).append(" · ")
                }
                append(
                    if (book.inLibrary) stringResource(R.string.for_you_in_library)
                    else book.sourceName
                )
            },
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpenBook(book.metadata) },
        )
        book.matchedHistoryTitle?.let { from ->
            Spacer(modifier = Modifier.height(NovelaSpacing.xxs))
            Text(
                text = stringResource(R.string.for_you_because_you_read_short, from),
                style = NovelaType.bookMeta,
                color = palette.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A rail cell: the cover, the title, and the rating exactly as the source wrote it — not a
 * percentage, which is the number the reference shows and the one this app cannot compute.
 */
@Composable
private fun ForYouRailCell(
    book: ForYouBook,
    showScore: Boolean,
    onOpenBook: (BookMetadata) -> Unit,
) {
    val cover = rememberResolvedBookImagePath(
        bookUrl = book.metadata.url,
        imagePath = book.metadata.coverImageUrl,
    )
    val subtitle = if (showScore) {
        book.metadata.rating.takeIf { it.isNotBlank() }?.let { "★ " + it } ?: book.sourceName
    } else if (book.inLibrary) {
        stringResource(R.string.for_you_in_library)
    } else {
        book.sourceName
    }
    NovelaBookCard(
        title = book.metadata.title,
        coverModel = cover,
        subtitle = subtitle,
        modifier = Modifier.width(NovelaSize.carouselCoverWidth),
        onClick = { onOpenBook(book.metadata) },
    )
}

/** A pool row, for the Layout setting's list mode: the same facts, in a line. */
@Composable
private fun ForYouListRow(
    book: ForYouBook,
    onOpenBook: (BookMetadata) -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val cover = rememberResolvedBookImagePath(
        bookUrl = book.metadata.url,
        imagePath = book.metadata.coverImageUrl,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.md))
            .clickable { onOpenBook(book.metadata) }
            .padding(vertical = NovelaSpacing.xs, horizontal = NovelaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .aspectRatio(NovelaSize.COVER_ASPECT)
                .clip(RoundedCornerShape(NovelaRadius.cover)),
        ) {
            ImageView(
                imageModel = cover,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.metadata.title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    val rating = book.metadata.rating
                    if (rating.isNotBlank()) append("★ ").append(rating).append(" · ")
                    append(if (book.inLibrary) stringResource(R.string.for_you_in_library) else book.sourceName)
                },
                style = NovelaType.bookMeta,
                color = palette.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ForYouPlaceholders() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp),
        contentPadding = PaddingValues(
            start = NovelaSpacing.screenH,
            end = NovelaSpacing.screenH,
            top = NovelaSpacing.sm,
        ),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NovelaSpacing.lg),
        userScrollEnabled = false,
    ) {
        items(count = 6) { index ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(NovelaSize.COVER_ASPECT)
                        .clip(RoundedCornerShape(NovelaRadius.cover))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha(index))
                        )
                )
                Spacer(modifier = Modifier.height(NovelaSpacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(NovelaRadius.xs))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha(index))
                        )
                )
            }
        }
    }
}

@Composable
private fun shimmerAlpha(index: Int): Float {
    val transition = rememberInfiniteTransition(label = "forYouShimmer")
    return transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 180 + index * 160, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "forYouShimmerAlpha",
    ).value
}

/**
 * The tab's content: the sideways rails, then everything the pass returned in a grid (or a
 * list, if the Layout setting says so) whose column count is the Layout setting's too.
 *
 * Rails are full-span items of the same scroller rather than their own scrolling column.
 * Nested vertical scrollers steal the fling from each other, which is exactly the "not smooth"
 * the previous version was criticized for; a full-span header inside the grid keeps one
 * gesture for the whole tab and still lets each rail swipe sideways.
 */
@Composable
private fun ForYouPool(
    views: List<ForYouRail>,
    state: ForYouUiState,
    onOpenBook: (BookMetadata) -> Unit,
    onLoadMore: () -> Unit,
    onExpand: (ForYouRail) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    val isList = state.layoutMode == ListLayoutMode.VerticalList
    LazyVerticalGrid(
        columns = forYouColumns(state),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = NovelaSpacing.screenH,
            end = NovelaSpacing.screenH,
            top = NovelaSpacing.sm,
            // The navigation pill floats over this tab, so the last row needs room of its own
            // or the bottom of the grid is permanently unreadable.
            bottom = NovelaSpacing.navBarClearance + NovelaSpacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NovelaSpacing.lg),
    ) {
        items(
            items = views,
            key = { "rail_" + (it.genre?.slug ?: "rated") },
            span = { GridItemSpan(maxLineSpan) },
        ) { rail ->
            ForYouRailBlock(
                rail = rail,
                totalBooks = state.books.size,
                onOpenBook = onOpenBook,
                onExpand = { onExpand(rail) },
            )
        }

        if (views.isNotEmpty() && state.pool.isNotEmpty()) {
            item(key = "pool_title", span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = stringResource(R.string.for_you_pool_title),
                        style = NovelaType.sectionTitle,
                        color = palette.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.for_you_pool_note, state.books.size),
                        style = NovelaType.bookMeta,
                        color = palette.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = NovelaSpacing.xxs),
                    )
                }
            }
        }

        items(items = state.pool, key = { it.key }, span = {
            if (isList) GridItemSpan(maxLineSpan) else GridItemSpan(1)
        }) { book ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            ) {
                if (isList) {
                    ForYouListRow(book = book, onOpenBook = onOpenBook)
                } else {
                    ForYouBookCell(book = book, onOpenBook = onOpenBook)
                }
            }
        }

        if (state.canLoadMore) {
            item(key = "for_you_more", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = onLoadMore,
                        enabled = !state.loadingMore,
                        modifier = Modifier.padding(bottom = NovelaSpacing.md),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.loadingMore) R.string.for_you_loading_more
                                else R.string.for_you_load_more
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** The grid's column spec, from the Settings → Layout preferences. 0 columns means "auto". */
@Composable
private fun forYouColumns(state: ForYouUiState): GridCells = when {
    state.layoutMode == ListLayoutMode.VerticalList -> GridCells.Fixed(1)
    state.gridColumns <= 0 -> GridCells.Adaptive(minSize = 112.dp)
    else -> GridCells.Fixed(state.gridColumns.coerceIn(2, 6))
}

/**
 * One category, sideways. The title is the genre the reader picked, and every cover under it
 * is a book that genre's query actually returned — the rail is a list of answers, not a
 * decoration over a shuffled pool. [totalBooks] is in the note so the count stays checkable.
 */
@Composable
private fun ForYouRailBlock(
    rail: ForYouRail,
    totalBooks: Int,
    onOpenBook: (BookMetadata) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    val genre = rail.genre
    val accent = railAccent(genre)
    val title = genre?.let { stringResource(it.labelRes) }
        ?: stringResource(R.string.for_you_highest_rated)
    val note = if (genre == null) {
        stringResource(R.string.for_you_highest_rated_note, totalBooks)
    } else {
        stringResource(R.string.for_you_rail_note, rail.books.size)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        ) {
            // A tinted chip rather than an icon per genre: there are no forty genre glyphs in
            // this app, and a wrong icon reads as broken faster than a neutral one.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(NovelaRadius.sm))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.take(1).ifEmpty { "★" },
                    style = NovelaType.badge,
                    color = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = NovelaType.sectionTitle,
                    color = palette.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = note,
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NovelaTileIconButton(
                contentDescription = stringResource(R.string.for_you_rail_expand, title),
                onClick = onExpand,
                accent = palette.surfaceVariant,
            ) {
                Icon(Icons.Outlined.Fullscreen, null, modifier = Modifier.size(18.dp))
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = NovelaSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        ) {
            items(items = rail.books, key = { "rail_cell_" + it.key }) { book ->
                Box(modifier = Modifier.animateItem()) {
                    ForYouRailCell(
                        book = book,
                        showScore = genre == null,
                        onOpenBook = onOpenBook,
                    )
                }
            }
        }
    }
}

/**
 * The rail at full screen. Expansion is what makes a sideways rail honest: the row shows five
 * covers and hides the rest, so the button has to lead somewhere that shows all of them.
 */
@Composable
private fun ForYouExpandedRail(
    rail: ForYouRail,
    state: ForYouUiState,
    onOpenBook: (BookMetadata) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val title = rail.genre?.let { stringResource(it.labelRes) }
        ?: stringResource(R.string.for_you_highest_rated)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = NovelaSpacing.screenH,
                            end = NovelaSpacing.xs,
                            top = NovelaSpacing.md,
                            bottom = NovelaSpacing.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.for_you_rail_items, rail.books.size),
                            style = NovelaType.bookMeta,
                            color = palette.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close))
                    }
                }
                LazyVerticalGrid(
                    // The reader's column choice, honored here too — except that a full screen
                    // of one column is a list, and the list mode has its own row rendering.
                    columns = if (state.gridColumns <= 0) GridCells.Adaptive(minSize = 112.dp)
                    else GridCells.Fixed(state.gridColumns.coerceIn(2, 6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = NovelaSpacing.screenH,
                        end = NovelaSpacing.screenH,
                        top = NovelaSpacing.xs,
                        bottom = NovelaSpacing.lg,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NovelaSpacing.lg),
                ) {
                    items(items = rail.books, key = { "expanded_" + it.key }) { book ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                        ) {
                            ForYouBookCell(book = book, onOpenBook = onOpenBook)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rail colors. A fixed set rather than a random pick: the same genre has to come back the same
 * color on every open, or the tabs feel like they are reshuffling underneath the reader.
 */
private val RAIL_ACCENTS = listOf(
    Color(0xFF7C4DFF),
    Color(0xFF00BFA5),
    Color(0xFFFF6D00),
    Color(0xFF2979FF),
    Color(0xFFD81B60),
    Color(0xFF43A047),
    Color(0xFFF9A825),
    Color(0xFF8E24AA),
)

@Composable
private fun railAccent(genre: NovelaGenre?): Color =
    if (genre == null) MaterialTheme.colorScheme.primary
    else RAIL_ACCENTS[genre.ordinal % RAIL_ACCENTS.size]

/**
 * Which source this tab reads. "Auto" is offered first and is what a fresh install runs on, so
 * binding a source stays a choice rather than a prerequisite for the screen to work.
 */
@Composable
private fun ForYouSourcePickerDialog(
    state: ForYouUiState,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.for_you_change_source)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(
                        R.string.for_you_change_source_hint,
                        stringResource(state.type.labelRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NovelaSpacing.md))
                ForYouPickerRow(
                    label = stringResource(R.string.for_you_bound_auto),
                    selected = state.boundSourceName == null,
                    onClick = { onPick("") },
                )
                state.sources.forEach { option ->
                    ForYouPickerRow(
                        label = option.name,
                        selected = option.id == state.boundSourceId,
                        onClick = { onPick(option.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ForYouPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.sm))
            .clickable(onClick = onClick)
            .padding(vertical = NovelaSpacing.sm, horizontal = NovelaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) palette.primary else palette.outline.copy(alpha = 0.3f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) palette.onSurface else palette.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Genre picking after first run — the same preferences the first-open sheet writes. */
@Composable
private fun ForYouGenresDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val saved = appPreferences.FOR_YOU_GENRES.value
    var selected by remember { mutableStateOf(saved) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.for_you_genres_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.for_you_genres_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NovelaGenre.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm)) {
                        row.forEach { genre ->
                            NovelaFilterChip(
                                label = stringResource(genre.labelRes),
                                selected = genre.slug in selected,
                                onClick = {
                                    selected = if (genre.slug in selected) {
                                        selected - genre.slug
                                    } else {
                                        selected + genre.slug
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    appPreferences.FOR_YOU_GENRES.value = selected
                    appPreferences.FOR_YOU_GENRES_PICKED.value = true
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.for_you_genres_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
