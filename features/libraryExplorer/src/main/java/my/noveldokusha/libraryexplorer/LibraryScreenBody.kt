package my.noveldokusha.libraryexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.domain.LibraryCategory
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.feature.local_database.BookWithContext

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
internal fun LibraryScreenBody(
    innerPadding: PaddingValues,
    topAppBarState: TopAppBarState? = null,
    onBookClick: (BookWithContext) -> Unit,
    onBookLongClick: (BookWithContext) -> Unit,
    gridColumns: Int = 3,
    sourceStripPosition: SourceStripPosition = SourceStripPosition.BelowCover,
    selectedBooks: Map<String, Boolean> = emptyMap(),
    isSelectionMode: Boolean = false,
    pendingRemoval: Set<String> = emptySet(),
    showCategories: Boolean = false,
    customCategories: List<String> = emptyList(),
    onCreateCategory: () -> Unit = {},
    onDeleteCategory: (String) -> Unit = {},
    onImportEpub: (() -> Unit)? = null,
    viewModel: LibraryPageViewModel = viewModel(),
) {
    val colors = NovelaTheme.colors
    val pullToRefreshState = rememberPullToRefreshState()
    val gridState = rememberLazyGridState()
    var showDeleteCategoryPicker by remember { mutableStateOf(false) }
    var showDeleteCategoryConfirm by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = viewModel.isUpdating,
        onRefresh = {
            viewModel.onLibraryCategoryRefresh(LibraryCategory.DEFAULT)
        },
        state = pullToRefreshState,
        modifier = Modifier.padding(innerPadding),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showCategories,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
                val catCounts by viewModel.categoryCounts
                val allCategories = buildList {
                    add("" to stringResource(R.string.reading))
                    add("Completed" to stringResource(R.string.completed))
                    customCategories.sorted().forEach { cat ->
                        add(cat to cat)
                    }
                }
                val totalCount = catCounts.values.sum()

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(NovelaSpacing.xs + 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovelaSpacing.screenH, vertical = NovelaSpacing.sm),
                ) {
                    NovelaFilterChip(
                        label = stringResource(R.string.all_categories),
                        count = totalCount,
                        selected = selectedCategories.isEmpty(),
                        onClick = { viewModel.clearCategoryFilters() },
                    )
                    allCategories.forEach { (category, label) ->
                        val count = catCounts[category] ?: 0
                        val isSelected = category in selectedCategories
                        NovelaFilterChip(
                            label = label,
                            count = count,
                            selected = isSelected,
                            onClick = { viewModel.toggleCategory(category) },
                        )
                    }
                    NovelaFilterChip(
                        label = stringResource(R.string.create_category),
                        selected = false,
                        onClick = onCreateCategory,
                        leadingIcon = Icons.Rounded.Add,
                    )
                    if (customCategories.isNotEmpty()) {
                        NovelaFilterChip(
                            label = stringResource(R.string.delete_category),
                            selected = false,
                            onClick = { showDeleteCategoryPicker = true },
                            leadingIcon = Icons.Outlined.DeleteOutline,
                        )
                    }
                }
            }

            val list by viewModel.filteredList
            val sources by viewModel.luaSources.collectAsStateWithLifecycle()
            val translatedTitles by viewModel.translatedTitles.collectAsStateWithLifecycle()

            if (viewModel.isLibraryLoaded) {
                LibraryPageBody(
                    list = list,
                    onClick = onBookClick,
                    onLongClick = onBookLongClick,
                    getSourceName = remember(sources) { { viewModel.getSourceName(it) } },
                    gridColumns = gridColumns,
                    sourceStripPosition = sourceStripPosition,
                    selectedBooks = selectedBooks,
                    isSelectionMode = isSelectionMode,
                    pendingRemoval = pendingRemoval,
                    gridState = gridState,
                    translatedTitles = translatedTitles,
                    onImportEpub = onImportEpub,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
        }
    }

    if (showDeleteCategoryPicker && customCategories.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteCategoryPicker = false },
            shape = RoundedCornerShape(NovelaRadius.sheet),
            title = {
                Text(
                    text = stringResource(R.string.delete_category),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = colors.borderSubtle)
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(customCategories) { category ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.danger,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        tint = colors.danger,
                                    )
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(NovelaRadius.sm))
                                    .clickable {
                                        showDeleteCategoryPicker = false
                                        showDeleteCategoryConfirm = category
                                    },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeleteCategoryPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    showDeleteCategoryConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirm = null },
            shape = RoundedCornerShape(NovelaRadius.sheet),
            title = {
                Text(
                    text = stringResource(R.string.delete_category),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.category_delete_confirm, category),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category)
                        showDeleteCategoryConfirm = null
                    },
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = colors.danger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCategoryConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
