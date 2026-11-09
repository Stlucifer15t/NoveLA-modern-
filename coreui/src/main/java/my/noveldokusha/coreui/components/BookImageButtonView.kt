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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.coreui.AppTestTags
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.coreui.theme.textPadding

enum class BookTitlePosition {
    Inside, Outside, Hidden
}

/** Content type icon resolver (tested by ContentTypeBadgeTest). */
fun String?.toContentTypeBadgeIcon(): Int = when (this) {
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
 * Global Search, Database Search, Book Info Hero, and Migration grids.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookImageButtonView(
    title: String,
    coverImageModel: Any,
    modifier: Modifier = Modifier,
    bookTitlePosition: BookTitlePosition = BookTitlePosition.Outside,
    indication: Indication = LocalIndication.current,
    interactionSource: MutableInteractionSource? = null,
    topLeftBadge: (@Composable () -> Unit)? = null,
    topRightBadge: (@Composable () -> Unit)? = null,
    sourceStripUnreadCount: Int? = null,
    sourceStripSourceName: String? = null,
    sourceStripPosition: SourceStripPosition = SourceStripPosition.BelowCover,
    forceCache: Boolean = false,
    referer: String? = null,
    fadeInDurationMillis: Int = 250,
    progressFraction: Float? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = { },
) {
    val colors = NovelaTheme.colors
    val rememberedInteractionSource = remember { MutableInteractionSource() }
    val effectiveInteractionSource = interactionSource ?: rememberedInteractionSource

    val unreadCount = sourceStripUnreadCount ?: 0
    val hasStripContent = !sourceStripSourceName.isNullOrBlank() || unreadCount > 0
    val showTitle = title.isNotBlank() && bookTitlePosition == BookTitlePosition.Outside
    val isInfoPanel = sourceStripPosition == SourceStripPosition.InfoPanel

    val isPressed by effectiveInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = NovelaMotion.fastMs),
        label = "bookCardPressScale",
    )

    val cardModifier = if (isInfoPanel) {
        modifier
            .testTag(AppTestTags.BOOK_IMAGE_BUTTON_VIEW)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(NovelaRadius.md))
            .background(colors.surfaceCard)
            .border(
                width = if (selected) 2.dp else NovelaSize.hairline,
                color = if (selected) colors.accent else colors.borderSubtle,
                shape = RoundedCornerShape(NovelaRadius.md),
            )
    } else {
        modifier
            .testTag(AppTestTags.BOOK_IMAGE_BUTTON_VIEW)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    }

    val coverShape = if (isInfoPanel && (showTitle || hasStripContent)) {
        RoundedCornerShape(topStart = NovelaRadius.md, topEnd = NovelaRadius.md)
    } else {
        RoundedCornerShape(NovelaRadius.cover)
    }

    Column(modifier = cardModifier) {
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
                    interactionSource = effectiveInteractionSource,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        ) {
            ImageView(
                imageModel = coverImageModel,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                fadeInDurationMillis = fadeInDurationMillis,
                error = R.drawable.default_book_cover,
                forceCache = forceCache,
                referer = referer,
            )

            if (title.isNotBlank() && bookTitlePosition == BookTitlePosition.Inside) {
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
                        .padding(top = 24.dp, bottom = 8.dp, start = 7.dp, end = 7.dp)
                ) {
                    Text(
                        text = title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = NovelaType.bookCardTitle.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }

            if (sourceStripPosition == SourceStripPosition.OnCover && hasStripContent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.3f to Color(0x9E07090F),
                                1f to Color(0xEB07090F),
                            )
                        )
                        .padding(start = 7.dp, end = 7.dp, top = 12.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = sourceStripSourceName.orEmpty(),
                        color = Color.White.copy(alpha = 0.90f),
                        style = NovelaType.bookCardMeta.copy(fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unreadCount > 0) {
                        UnreadCountCapsule(unreadCount = unreadCount)
                    }
                }
            }

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

            if (topLeftBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    topLeftBadge()
                }
            }

            if (topRightBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                ) {
                    topRightBadge()
                }
            }
        }

        when (sourceStripPosition) {
            SourceStripPosition.BelowCover -> {
                if (showTitle) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = NovelaType.bookCardTitle,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                    )
                }
                if (hasStripContent) {
                    Spacer(Modifier.height(3.dp))
                    BelowCoverMetaRow(
                        sourceName = sourceStripSourceName,
                        unreadCount = unreadCount,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }
            SourceStripPosition.InfoPanel -> {
                if (showTitle || hasStripContent) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (showTitle) {
                            Text(
                                text = title,
                                maxLines = 2,
                                minLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = NovelaType.bookCardTitle,
                                color = colors.textPrimary,
                            )
                        }
                        if (hasStripContent) {
                            BelowCoverMetaRow(
                                sourceName = sourceStripSourceName,
                                unreadCount = unreadCount,
                            )
                        }
                    }
                }
            }
            SourceStripPosition.OnCover -> {
                if (showTitle) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = NovelaType.bookCardTitle,
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
private fun BelowCoverMetaRow(
    sourceName: String?,
    unreadCount: Int,
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
                style = NovelaType.bookCardMeta.copy(fontSize = 10.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (unreadCount > 0) {
            UnreadCountCapsule(unreadCount = unreadCount)
        }
    }
}

@Composable
private fun UnreadCountCapsule(
    unreadCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    val label = if (unreadCount > 999) "999+" else unreadCount.toString()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.accent,
    ) {
        Text(
            text = label,
            color = colors.onAccent,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 11.sp,
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

@PreviewThemes
@Composable
private fun PreviewView() {
    InternalTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookImageButtonView(
                title = "Shadow Slave",
                coverImageModel = "",
                onClick = { },
                onLongClick = { },
                sourceStripUnreadCount = 12,
                sourceStripSourceName = "WebNovel",
                sourceStripPosition = SourceStripPosition.BelowCover,
                modifier = Modifier.weight(1f)
            )
            BookImageButtonView(
                title = "Lord of the Mysteries: Circle of Inevitability",
                coverImageModel = "",
                onClick = { },
                onLongClick = { },
                sourceStripUnreadCount = 3,
                sourceStripSourceName = "RoyalRoad",
                sourceStripPosition = SourceStripPosition.InfoPanel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
