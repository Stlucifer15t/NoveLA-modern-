package my.noveldokusha.features.chapterslist

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.chapterslist.R
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.components.BookImageButtonView
import my.noveldokusha.coreui.components.BookRatingChip
import my.noveldokusha.coreui.components.ExpandableText
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.components.NovelaStatStrip
import my.noveldokusha.coreui.theme.ImageBorderShape
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.clickableNoIndicator

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChaptersScreenHeader(
    bookState: ChaptersScreenState.BookState,
    genres: List<String>,
    rating: String,
    status: String,
    lastUpdateDate: String,
    sourceCatalogName: String,
    numberOfChapters: Int,
    readChapters: Int,
    paddingValues: PaddingValues,
    translatedTitle: String?,
    translatedDescription: String?,
    isTranslating: Boolean,
    showTranslateButton: Boolean,
    onTranslateClick: () -> Unit,
    onClearTranslationClick: () -> Unit,
    modifier: Modifier = Modifier,
    onResumeReading: () -> Unit = {},
    onLibraryToggle: () -> Unit = {},
    onOpenSortSheet: () -> Unit = {},
    onCoverLongClick: () -> Unit,
    onGlobalSearchClick: (input: String, contentType: String) -> Unit,
    onScrollToLastRead: (() -> Unit)?,
    onScrollToChapter: () -> Unit,
    bookCategory: String,
    categories: () -> List<String>,
    onCategoryClick: () -> Unit,
    coverReferer: String? = null,
) {
    val colors = NovelaTheme.colors
    val bgColor = MaterialTheme.colorScheme.background

    val coverImageModel = bookState.coverImageUrl?.let {
        rememberResolvedBookImagePath(
            bookUrl = bookState.url,
            imagePath = it,
        )
    } ?: R.drawable.ic_baseline_empty_24

    val progressPercent = if (numberOfChapters > 0) {
        (readChapters * 100 / numberOfChapters).coerceIn(0, 100)
    } else {
        0
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // ── Atmospheric Cover Hero Wash ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
        ) {
            ImageView(
                imageModel = coverImageModel,
                referer = coverReferer,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (colors.isDark) 0.32f else 0.24f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to bgColor.copy(alpha = 0.25f),
                            0.45f to bgColor.copy(alpha = 0.72f),
                            1.0f to bgColor,
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding() + 8.dp)
                .padding(horizontal = NovelaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Hero Cover + Book Metadata Row ───────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                var showImageFullScreen by rememberSaveable { mutableStateOf(false) }

                BookImageButtonView(
                    title = "",
                    coverImageModel = coverImageModel,
                    referer = coverReferer,
                    onClick = { showImageFullScreen = true },
                    onLongClick = onCoverLongClick,
                    sourceStripPosition = SourceStripPosition.BelowCover,
                    modifier = Modifier
                        .width(NovelaSize.coverWidthHero)
                        .shadow(12.dp, RoundedCornerShape(NovelaRadius.coverHero)),
                )

                if (showImageFullScreen) {
                    Dialog(
                        onDismissRequest = { showImageFullScreen = false },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickableNoIndicator { showImageFullScreen = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            ImageView(
                                imageModel = coverImageModel,
                                referer = coverReferer,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .clip(ImageBorderShape)
                                    .fillMaxWidth()
                                    .aspectRatio(1f / 1.45f)
                                    .clickableNoIndicator { showImageFullScreen = false },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = translatedTitle ?: bookState.title,
                            style = NovelaType.heroTitle,
                            color = colors.textPrimary,
                            modifier = Modifier.clickableNoIndicator {
                                onGlobalSearchClick(bookState.title, bookState.contentType)
                            },
                        )
                    }

                    // Source + Category Pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.accentSoft,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = colors.accent,
                                )
                                SelectionContainer {
                                    Text(
                                        text = sourceCatalogName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        val categoryLabel = when (bookCategory) {
                            "" -> stringResource(R.string.reading)
                            "Completed" -> stringResource(R.string.completed)
                            else -> bookCategory
                        }
                        Surface(
                            onClick = onCategoryClick,
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceCard,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = colors.textSecondary,
                                )
                                Text(
                                    text = categoryLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // Status & Last Update
                    if (status.isNotBlank()) {
                        val isCompleted = status.contains("complete", ignoreCase = true) ||
                            status.contains("заверш", ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = if (isCompleted) colors.success else colors.accent,
                                        shape = CircleShape,
                                    )
                            )
                            Text(
                                text = stringResource(R.string.book_status_format, status),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }

                    if (lastUpdateDate.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Update,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.textMuted,
                            )
                            Text(
                                text = stringResource(R.string.book_last_update_format, lastUpdateDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                            )
                        }
                    }

                    if (rating.isNotBlank()) {
                        BookRatingChip(rating = rating)
                    }
                }
            }

            // ── Primary WebNovel Action Bar (Read CTA + In-Library Toggle) ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NovelaPrimaryButton(
                    text = stringResource(id = R.string.resume_reading),
                    leadingIcon = Icons.Rounded.PlayArrow,
                    onClick = onResumeReading,
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    onClick = onLibraryToggle,
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = if (bookState.inLibrary) colors.favoriteSoft else colors.surfaceCard,
                    border = BorderStroke(
                        width = NovelaSize.hairline,
                        color = if (bookState.inLibrary) {
                            colors.favoriteCrimson.copy(alpha = 0.45f)
                        } else {
                            colors.borderStrong
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (bookState.inLibrary) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = null,
                            tint = if (bookState.inLibrary) colors.favoriteCrimson else colors.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.title_library),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (bookState.inLibrary) colors.favoriteCrimson else colors.textPrimary,
                        )
                    }
                }
            }

            // ── WebNovel 3-Column Stats Strip ────────────────────────────────
            NovelaStatStrip(
                stats = listOf(
                    numberOfChapters.toString() to stringResource(R.string.chapters),
                    "$readChapters / $numberOfChapters" to stringResource(R.string.read),
                    "$progressPercent%" to (status.takeIf { it.isNotBlank() } ?: sourceCatalogName),
                ),
                bottomContent = if (numberOfChapters > 0) {
                    {
                        LinearProgressIndicator(
                            progress = { readChapters.toFloat() / numberOfChapters.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(NovelaRadius.pill)),
                            color = colors.accent,
                            trackColor = colors.accentSoft,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                    }
                } else null,
            )

            // ── Genres Cloud ─────────────────────────────────────────────────
            if (genres.isNotEmpty()) {
                val genresCollapsedCount = 6
                var genresExpanded by rememberSaveable { mutableStateOf(false) }
                val visibleGenres = if (genresExpanded || genres.size <= genresCollapsedCount) {
                    genres
                } else {
                    genres.take(genresCollapsedCount)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (genres.size > genresCollapsedCount) {
                                    genresExpanded = !genresExpanded
                                }
                            },
                        ),
                ) {
                    visibleGenres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceSubtle,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                    if (!genresExpanded && genres.size > genresCollapsedCount) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.accentSoft,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { genresExpanded = true },
                            ),
                        ) {
                            Text(
                                text = "+${genres.size - genresCollapsedCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }

            // ── Editorial Synopsis Card ──────────────────────────────────────
            val displayDesc = (translatedDescription ?: bookState.description).trim()
            if (displayDesc.isNotEmpty() || showTranslateButton) {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.card),
                    color = colors.surfaceCard,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (showTranslateButton) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val hasTranslation = translatedTitle != null || translatedDescription != null
                                Surface(
                                    onClick = {
                                        if (hasTranslation) onClearTranslationClick() else onTranslateClick()
                                    },
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.accentSoft,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        if (isTranslating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(13.dp),
                                                strokeWidth = 1.8.dp,
                                                color = colors.accent,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (hasTranslation) {
                                                    Icons.Outlined.Close
                                                } else {
                                                    Icons.Outlined.GTranslate
                                                },
                                                contentDescription = if (hasTranslation) {
                                                    stringResource(R.string.clear_translation)
                                                } else {
                                                    stringResource(R.string.translate)
                                                },
                                                tint = colors.accent,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Text(
                                            text = if (hasTranslation) {
                                                stringResource(R.string.clear_translation)
                                            } else {
                                                stringResource(R.string.translate)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                        )
                                    }
                                }
                            }
                        }

                        if (displayDesc.isNotEmpty()) {
                            val descText by remember(displayDesc) { derivedStateOf { displayDesc } }
                            SelectionContainer {
                                ExpandableText(
                                    text = descText,
                                    linesForExpand = 4,
                                )
                            }
                        }
                    }
                }
            }

            // ── Table of Contents Directory Bar ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(17.dp)
                        .clip(RoundedCornerShape(NovelaRadius.pill))
                        .background(colors.accent)
                )
                Text(
                    text = stringResource(R.string.chapters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accentSoft,
                ) {
                    Text(
                        text = numberOfChapters.toString(),
                        style = NovelaType.badge,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                if (onScrollToLastRead != null) {
                    Surface(
                        onClick = onScrollToLastRead,
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accentSoft,
                        border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdded,
                                contentDescription = stringResource(id = R.string.scroll_to_last_read_chapter),
                                tint = colors.accent,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.last_read),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Surface(
                    onClick = onScrollToChapter,
                    shape = CircleShape,
                    color = colors.surfaceSubtle,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(id = R.string.go_to_chapter),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                Surface(
                    onClick = onOpenSortSheet,
                    shape = CircleShape,
                    color = colors.surfaceSubtle,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = stringResource(id = R.string.sort),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}
