package my.noveldokusha.coreui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.coreui.AppTestTags
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

/**
 * Editorial WebNovel-style vertical book card with subtle press scale, optional unread count,
 * progress indicator, and badge slots.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelaBookCard(
    title: String,
    coverImageModel: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    unreadCount: Int = 0,
    progress: Float? = null,
    selected: Boolean = false,
    referer: String? = null,
    topStartBadge: (@Composable () -> Unit)? = null,
    topEndBadge: (@Composable () -> Unit)? = null,
    onLongClick: () -> Unit = {},
) {
    val colors = NovelaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = NovelaMotion.fastMs),
        label = "novelaCardScale",
    )

    val coverShape = RoundedCornerShape(NovelaRadius.cover)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .testTag(AppTestTags.BOOK_IMAGE_BUTTON_VIEW)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(NovelaSize.coverAspectRatio)
                .clip(coverShape)
                .background(colors.surfaceSubtle)
                .border(
                    width = if (selected) 2.dp else NovelaSize.hairline,
                    color = if (selected) colors.accent else colors.borderSubtle,
                    shape = coverShape,
                )
        ) {
            ImageView(
                imageModel = coverImageModel,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = R.drawable.default_book_cover,
                referer = referer,
            )

            if (unreadCount > 0 || (progress != null && progress > 0f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.65f),
                            )
                        )
                )
            }

            if (topStartBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(NovelaSpacing.xs + 2.dp)
                ) {
                    topStartBadge()
                }
            }

            if (topEndBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(NovelaSpacing.xs + 2.dp)
                ) {
                    topEndBadge()
                }
            }

            if (unreadCount > 0) {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                ) {
                    Text(
                        text = if (unreadCount > 999) "999+" else unreadCount.toString(),
                        style = NovelaType.badge,
                        color = colors.onAccent,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            if (progress != null && progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.BottomCenter),
                    color = colors.accent,
                    trackColor = Color.White.copy(alpha = 0.24f),
                    strokeCap = StrokeCap.Square,
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.accent.copy(alpha = 0.22f))
                )
            }
        }

        Spacer(Modifier.height(NovelaSpacing.xs + 2.dp))

        Text(
            text = title,
            style = NovelaType.bookCardTitle,
            color = colors.textPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = NovelaType.bookCardMeta,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * WebNovel Horizontal Novel Card — used in VerticalList catalog mode, ranked lists, and search rails.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelaBookRow(
    title: String,
    coverImageModel: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    rating: String? = null,
    referer: String? = null,
    topLeftBadge: (@Composable () -> Unit)? = null,
    rank: Int? = null,
    progress: Float? = null,
    onLongClick: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    val coverShape = RoundedCornerShape(NovelaRadius.sm)

    Surface(
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.card))
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovelaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rank != null) {
                RankBadge(rank = rank)
                Spacer(Modifier.width(NovelaSpacing.md))
            }

            Box(
                modifier = Modifier
                    .width(62.dp)
                    .aspectRatio(NovelaSize.coverAspectRatio)
                    .clip(coverShape)
                    .background(colors.surfaceSubtle)
                    .border(NovelaSize.hairline, colors.borderSubtle, coverShape)
            ) {
                ImageView(
                    imageModel = coverImageModel,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    error = R.drawable.default_book_cover,
                    referer = referer,
                )
                if (topLeftBadge != null) {
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        topLeftBadge()
                    }
                }
            }

            Spacer(Modifier.width(NovelaSpacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!rating.isNullOrBlank()) {
                    BookRatingChip(rating = rating)
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = NovelaType.bookCardMeta,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (progress != null) {
                    Spacer(Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(NovelaRadius.pill)),
                        color = colors.accent,
                        trackColor = colors.accentSoft,
                    )
                }
            }

            Spacer(Modifier.width(NovelaSpacing.sm))

            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val colors = NovelaTheme.colors
    val badgeColor = when (rank) {
        1 -> Color(0xFFFFBC1F)
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFFD97706)
        else -> colors.textMuted
    }
    Text(
        text = rank.toString().padStart(2, '0'),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
        ),
        color = badgeColor,
        modifier = modifier.width(24.dp),
    )
}
