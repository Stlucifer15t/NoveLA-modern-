package my.dreamora.coreui.theme

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import my.dreamora.coreui.AppThemeProvider
import my.dreamora.coreui.theme.colorscheme.BaseColorScheme
import my.dreamora.coreui.theme.colorscheme.CatppuccinColorScheme
import my.dreamora.coreui.theme.colorscheme.CloudflareColorScheme
import my.dreamora.coreui.theme.colorscheme.CottoncandyColorScheme
import my.dreamora.coreui.theme.colorscheme.DoomColorScheme
import my.dreamora.coreui.theme.colorscheme.GreenAppleColorScheme
import my.dreamora.coreui.theme.colorscheme.LavenderColorScheme
import my.dreamora.coreui.theme.colorscheme.MatrixColorScheme
import my.dreamora.coreui.theme.colorscheme.MidnightDuskColorScheme
import my.dreamora.coreui.theme.colorscheme.MochaColorScheme
import my.dreamora.coreui.theme.colorscheme.MonetColorScheme
import my.dreamora.coreui.theme.colorscheme.MonochromeColorScheme
import my.dreamora.coreui.theme.colorscheme.NordColorScheme
import my.dreamora.coreui.theme.colorscheme.SapphireColorScheme
import my.dreamora.coreui.theme.colorscheme.StrawberryDaiquiriColorScheme
import my.dreamora.coreui.theme.colorscheme.TachiyomiColorScheme
import my.dreamora.coreui.theme.colorscheme.TakoColorScheme
import my.dreamora.coreui.theme.colorscheme.TealTurquoiseColorScheme
import my.dreamora.coreui.theme.colorscheme.TidalWaveColorScheme
import my.dreamora.coreui.theme.colorscheme.YinYangColorScheme
import my.dreamora.coreui.theme.colorscheme.YotsubaColorScheme

val LocalAppTheme = staticCompositionLocalOf<AppTheme> { AppTheme.DEFAULT }
val LocalIsDark = staticCompositionLocalOf<Boolean> { false }

@Composable
fun Theme(
    themeProvider: AppThemeProvider,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val darkMode = themeProvider.currentDarkMode(coroutineScope).value
    val resolvedAppTheme = themeProvider.currentAppTheme(coroutineScope).value

    val isDark = when (darkMode) {
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
        DarkMode.BLACK -> true
        DarkMode.SYSTEM -> isSystemInDarkTheme()
    }

    InternalTheme(
        appTheme = resolvedAppTheme,
        isDark = isDark,
        isAmoled = darkMode == DarkMode.BLACK,
        content = content,
    )
}

@Composable
fun Theme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (darkMode) {
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
        DarkMode.BLACK -> true
        DarkMode.SYSTEM -> isSystemInDarkTheme()
    }

    InternalTheme(
        appTheme = appTheme,
        isDark = isDark,
        isAmoled = darkMode == DarkMode.BLACK,
        content = content,
    )
}

@Composable
fun InternalTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isDark: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(context, appTheme, isDark, isAmoled) {
        val baseScheme = getBaseColorScheme(appTheme, context)
        baseScheme.getColorScheme(
            isDark = isDark,
            isAmoled = isAmoled,
        )
    }

    DisposableEffect(appTheme, isDark) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { isDark },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { isDark }
        )
        onDispose {}
    }

    val textSelectionColors = remember(colorScheme.primary) {
        TextSelectionColors(
            handleColor = colorScheme.primary,
            backgroundColor = colorScheme.primary.copy(alpha = 0.3f)
        )
    }

    Box(Modifier) {
        CompositionLocalProvider(
            LocalAppTheme provides appTheme,
            LocalIsDark provides isDark,
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = shapes,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    LocalTextSelectionColors provides textSelectionColors,
                    LocalNovelaColors provides rememberNovelaColors(),
                    content = content
                )
            }
        }
    }
}

@Composable
fun InternalTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    InternalTheme(
        appTheme = appTheme,
        isDark = when (darkMode) {
            DarkMode.LIGHT -> false
            DarkMode.DARK, DarkMode.BLACK -> true
            DarkMode.SYSTEM -> isSystemInDarkTheme()
        },
        isAmoled = darkMode == DarkMode.BLACK,
        content = content,
    )
}

private fun getBaseColorScheme(appTheme: AppTheme, context: Context): BaseColorScheme {
    return when (appTheme) {
        AppTheme.DEFAULT -> MonetColorScheme(context)
        AppTheme.TACHIYOMI -> TachiyomiColorScheme
        AppTheme.GREEN_APPLE -> GreenAppleColorScheme
        AppTheme.LAVENDER -> LavenderColorScheme
        AppTheme.MIDNIGHT_DUSK -> MidnightDuskColorScheme
        AppTheme.STRAWBERRY_DAIQUIRI -> StrawberryDaiquiriColorScheme
        AppTheme.TAKO -> TakoColorScheme
        AppTheme.TEALTURQUOISE -> TealTurquoiseColorScheme
        AppTheme.TIDAL_WAVE -> TidalWaveColorScheme
        AppTheme.YOTSUBA -> YotsubaColorScheme
        AppTheme.MONOCHROME -> MonochromeColorScheme
        AppTheme.CATPPUCCIN -> CatppuccinColorScheme
        AppTheme.NORD -> NordColorScheme
        AppTheme.YINYANG -> YinYangColorScheme
        AppTheme.CLOUDFLARE -> CloudflareColorScheme
        AppTheme.COTTONCANDY -> CottoncandyColorScheme
        AppTheme.DOOM -> DoomColorScheme
        AppTheme.MATRIX -> MatrixColorScheme
        AppTheme.MOCHA -> MochaColorScheme
        AppTheme.SAPPHIRE -> SapphireColorScheme
    }
}

