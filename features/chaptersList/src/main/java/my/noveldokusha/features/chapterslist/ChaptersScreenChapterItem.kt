package my.noveldokusha.features.chapterslist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import my.noveldokusha.chapterslist.R
import my.noveldokusha.coreui.components.AnimatedTransition
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.feature.local_database.ChapterWithContext
import my.noveldokusha.feature.local_database.tables.Chapter
import my.noveldokusha.features.reader.video.ChapterDownloadUi
import my.noveldokusha.features.reader.video.ChapterDownloadUiState

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun ChaptersScreenChapterItem(
    chapterWithContext: ChapterWithContext,
    translatedTitle: String? = null,
    chapterSize: ChapterSize? = null,
    downloadUi: ChapterDownloadUi? = null,
    selected: Boolean,
    isLocalSource: Boolean,
    // Кнопка отмены (X) показывается только у видео-книг: у текста/манги
    // отмена снимает всю задачу книги, а не одну главу (см. onCancelChapterDownload).
    showCancelAction: Boolean,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val chapter = chapterWithContext.chapter
    val isLastRead = chapterWithContext.lastReadChapter
    val sizeLabel = chapterSize?.sizeBytes?.let { formatBytes(it) }

    val targetContainerColor = when {
        selected -> colors.accentSoft
        highlighted -> colors.accentSoft
        isLastRead -> colors.accentSoft.copy(alpha = if (colors.isDark) 0.45f else 0.55f)
        else -> colors.surfaceCard
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 200),
        label = "chapterItemBackground",
    )

    val borderColor = when {
        selected || highlighted -> colors.accent
        isLastRead -> colors.accentLine
        else -> colors.borderSubtle
    }

    val stableOnClick = remember(onClick) { onClick }
    val stableOnLongClick = remember(onLongClick) { onLongClick }
    val stableOnDownload = remember(onDownload) { onDownload }
    val stableOnPauseDownload = remember(onPauseDownload) { onPauseDownload }
    val stableOnResumeDownload = remember(onResumeDownload) { onResumeDownload }
    val stableOnCancelDownload = remember(onCancelDownload) { onCancelDownload }

    Surface(
        shape = RoundedCornerShape(NovelaRadius.md),
        color = containerColor,
        border = BorderStroke(
            width = if (selected || highlighted) 1.25.dp else NovelaSize.hairline,
            color = borderColor,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.lg, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NovelaRadius.md))
                .combinedClickable(
                    onClick = stableOnClick,
                    onLongClick = stableOnLongClick,
                )
                .padding(start = 14.dp, end = if (isLocalSource) 14.dp else 4.dp, top = 11.dp, bottom = 11.dp),
        ) {
            // Leading state marker (selection check, last-read bar, or unread dot)
            when {
                selected -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                isLastRead -> {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(NovelaRadius.pill))
                            .background(colors.accent)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                !chapter.read -> {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colors.accent, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = translatedTitle ?: chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = when {
                        isLastRead -> FontWeight.SemiBold
                        !chapter.read -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    color = when {
                        isLastRead -> colors.accent
                        chapter.read -> colors.textMuted
                        else -> colors.textPrimary
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isLastRead || chapter.read || sizeLabel != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when {
                            isLastRead -> {
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.accentSoft,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.last_read),
                                        style = NovelaType.badge,
                                        color = colors.accent,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            chapter.read -> {
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.surfaceSubtle,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.read),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp),
                                    )
                                }
                            }
                        }

                        if (sizeLabel != null) {
                            Text(
                                text = sizeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            }

            if (!isLocalSource) {
                Spacer(Modifier.width(4.dp))
                val uiState = when {
                    downloadUi == null || downloadUi.state == ChapterDownloadUiState.NONE ->
                        if (chapterWithContext.downloaded) ChapterDownloadUiState.COMPLETED
                        else ChapterDownloadUiState.NONE
                    else -> downloadUi.state
                }
                AnimatedTransition(targetState = uiState) { state ->
                    when (state) {
                        ChapterDownloadUiState.NONE -> IconButton(onClick = stableOnDownload) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                stringResource(id = R.string.download),
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        ChapterDownloadUiState.COMPLETED -> IconButton(onClick = stableOnDownload) {
                            Icon(
                                Icons.Filled.CloudDone,
                                stringResource(id = R.string.download),
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        ChapterDownloadUiState.QUEUED -> Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showCancelAction) {
                                DownloadCancelButton(onCancelDownload = stableOnCancelDownload)
                            }
                            IconButton(onClick = stableOnPauseDownload) {
                                val pulse by rememberInfiniteTransition(label = "queuedPulse").animateFloat(
                                    initialValue = 0.35f,
                                    targetValue = 0.90f,
                                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                    label = "queuedPulseAlpha",
                                )
                                Icon(
                                    Icons.Outlined.CloudDownload,
                                    stringResource(id = R.string.download_pause),
                                    tint = colors.accent.copy(alpha = pulse),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        ChapterDownloadUiState.DOWNLOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showCancelAction) {
                                DownloadCancelButton(onCancelDownload = stableOnCancelDownload)
                            }
                            IconButton(onClick = stableOnPauseDownload) {
                                DownloadRing(
                                    progress = downloadUi?.progress,
                                    description = stringResource(id = R.string.download_pause),
                                )
                            }
                        }

                        ChapterDownloadUiState.PAUSED -> Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showCancelAction) {
                                DownloadCancelButton(onCancelDownload = stableOnCancelDownload)
                            }
                            IconButton(onClick = stableOnResumeDownload) {
                                val description = stringResource(id = R.string.download_resume)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clearAndSetSemantics { contentDescription = description },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val pausedProgress = downloadUi?.progress
                                    if (pausedProgress != null) {
                                        CircularProgressIndicator(
                                            progress = { pausedProgress },
                                            modifier = Modifier.size(36.dp),
                                            strokeWidth = 2.5.dp,
                                            color = colors.textSecondary,
                                            trackColor = colors.surfaceSubtle,
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .size(36.dp)
                                                .border(2.dp, colors.borderStrong, CircleShape)
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.Pause,
                                        null,
                                        modifier = Modifier.size(15.dp),
                                        tint = colors.textPrimary,
                                    )
                                }
                            }
                        }

                        ChapterDownloadUiState.FAILED -> IconButton(onClick = stableOnDownload) {
                            Icon(
                                Icons.Outlined.CloudOff,
                                stringResource(id = R.string.download),
                                tint = colors.danger,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadCancelButton(onCancelDownload: () -> Unit) {
    val colors = NovelaTheme.colors
    IconButton(onClick = onCancelDownload) {
        Icon(
            Icons.Outlined.Close,
            stringResource(id = R.string.download_cancel),
            modifier = Modifier.size(18.dp),
            tint = colors.textMuted,
        )
    }
}

@Composable
private fun DownloadRing(progress: Float?, description: String) {
    val colors = NovelaTheme.colors
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(300),
        label = "chapterDownloadProgress",
    )
    val isSpinning = progress == null || progress < 0.03f
    Box(
        modifier = Modifier
            .size(38.dp)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (isSpinning) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 2.5.dp,
                color = colors.accent,
            )
        } else {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(36.dp),
                strokeWidth = 2.5.dp,
                color = colors.accent,
                trackColor = colors.accentSoft,
            )
        }
        if (progress == null) {
            Icon(
                Icons.Outlined.CloudDownload,
                null,
                modifier = Modifier.size(15.dp),
                tint = colors.accent,
            )
        } else {
            Text(
                text = "${(animatedProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textPrimary,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView(
    @PreviewParameter(PreviewProvider::class) previewProviderState: PreviewProviderState,
) {
    InternalTheme {
        ChaptersScreenChapterItem(
            chapterWithContext = previewProviderState.chapterWithContext,
            downloadUi = previewProviderState.downloadUi,
            selected = previewProviderState.selected,
            isLocalSource = false,
            showCancelAction = previewProviderState.showCancelAction,
            onLongClick = {},
            onClick = {},
            onDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancelDownload = {},
        )
    }
}

private data class PreviewProviderState(
    val chapterWithContext: ChapterWithContext,
    val downloadUi: ChapterDownloadUi? = null,
    val selected: Boolean,
    val showCancelAction: Boolean = true,
)

private class PreviewProvider : PreviewParameterProvider<PreviewProviderState> {
    override val values = sequenceOf(
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Chapter 1: Awakening in the Abyss",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 0,
                    read = false,
                ),
                downloaded = false,
                lastReadChapter = false,
            ),
            downloadUi = ChapterDownloadUi(ChapterDownloadUiState.DOWNLOADING, progress = 0.42f),
            selected = false,
        ),
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Chapter 2",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 1,
                    read = false,
                ),
                downloaded = false,
                lastReadChapter = false,
            ),
            downloadUi = ChapterDownloadUi(ChapterDownloadUiState.DOWNLOADING, progress = 0f),
            selected = false,
        ),
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Chapter 3: A Very Long Chapter Title Spanning Multiple Lines in the Directory",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 0,
                    read = true,
                ),
                downloaded = true,
                lastReadChapter = false,
            ),
            downloadUi = ChapterDownloadUi(ChapterDownloadUiState.PAUSED, progress = 0.75f),
            selected = false,
            showCancelAction = false,
        ),
        PreviewProviderState(
            chapterWithContext = ChapterWithContext(
                chapter = Chapter(
                    title = "Chapter 4: Last Read Milestone",
                    url = "url",
                    bookUrl = "bookUrl",
                    lastReadOffset = 0,
                    lastReadPosition = 0,
                    position = 0,
                    read = false,
                ),
                downloaded = true,
                lastReadChapter = true,
            ),
            selected = true,
        ),
    )
}
