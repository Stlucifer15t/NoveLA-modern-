package my.noveldokusha.features.chapterslist

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
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.theme.clickableNoIndicator
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.strings.R as StringsR

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
 * Вкладка «Отзывы». Источники, с которыми работает приложение, отзывов не отдают,
 * поэтому показываем честное пустое состояние, а не выдуманные отзывы.
 */
@Composable
internal fun ChaptersReviewsContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        NovelaEmptyState(
            icon = Icons.Outlined.StarBorder,
            title = stringResource(StringsR.string.book_reviews_title),
            message = stringResource(StringsR.string.book_reviews_message),
        )
    }
}
