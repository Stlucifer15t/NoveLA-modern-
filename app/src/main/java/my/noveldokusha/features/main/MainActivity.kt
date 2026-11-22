package my.noveldokusha.features.main

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import my.noveldokusha.coreui.theme.AppTheme
import my.noveldokusha.coreui.theme.DarkMode
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.settings.NovelaOnboardingActivity
import my.noveldokusha.features.foryou.ForYouViewModel
import my.noveldokusha.features.foryou.NovelaForYouScreen
import my.noveldokusha.features.feeds.FeedsState
import my.noveldokusha.features.feeds.FeedsViewModel
import my.noveldokusha.features.feeds.NovelaFeedsScreen
import my.noveldokusha.coreui.BaseActivity
import my.noveldokusha.tooling.application_workers.setup.PeriodicWorkersInitializer
import my.noveldokusha.coreui.theme.Theme
import my.noveldokusha.R
import my.noveldokusha.catalogexplorer.CatalogExplorerScreen
import my.noveldokusha.libraryexplorer.LibraryScreen
import my.noveldokusha.settings.SettingsScreen
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.tooling.epub_importer.BookImportService
import my.noveldokusha.historyexplorer.HistoryScreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize

private data class Page(
    @DrawableRes val iconRes: Int,
    @StringRes val stringRes: Int,
)

private val pages = listOf(
    Page(iconRes = R.drawable.ic_baseline_home_24, stringRes = R.string.title_library),
    Page(iconRes = R.drawable.ic_for_you_24, stringRes = R.string.for_you_title),
    Page(iconRes = R.drawable.new_chapters_icon_notification, stringRes = R.string.title_feeds),
    Page(iconRes = R.drawable.ic_baseline_menu_book_24, stringRes = R.string.title_finder),
    Page(iconRes = R.drawable.ic_baseline_history_24, stringRes = R.string.title_history),
    Page(iconRes = R.drawable.ic_twotone_settings_24, stringRes = R.string.title_settings),
)


@AndroidEntryPoint
open class MainActivity : BaseActivity() {

    companion object {
        private var isColdStart = true
    }

    @Inject
    lateinit var periodicWorkersInitializer: PeriodicWorkersInitializer

    @Inject
    lateinit var navigationRoutes: NavigationRoutes

    @Inject
    lateinit var appPreferences: AppPreferences

    /**
     * Home's "For you" rail. Scoped to the activity, so the rail survives tab switches and
     * isn't refetched every time the reader comes back to the first page.
     */
    private val forYouViewModel: ForYouViewModel by viewModels()

