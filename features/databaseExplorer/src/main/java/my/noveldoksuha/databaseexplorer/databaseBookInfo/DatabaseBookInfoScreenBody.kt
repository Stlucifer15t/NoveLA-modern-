package my.noveldokusha.databaseexplorer.databaseBookInfo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.coreui.components.BookImageButtonView
import my.noveldokusha.coreui.components.ExpandableText
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaBookRow
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.theme.ImageBorderShape
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.clickableNoIndicator
import my.noveldokusha.databaseexplorer.R
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.mappers.mapToBookMetadata
import my.noveldokusha.scraper.SearchGenre

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DatabaseBookInfoScreenBody(
    state: DatabaseBookInfoState,
    scrollState: ScrollState,
    onSourcesClick: () -> Unit,
    onGenresClick: (genresIds: List<SearchGenre>) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    val bgColor = MaterialTheme.colorScheme.background
    val book = state.book.value
    val coverImg = book.coverImageUrl ?: R.drawable.ic_baseline_empty_24

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .background(bgColor),
    ) {
        // ── Editorial Hero Header ────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
            ) {
                ImageView(
                    imageModel = coverImg,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (colors.isDark) 0.30f else 0.22f),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to bgColor.copy(alpha = 0.25f),
                                0.50f to bgColor.copy(alpha = 0.75f),
                                1.0f to bgColor,
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                    .padding(horizontal = NovelaSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    var showImageFullScreen by rememberSaveable { mutableStateOf(false) }

                    BookImageButtonView(
                        title = "",
                        coverImageModel = coverImg,
                        onClick = { showImageFullScreen = true },
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
                                    imageModel = book.coverImageUrl,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clip(ImageBorderShape)
                                        .fillMaxWidth()
                                        .aspectRatio(1f / 1.45f),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.accentSoft,
                        ) {
                            Text(
                                text = stringResource(id = state.databaseNameStrId.value),
                                style = NovelaType.badge,
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }

                        SelectionContainer {
                            Text(
                                text = book.title,
                                style = NovelaType.heroTitle,
                                color = colors.textPrimary,
                            )
                        }

                        if (!book.bookType.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                color = colors.surfaceCard,
                                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                            ) {
                                Text(
                                    text = book.bookType.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                NovelaPrimaryButton(
                    text = stringResource(R.string.search_for_sources),
                    leadingIcon = Icons.Rounded.TravelExplore,
                    onClick = onSourcesClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = NovelaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── Synopsis Card ────────────────────────────────────────────────
            if (book.description.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.card),
                    color = colors.surfaceCard,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer(modifier = Modifier.padding(14.dp)) {
                        ExpandableText(
                            text = book.description,
                            linesForExpand = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── Authors ──────────────────────────────────────────────────────
            InfoSection(title = stringResource(R.string.authors)) {
                if (book.authors.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    SelectionContainer {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            book.authors.forEach { author ->
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.surfaceCard,
                                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = author.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Genres ───────────────────────────────────────────────────────
            InfoSection(
                title = stringResource(R.string.genres),
                actionText = if (book.genres.size > 1) {
                    stringResource(R.string.search_with_filters)
                } else null,
                onActionClick = if (book.genres.size > 1) {
                    { onGenresClick(book.genres) }
                } else null,
            ) {
                if (book.genres.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        book.genres.forEach { genre ->
                            Surface(
                                onClick = { onGenresClick(listOf(genre)) },
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                color = colors.accentSoft,
                                border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                            ) {
                                Text(
                                    text = genre.genreName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Alternative Titles ───────────────────────────────────────────
            InfoSection(title = stringResource(R.string.alternative_titles)) {
                val titles by remember(book.alternativeTitles) {
                    derivedStateOf {
                        book.alternativeTitles.joinToString(separator = "\n")
                    }
                }
                if (book.alternativeTitles.isEmpty()) {
                    TextAnimated(text = stringResource(R.string.none_found))
                } else {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.card),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SelectionContainer(modifier = Modifier.padding(14.dp)) {
                            ExpandableText(
                                text = titles,
                                linesForExpand = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // ── Tags ─────────────────────────────────────────────────────────
            InfoSection(title = stringResource(R.string.tags)) {
                if (book.tags.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.card),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val tagsText by remember(book.tags) {
                            derivedStateOf { book.tags.joinToString(" · ") }
                        }
                        SelectionContainer(modifier = Modifier.padding(14.dp)) {
                            ExpandableText(
                                text = tagsText,
                                linesForExpand = 3,
                            )
                        }
                    }
                }
            }

            // ── Related Books ────────────────────────────────────────────────
            InfoSection(
                title = stringResource(R.string.related_books),
                badgeText = book.relatedBooks.size.takeIf { it > 0 }?.toString(),
            ) {
                if (book.relatedBooks.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (related in book.relatedBooks) {
                            NovelaBookRow(
                                title = related.title,
                                coverImageModel = related.coverImageUrl.ifBlank { R.drawable.ic_baseline_empty_24 },
                                description = related.description.takeIf { it.isNotBlank() },
                                onClick = { onBookClick(related.mapToBookMetadata()) },
                            )
                        }
                    }
                }
            }

            // ── Similar Recommended ──────────────────────────────────────────
            InfoSection(
                title = stringResource(R.string.similar_recommended),
                badgeText = book.similarRecommended.size.takeIf { it > 0 }?.toString(),
            ) {
                if (book.similarRecommended.isEmpty()) {
                    TextAnimated(text = stringResource(id = R.string.none_found))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (rec in book.similarRecommended) {
                            NovelaBookRow(
                                title = rec.title,
                                coverImageModel = rec.coverImageUrl.ifBlank { R.drawable.ic_baseline_empty_24 },
                                description = rec.description.takeIf { it.isNotBlank() },
                                onClick = { onBookClick(rec.mapToBookMetadata()) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
private fun InfoSection(
    title: String,
    badgeText: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NovelaSectionHeader(
            title = title,
            badgeText = badgeText,
            actionText = actionText,
            onActionClick = onActionClick,
            modifier = Modifier.padding(horizontal = 0.dp),
        )
        content()
    }
}

@Composable
private fun TextAnimated(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "dbInfoText",
        modifier = modifier,
    ) { target ->
        Text(
            text = target,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )
    }
}
