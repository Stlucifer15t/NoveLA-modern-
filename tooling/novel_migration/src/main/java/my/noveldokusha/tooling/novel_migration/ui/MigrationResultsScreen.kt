package my.noveldokusha.tooling.novel_migration.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.strings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationResultsScreen(
    title: String,
    migratingBooks: List<MigratingBook>,
    isSearchingAll: Boolean,
    isMigrating: Boolean,
    isComplete: Boolean,
    migrateProgress: Int,
    migrateTotal: Int,
    successCount: Int,
    skipCount: Int,
    failCount: Int,
    chaptersMatched: Int = 0,
    chaptersTotal: Int = 0,
    onPressBack: () -> Unit,
    onSkip: (Int) -> Unit,
    onMigrateAll: () -> Unit,
    onSelectResult: ((ScoredSearchResult) -> Unit)? = null,
    onSelectAlternative: ((bookIndex: Int, resultIndex: Int) -> Unit)? = null,
    onReturnToLibrary: (() -> Unit)? = null,
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
                            text = title,
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
            if (isSearchingAll) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.accentSoft,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.migration_searching),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        val searchingIndex = migratingBooks.indexOfFirst { it.isSearching }
                        if (searchingIndex >= 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.migration_searching_progress,
                                    searchingIndex + 1,
                                    migratingBooks.size,
                                    migratingBooks[searchingIndex].book.title,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            } else if (isComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.sheet),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accentSoft,
                                modifier = Modifier.size(68.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp),
                                        tint = colors.accent,
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.migration_complete),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                            if (chaptersTotal > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.migration_chapters_matched,
                                        chaptersMatched,
                                        chaptersTotal,
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.accent,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.migration_success,
                                    successCount,
                                    skipCount,
                                    failCount,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(24.dp))
                            NovelaPrimaryButton(
                                text = stringResource(R.string.migration_return_to_library),
                                onClick = onReturnToLibrary ?: onPressBack,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(NovelaSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        if (isMigrating) {
                            LinearProgressIndicator(
                                progress = {
                                    if (migrateTotal > 0) migrateProgress.toFloat() / migrateTotal else 0f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(NovelaRadius.pill)),
                                color = colors.accent,
                                trackColor = colors.accentSoft,
                                gapSize = 0.dp,
                                drawStopIndicator = {},
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.migration_progress, migrateProgress, migrateTotal),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    itemsIndexed(migratingBooks) { index, mb ->
                        MigrationResultItem(
                            migratingBook = mb,
                            bookIndex = index,
                            onSkip = { onSkip(index) },
                            onClick = onSelectResult?.let { { mb.result?.let(it) } },
                            onSelectAlternative = onSelectAlternative,
                        )
                    }
                }

                if (!isMigrating) {
                    val canMigrate = migratingBooks.any { it.result != null && !it.isSkipped && !it.isDone }
                    HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = NovelaSpacing.lg, vertical = 12.dp),
                    ) {
                        NovelaPrimaryButton(
                            text = stringResource(R.string.migration_migrate_all),
                            onClick = onMigrateAll,
                            enabled = canMigrate,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrationResultItem(
    migratingBook: MigratingBook,
    bookIndex: Int,
    onSkip: () -> Unit,
    onClick: (() -> Unit)? = null,
    onSelectAlternative: ((bookIndex: Int, resultIndex: Int) -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick ?: {},
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = migratingBook.book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            val errorMsg = migratingBook.error
            if (migratingBook.isDone && migratingBook.isSkipped) {
                Icon(Icons.Default.Close, contentDescription = null, tint = colors.danger)
            } else if (migratingBook.isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.success)
            } else if (migratingBook.result != null && errorMsg != null) {
                Icon(Icons.Default.Close, contentDescription = null, tint = colors.danger)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.danger,
                    )
                }
            } else if (migratingBook.result != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = migratingBook.result!!.book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = migratingBook.result!!.source.resolveName(context),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                    )
                    Text(
                        text = stringResource(
                            R.string.migration_chapters_count,
                            migratingBook.result!!.chapters.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = colors.textMuted,
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.migration_skip)) },
                            onClick = {
                                showMenu = false
                                onSkip()
                            },
                        )
                        if (onSelectAlternative != null) {
                            migratingBook.results.forEachIndexed { idx, alt ->
                                if (idx != migratingBook.selectedIndex) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    R.string.migration_source_with_chapters,
                                                    alt.source.resolveName(context),
                                                    alt.chapters.size,
                                                ),
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onSelectAlternative(bookIndex, idx)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Icon(Icons.Default.Close, contentDescription = null, tint = colors.danger)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.migration_no_match),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
            }
        }
    }
}
