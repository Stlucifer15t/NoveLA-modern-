package my.noveldokusha.features.chapterslist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.chapterslist.R
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.ChapterWithContext

@Composable
internal fun GoToChapterDialog(
    chapters: List<ChapterWithContext>,
    // index — позиция в LazyColumn (+1 за header), url — для подсветки
    onChapterSelected: (index: Int, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = NovelaTheme.colors
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = if (query.isBlank()) {
        chapters
    } else {
        chapters.filter { it.chapter.title.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(NovelaRadius.sheet),
            color = colors.surfaceFloating,
            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp)
                .padding(vertical = 24.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.go_to_chapter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = colors.textMuted,
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.search_chapter_hint),
                            color = colors.textMuted,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.surfaceSubtle,
                        unfocusedContainerColor = colors.surfaceSubtle,
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                HorizontalDivider(
                    thickness = NovelaSize.hairline,
                    color = colors.borderSubtle,
                    modifier = Modifier.padding(top = 12.dp),
                )

                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_chapters_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    val chapterIndexByUrl = remember(chapters) {
                        chapters.withIndex().associate { (i, c) -> c.chapter.url to i + 1 }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(
                            items = filtered,
                            key = { it.chapter.url },
                        ) { chapterWithContext ->
                            val realIndex = chapterIndexByUrl[chapterWithContext.chapter.url] ?: 0
                            val isLastRead = chapterWithContext.lastReadChapter
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChapterSelected(realIndex, chapterWithContext.chapter.url)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                            ) {
                                if (!chapterWithContext.chapter.read) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(colors.accent, CircleShape)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    text = chapterWithContext.chapter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isLastRead) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        isLastRead -> colors.accent
                                        chapterWithContext.chapter.read -> colors.textMuted
                                        else -> colors.textPrimary
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isLastRead) {
                                    Spacer(Modifier.width(8.dp))
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
                            }
                            HorizontalDivider(
                                thickness = NovelaSize.hairline,
                                color = colors.borderSubtle,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
