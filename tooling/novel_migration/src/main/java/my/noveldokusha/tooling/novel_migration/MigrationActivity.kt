package my.noveldokusha.tooling.novel_migration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import my.noveldokusha.core.appPreferences.SourceStripPosition
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.core.utils.Extra_Boolean
import my.noveldokusha.core.utils.Extra_String
import my.noveldokusha.core.utils.Extra_StringNullable
import my.noveldokusha.coreui.BaseActivity
import my.noveldokusha.coreui.components.BookImageButtonView
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.Theme
import my.noveldokusha.data.ScraperRepository
import my.noveldokusha.feature.local_database.tables.Book
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.strings.R
import my.noveldokusha.tooling.novel_migration.data.MigrationOptions
import my.noveldokusha.tooling.novel_migration.ui.MassMigrationViewModel
import my.noveldokusha.tooling.novel_migration.ui.MigratingBook
import my.noveldokusha.tooling.novel_migration.ui.MigrationBookPickerScreen
import my.noveldokusha.tooling.novel_migration.ui.MigrationHistoryScreen
import my.noveldokusha.tooling.novel_migration.ui.MigrationHistoryViewModel
import my.noveldokusha.tooling.novel_migration.ui.MigrationOptionsDialog
import my.noveldokusha.tooling.novel_migration.ui.MigrationResultsScreen
import my.noveldokusha.tooling.novel_migration.ui.MigrationUiState
import my.noveldokusha.tooling.novel_migration.ui.MigrationViewModel

private enum class MigrationStep {
    BOOK_PICKER,
    RESULTS,
}

@AndroidEntryPoint
class MigrationActivity : BaseActivity() {

