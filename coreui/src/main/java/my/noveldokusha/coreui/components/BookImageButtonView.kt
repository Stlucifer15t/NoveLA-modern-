package my.noveldokusha.coreui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.ImageBorderShape
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.textPadding
import my.noveldokusha.coreui.uitesting.AppTestTags

enum class BookTitlePosition {
    Inside, Outside, Hidden
}

@Immutable
private data class StripSizing(
    val fontSize: TextUnit,
    val badgeFontSize: TextUnit,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val hPad: Dp,
    val vPad: Dp,
)

private fun stripSizingFor(gridColumns: Int): StripSizing = when {
    gridColumns <= 2 -> StripSizing(11.5.sp, 10.5.sp, 14.sp, 19.sp, 8.dp, 4.dp)
    gridColumns == 3 -> StripSizing(10.5.sp, 10.sp, 13.sp, 17.5.sp, 6.dp, 3.dp)
    gridColumns == 4 -> StripSizing(9.5.sp, 9.sp, 11.5.sp, 15.5.sp, 5.dp, 2.5.dp)
    else -> StripSizing(8.5.sp, 8.5.sp, 10.5.sp, 14.sp, 4.dp, 2.dp)
}

internal fun String?.toContentTypeBadgeIcon(): Int = when (this) {
    "manga" -> R.drawable.ic_content_type_manga
    "video" -> R.drawable.ic_content_type_video
    else -> R.drawable.ic_content_type_novel
}

/**
 * Smoked frosted-glass content-type cover badge (Novel / Manga / Video).
 */
@Composable
fun BookContentTypeBadge(
    contentType: String?,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = colors.coverBadgeBg,
        border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
        shadowElevation = 2.dp,
    ) {
        Icon(
            painter = painterResource(id = contentType.toContentTypeBadgeIcon()),
            contentDescription = contentType ?: "novel",
            modifier = Modifier
                .padding(4.dp)
                .size(12.dp),
            tint = Color.White.copy(alpha = 0.92f),
        )
    }
}

