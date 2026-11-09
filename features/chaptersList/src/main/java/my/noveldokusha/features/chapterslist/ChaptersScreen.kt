package my.noveldokusha.features.chapterslist

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PublishedWithChanges
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DoneOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.noveldokusha.chapterslist.R
import my.noveldokusha.core.isLocalUri
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.components.NovelaSecondaryButton
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.isAtTop
import my.noveldokusha.feature.local_database.ChapterWithContext
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.strings.R as StringsR
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ChaptersScreen(
    state: ChaptersScreenState,
    onLibraryToggle: () -> Unit,
    onSearchBookInDatabase: () -> Unit,
    onResumeReading: () -> Unit,
    onPressBack: () -> Unit,
    onSelectedDeleteDownloads: () -> Unit,
    onSelectedDownload: () -> Unit,
    onSelectedTranslate: () -> Unit = {},
    onSelectedDeleteTranslations: () -> Unit = {},
    onSelectedSetRead: () -> Unit,
    onSelectedSetUnread: () -> Unit,
    onSelectedSetReadUpToChapterRead: () -> Unit,
    onSelectedSetReadUpToChapterUnread: () -> Unit,
    onSelectedInvertSelection: () -> Unit,
    onSelectAllChapters: () -> Unit,
    onCloseSelectionBar: () -> Unit,
    onChapterClick: (chapter: ChapterWithContext) -> Unit,
    onChapterLongClick: (chapter: ChapterWithContext) -> Unit,
    onSelectionModeChapterClick: (chapter: ChapterWithContext) -> Unit,
    onSelectionModeChapterLongClick: (chapter: ChapterWithContext) -> Unit,
    onChapterDownload: (chapter: ChapterWithContext) -> Unit,
    onPauseDownload: (chapter: ChapterWithContext) -> Unit,
    onResumeDownload: (chapter: ChapterWithContext) -> Unit,
    onCancelDownload: (chapter: ChapterWithContext) -> Unit,
    onPullRefresh: () -> Unit,
    onCoverLongClick: () -> Unit,
    onChangeCover: () -> Unit,
    onOpenInBrowser: (url: String) -> Unit,
    onGlobalSearchClick: (input: String, contentType: String) -> Unit,
    onDownloadNext100Chapters: () -> Unit,
    onDownloadAllChapters: () -> Unit,
    onExport: (bookUrl: String, bookTitle: String) -> Unit,
    onExportContentChosen: (mode: String, sourceLang: String, targetLang: String) -> Unit,
    onExportDirectorySaved: (uri: String) -> Unit,
    onExportDialogDismiss: () -> Unit,
    exportDialogState: ExportDialogState,
    exportMessage: String?,
    onExportMessageShown: () -> Unit,
    onMigrateBook: () -> Unit = {},
    onDeleteTranslations: () -> Unit = {},
    onFixBook: () -> Unit = {},
    categories: () -> List<String>,
    onUpdateCategory: (String) -> Unit,
    translatedTitle: String?,
    translatedDescription: String?,
    isTranslatingInfo: Boolean,
    showTranslateButton: Boolean,
    onTranslateBookInfo: () -> Unit,
    onClearBookInfoTranslation: () -> Unit,
    scraper: Scraper,
) {
    val colors = NovelaTheme.colors
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val directoryPicker = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeReadPersistent(),
    ) { uri ->
        if (uri == null) {
            if (exportDialogState is ExportDialogState.NeedDirectory) {
                onExportDialogDismiss()
            }
        } else {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                Timber.w("Failed to take persistable permission for $uri: ${e.message}")
            }
            onExportDirectorySaved(uri.toString())
        }
    }

    LaunchedEffect(exportMessage) {
        if (exportMessage != null) {
            snackbarHostState.showSnackbar(exportMessage)
            onExportMessageShown()
        }
    }

    val areSelectedChaptersRead by remember {
        derivedStateOf {
            val readUrls = state.chapters.asSequence()
                .filter { it.chapter.read }
                .map { it.chapter.url }
                .toHashSet()
            state.selectedChaptersUrl.keys.all { it in readUrls }
        }
    }

    if (state.isInSelectionMode.value) {
        BackHandler { onCloseSelectionBar() }
    }

    val isAtTop by lazyListState.isAtTop(threshold = 72.dp)
    val isPastHeroCta by lazyListState.isAtTop(threshold = 240.dp)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val alpha by animateFloatAsState(
                targetValue = if (isAtTop) 0f else 0.96f,
                label = "chaptersBarAlpha",
            )
            val backgroundColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.background.copy(alpha = alpha),
                label = "chaptersBarBg",
            )
            val titleColor by animateColorAsState(
                targetValue = colors.textPrimary.copy(alpha = alpha),
                label = "chaptersBarTitle",
            )

            Box {
                Surface(color = backgroundColor) {
                    Column {
                        TopAppBar(
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                            ),
                            title = {
                                Text(
                                    text = translatedTitle ?: state.book.value.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = titleColor,
                                )
                            },
                            navigationIcon = {
                                TopBarCircleIcon(
                                    isTranslucent = isAtTop,
                                    onClick = onPressBack,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = if (isAtTop) Color.White else colors.textPrimary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            actions = {
                                TopBarCircleIcon(
                                    isTranslucent = isAtTop,
                                    onClick = onLibraryToggle,
                                ) {
                                    Icon(
                                        imageVector = if (state.book.value.inLibrary) {
                                            Icons.Rounded.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = stringResource(R.string.open_the_web_view),
                                        tint = if (state.book.value.inLibrary) {
                                            colors.favoriteCrimson
                                        } else if (isAtTop) {
                                            Color.White
                                        } else {
                                            colors.textPrimary
                                        },
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                TopBarCircleIcon(
                                    isTranslucent = isAtTop,
                                    onClick = { showBottomSheet = !showBottomSheet },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                                        contentDescription = stringResource(R.string.filter),
                                        tint = if (isAtTop) Color.White else colors.accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Box {
                                    TopBarCircleIcon(
                                        isTranslucent = isAtTop,
                                        onClick = { showDropDown = !showDropDown },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(R.string.options_panel),
                                            tint = if (isAtTop) Color.White else colors.textPrimary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showDropDown,
                                        onDismissRequest = { showDropDown = false },
                                    ) {
                                        ChaptersDropDown(
                                            isLocalSource = state.isLocalSource.value,
                                            bookUrl = state.book.value.url,
                                            bookTitle = state.book.value.title,
                                            openInBrowser = {
                                                showDropDown = false
                                                if (!state.book.value.url.isLocalUri) {
                                                    onOpenInBrowser(state.book.value.url)
                                                }
                                            },
                                            onSearchBookInDatabase = {
                                                showDropDown = false
                                                onSearchBookInDatabase()
                                            },
                                            onResumeReading = {
                                                showDropDown = false
                                                onResumeReading()
                                            },
                                            onChangeCover = {
                                                showDropDown = false
                                                onChangeCover()
                                            },
                                            onDownloadNext100Chapters = {
                                                showDropDown = false
                                                onDownloadNext100Chapters()
                                            },
                                            onDownloadAllChapters = {
                                                showDropDown = false
                                                onDownloadAllChapters()
                                            },
                                            onExport = { bUrl, bTitle ->
                                                showDropDown = false
                                                onExport(bUrl, bTitle)
                                            },
                                            onMigrateBook = {
                                                showDropDown = false
                                                onMigrateBook()
                                            },
                                            onDeleteTranslations = {
                                                showDropDown = false
                                                onDeleteTranslations()
                                            },
                                            onFixBook = {
                                                showDropDown = false
                                                onFixBook()
                                            },
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                            },
                        )
                        HorizontalDivider(
                            thickness = NovelaSize.hairline,
                            color = colors.borderSubtle,
                            modifier = Modifier.alpha(alpha),
                        )
                    }
                }

                // Multi-chapter selection top bar
                AnimatedVisibility(
                    visible = state.isInSelectionMode.value,
                    enter = expandVertically(initialHeight = { it / 8 }, expandFrom = Alignment.Top) +
                        fadeIn(animationSpec = tween(120)),
                    exit = shrinkVertically(targetHeight = { it / 8 }, shrinkTowards = Alignment.Top) +
                        fadeOut(animationSpec = tween(120)),
                ) {
                    Surface(color = colors.surfaceFloating) {
                        Column {
                            TopAppBar(
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = colors.surfaceFloating,
                                    scrolledContainerColor = colors.surfaceFloating,
                                ),
                                title = {
                                    Surface(
                                        shape = RoundedCornerShape(NovelaRadius.pill),
                                        color = colors.accentSoft,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                StringsR.string.selected_count,
                                                state.selectedChaptersUrl.size,
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                                .animateContentSize(),
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onCloseSelectionBar) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            stringResource(id = R.string.close_selection_bar),
                                            tint = colors.textPrimary,
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = onSelectAllChapters) {
                                        Icon(
                                            Icons.Outlined.SelectAll,
                                            stringResource(id = R.string.select_all_chapters),
                                            tint = colors.textPrimary,
                                        )
                                    }
                                    IconButton(onClick = onSelectedInvertSelection) {
                                        Icon(
                                            Icons.Outlined.PublishedWithChanges,
                                            stringResource(id = R.string.close_selection_bar),
                                            tint = colors.textPrimary,
                                        )
                                    }
                                },
                            )
                            HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            ChaptersScreenBody(
                state = state,
                lazyListState = lazyListState,
                innerPadding = innerPadding,
                translatedTitle = translatedTitle,
                translatedDescription = translatedDescription,
                isTranslating = isTranslatingInfo,
                showTranslateButton = showTranslateButton,
                onTranslateClick = onTranslateBookInfo,
                onClearTranslationClick = onClearBookInfoTranslation,
                onResumeReading = onResumeReading,
                onLibraryToggle = onLibraryToggle,
                onOpenSortSheet = { showBottomSheet = true },
                onChapterClick = if (state.isInSelectionMode.value) {
                    onSelectionModeChapterClick
                } else {
                    onChapterClick
                },
                onChapterLongClick = if (state.isInSelectionMode.value) {
                    onSelectionModeChapterLongClick
                } else {
                    onChapterLongClick
                },
                onChapterDownload = onChapterDownload,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onCancelDownload = onCancelDownload,
                onPullRefresh = onPullRefresh,
                onCoverLongClick = onCoverLongClick,
                onGlobalSearchClick = onGlobalSearchClick,
                bookCategory = state.book.value.category,
                categories = categories,
                onCategoryClick = { showCategoryPicker = true },
                scraper = scraper,
            )
            Box(Modifier.padding(innerPadding)) {
                InternalLazyColumnScrollbar(state = lazyListState)
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isInSelectionMode.value,
                enter = expandVertically(initialHeight = { it / 8 }) + fadeIn(animationSpec = tween(120)),
                exit = shrinkVertically(targetHeight = { it / 8 }) + fadeOut(animationSpec = tween(120)),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
                    color = colors.glassSurface,
                    border = BorderStroke(NovelaSize.hairline, colors.glassBorder),
                    shadowElevation = 12.dp,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = NovelaSpacing.md, vertical = 8.dp),
                    ) {
                        if (!state.isLocalSource.value) {
                            IconButton(onClick = onSelectedDeleteDownloads) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    stringResource(id = R.string.remove_selected_chapters_downloads),
                                    tint = colors.danger,
                                )
                            }
                            IconButton(onClick = onSelectedDownload) {
                                Icon(
                                    Icons.Outlined.CloudDownload,
                                    stringResource(id = R.string.download_selected_chapters),
                                    tint = colors.accent,
                                )
                            }
                        }
                        IconButton(onClick = onSelectedTranslate) {
                            Icon(
                                Icons.Outlined.Translate,
                                stringResource(id = R.string.translate_selected_chapters),
                                tint = colors.accent,
                            )
                        }
                        if (state.isLocalSource.value) {
                            IconButton(onClick = onSelectedDeleteTranslations) {
                                Icon(
                                    Icons.Outlined.DeleteSweep,
                                    stringResource(id = R.string.delete_selected_chapters_translations),
                                    tint = colors.danger,
                                )
                            }
                        }
                        if (areSelectedChaptersRead) {
                            IconButton(onClick = onSelectedSetUnread) {
                                Icon(
                                    Icons.Rounded.DoneOutline,
                                    stringResource(id = R.string.set_as_not_read_selected_chapters),
                                    tint = colors.textPrimary,
                                )
                            }
                        } else {
                            IconButton(onClick = onSelectedSetRead) {
                                Icon(
                                    Icons.Rounded.Done,
                                    stringResource(id = R.string.set_as_read_selected_chapters),
                                    tint = colors.accent,
                                )
                            }
                        }
                        if (state.selectedChaptersUrl.size <= 1) {
                            if (areSelectedChaptersRead) {
                                IconButton(onClick = onSelectedSetReadUpToChapterUnread) {
                                    Icon(
                                        Icons.Rounded.RemoveDone,
                                        stringResource(id = R.string.set_as_Unread_up_to_selected_chapter),
                                        tint = colors.textPrimary,
                                    )
                                }
                            } else {
                                IconButton(onClick = onSelectedSetReadUpToChapterRead) {
                                    Icon(
                                        Icons.Rounded.DoneAll,
                                        stringResource(id = R.string.set_as_read_up_to_selected_chapter),
                                        tint = colors.accent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isPastHeroCta && !state.isInSelectionMode.value,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(150)),
            ) {
                FloatingActionButton(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                    onClick = onResumeReading,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(id = R.string.open_last_read_chapter),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(id = R.string.resume_reading),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    )

    ChaptersBottomSheet(
        visible = showBottomSheet,
        onDismiss = { showBottomSheet = false },
        state = state,
    )

    if (showCategoryPicker) {
        val categoryList = categories().map { category ->
            val label = when (category) {
                "" -> stringResource(R.string.reading)
                "Completed" -> stringResource(R.string.completed)
                else -> category
            }
            category to label
        }
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            shape = RoundedCornerShape(NovelaRadius.sheet),
            containerColor = colors.surfaceFloating,
            title = {
                Text(
                    text = stringResource(R.string.category_name),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    categoryList.forEach { (category, label) ->
                        val isSelected = state.book.value.category == category
                        NovelaFilterChip(
                            label = label,
                            selected = isSelected,
                            onClick = {
                                onUpdateCategory(category)
                                showCategoryPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryPicker = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }

    when (val ds = exportDialogState) {
        is ExportDialogState.ContentChoice -> {
            var selectedMode by rememberSaveable { mutableStateOf("original") }
            var selectedSourceLang by rememberSaveable { mutableStateOf("") }
            var selectedTargetLang by rememberSaveable { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = onExportDialogDismiss,
                shape = RoundedCornerShape(NovelaRadius.sheet),
                containerColor = colors.surfaceFloating,
                title = {
                    Text(
                        text = stringResource(StringsR.string.export),
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportContentOption(
                            text = stringResource(StringsR.string.export_original),
                            selected = selectedMode == "original",
                            onClick = {
                                selectedMode = "original"
                                selectedSourceLang = ""
                                selectedTargetLang = ""
                            },
                        )

                        ds.availableTranslations.forEach { pair ->
                            val modeKey = "translation_${pair.sourceLang}_${pair.targetLang}"
                            val isSelected = selectedMode == modeKey
                            ExportContentOption(
                                text = stringResource(
                                    StringsR.string.export_translation,
                                    pair.sourceLang,
                                    pair.targetLang,
                                ),
                                selected = isSelected,
                                onClick = {
                                    selectedMode = modeKey
                                    selectedSourceLang = pair.sourceLang
                                    selectedTargetLang = pair.targetLang
                                },
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.md),
                            color = colors.surfaceSubtle,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Text(
                                    text = stringResource(
                                        StringsR.string.export_folder,
                                        ds.exportDirectoryName ?: stringResource(StringsR.string.not_set),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { directoryPicker.launch(null) }) {
                                    Text(
                                        text = stringResource(StringsR.string.export_change_folder),
                                        color = colors.accent,
                                    )
                                }
                            }
                        }

                        val (shownDownloaded, shownTotal) = if (selectedMode == "original") {
                            ds.downloadedChapters to ds.totalChapters
                        } else {
                            val pair = ds.availableTranslations.firstOrNull {
                                it.sourceLang == selectedSourceLang &&
                                    it.targetLang == selectedTargetLang
                            }
                            (pair?.translatedChapters ?: ds.downloadedChapters) to ds.totalChapters
                        }
                        Text(
                            text = stringResource(
                                R.string.chapter_x_over_n,
                                shownDownloaded,
                                shownTotal,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NovelaSecondaryButton(
                            text = stringResource(android.R.string.cancel),
                            onClick = onExportDialogDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        NovelaPrimaryButton(
                            text = stringResource(StringsR.string.export),
                            onClick = {
                                val mode = if (selectedMode == "original") "original" else "translation"
                                val source = if (selectedMode == "original") "" else selectedSourceLang
                                val target = if (selectedMode == "original") "" else selectedTargetLang
                                onExportContentChosen(mode, source, target)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
                dismissButton = {},
            )
        }
        ExportDialogState.NeedDirectory -> {
            LaunchedEffect(Unit) {
                directoryPicker.launch(null)
            }
        }
        ExportDialogState.Hidden -> Unit
    }
}

@Composable
private fun TopBarCircleIcon(
    isTranslucent: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = NovelaTheme.colors
    if (isTranslucent) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = colors.coverBadgeBg,
            border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    } else {
        IconButton(onClick = onClick) {
            content()
        }
    }
}

private class OpenDocumentTreeReadPersistent : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input)
            .addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
    }
}

@Composable
private fun ExportContentOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.md),
        color = if (selected) colors.accentSoft else colors.surfaceSubtle,
        border = BorderStroke(
            width = if (selected) 1.25.dp else NovelaSize.hairline,
            color = if (selected) colors.accent else colors.borderSubtle,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.accent else colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
