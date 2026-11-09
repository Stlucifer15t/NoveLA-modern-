package my.noveldokusha.libraryexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.core.utils.refererFor
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.components.BookContentTypeBadge
import my.noveldokusha.coreui.components.BookImageButtonView
import my.noveldokusha.coreui.components.BookRatingBadge
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.feature.local_database.BookWithContext

@Composable
internal fun LibraryPageBody(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onLongClick: (BookWithContext) -> Unit,
    getSourceName: (String) -> String,
    gridColumns: Int = 3,
    sourceStripPosition: SourceStripPosition = SourceStripPosition.BelowCover,
    selectedBooks: Map<String, Boolean> = emptyMap(),
    isSelectionMode: Boolean = false,
    pendingRemoval: Set<String> = emptySet(),
    gridState: LazyGridState = rememberLazyGridState(),
    translatedTitles: Map<String, String> = emptyMap(),
    onImportEpub: (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors

    if (list.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp),
            contentAlignment = Alignment.Center,
        ) {
            NovelaEmptyState(
                icon = Icons.Outlined.AutoStories,
                title = stringResource(R.string.no_books_in_library),
                actionLabel = if (onImportEpub != null) stringResource(R.string.import_epub) else null,
                onActionClick = onImportEpub,
            )
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns.coerceIn(2, 6)),
            contentPadding = PaddingValues(
                top = NovelaSpacing.sm,
                bottom = 120.dp,
                start = NovelaSpacing.gridHorizontal,
                end = NovelaSpacing.gridHorizontal,
            ),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.gridGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = list,
                key = { it.book.url },
                contentType = { "book" },
            ) { item ->
                val isSelected = selectedBooks[item.book.url] ?: false
                val isRemoving = item.book.url in pendingRemoval

                AnimatedVisibility(
                    visible = !isRemoving,
                    exit = fadeOut(animationSpec = tween(260)) + scaleOut(animationSpec = tween(260)),
                ) {
                    Box {
                        val notReadCount = (item.chaptersCount - item.chaptersReadCount).coerceAtLeast(0)
                        val progress = if (item.chaptersCount > 0 && item.chaptersReadCount > 0) {
                            item.chaptersReadCount.toFloat() / item.chaptersCount.toFloat()
                        } else {
                            null
                        }

                        BookImageButtonView(
                            title = translatedTitles[item.book.url]?.takeIf { t -> t.isNotBlank() }
                                ?: item.book.title,
                            coverImageModel = rememberResolvedBookImagePath(
                                bookUrl = item.book.url,
                                imagePath = item.book.coverImageUrl,
                            ),
                            onClick = { onClick(item) },
                            onLongClick = { onLongClick(item) },
                            sourceStripUnreadCount = notReadCount,
                            sourceStripSourceName = getSourceName(item.book.url),
                            sourceStripPosition = sourceStripPosition,
                            progressFraction = progress,
                            fadeInDurationMillis = 220,
                            topLeftBadge = {
                                BookContentTypeBadge(contentType = item.book.contentType)
                            },
                            topRightBadge = if (isSelectionMode) {
                                {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) colors.accent else colors.coverBadgeBg,
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        ),
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        if (isSelected) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = colors.onAccent,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (item.book.rating.isNotBlank()) {
                                { BookRatingBadge(rating = item.book.rating) }
                            } else null,
                            forceCache = true,
                        )

                        if (isSelectionMode && isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(NovelaRadius.cover))
                                    .background(colors.accent.copy(alpha = 0.14f))
                                    .border(
                                        width = 2.dp,
                                        color = colors.accent,
                                        shape = RoundedCornerShape(NovelaRadius.cover),
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Prefetch next images while scrolling to avoid decode spikes
        val context = LocalContext.current
        val imageLoader = SingletonImageLoader.get(context)
        val appFileResolver = remember(context) { AppFileResolver(context) }
        LaunchedEffect(gridState, list) {
            val pendingPrefetch = mutableListOf<coil3.request.Disposable>()
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            }.collect { lastVisibleIndex ->
                pendingPrefetch.forEach { it.dispose() }
                pendingPrefetch.clear()
                val prefetchCount = 5
                val startIndex = lastVisibleIndex + 1
                val endIndex = minOf(startIndex + prefetchCount, list.size)
                for (i in startIndex until endIndex) {
                    val book = list[i]
                    val request = ImageRequest.Builder(context)
                        .data(
                            appFileResolver.resolvedBookImagePath(
                                bookUrl = book.book.url,
                                imagePath = book.book.coverImageUrl,
                            )
                        )
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .apply {
                            val url = book.book.coverImageUrl
                            val referer = url
                                ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                                ?.let(::refererFor)
                            if (!referer.isNullOrEmpty()) {
                                httpHeaders(
                                    NetworkHeaders.Builder()
                                        .set("Referer", referer)
                                        .build()
                                )
                            }
                        }
                        .build()
                    pendingPrefetch.add(imageLoader.enqueue(request))
                }
            }
        }
    }
}
