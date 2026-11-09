package my.noveldokusha.tooling.novel_migration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.components.BookImageButtonView
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.tables.Book
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.displayName
import my.noveldokusha.strings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationBookPickerScreen(
    sourceName: String,
    books: List<Book>,
    bookChapterCounts: Map<String, Int>,
    selectedBooks: Set<String>,
    targetSource: SourceInterface.Catalog?,
    targetSources: List<SourceInterface.Catalog>,
    onPressBack: () -> Unit,
    onToggleSelection: (Book) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSetTargetSource: (SourceInterface.Catalog?) -> Unit,
    onStartSearch: () -> Unit,
) {
    val colors = NovelaTheme.colors

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    title = {
                        Text(
                            text = stringResource(R.string.migration_select_books, sourceName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onPressBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                tint = colors.textPrimary,
                            )
                        }
                    },
                )
                HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (targetSources.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.migration_migrate_to).uppercase(),
                    style = NovelaType.sectionEyebrow,
                    color = colors.accent,
                    modifier = Modifier.padding(start = NovelaSpacing.lg, top = 12.dp, bottom = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = NovelaSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NovelaFilterChip(
                        label = stringResource(R.string.all_sources),
                        selected = targetSource == null,
                        onClick = { onSetTargetSource(null) },
                    )
                    targetSources.forEach { source ->
                        NovelaFilterChip(
                            label = source.displayName(),
                            selected = targetSource == source,
                            onClick = { onSetTargetSource(source) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovelaSpacing.lg, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.migration_books_count, books.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onSelectAll,
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = colors.accentSoft,
                    ) {
                        Text(
                            text = stringResource(R.string.select_all),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    if (selectedBooks.isNotEmpty()) {
                        Surface(
                            onClick = onClearSelection,
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceSubtle,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        ) {
                            Text(
                                text = stringResource(R.string.migration_clear_selection),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = NovelaSpacing.lg, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(books, key = { it.url }) { book ->
                    val isSelected = book.url in selectedBooks
                    val chapterCount = bookChapterCounts[book.url] ?: 0
                    BookImageButtonView(
                        title = book.title,
                        coverImageModel = rememberResolvedBookImagePath(
                            bookUrl = book.url,
                            imagePath = book.coverImageUrl,
                        ),
                        selected = isSelected,
                        onClick = { onToggleSelection(book) },
                        onLongClick = {},
                        modifier = Modifier.width(108.dp),
                        sourceStripPosition = SourceStripPosition.InfoPanel,
                        topLeftBadge = if (chapterCount > 0) {
                            {
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.badge),
                                    color = colors.coverBadgeBg,
                                    border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
                                ) {
                                    Text(
                                        text = "$chapterCount",
                                        color = Color.White,
                                        style = NovelaType.badge,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        } else null,
                        topRightBadge = if (isSelected) {
                            {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.accent,
                                    border = BorderStroke(1.5.dp, Color.White),
                                    modifier = Modifier.size(22.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.onAccent,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        } else null,
                    )
                }
            }

            HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = NovelaSpacing.lg, vertical = 12.dp),
            ) {
                NovelaPrimaryButton(
                    text = stringResource(R.string.migration_search_matches),
                    onClick = onStartSearch,
                    enabled = selectedBooks.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
