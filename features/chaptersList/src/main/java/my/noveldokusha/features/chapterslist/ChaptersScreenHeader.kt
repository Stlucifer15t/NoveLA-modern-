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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import my.noveldokusha.coreui.theme.ImageBorderShape
import my.noveldokusha.coreui.theme.clickableNoIndicator
import my.noveldokusha.strings.R as StringsR

/**
 * Шапка книги в духе Novery: обложка с названием и источником, ряд статистики,
 * главное действие, раскрывающееся описание и жанры. Цвета — из темы приложения.
 * Все значения реальные: глава/прочитано/сохранено из базы, рейтинг из источника.
 */
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
    savedChapters: Int,
    paddingValues: PaddingValues,
    translatedTitle: String?,
    translatedDescription: String?,
    isTranslating: Boolean,
    showTranslateButton: Boolean,
    onTranslateClick: () -> Unit,
    onClearTranslationClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCoverLongClick: () -> Unit,
    onGlobalSearchClick: (input: String, contentType: String) -> Unit,
    onResumeReading: () -> Unit,
    onScrollToLastRead: (() -> Unit)?,
    onScrollToChapter: () -> Unit,
    canDownload: Boolean,
    onDownloadNext100: () -> Unit,
    bookCategory: String,
    onCategoryClick: () -> Unit,
    coverReferer: String? = null,
) {
    val coverImageModel = bookState.coverImageUrl?.let {
        rememberResolvedBookImagePath(
            bookUrl = bookState.url,
            imagePath = it
        )
    } ?: R.drawable.ic_baseline_empty_24

    Box(modifier = modifier) {
        Box(Modifier.matchParentSize()) {
            ImageView(
                imageModel = coverImageModel,
                referer = coverReferer,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .alpha(0.2f)
                    .fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            1f to MaterialTheme.colorScheme.background,
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
        ) {
            // ─── Обложка, название, источник ───
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                var showImageFullScreen by rememberSaveable { mutableStateOf(false) }
                BookImageButtonView(
                    title = "",
                    coverImageModel = coverImageModel,
                    referer = coverReferer,
                    onClick = { showImageFullScreen = true },
                    onLongClick = onCoverLongClick,
                    sourceStripPosition = SourceStripPosition.BelowCover,
                    modifier = Modifier.width(132.dp)
                )
                if (showImageFullScreen) Dialog(
                    onDismissRequest = { showImageFullScreen = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    // Фон не закрываем: окно диалога прозрачно, полупрозрачное
                    // затемнение даёт platform dim (FLAG_DIM_BEHIND), как в
                    // исходной версии. Клик по экрану закрывает.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickableNoIndicator { showImageFullScreen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        ImageView(
                            imageModel = coverImageModel,
                            referer = coverReferer,
                            // Как у коверов в каталоге: скругление ImageBorderShape + отступы
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(ImageBorderShape)
                                .fillMaxWidth()
                                .aspectRatio(1f / 1.45f)
                                .clickableNoIndicator { showImageFullScreen = false },
                            // Crop, как у коверов в каталоге: картинка заполняет
                            // рамку и скругление углов реально видно.
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxHeight()
                        .weight(1f),
                ) {
                    SelectionContainer {
                        // Название без ограничения строк: длинные названия видны полностью.
                        Text(
                            text = translatedTitle ?: bookState.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickableNoIndicator {
                                onGlobalSearchClick(bookState.title, bookState.contentType)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SelectionContainer {
                            Text(
                                text = sourceCatalogName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // Статус и дата последнего обновления, парсятся с сайта источника.
                    if (status.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.book_status_format, status),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (lastUpdateDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Update,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.book_last_update_format, lastUpdateDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    BookRatingChip(rating)
                    Spacer(modifier = Modifier.height(6.dp))
                    // Категория библиотеки: по нажатию меняется.
                    val categoryLabel = when (bookCategory) {
                        "" -> stringResource(R.string.reading)
                        "Completed" -> stringResource(R.string.completed)
                        else -> bookCategory
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCategoryClick,
                            ),
                        ) {
                            Text(
                                text = categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Статистика: только реальные числа ───
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                ) {
                    StatCell(
                        value = numberOfChapters.toString(),
                        label = stringResource(StringsR.string.book_stat_chapters),
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatCell(
                        value = readChapters.toString(),
                        label = stringResource(StringsR.string.book_stat_read),
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatCell(
                        value = savedChapters.toString(),
                        label = stringResource(StringsR.string.book_stat_saved),
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatCell(
                        value = rating.ifBlank { "—" },
                        label = stringResource(StringsR.string.book_stat_rating),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Действия ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Button(
                    onClick = onResumeReading,
                    shape = my.noveldokusha.coreui.theme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (bookState.lastReadChapter != null) StringsR.string.book_action_continue
                            else StringsR.string.book_action_start
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (canDownload) {
                    FilledTonalButton(
                        onClick = onDownloadNext100,
                        shape = my.noveldokusha.coreui.theme.shapes.large,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(StringsR.string.download_next_100_chapters),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Описание: раскрывается по нажатию ───
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(StringsR.string.book_synopsis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (onScrollToLastRead != null) {
                    IconButton(onClick = onScrollToLastRead) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkAdded,
                            contentDescription = stringResource(id = R.string.scroll_to_last_read_chapter),
                        )
                    }
                }
                if (showTranslateButton) {
                    IconButton(
                        onClick = {
                            if (translatedTitle != null || translatedDescription != null) {
                                onClearTranslationClick()
                            } else {
                                onTranslateClick()
                            }
                        },
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (translatedTitle != null || translatedDescription != null) Icons.Outlined.Close else Icons.Outlined.GTranslate,
                                contentDescription = if (translatedTitle != null || translatedDescription != null) stringResource(R.string.clear_translation) else stringResource(R.string.translate),
                            )
                        }
                    }
                }
                IconButton(onClick = onScrollToChapter) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(id = R.string.go_to_chapter),
                    )
                }
            }

            val displayDesc = (translatedDescription ?: bookState.description).trim()
            if (displayDesc.isNotEmpty()) {
                val descText by remember(displayDesc) { derivedStateOf { displayDesc } }
                SelectionContainer {
                    ExpandableText(
                        text = descText,
                        linesForExpand = 4,
                    )
                }
            }

            // ─── Жанры ───
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val genresCollapsedCount = 4
                var genresExpanded by rememberSaveable { mutableStateOf(false) }
                val visibleGenres = if (genresExpanded || genres.size <= genresCollapsedCount)
                    genres
                else
                    genres.take(genresCollapsedCount)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (genres.size > genresCollapsedCount) genresExpanded = !genresExpanded }
                        )
                ) {
                    visibleGenres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    if (!genresExpanded && genres.size > genresCollapsedCount) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "+${genres.size - genresCollapsedCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(Dp.Hairline)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
