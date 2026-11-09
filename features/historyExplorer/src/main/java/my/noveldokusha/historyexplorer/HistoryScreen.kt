package my.noveldokusha.historyexplorer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.core.utils.refererFor
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.components.NovelaScreenHeader
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.navigation.NavigationRouteViewModel
import my.noveldokusha.strings.R as StringsR
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navigationRouteViewModel: NavigationRouteViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
) {
    val colors = NovelaTheme.colors
    val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val lastClickTime = remember { mutableStateOf(0L) }

    val subtitle = (uiState as? HistoryUiState.Content)?.let { content ->
        "${content.items.size} · ${stringResource(StringsR.string.read)}"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NovelaScreenHeader(
                title = stringResource(id = StringsR.string.title_history),
                subtitle = subtitle,
                actions = {
                    if (uiState is HistoryUiState.Content) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(StringsR.string.options_panel),
                                    tint = colors.textPrimary,
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.DeleteSweep,
                                            stringResource(StringsR.string.delete),
                                            tint = colors.danger,
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = stringResource(StringsR.string.clear_all_history),
                                            color = colors.danger,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        historyViewModel.deleteAll()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            when (val state = uiState) {
                is HistoryUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }

                is HistoryUiState.Empty -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = 88.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    NovelaEmptyState(
                        icon = Icons.Outlined.History,
                        title = stringResource(StringsR.string.no_history_yet),
                        message = "",
                    )
                }

                is HistoryUiState.Content -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(
                            start = NovelaSpacing.lg,
                            end = NovelaSpacing.lg,
                            top = NovelaSpacing.sm,
                            bottom = 120.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.items, key = { it.bookUrl }) { item ->
                            HistoryItemCard(
                                item = item,
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    if (now - lastClickTime.value >= 200L) {
                                        lastClickTime.value = now
                                        navigationRouteViewModel.chapters(
                                            context = context,
                                            bookMetadata = BookMetadata(
                                                title = item.bookTitle,
                                                url = item.bookUrl,
                                                coverImageUrl = item.bookCoverUrl,
                                            ),
                                        ).let(context::startActivity)
                                    }
                                },
                                onResumeClick = item.lastReadChapterUrl?.takeIf { it.isNotBlank() }?.let { chapterUrl ->
                                    {
                                        val now = System.currentTimeMillis()
                                        if (now - lastClickTime.value >= 200L) {
                                            lastClickTime.value = now
                                            navigationRouteViewModel.reader(
                                                context = context,
                                                bookUrl = item.bookUrl,
                                                chapterUrl = chapterUrl,
                                                scrollToSpeakingItem = false,
                                            ).let(context::startActivity)
                                        }
                                    }
                                },
                                onDeleteClick = { historyViewModel.delete(item.bookUrl) },
                            )
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onResumeClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val coverShape = RoundedCornerShape(NovelaRadius.sm)

    Surface(
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.card))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val coverImageModel = rememberResolvedBookImagePath(item.bookUrl, item.bookCoverUrl)
            val referer = (coverImageModel as? String)
                ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                ?.let(::refererFor)

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverImageModel)
                    .apply {
                        if (!referer.isNullOrEmpty()) {
                            httpHeaders(
                                NetworkHeaders.Builder()
                                    .set("Referer", referer)
                                    .build()
                            )
                        }
                    }
                    .build(),
                contentDescription = item.bookTitle,
                modifier = Modifier
                    .size(width = 60.dp, height = 87.dp)
                    .clip(coverShape)
                    .background(colors.surfaceSubtle)
                    .border(NovelaSize.hairline, colors.borderSubtle, coverShape),
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!item.sourceName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.accentSoft,
                        ) {
                            Text(
                                text = item.sourceName,
                                style = NovelaType.badge,
                                color = colors.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = relativeTime(item.lastReadEpochTimeMilli),
                        style = NovelaType.bookCardMeta,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }

                Text(
                    text = item.bookTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!item.lastReadChapterTitle.isNullOrBlank()) {
                    Text(
                        text = item.lastReadChapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (item.totalChapters > 0) {
                    val ratio = (item.readChapters.toFloat() / item.totalChapters.toFloat()).coerceIn(0f, 1f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(NovelaRadius.pill)),
                            color = colors.accent,
                            trackColor = colors.accentSoft,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                        Text(
                            text = "${item.readChapters}/${item.totalChapters}",
                            style = NovelaType.bookCardMeta,
                            color = colors.textMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (onResumeClick != null) {
                    Surface(
                        onClick = onResumeClick,
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accent,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(StringsR.string.read),
                                tint = colors.onAccent,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = stringResource(StringsR.string.read),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onAccent,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(StringsR.string.delete),
                        tint = colors.textMuted,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun relativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> stringResource(StringsR.string.time_just_now)
        minutes < 60 -> stringResource(StringsR.string.time_minutes_ago, minutes)
        hours < 24 -> stringResource(StringsR.string.time_hours_ago, hours)
        days < 7 -> stringResource(StringsR.string.time_days_ago, days)
        days < 30 -> stringResource(StringsR.string.time_weeks_ago, days / 7)
        days < 365 -> stringResource(StringsR.string.time_months_ago, days / 30)
        else -> stringResource(StringsR.string.time_years_ago, days / 365)
    }
}
