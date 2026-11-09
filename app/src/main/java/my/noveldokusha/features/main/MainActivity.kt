package my.noveldokusha.features.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import my.noveldokusha.R
import my.noveldokusha.catalogexplorer.CatalogExplorerScreen
import my.noveldokusha.coreui.BaseActivity
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.Theme
import my.noveldokusha.historyexplorer.HistoryScreen
import my.noveldokusha.libraryexplorer.LibraryScreen
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.settings.SettingsScreen
import my.noveldokusha.tooling.application_workers.setup.PeriodicWorkersInitializer
import my.noveldokusha.tooling.epub_importer.BookImportService

private data class NavDestination(
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    @StringRes val stringRes: Int,
)

private val destinations = listOf(
    NavDestination(
        activeIcon = Icons.Rounded.AutoStories,
        inactiveIcon = Icons.Outlined.AutoStories,
        stringRes = R.string.title_library,
    ),
    NavDestination(
        activeIcon = Icons.Rounded.Explore,
        inactiveIcon = Icons.Outlined.Explore,
        stringRes = R.string.title_finder,
    ),
    NavDestination(
        activeIcon = Icons.Rounded.History,
        inactiveIcon = Icons.Outlined.History,
        stringRes = R.string.title_history,
    ),
    NavDestination(
        activeIcon = Icons.Rounded.Settings,
        inactiveIcon = Icons.Outlined.Settings,
        stringRes = R.string.title_settings,
    ),
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

        setContent {
            var activePageIndex by rememberSaveable { mutableIntStateOf(0) }

            BackHandler(enabled = activePageIndex != 0) {
                activePageIndex = 0
            }

            Theme(themeProvider = themeProvider) {
                val colors = NovelaTheme.colors

                Box(Modifier.fillMaxSize()) {
                    // All 4 screens remain alive in composition simultaneously for instant switching.
                    val libraryAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 0) 1f else 0f,
                        animationSpec = tween(NovelaMotion.fastMs),
                        label = "libAlpha",
                    )
                    val finderAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 1) 1f else 0f,
                        animationSpec = tween(NovelaMotion.fastMs),
                        label = "finderAlpha",
                    )
                    val historyAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 2) 1f else 0f,
                        animationSpec = tween(NovelaMotion.fastMs),
                        label = "historyAlpha",
                    )
                    val settingsAlpha by animateFloatAsState(
                        targetValue = if (activePageIndex == 3) 1f else 0f,
                        animationSpec = tween(NovelaMotion.fastMs),
                        label = "settingsAlpha",
                    )

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
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
                                .graphicsLayer { alpha = finderAlpha }
                                .zIndex(if (activePageIndex == 1) 1f else 0f)
                                .then(
                                    if (activePageIndex != 1) Modifier.pointerInput(Unit) {
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
                                .zIndex(if (activePageIndex == 2) 1f else 0f)
                                .then(
                                    if (activePageIndex != 2) Modifier.pointerInput(Unit) {
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
                                .zIndex(if (activePageIndex == 3) 1f else 0f)
                                .then(
                                    if (activePageIndex != 3) Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                                    } else Modifier
                                )
                        ) {
                            SettingsScreen(
                                navigationRoutes = navigationRoutes,
                                onRestartApp = { recreate() },
                            )
                        }
                    }

                    // WebNovel-style Frosted Bottom Navigation Dock with Icon + Label
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth()
                            .height(NovelaSize.navBarHeight),
                        shape = RoundedCornerShape(NovelaRadius.xxl),
                        color = colors.glassSurface,
                        shadowElevation = 10.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            width = NovelaSize.hairline,
                            color = colors.glassBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            destinations.forEachIndexed { pageIndex, dest ->
                                val isSelected = activePageIndex == pageIndex
                                val pillWidth by animateDpAsState(
                                    targetValue = if (isSelected) 54.dp else 36.dp,
                                    animationSpec = tween(
                                        durationMillis = NovelaMotion.STANDARD,
                                        easing = NovelaMotion.StandardEasing,
                                    ),
                                    label = "navPillWidth$pageIndex",
                                )
                                val pillColor by animateColorAsState(
                                    targetValue = if (isSelected) colors.accentSoft else Color.Transparent,
                                    animationSpec = tween(
                                        durationMillis = NovelaMotion.STANDARD,
                                        easing = NovelaMotion.StandardEasing,
                                    ),
                                    label = "navPillColor$pageIndex",
                                )
                                val contentColor by animateColorAsState(
                                    targetValue = if (isSelected) colors.accent else colors.textMuted,
                                    animationSpec = tween(NovelaMotion.fastMs),
                                    label = "navContentColor$pageIndex",
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .selectable(
                                            selected = isSelected,
                                            onClick = { activePageIndex = pageIndex },
                                            role = Role.Tab,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(28.dp)
                                            .width(pillWidth)
                                            .clip(RoundedCornerShape(NovelaRadius.pill))
                                            .background(pillColor),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) dest.activeIcon else dest.inactiveIcon,
                                            contentDescription = stringResource(id = dest.stringRes),
                                            modifier = Modifier.size(21.dp),
                                            tint = contentColor,
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = stringResource(id = dest.stringRes),
                                        style = NovelaType.navLabel.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
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
