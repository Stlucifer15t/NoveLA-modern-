package my.dreamora.features.chapterslist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.components.ImageView
import my.dreamora.coreui.components.NovelaEmptyState
import my.dreamora.coreui.theme.clickableNoIndicator
import my.dreamora.feature.local_database.BookMetadata
import my.dreamora.scraper.Scraper
import my.dreamora.strings.R as StringsR
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import my.dreamora.coreui.components.ExpandableText
import my.dreamora.scraper.domain.BookReview
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

/** Нижняя панель вкладок книги: Главы / Похожие / Отзывы. */
@Composable
internal fun ChaptersTabBar(
    selected: ChaptersTab,
    chapterCount: Int,
    onSelect: (ChaptersTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        ChaptersTab.entries.forEach { tab ->
            val label = when (tab) {
                ChaptersTab.Chapters -> stringResource(StringsR.string.book_tab_chapters)
                ChaptersTab.Related -> stringResource(StringsR.string.book_tab_related)
                ChaptersTab.Reviews -> stringResource(StringsR.string.book_tab_reviews)
            }
            val icon = when (tab) {
                ChaptersTab.Chapters -> Icons.AutoMirrored.Outlined.List
                ChaptersTab.Related -> Icons.Outlined.Explore
                ChaptersTab.Reviews -> Icons.Outlined.StarBorder
            }
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = {
                    BadgedBox(
                        badge = {
                            // Счётчик только у глав: это реальное число из базы.
                            if (tab == ChaptersTab.Chapters && chapterCount > 0) {
                                Badge { Text(text = chapterCount.toString()) }
                            }
                        }
                    ) {
                        Icon(imageVector = icon, contentDescription = null)
                    }
                },
                label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** Вкладка «Похожие»: результат поиска в источнике по жанру книги. */
@Composable
internal fun ChaptersRelatedContent(
    state: RelatedBooksState,
    scraper: Scraper,
    onRetry: () -> Unit,
    onLoad: () -> Unit,
    onBookClick: (BookMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Загрузка ленивая: первый показ вкладки запускает поиск, повторно не грузим.
    LaunchedEffect(Unit) { onLoad() }

    val title = stringResource(StringsR.string.book_tab_related)
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        when (state) {
            RelatedBooksState.Idle, RelatedBooksState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            RelatedBooksState.NoGenre -> NovelaEmptyState(
                icon = Icons.Outlined.Explore,
                title = title,
                message = stringResource(StringsR.string.book_related_no_genre),
            )

            RelatedBooksState.Failed -> NovelaEmptyState(
                icon = Icons.Outlined.Explore,
                title = title,
                message = stringResource(StringsR.string.book_related_failed),
                actionLabel = stringResource(StringsR.string.book_related_retry),
                onAction = onRetry,
            )

            is RelatedBooksState.Loaded -> Column {
                Text(
                    text = stringResource(
                        StringsR.string.book_related_query,
                        state.query,
                        state.sourceName,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (state.books.isEmpty()) {
                    Text(
                        text = stringResource(StringsR.string.book_related_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.books.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { book ->
                                RelatedBookCell(
                                    book = book,
                                    scraper = scraper,
                                    onClick = { onBookClick(book) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedBookCell(
    book: BookMetadata,
    scraper: Scraper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickableNoIndicator(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            ImageView(
                imageModel = book.coverImageUrl.ifBlank { null },
                referer = scraper.coverReferer(book.url),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (book.rating.isNotBlank()) {
            Text(
                text = book.rating,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Вкладка «Отзывы»: отзывы из скрипта источника (функция getBookReviews).
 * Без поддержки — честное «недоступно», без отзывов — «пока нет». Выдуманных отзывов нет.
 */
@Composable
internal fun ChaptersReviewsContent(
    state: BookReviewsUiState,
    onLoad: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onLoad() }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        when (state) {
            BookReviewsUiState.Idle, BookReviewsUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            BookReviewsUiState.Unsupported -> NovelaEmptyState(
                icon = Icons.Outlined.StarBorder,
                title = stringResource(StringsR.string.book_reviews_unsupported_title),
                message = stringResource(StringsR.string.book_reviews_unsupported_message),
            )

            BookReviewsUiState.Failed -> NovelaEmptyState(
                icon = Icons.Outlined.StarBorder,
                title = stringResource(StringsR.string.book_tab_reviews),
                message = stringResource(StringsR.string.book_reviews_failed),
                actionLabel = stringResource(StringsR.string.book_reviews_retry),
                onAction = onLoad,
            )

            is BookReviewsUiState.Loaded -> if (state.reviews.isEmpty()) {
                NovelaEmptyState(
                    icon = Icons.Outlined.StarBorder,
                    title = stringResource(StringsR.string.book_reviews_empty_title),
                    message = stringResource(StringsR.string.book_reviews_empty_message),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.reviews.forEach { review -> ReviewCard(review = review) }
                    ReviewsFooter(state = state, onLoadMore = onLoadMore)
                }
            }
        }
    }
}

@Composable
private fun ReviewsFooter(state: BookReviewsUiState.Loaded, onLoadMore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.loadMoreFailed) {
            Text(
                text = stringResource(StringsR.string.book_reviews_load_more_failed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (state.isLoadingMore) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else if (state.hasNext) {
            FilledTonalButton(onClick = onLoadMore) {
                Text(
                    text = stringResource(
                        if (state.loadMoreFailed) StringsR.string.book_reviews_retry
                        else StringsR.string.book_reviews_load_more
                    )
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: BookReview, modifier: Modifier = Modifier) {
    var revealed by rememberSaveable(review.id, review.content) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = review.author.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.author.ifBlank { "—" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    review.time?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (review.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(StringsR.string.book_review_pinned),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            review.rating?.let { rating ->
                Spacer(Modifier.height(8.dp))
                ReviewRatingRow(rating = rating)
            }

            Spacer(Modifier.height(10.dp))

            if (review.isSpoiler && !revealed) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoIndicator { revealed = true },
                ) {
                    Text(
                        text = stringResource(StringsR.string.book_review_spoiler_hidden),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                ExpandableText(text = review.content, linesForExpand = 6)
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = review.likes.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Ответы показываем числом: открыть ветку пока нельзя, поэтому не кнопка.
                if (review.replyCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(StringsR.string.book_review_replies, review.replyCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (review.isEdited) {
                    Text(
                        text = stringResource(StringsR.string.book_review_edited),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRatingRow(rating: Float) {
    val filled = rating.roundToInt().coerceIn(0, 5)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