    /**
     * The feeds tab. Its own view model for the same reason For You has one: the shelf read
     * stays alive across tab switches instead of restarting on every visit.
     */
    private val feedsViewModel: FeedsViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isColdStart) {
            installSplashScreen()
            isColdStart = false
        }
        super.onCreate(savedInstanceState)

        // Defer WorkManager init to first onResume (cold start optimization)
        var initCalled = false
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !initCalled) {
                initCalled = true
                periodicWorkersInitializer.init()
            }
        })

        requestPushNotificationPermission()

        // First open asks what the reader actually likes. The sheet writes the flag that
        // guards this line, so answering — including "Skip" — means it never runs again.
        if (!appPreferences.FOR_YOU_GENRES_PICKED.value) {
            startActivity(NovelaOnboardingActivity.createIntent(this))
        }

        setContent {
            var activePageIndex by rememberSaveable { mutableIntStateOf(0) }

            BackHandler(enabled = activePageIndex != 0) {
                activePageIndex = 0
            }

            Theme(themeProvider = themeProvider) {
                Box(Modifier.fillMaxSize()) {
                    // All screens live in composition always.
                    // Switching is instant — only alpha changes via graphicsLayer.
                    val libraryAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 0) 1f else 0f,
                        animationSpec = tween(150), label = "libAlpha"
                    )

                    val forYouAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 1) 1f else 0f,
                        animationSpec = tween(150), label = "forYouAlpha"
                    )

                    val feedsAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 2) 1f else 0f,
                        animationSpec = tween(150), label = "feedsAlpha"
                    )

                    val finderAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 3) 1f else 0f,
                        animationSpec = tween(150), label = "finderAlpha"
                    )

                    val historyAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 4) 1f else 0f,
                        animationSpec = tween(150), label = "historyAlpha"
                    )

                    val settingsAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 5) 1f else 0f,
                        animationSpec = tween(150), label = "settingsAlpha"
                    )

                    // The tab only asks the network once somebody looks at it, but installing
                    // the default source is a one-shot that must not wait for that: the first
                    // pass of the tab then has something bound to it.
                    LaunchedEffect(Unit) { forYouViewModel.prepareDefaultSource() }
                    LaunchedEffect(activePageIndex) {
                        if (activePageIndex == 1) forYouViewModel.activate()
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = libraryAlpha }
                                .zIndex(if (activePageIndex == 0) 1f else 0f)
                                .then(
                                    if (activePageIndex != 0) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            LibraryScreen()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = forYouAlpha }
                                .zIndex(if (activePageIndex == 1) 1f else 0f)
                                .then(
                                    if (activePageIndex != 1) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            NovelaForYouScreen(
                                state = forYouViewModel.state,
                                appPreferences = appPreferences,
                                onOpenBook = { meta ->
                                    startActivity(navigationRoutes.chapters(this@MainActivity, meta))
                                },
                                onOpenExtensions = { activePageIndex = 3 },
                                onSetType = forYouViewModel::setType,
                                onSetBoundSource = forYouViewModel::setBoundSource,
                                onRefresh = forYouViewModel::refresh,
                                onLoadMore = forYouViewModel::loadMore,
                                onHide = forYouViewModel::hide,
                                onUndoHide = forYouViewModel::undoHide,
                            )
                        }

                        // The feed is a local aggregation over the whole shelf (a join with
                        // Chapter, grouped per book), and every tab here stays in composition.
                        // So the subscription starts on the first visit to this page and stays
                        // on from there — an empty Box costs nothing while nobody looks, and the
                        // cross-fade still has something to fade in to.
                        var feedsVisited by rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(activePageIndex) {
                            if (activePageIndex == 2) feedsVisited = true
                        }
                        val feedsState =
                            if (feedsVisited) feedsViewModel.state.collectAsState().value
                            else FeedsState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = feedsAlpha }
                                .zIndex(if (activePageIndex == 2) 1f else 0f)
                                .then(
                                    if (activePageIndex != 2) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            NovelaFeedsScreen(
                                state = feedsState,
                                onOpenBook = { meta ->
                                    startActivity(navigationRoutes.chapters(this@MainActivity, meta))
                                },
                                onCheckForUpdates = feedsViewModel::checkForUpdates,
                                onCancelUpdate = feedsViewModel::cancelUpdate,
                                onOpenLibrary = { activePageIndex = 0 },
                                onSetFilter = feedsViewModel::setFilter,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = finderAlpha }
                                
                        .zIndex(if (activePageIndex == 3) 1f else 0f)
                                .then(
                                    if (activePageIndex != 3) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            CatalogExplorerScreen()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = historyAlpha }
                                
                        .zIndex(if (activePageIndex == 4) 1f else 0f)
                                .then(
                                    if (activePageIndex != 4) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            HistoryScreen()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = settingsAlpha }
                                
                        .zIndex(if (activePageIndex == 5) 1f else 0f)
                                .then(
                                    if (activePageIndex != 5) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            SettingsScreen(
                                navigationRoutes = navigationRoutes,
                                onRestartApp = {
                                    recreate()
                                }
                            )
                        }
                    }

                    // Floating bottom navigation: frosted pill with a sliding
                    // accent capsule behind the active tab.
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            // Six tabs need the room: at five the pill sits 40 dp off each
                            // edge, at six the capsule would overlap its neighbour there.
                            .padding(
                                horizontal = if (pages.size > 5) 20.dp else 40.dp,
                                vertical = 16.dp,
                            )
                            .height(NovelaSize.navBarHeight),
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                        shadowElevation = 12.dp,
                        tonalElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pages.forEachIndexed { pageIndex, page ->
                                val isSelected = activePageIndex == pageIndex
                                // Animate the selected capsule width so the bar
                                // feels like one object rather than four toggles.
                                val capsuleWidth by animateDpAsState(
                                    targetValue = if (isSelected) {
                        if (pages.size > 5) 48.dp else 56.dp
                    } else {
                        if (pages.size > 5) 38.dp else 44.dp
                    },
                                    animationSpec = tween(
                                        durationMillis = NovelaMotion.STANDARD,
                                        easing = NovelaMotion.StandardEasing,
                                    ),
                                    label = "navCapsuleW$pageIndex",
                                )
                                val capsuleColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    animationSpec = tween(
                                        durationMillis = NovelaMotion.STANDARD,
                                        easing = NovelaMotion.StandardEasing,
                                    ),
                                    label = "navCapsuleC$pageIndex",
                                )
                                val iconScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.92f,
                                    animationSpec = tween(NovelaMotion.STANDARD),
                                    label = "navIconScale$pageIndex",
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .selectable(
                                            selected = isSelected,
                                            onClick = { activePageIndex = pageIndex },
                                            role = Role.Tab,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null // Clean click
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .width(capsuleWidth)
                                            .clip(RoundedCornerShape(NovelaRadius.pill))
                                            .background(capsuleColor),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = page.iconRes),
                                            contentDescription = stringResource(id = page.stringRes),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                },
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        handleIntent(intent)
    }

    private fun requestPushNotificationPermission() {
        // check if sdk level is more than 33
        if (VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
            return
        }

        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (result != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                if (type == "application/epub+zip"
                    || type == "application/octet-stream"
                    || type == "application/fb2"
                    || type == "application/x-fictionbook+xml"
                ) {
                    handleSharedEpub(intent)
                }
            }

            Intent.ACTION_VIEW -> {
                handleViewedEpub(intent)
            }
        }
    }

    private fun handleViewedEpub(intent: Intent) {
        val epubUri: Uri? = intent.data
        if (epubUri != null) {
            BookImportService.start(ctx = this, uri = epubUri)
        }
    }

    private fun handleSharedEpub(intent: Intent) {
        val epubUri: Uri? = IntentCompat.getParcelableExtra(
            intent, Intent.EXTRA_STREAM, Uri::class.java
        )
        if (epubUri != null) {
            BookImportService.start(ctx = this, uri = epubUri)
        }
    }
}
