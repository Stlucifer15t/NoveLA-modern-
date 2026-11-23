package my.dreamora.features.feeds

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.dreamora.R
import my.dreamora.core.rememberResolvedBookImagePath
import my.dreamora.coreui.components.ImageView
import my.dreamora.coreui.components.NovelaEmptyState
import my.dreamora.coreui.components.NovelaFilterChip
import my.dreamora.coreui.components.NovelaTileIconButton
import my.dreamora.coreui.theme.NovelaMotion
import my.dreamora.coreui.theme.NovelaRadius
import my.dreamora.coreui.theme.NovelaSize
import my.dreamora.coreui.theme.NovelaSpacing
import my.dreamora.coreui.theme.NovelaType
import my.dreamora.feature.local_database.BookMetadata

/**
 * The feeds tab: shelf titles that have chapters this device has not opened, most-new-first.
 *
 * The honest version of a "notifications" screen for this app. There is no push service and no
 * background poller of its own, so nothing here claims to have arrived at a time it cannot
 * prove; the count is the shelf's own unread total and the button is the library update worker
 * that actually asks the sources.
 */
@Composable
fun NovelaFeedsScreen(
    state: FeedsState,
    onOpenBook: (BookMetadata) -> Unit,
    onCheckForUpdates: () -> Unit,
    onCancelUpdate: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSetFilter: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    val rows = state.visible()

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
                    Icons.Outlined.LibraryBooks,
                    null,
                    modifier = Modifier.size(22.dp),
                    tint = palette.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.feeds_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.onBackground,
                )
                Text(
                    text = stringResource(R.string.feeds_subtitle),
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NovelaTileIconButton(
                contentDescription = stringResource(R.string.feeds_check),
                onClick = if (state.checking) onCancelUpdate else onCheckForUpdates,
                accent = if (state.checking) palette.primary else palette.surfaceVariant,
            ) {
                Icon(Icons.Outlined.Update, null, modifier = Modifier.size(18.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.screenH)
                .padding(bottom = NovelaSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        ) {
            FeedFilter.entries.forEach { filter ->
                NovelaFilterChip(
                    label = stringResource(
                        when (filter) {
                            FeedFilter.All -> R.string.feeds_filter_all
                            FeedFilter.Books -> R.string.stats_books
                            FeedFilter.Episodes -> R.string.stats_movies
                        }
                    ),
                    selected = filter == state.filter,
                    onClick = { onSetFilter(filter) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.checking) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovelaSpacing.screenH),
                shape = NovelaRadius.cardShape,
                color = palette.primary.copy(alpha = 0.10f),
            ) {
                Text(
                    text = stringResource(R.string.feeds_checking_now),
                    style = NovelaType.bookMeta,
                    color = palette.primary,
                    modifier = Modifier.padding(horizontal = NovelaSpacing.md, vertical = NovelaSpacing.xs),
                )
            }
        }

        if (rows.isEmpty()) {
            // Two different nothings: no releases at all (the feed is simply quiet, and an empty
            // shelf can be filled), and releases that exist but not of the kind the segment asks
            // for. Only the first one has somewhere useful to send the reader.
            val nothingAtAll = state.rows.isEmpty()
            NovelaEmptyState(
                icon = Icons.Outlined.LibraryBooks,
                title = stringResource(
                    if (nothingAtAll) R.string.feeds_empty_title else R.string.feeds_empty_filter_title
                ),
                message = stringResource(R.string.feeds_empty_message),
                actionLabel = if (state.shelfCount == 0) stringResource(R.string.feeds_open_library) else null,
                onAction = if (state.shelfCount == 0) onOpenLibrary else null,
                modifier = Modifier.padding(horizontal = NovelaSpacing.screenH),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(
                start = NovelaSpacing.screenH,
                end = NovelaSpacing.screenH,
                top = NovelaSpacing.xs,
                bottom = NovelaSpacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
        ) {
            items(items = rows, key = { it.bookUrl }) { row ->
                FeedsRow(
                    row = row,
                    onClick = {
                        onOpenBook(
                            BookMetadata(
                                title = row.title,
                                url = row.bookUrl,
                                coverImageUrl = row.coverUrl,
                            )
                        )
                    },
                )
            }
            item(key = "feeds_note") {
                Text(
                    text = stringResource(R.string.feeds_note),
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = NovelaSpacing.md),
                )
            }
        }
    }
}

@Composable
private fun FeedsRow(
    row: FeedRow,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val cover = rememberResolvedBookImagePath(bookUrl = row.bookUrl, imagePath = row.coverUrl)
    val latest = row.latest
    val foundAgo = remember(latest?.foundAtMillis) {
        latest?.let {
            android.text.format.DateUtils.getRelativeTimeSpanString(
                it.foundAtMillis,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS,
                android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.md))
            .clickable(onClick = onClick)
            .padding(vertical = NovelaSpacing.xs, horizontal = NovelaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
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
                text = row.title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (row.unread) {
                    1 -> stringResource(
                        if (row.isVideo) R.string.feeds_new_episode else R.string.feeds_new_chapter
                    )
                    else -> stringResource(
                        if (row.isVideo) R.string.feeds_new_episodes else R.string.feeds_new_chapters,
                        row.unread,
                    )
                },
                style = NovelaType.bookMeta,
                color = palette.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            latest?.chapterTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    style = NovelaType.bookMeta,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            foundAgo?.let {
                Text(
                    text = it,
                    style = NovelaType.badge,
                    color = palette.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            null,
            modifier = Modifier.size(18.dp),
            tint = palette.primary,
        )
    }
}