    class IntentData : Intent {
        var bookUrl by Extra_String()
        var bookTitle by Extra_String()
        var massMigrationSourceUrl by Extra_StringNullable()
        var massMigrationUnknownSource by Extra_Boolean()
        var showHistory by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookUrl: String, bookTitle: String) : super(ctx, MigrationActivity::class.java) {
            this.bookUrl = bookUrl
            this.bookTitle = bookTitle
        }
    }

    @Inject
    lateinit var scraperRepository: ScraperRepository

    @Inject
    lateinit var navigationRoutes: NavigationRoutes

    private val singleViewModel: MigrationViewModel by viewModels()
    private val massViewModel: MassMigrationViewModel by viewModels()
    private val historyViewModel: MigrationHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentData = IntentData(intent)

        val isMassMigration = intentData.massMigrationSourceUrl?.isNotEmpty() == true ||
            intentData.massMigrationUnknownSource
        val isHistory = intentData.showHistory

        if (isHistory) {
            showHistoryScreen(intentData)
            return
        }

        if (isMassMigration) {
            initMassMigration(intentData)
        } else {
            singleViewModel.init(intentData.bookUrl, intentData.bookTitle)
            initSingleMigration(intentData)
        }
    }

    private fun navigateToLibrary() {
        startActivity(
            navigationRoutes.main(this).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
        finish()
    }

    private fun showHistoryScreen(@Suppress("UNUSED_PARAMETER") intentData: IntentData) {
        setContent {
            Theme(themeProvider = themeProvider) {
                val groups by historyViewModel.groups.collectAsStateWithLifecycle()
                MigrationHistoryScreen(
                    groups = groups,
                    onPressBack = { finish() },
                    onDeleteGroup = { oldSourceId, newSourceId ->
                        historyViewModel.deleteGroup(oldSourceId, newSourceId)
                    },
                )
            }
        }
    }

    private fun initSingleMigration(@Suppress("UNUSED_PARAMETER") intentData: IntentData) {
        setContent {
            var showOptions by remember { mutableStateOf(false) }

            Theme(themeProvider = themeProvider) {
                val state by singleViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    if (!state.isSearching && state.searchResults.isEmpty() && state.error == null && !state.isComplete && state.selectedResult == null) {
                        singleViewModel.searchAllSources()
                    }
                }

                when {
                    state.isComplete -> {
                        MigrationResultsScreen(
                            title = state.bookTitle,
                            migratingBooks = emptyList(),
                            isSearchingAll = false,
                            isMigrating = false,
                            isComplete = true,
                            migrateProgress = state.chaptersTotal,
                            migrateTotal = state.chaptersTotal,
                            successCount = if (state.migrationSuccess) 1 else 0,
                            skipCount = 0,
                            failCount = if (state.migrationSuccess) 0 else 1,
                            chaptersMatched = state.chaptersMatched,
                            chaptersTotal = state.chaptersTotal,
                            onPressBack = { navigateToLibrary() },
                            onSkip = { navigateToLibrary() },
                            onMigrateAll = {},
                            onReturnToLibrary = { navigateToLibrary() },
                        )
                    }
                    state.searchResults.isNotEmpty() && state.selectedResult == null -> {
                        PickerScreen(
                            state = state,
                            onResultPicked = { index -> singleViewModel.onResultPicked(index) },
                            onPressBack = { finish() },
                        )
                    }
                    else -> {
                        val migratingBooks = if (state.isSearching || state.searchResults.isNotEmpty() || state.isMatching || state.isMigrating) {
                            listOf(
                                MigratingBook(
                                    book = Book(title = state.bookTitle, url = state.bookUrl),
                                    result = state.selectedResult,
                                    isSearching = state.isSearching || state.isMatching,
                                    isDone = state.isComplete,
                                    isSkipped = state.error != null && state.searchResults.isEmpty(),
                                    error = state.error,
                                ),
                            )
                        } else {
                            emptyList()
                        }

                        MigrationResultsScreen(
                            title = state.bookTitle,
                            migratingBooks = migratingBooks,
                            isSearchingAll = state.isSearching,
                            isMigrating = state.isMigrating,
                            isComplete = state.isComplete,
                            migrateProgress = if (state.isMigrating) 0 else 1,
                            migrateTotal = 1,
                            successCount = 0,
                            skipCount = 0,
                            failCount = 0,
                            onPressBack = { finish() },
                            onSkip = { finish() },
                            onMigrateAll = { showOptions = true },
                        )
                    }
                }

                if (showOptions) {
                    MigrationOptionsDialog(
                        initialOptions = MigrationOptions(),
                        onConfirm = { options ->
                            singleViewModel.startMigration(options)
                            showOptions = false
                        },
                        onDismiss = { showOptions = false },
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PickerScreen(
        state: MigrationUiState,
        onResultPicked: (Int) -> Unit,
        onPressBack: () -> Unit,
    ) {
        val colors = NovelaTheme.colors
        val groupedResults = remember(state.searchResults) {
            state.searchResults.withIndex()
                .groupBy({ (_, r) -> r.source }, { (idx, r) -> idx to r })
        }

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
                                text = state.bookTitle,
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
            if (state.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.accent)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.migration_searching),
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (state.error != null) {
                        item {
                            Text(
                                text = state.error,
                                color = colors.danger,
                                modifier = Modifier.padding(horizontal = NovelaSpacing.lg),
                            )
                        }
                    }

                    groupedResults.forEach { (source, indexedResults) ->
                        item {
                            Column {
                                NovelaSectionHeader(
                                    title = source.resolveName(this@MigrationActivity),
                                    badgeText = indexedResults.size.toString(),
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(
                                        start = NovelaSpacing.lg,
                                        end = NovelaSpacing.xl,
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(indexedResults) { (flatIndex, result) ->
                                        BookImageButtonView(
                                            title = result.book.title,
                                            coverImageModel = rememberResolvedBookImagePath(
                                                bookUrl = result.book.url,
                                                imagePath = result.book.coverImageUrl ?: "",
                                            ),
                                            onClick = { onResultPicked(flatIndex) },
                                            onLongClick = {},
                                            modifier = Modifier.width(120.dp),
                                            sourceStripPosition = SourceStripPosition.InfoPanel,
                                            topLeftBadge = {
                                                if (state.chaptersLoadingIndex == flatIndex) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = colors.accent,
                                                    )
                                                } else if (result.isLoaded) {
                                                    Surface(
                                                        shape = RoundedCornerShape(NovelaRadius.badge),
                                                        color = colors.coverBadgeBg,
                                                        border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
                                                    ) {
                                                        Text(
                                                            text = "${result.chapterCount}",
                                                            color = Color.White,
                                                            style = NovelaType.badge,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initMassMigration(intentData: IntentData) {
        val sourceUrl = intentData.massMigrationSourceUrl
        val isUnknownSource = intentData.massMigrationUnknownSource

        setContent {
            var step by remember { mutableStateOf(MigrationStep.BOOK_PICKER) }
            var showOptions by remember { mutableStateOf(false) }
            var selectedSource by remember { mutableStateOf<SourceInterface.Catalog?>(null) }
            val massState by massViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(sourceUrl) {
                if (isUnknownSource) {
                    massViewModel.setUnknownSource()
                } else {
                    val source = scraperRepository.scraper.sourcesList
                        .filterIsInstance<SourceInterface.Catalog>()
                        .find { it.baseUrl == sourceUrl }
                    if (source != null) {
                        selectedSource = source
                        massViewModel.setSource(source)
                    }
                }
            }

            val sourceName = selectedSource?.let { it.resolveName(this@MigrationActivity) }
                ?: if (isUnknownSource) stringResource(R.string.migration_unknown_source) else null

            Theme(themeProvider = themeProvider) {
                when (step) {
                    MigrationStep.BOOK_PICKER -> {
                        MigrationBookPickerScreen(
                            sourceName = sourceName ?: stringResource(R.string.migration_select_source),
                            books = massState.books,
                            bookChapterCounts = massState.bookChapterCounts,
                            selectedBooks = massState.selectedBooks,
                            targetSource = massState.targetSource,
                            targetSources = massState.targetSources,
                            onPressBack = { finish() },
                            onToggleSelection = { massViewModel.toggleBookSelection(it) },
                            onSelectAll = { massViewModel.selectAllBooks() },
                            onClearSelection = { massViewModel.clearSelection() },
                            onSetTargetSource = { massViewModel.setTargetSource(it) },
                            onStartSearch = {
                                massViewModel.startSearch()
                                step = MigrationStep.RESULTS
                            },
                        )
                    }

                    MigrationStep.RESULTS -> {
                        MigrationResultsScreen(
                            title = sourceName ?: stringResource(R.string.migration_title),
                            migratingBooks = massState.migratingBooks,
                            isSearchingAll = massState.isSearchingAll,
                            isMigrating = massState.isMigrating,
                            isComplete = massState.isComplete,
                            migrateProgress = massState.migrateProgress,
                            migrateTotal = massState.migrateTotal,
                            successCount = massState.successCount,
                            skipCount = massState.skipCount,
                            failCount = massState.failCount,
                            onPressBack = {
                                if (massState.isComplete) {
                                    navigateToLibrary()
                                } else {
                                    massViewModel.reset()
                                    if (isUnknownSource) {
                                        massViewModel.setUnknownSource()
                                    } else {
                                        selectedSource?.let { massViewModel.setSource(it) }
                                    }
                                    step = MigrationStep.BOOK_PICKER
                                }
                            },
                            onSkip = { index -> massViewModel.skipBook(index) },
                            onMigrateAll = { showOptions = true },
                            onSelectAlternative = { bookIndex, resultIndex ->
                                massViewModel.setResultForBook(bookIndex, resultIndex)
                            },
                            onReturnToLibrary = { navigateToLibrary() },
                        )
                    }
                }

                if (showOptions) {
                    MigrationOptionsDialog(
                        initialOptions = MigrationOptions(),
                        onConfirm = { options ->
                            massViewModel.migrateAll(options)
                            showOptions = false
                        },
                        onDismiss = { showOptions = false },
                    )
                }
            }
        }
    }
}