/**
 * Primary WebNovel-inspired Book Cover Card used across Library, Source Catalog,
 * Global Search, Database Search, and Migration grids.
 *
 * Supports all three [SourceStripPosition] styles (`OnCover`, `BelowCover`, `InfoPanel`)
 * and all [BookTitlePosition] modes while maintaining test tag compatibility.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookImageButtonView(
    title: String,
    coverImageModel: Any?,
    modifier: Modifier = Modifier,
    bookTitlePosition: BookTitlePosition = BookTitlePosition.Outside,
    indication: Indication? = LocalIndication.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    sourceName: String? = null,
    unreadCount: Int = 0,
    sourceStripPosition: SourceStripPosition = SourceStripPosition.BelowCover,
    gridColumns: Int = 3,
    progressFraction: Float? = null,
    topStartBadge: (@Composable () -> Unit)? = null,
    topEndBadge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = { },
) {
    val colors = NovelaTheme.colors
    val hasStripContent = !sourceName.isNullOrBlank() || unreadCount > 0
    val sizing = remember(gridColumns) { stripSizingFor(gridColumns) }

    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = NovelaMotion.fastMs),
        label = "bookCardPressScale",
    )

    val cardCorner = NovelaRadius.cover
    val isInfoPanel = sourceStripPosition == SourceStripPosition.InfoPanel

    val containerModifier = if (isInfoPanel) {
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(NovelaRadius.md))
            .background(colors.surfaceCard)
            .border(
                width = NovelaSize.hairline,
                color = colors.borderSubtle,
                shape = RoundedCornerShape(NovelaRadius.md),
            )
    } else {
        modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

    Column(modifier = containerModifier) {
        val coverShape = if (isInfoPanel) {
            RoundedCornerShape(topStart = NovelaRadius.md, topEnd = NovelaRadius.md)
        } else {
            RoundedCornerShape(cardCorner)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(NovelaSize.coverAspectRatio)
                .clip(coverShape)
                .background(colors.surfaceSubtle)
                .then(
                    if (!isInfoPanel) {
                        Modifier.border(
                            width = NovelaSize.hairline,
                            color = colors.borderSubtle,
                            shape = coverShape,
                        )
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    indication = indication,
                    interactionSource = interactionSource,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .testTag(AppTestTags.BOOK_IMAGE_BUTTON_VIEW)
        ) {
            ImageView(
                imageModel = coverImageModel,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = R.drawable.default_book_cover,
            )

            // Inside-cover title gradient (used in horizontal rails like Global Search)
            if (bookTitlePosition == BookTitlePosition.Inside) {
                val bottomReserved = if (sourceStripPosition == SourceStripPosition.OnCover && hasStripContent) {
                    24.dp
                } else {
                    0.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Color(0x8807090F),
                                1f to Color(0xEB07090F),
                            )
                        )
                        .padding(
                            top = 24.dp,
                            bottom = 8.dp + bottomReserved,
                            start = 7.dp,
                            end = 7.dp,
                        )
                ) {
                    Text(
                        text = title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = NovelaType.bookCardTitle.copy(
                            fontSize = sizing.titleFontSize,
                            lineHeight = sizing.titleLineHeight,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }
            }

            // OnCover mode: dark frosted gradient bar along cover bottom
            if (sourceStripPosition == SourceStripPosition.OnCover && hasStripContent) {
                OnCoverMetadataOverlay(
                    sourceName = sourceName,
                    unreadCount = unreadCount,
                    sizing = sizing,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else if (unreadCount > 0 && bookTitlePosition == BookTitlePosition.Inside) {
                UnreadCountCapsule(
                    unreadCount = unreadCount,
                    fontSize = sizing.badgeFontSize,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            }

            // Optional thin reading progress bar along bottom edge of cover
            if (progressFraction != null && progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .height(2.5.dp)
                            .background(colors.accent)
                    )
                }
            }

            if (topStartBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    topStartBadge()
                }
            }

            if (topEndBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    topEndBadge()
                }
            }
        }

        // BelowCover & InfoPanel metadata block
        when (sourceStripPosition) {
            SourceStripPosition.BelowCover -> {
                if (bookTitlePosition == BookTitlePosition.Outside) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = NovelaType.bookCardTitle.copy(
                            fontSize = sizing.titleFontSize,
                            lineHeight = sizing.titleLineHeight,
                        ),
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                    )
                }
                if (hasStripContent) {
                    Spacer(Modifier.height(3.dp))
                    BelowCoverMetaRow(
                        sourceName = sourceName,
                        unreadCount = unreadCount,
                        sizing = sizing,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }
            SourceStripPosition.InfoPanel -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (bookTitlePosition == BookTitlePosition.Outside) {
                        Text(
                            text = title,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = NovelaType.bookCardTitle.copy(
                                fontSize = sizing.titleFontSize,
                                lineHeight = sizing.titleLineHeight,
                            ),
                            color = colors.textPrimary,
                        )
                    }
                    if (hasStripContent) {
                        BelowCoverMetaRow(
                            sourceName = sourceName,
                            unreadCount = unreadCount,
                            sizing = sizing,
                        )
                    }
                }
            }
            SourceStripPosition.OnCover -> {
                if (bookTitlePosition == BookTitlePosition.Outside) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = NovelaType.bookCardTitle.copy(
                            fontSize = sizing.titleFontSize,
                            lineHeight = sizing.titleLineHeight,
                        ),
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnCoverMetadataOverlay(
    sourceName: String?,
    unreadCount: Int,
    sizing: StripSizing,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.3f to Color(0x9E07090F),
                    1f to Color(0xEB07090F),
                )
            )
            .padding(
                start = sizing.hPad,
                end = sizing.hPad,
                top = 12.dp,
                bottom = sizing.vPad + 2.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = sourceName.orEmpty(),
            color = Color.White.copy(alpha = 0.90f),
            style = NovelaType.bookCardMeta.copy(fontSize = sizing.fontSize),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (unreadCount > 0) {
            UnreadCountCapsule(
                unreadCount = unreadCount,
                fontSize = sizing.badgeFontSize,
            )
        }
    }
}

@Composable
private fun BelowCoverMetaRow(
    sourceName: String?,
    unreadCount: Int,
    sizing: StripSizing,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!sourceName.isNullOrBlank()) {
            Text(
                text = sourceName,
                color = colors.textMuted,
                style = NovelaType.bookCardMeta.copy(fontSize = sizing.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (unreadCount > 0) {
            UnreadCountCapsule(
                unreadCount = unreadCount,
                fontSize = sizing.badgeFontSize,
            )
        }
    }
}

@Composable
private fun UnreadCountCapsule(
    unreadCount: Int,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.accent,
    ) {
        Text(
            text = unreadCount.toString(),
            color = colors.onAccent,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun BookTitleMetadata(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                drawStyle = Stroke(
                    miter = 4f,
                    width = 4f,
                    join = StrokeJoin.Miter
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .textPadding(),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .textPadding(),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        Row {
            BookImageButtonView(
                title = "Shadow Slave",
                coverImageModel = null,
                onClick = { },
                onLongClick = { },
                sourceName = "WebNovel",
                unreadCount = 12,
                bookTitlePosition = BookTitlePosition.Outside,
                modifier = Modifier.weight(1f)
            )
            BookImageButtonView(
                title = "Lord of the Mysteries: Circle of Inevitability",
                coverImageModel = null,
                onClick = { },
                onLongClick = { },
                sourceName = "RoyalRoad",
                unreadCount = 3,
                sourceStripPosition = SourceStripPosition.InfoPanel,
                bookTitlePosition = BookTitlePosition.Outside,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
