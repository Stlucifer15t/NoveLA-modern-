package my.noveldokusha.features.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.reader.R

/** Одна глава в оглавлении: только адрес и название, без выдуманных данных. */
data class TocEntry(val url: String, val title: String)

/**
 * Оглавление (новелла и манга): шит «Table of Contents», сетка 4 в ряд,
 * плитки «N: Название». Текущая глава — чёрная плитка с белым жирным текстом.
 * Номер не дублируется, если уже есть в названии главы.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsSheet(
    entries: List<TocEntry>,
    currentIndex: Int,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
        ) {
            Text(
                text = stringResource(R.string.table_of_contents_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            HorizontalDivider()
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.manga_reader_chapter_list_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@ModalBottomSheet
            }
            val current = currentIndex.coerceIn(0, entries.lastIndex)
            // Стартуем с ряда текущей главы, чтобы она была видна сразу.
            val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (current / 4) * 4)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(entries, key = { _, item -> item.url }) { index, entry ->
                    val isCurrent = index == current
                    val label = if (entry.title.contains(Regex("\\b${index + 1}\\b"))) {
                        entry.title
                    } else {
                        "${index + 1}: ${entry.title}"
                    }
                    Surface(
                        onClick = { onSelect(entry.url) },
                        shape = RoundedCornerShape(12.dp),
                        // Текущая — чёрная плитка с белым жирным текстом (как в макете).
                        color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
