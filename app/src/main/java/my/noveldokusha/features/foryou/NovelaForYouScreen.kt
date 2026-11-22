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
import androidx.compose.material.icons.outlined.Update
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

        // The screenshot this was asked for has a "Highest Rated" rail, and it would be easy to
        // fake with a shuffled slice. This one is not fake: it is the same pass the grid shows,
        // ordered by the rating each source declares for the book itself, and it disappears
        // when nothing in the pass carries a score worth calling high.
        val topRated = remember(state.books) { state.books.highestRated() }
        if (topRated.size >= 2) {
            Column(modifier = Modifier.padding(top = NovelaSpacing.lg)) {
                Text(
                    text = stringResource(R.string.for_you_highest_rated),
                    style = NovelaType.sectionTitle,
                    color = palette.onBackground,
                    modifier = Modifier.padding(horizontal = NovelaSpacing.screenH),
                )
                Text(
                    text = stringResource(R.string.for_you_highest_rated_note, state.books.size),
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(
                        start = NovelaSpacing.screenH,
                        end = NovelaSpacing.screenH,
                        top = NovelaSpacing.xxs,
                    ),
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = NovelaSpacing.sm),
                    contentPadding = PaddingValues(
                        start = NovelaSpacing.screenH,
                        end = NovelaSpacing.screenH,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
                ) {
                    items(items = topRated, key = { "rated_" + it.key }) { book ->
                        ForYouRatedCell(book = book, onOpenBook = onOpenBook)
                    }
                }
            }
        }

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
                message = stringResource(
                    if (state.queriedSources > 0) R.string.for_you_no_answer_message
                    else R.string.for_you_no_answer_offline
                ),
                actionLabel = stringResource(R.string.for_you_refresh),
                onAction = onRefresh,
                modifier = Modifier.padding(horizontal = NovelaSpacing.screenH),
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = NovelaSpacing.screenH,
                    end = NovelaSpacing.screenH,
                    top = NovelaSpacing.sm,
                ),
                horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NovelaSpacing.lg),
            ) {
                items(items = state.books, key = { it.key }) { book ->
                    ForYouBookCell(book = book, onOpenBook = onOpenBook)
                }
                if (state.canLoadMore) {
                    item(key = "for_you_more", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(onClick = onLoadMore, enabled = !state.loadingMore) {
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
private fun ForYouRatedCell(
    book: ForYouBook,
    onOpenBook: (BookMetadata) -> Unit,
) {
    val cover = rememberResolvedBookImagePath(
        bookUrl = book.metadata.url,
        imagePath = book.metadata.coverImageUrl,
    )
    NovelaBookCard(
        title = book.metadata.title,
        coverModel = cover,
        subtitle = book.metadata.rating.takeIf { it.isNotBlank() }?.let { "★ " + it }
            ?: book.sourceName,
        modifier = Modifier.width(NovelaSize.carouselCoverWidth),
        onClick = { onOpenBook(book.metadata) },
    )
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
