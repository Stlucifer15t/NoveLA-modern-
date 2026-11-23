package my.dreamora.features.chapterslist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.dreamora.chapterslist.R
import my.dreamora.core.appPreferences.SourceStripPosition
import my.dreamora.core.rememberResolvedBookImagePath
import my.dreamora.coreui.components.BookImageButtonView
import my.dreamora.coreui.components.ImageView
import my.dreamora.coreui.theme.ImageBorderShape
import my.dreamora.coreui.theme.clickableNoIndicator
import my.dreamora.strings.R as StringsR

/**
 * Шапка книги по макету Novery: обложка с названием и источником, кнопка «В библиотеку»,
 * «Читать» и «Скачать», ряд статистики, раскрывающееся описание и жанры.
 * Все значения реальные: глава/прочитано/сохранено из базы, рейтинг из источника.
 */
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
    inLibrary: Boolean,
    onLibraryToggle: () -> Unit,
    readingStatus: String,
    onStatusClick: () -> Unit,
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
                    // Фон не закрываем: окно диалога прозрачно, затемнение даёт platform dim.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickableNoIndicator { showImageFullScreen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        ImageView(
                            imageModel = coverImageModel,
                            referer = coverReferer,
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(ImageBorderShape)
                                .fillMaxWidth()
                                .aspectRatio(1f / 1.45f)
                                .clickableNoIndicator { showImageFullScreen = false },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(top = 4.dp)
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
                    Spacer(modifier = Modifier.height(10.dp))
                    // Источник — пилюлей, как «WEBNOVEL» в макете.
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            SelectionContainer {
                                Text(
                                    text = sourceCatalogName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (status.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MetaLine(Icons.Outlined.Flag, stringResource(R.string.book_status_format, status))
                    }
                    if (lastUpdateDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        MetaLine(Icons.Outlined.Update, stringResource(R.string.book_last_update_format, lastUpdateDate))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onStatusClick,
                            ),
                        ) {
                            Text(
                                text = readingStatusLabel(readingStatus),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── В библиотеку: та же операция, что была в верхней панели ───
            OutlinedButton(
                onClick = onLibraryToggle,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(
                    imageVector = if (inLibrary) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (inLibrary) StringsR.string.book_in_library else StringsR.string.book_add_to_library
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Читать и скачать ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Button(
                    onClick = onResumeReading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                    OutlinedButton(
                        onClick = onDownloadNext100,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(StringsR.string.book_action_download),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Статистика: только реальные числа ───
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                ) {
                    StatCell(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        value = numberOfChapters.toString(),
                        label = stringResource(StringsR.string.book_stat_chapters),
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatCell(
                        icon = Icons.Outlined.Visibility,
                        value = readChapters.toString(),
                        label = stringResource(StringsR.string.book_stat_read),
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatCell(
                        icon = Icons.Outlined.DoneAll,
                        value = savedChapters.toString(),
                        label = stringResource(StringsR.string.book_stat_saved),
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatCell(
                        icon = Icons.Filled.Star,
                        value = rating.ifBlank { "—" },
                        label = stringResource(StringsR.string.book_stat_rating),
                        modifier = Modifier.weight(1f),
                        accent = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Описание: карточка, раскрывается по нажатию ───
            SynopsisCard(
                description = (translatedDescription ?: bookState.description).trim(),
                showTranslateButton = showTranslateButton,
                isTranslating = isTranslating,
                isTranslated = translatedTitle != null || translatedDescription != null,
                onTranslateClick = onTranslateClick,
                onClearTranslationClick = onClearTranslationClick,
                onScrollToLastRead = onScrollToLastRead,
                onScrollToChapter = onScrollToChapter,
            )

            // ─── Жанры: одна строка с прокруткой, как в макете ───
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    genres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SynopsisCard(
    description: String,
    showTranslateButton: Boolean,
    isTranslating: Boolean,
    isTranslated: Boolean,
    onTranslateClick: () -> Unit,
    onClearTranslationClick: () -> Unit,
    onScrollToLastRead: (() -> Unit)?,
    onScrollToChapter: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember(description) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = description.isNotEmpty(),
                onClick = { expanded = !expanded },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = stringResource(StringsR.string.book_synopsis),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (onScrollToLastRead != null) {
                    IconButton(onClick = onScrollToLastRead, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkAdded,
                            contentDescription = stringResource(id = R.string.scroll_to_last_read_chapter),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (showTranslateButton) {
                    IconButton(
                        onClick = {
                            if (isTranslated) onClearTranslationClick() else onTranslateClick()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (isTranslated) Icons.Outlined.Close else Icons.Outlined.GTranslate,
                                contentDescription = if (isTranslated) stringResource(R.string.clear_translation) else stringResource(R.string.translate),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onScrollToChapter, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(id = R.string.go_to_chapter),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).size(24.dp),
                )
            }
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result: TextLayoutResult ->
                            if (!expanded) overflows = result.hasVisualOverflow
                        },
                    )
                }
                if (!expanded && overflows) {
                    Text(
                        text = stringResource(StringsR.string.book_read_more),
                        style = MaterialTheme.typography.labelLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun readingStatusLabel(status: String): String = stringResource(
    when (status) {
        "READING" -> StringsR.string.reading_status_reading
        "PLAN_TO_READ" -> StringsR.string.reading_status_plan
        "ON_HOLD" -> StringsR.string.reading_status_on_hold
        "COMPLETED" -> StringsR.string.reading_status_completed
        "DROPPED" -> StringsR.string.reading_status_dropped
        "SPICY" -> StringsR.string.reading_status_spicy
        else -> StringsR.string.reading_status_auto
    }
)

@Composable
private fun MetaLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (accent) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (accent) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
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
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
