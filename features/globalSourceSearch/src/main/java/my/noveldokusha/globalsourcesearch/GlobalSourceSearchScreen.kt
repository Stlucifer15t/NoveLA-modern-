package my.noveldokusha.globalsourcesearch

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.AnimatedTransition
import my.noveldokusha.coreui.components.LibraryBadgeMaps
import my.noveldokusha.coreui.components.LibraryBadgeState
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.ToolbarMode
import my.noveldokusha.coreui.components.TopAppBarSearch
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun GlobalSourceSearchScreen(
    searchInput: String,
    listSources: List<SourceResults>,
    onSearchInputChange: (String) -> Unit,
    onSearchInputSubmit: (String) -> Unit,
    onBookClick: (book: BookMetadata) -> Unit,
    onPressBack: () -> Unit,
    getLibraryBadge: (String, String) -> LibraryBadgeState? = { _, _ -> null },
    libraryBadgeData: State<LibraryBadgeMaps> = mutableStateOf(LibraryBadgeMaps()),
    selectedScope: String = "",
    onScopeChange: (String) -> Unit = {},
) {
    val colors = NovelaTheme.colors
    val focusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null,
    )

    var toolbarMode by remember {
        mutableStateOf(if (searchInput.isNotEmpty()) ToolbarMode.SEARCH else ToolbarMode.MAIN)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                AnimatedTransition(targetState = toolbarMode) { target ->
                    when (target) {
                        ToolbarMode.MAIN -> {
                            TopAppBar(
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                                ),
                                title = {
                                    Text(
                                        text = stringResource(R.string.global_search),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textPrimary,
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
                                actions = {
                                    IconButton(onClick = { toolbarMode = ToolbarMode.SEARCH }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = stringResource(R.string.global_search),
                                            tint = colors.textPrimary,
                                        )
                                    }
                                },
                            )
                        }

                        ToolbarMode.SEARCH -> {
                            TopAppBarSearch(
                                focusRequester = focusRequester,
                                searchTextInput = searchInput,
                                onSearchTextChange = onSearchInputChange,
                                onTextDone = onSearchInputSubmit,
                                onClose = {
                                    if (searchInput.isEmpty()) {
                                        toolbarMode = ToolbarMode.MAIN
                                    } else {
                                        onPressBack()
                                    }
                                },
                                placeholderText = stringResource(R.string.global_search),
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = NovelaSpacing.lg, vertical = 8.dp),
                ) {
                    val scopeOptions = listOf(
                        "" to stringResource(StringsR.string.all_categories),
                        "novel" to stringResource(StringsR.string.content_type_novel),
                        "manga" to stringResource(StringsR.string.content_type_manga),
                        "video" to stringResource(StringsR.string.content_type_video),
                    )
                    scopeOptions.forEach { (value, label) ->
                        NovelaFilterChip(
                            label = label,
                            selected = selectedScope == value,
                            onClick = { onScopeChange(value) },
                        )
                    }
                }

                val progress by remember {
                    derivedStateOf {
                        val totalCount = listSources.size.coerceAtLeast(1)
                        val finishedCount = listSources.count { it.fetchIterator.hasFinished }
                        finishedCount.toFloat() / totalCount.toFloat()
                    }
                }
                if (progress < 1f && listSources.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = colors.accent,
                        trackColor = colors.accentSoft,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                } else {
                    HorizontalDivider(
                        thickness = NovelaSize.hairline,
                        color = colors.borderSubtle,
                    )
                }
            }
        },
        content = { innerPadding ->
            GlobalSourceSearchScreenBody(
                listSources = listSources,
                contentPadding = innerPadding,
                onBookClick = onBookClick,
                getLibraryBadge = getLibraryBadge,
                libraryBadgeData = libraryBadgeData,
            )
        },
    )
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        GlobalSourceSearchScreen(
            searchInput = "Some text here",
            listSources = listOf(),
            onSearchInputChange = { },
            onSearchInputSubmit = { },
            onBookClick = { },
            onPressBack = { },
        )
    }
}
