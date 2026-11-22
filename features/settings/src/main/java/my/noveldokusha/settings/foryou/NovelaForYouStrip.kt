package my.noveldokusha.settings.foryou

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaBookCard
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.settings.R

/**
 * The "For you" rail that sits on top of Home. Horizontal on purpose: it is a glanceable
 * strip above the shelf, not a second list to scroll, and a vertical rail here would push
 * the library off the first screen.
 */
@Composable
fun NovelaForYouStrip(
    state: ForYouState,
    onBookClick: (BookMetadata) -> Unit,
    onChangeGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.screenH),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.for_you_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val genres = state.genres
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = genres.joinToString(" · ") { stringResource(it.labelRes) },
                    style = NovelaType.bookMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onChangeGenres) {
                Text(
                    text = stringResource(
                        if (genres.isEmpty()) R.string.for_you_pick_genres else R.string.for_you_change
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        when {
            state.books.isNotEmpty() -> LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = NovelaSpacing.screenH,
                    end = NovelaSpacing.screenH,
                    bottom = NovelaSpacing.sm,
                ),
                horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
            ) {
                items(items = state.books, key = { it.key }) { book ->
                    val cover = rememberResolvedBookImagePath(
                        bookUrl = book.metadata.url,
                        imagePath = book.metadata.coverImageUrl,
                    )
                    NovelaBookCard(
                        title = book.metadata.title,
                        coverModel = cover,
                        subtitle = if (book.inLibrary) {
                            stringResource(R.string.for_you_in_library)
                        } else {
                            book.metadata.rating.takeIf { it.isNotBlank() } ?: book.sourceName
                        },
                        modifier = Modifier.width(118.dp),
                        onClick = { onBookClick(book.metadata) },
                    )
                }
            }

            state.loading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(206.dp)
                    .padding(horizontal = NovelaSpacing.screenH, vertical = NovelaSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
            ) {
                repeat(3) { PlaceholderCard(it) }
            }

            else -> Text(
                text = stringResource(
                    when (state.notice) {
                        ForYouNotice.NoSources -> R.string.for_you_no_sources
                        else -> R.string.for_you_no_answers
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovelaSpacing.screenH)
                    .padding(bottom = NovelaSpacing.md),
            )
        }
    }
}

/**
 * A breathing placeholder. Small and local on purpose: the app has no shared skeleton
 * component, and a rail is not worth inventing one for.
 */
@Composable
private fun PlaceholderCard(index: Int) {
    val transition = rememberInfiniteTransition(label = "forYouPlaceholder")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                delayMillis = 180 + index * 160,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "forYouPlaceholderAlpha",
    )
    Box(
        modifier = Modifier
            .width(118.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(NovelaRadius.cover))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}
