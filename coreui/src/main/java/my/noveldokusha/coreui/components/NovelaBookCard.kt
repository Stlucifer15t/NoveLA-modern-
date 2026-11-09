package my.noveldokusha.coreui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

/**
 * The redesigned book card.
 *
 * Layout (top to bottom):
 *  - cover, 1:1.45, 8dp corners, hairline border, subtle press scale
 *  - optional badges floating on the cover (unread count, "new", selection tick)
 *  - bold 2-line title
 *  - optional muted metadata line (source / chapter progress)
 *
 * This is additive: [BookImageButtonView] is untouched, so existing call sites
 * keep working while screens migrate to this one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelaBookCard(
    title: String,
    coverModel: Any?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    unreadCount: Int? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = NovelaRadius.cover,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(NovelaMotion.INSTANT),
        label = "cardPress",
    )

    Column(
        modifier = modifier
            .widthIn(min = 96.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(NovelaSize.COVER_ASPECT)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 0.5.dp,
                    color = NovelaTheme.colors.hairline,
                    shape = RoundedCornerShape(cornerRadius),
                ),
        ) {
            ImageView(
                imageModel = coverModel,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = R.drawable.default_book_cover,
            )

            // Bottom scrim: keeps a floating unread pill readable on busy art.
            if (unreadCount != null && unreadCount > 0) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.72f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.45f),
                            )
                        )
                )
                UnreadPill(
                    count = unreadCount,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(NovelaSpacing.xs),
                )
            }

            // Selection tick, top-start, matching the library's selection affordance.
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(NovelaSpacing.xs)
                        .size(24.dp)
                        .clip(RoundedCornerShape(NovelaRadius.sm))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.35f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Dim the cover while a book is selected, so the set reads at a glance.
            if (isSelectionMode && isSelected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
            }
        }

        Spacer(Modifier.height(NovelaSpacing.sm))

        Text(
            text = title,
            style = NovelaType.bookTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = NovelaType.bookMeta,
                color = NovelaTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Compact horizontal card for "Continue reading" carousels: small cover on the
 * left, title + progress on the right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelaBookRow(
    title: String,
    coverModel: Any?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(NovelaMotion.INSTANT),
        label = "rowPress",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(NovelaRadius.cardShape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = NovelaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(NovelaSize.carouselCoverWidth)
                .aspectRatio(NovelaSize.COVER_ASPECT)
                .clip(NovelaRadius.coverShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(0.5.dp, NovelaTheme.colors.hairline, NovelaRadius.coverShape),
        ) {
            ImageView(
                imageModel = coverModel,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = R.drawable.default_book_cover,
            )
        }

        Spacer(Modifier.width(NovelaSpacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = NovelaType.bookMeta,
                    color = NovelaTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (progress != null && progress > 0f) {
                Spacer(Modifier.height(NovelaSpacing.sm))
                ProgressBar(progress = progress.coerceIn(0f, 1f))
            }
        }
    }
}

@Composable
private fun UnreadPill(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(NovelaRadius.chipShape)
            .background(NovelaTheme.colors.badgeBackground)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = NovelaType.badge,
            color = NovelaTheme.colors.badgeText,
        )
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(NovelaRadius.chipShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(NovelaRadius.chipShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Ranked-list badge: a large ordinal in the accent color, used by "Top" rails.
 */
@Composable
fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = rank.toString(),
        style = NovelaType.rank,
        color = if (rank <= 3) MaterialTheme.colorScheme.primary else NovelaTheme.colors.textMuted,
        modifier = modifier.width(20.dp),
    )
}
