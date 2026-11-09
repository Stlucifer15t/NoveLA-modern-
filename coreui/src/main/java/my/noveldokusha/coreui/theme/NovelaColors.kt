package my.noveldokusha.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Semantic colors that Material 3's [androidx.compose.material3.ColorScheme] has no
 * slot for, but that the redesigned screens need.
 *
 * Everything here is derived from the active theme's own colors, so all 21 existing
 * themes keep working untouched — a screen can ask for "a soft accent wash" and get
 * one in Monet, Nord, or Catppuccin without knowing which is active.
 *
 * Reader screens must NOT use these: the reader has its own neutral palette in
 * `BaseColorScheme` so long-form reading never inherits an accent tint.
 */
data class NovelaColors(
    /** Very low-alpha accent used behind selected chips and active nav items. */
    val accentSoft: Color,

    /** Mid-alpha accent for strokes on top of [accentSoft]. */
    val accentLine: Color,

    /** Vertical wash for hero headers and gradient buttons. */
    val accentGradient: Brush,

    /** Scrim painted at the bottom of a cover so overlaid text stays legible. */
    val coverScrim: Brush,

    /** Hairline used between rows and around cards. */
    val hairline: Color,

    /** Slightly lifted surface used for cards that sit on [ColorScheme.surface]. */
    val elevatedSurface: Color,

    /** Text color for secondary metadata ("chapter 12 of 300"). */
    val textMuted: Color,

    /** Unread-count pill background. */
    val badgeBackground: Color,

    /** Unread-count pill label color. */
    val badgeText: Color,

    /** True when the active scheme is light; handy for one-off tweaks. */
    val isLight: Boolean,
)

/**
 * Builds the extended palette from the current [MaterialTheme].
 * Called once per theme change inside `InternalTheme`.
 */
@Composable
fun rememberNovelaColors(): NovelaColors {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f || (!isSystemInDarkTheme() && scheme.background.luminance() > 0.35f)

    return NovelaColors(
        accentSoft = scheme.primary.copy(alpha = if (isLight) 0.10f else 0.18f),
        accentLine = scheme.primary.copy(alpha = if (isLight) 0.30f else 0.45f),
        accentGradient = Brush.verticalGradient(
            colors = listOf(
                scheme.primary.copy(alpha = if (isLight) 0.16f else 0.28f),
                Color.Transparent,
            )
        ),
        coverScrim = Brush.verticalGradient(
            0.45f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.78f),
        ),
        hairline = scheme.outlineVariant.copy(alpha = if (isLight) 0.55f else 0.40f),
        elevatedSurface = if (isLight) {
            scheme.surfaceContainerLowest
        } else {
            scheme.surfaceContainerHigh
        },
        textMuted = scheme.onSurfaceVariant.copy(alpha = 0.82f),
        badgeBackground = scheme.primary,
        badgeText = scheme.onPrimary,
        isLight = isLight,
    )
}

/**
 * Access point inside composables: `NovelaTheme.colors.textMuted`.
 * Falls back to a sensible default so previews and isolated components that are
 * rendered outside [InternalTheme] still compose instead of crashing.
 */
object NovelaTheme {
    val colors: NovelaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNovelaColors.current
}

val LocalNovelaColors = staticCompositionLocalOf<NovelaColors> {
    // Safe defaults so a composable rendered outside the theme (preview, dialog
    // window, isolated test) still works instead of throwing.
    NovelaColors(
        accentSoft = Color(0x1F0088FF),
        accentLine = Color(0x4D0088FF),
        accentGradient = Brush.verticalGradient(listOf(Color(0x290088FF), Color.Transparent)),
        coverScrim = Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xCC000000)),
        hairline = Color(0x268C8C8C),
        elevatedSurface = Color(0xFFF8F8F8),
        textMuted = Color(0xFF8C8C8C),
        badgeBackground = Color(0xFF0088FF),
        badgeText = Color.White,
        isLight = true,
    )
}
