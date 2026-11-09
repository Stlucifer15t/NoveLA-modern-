package my.noveldokusha.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * Extended semantic palette derived from the active Material3 [ColorScheme] and enhanced with
 * signature WebNovel semantic accents (gold star ratings, crimson bookmark heart, status emerald,
 * frosted cover capsules, and theme-adaptive CTA gradients).
 *
 * Works seamlessly across all 20 NoveLA themes and Light / Dark / AMOLED Black modes.
 */
@Immutable
data class NovelaColors(
    // Core surfaces & borders
    val background: Color,
    val surfaceSubtle: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceFloating: Color,
    val elevatedSurface: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val hairline: Color,

    // Typography hierarchy
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // Brand & semantic accents
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentLine: Color,
    val accentGradient: Brush,
    val coverScrim: Brush,
    val badgeBackground: Color,
    val badgeText: Color,

    // WebNovel signature semantic tokens
    val starGold: Color,
    val starGoldSoft: Color,
    val favoriteCrimson: Color,
    val favoriteSoft: Color,
    val coverBadgeBg: Color,
    val coverBadgeBorder: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrimBottom: Color,

    val isDark: Boolean,
    val isLight: Boolean,
) {
    val surfaceMuted: Color
        get() = surfaceSubtle

    val goldStar: Color
        get() = starGold

    val goldStarSoft: Color
        get() = starGoldSoft

    val ctaBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(
                accent,
                accent.copy(alpha = 0.86f).compositeOver(if (isDark) Color.White else Color.Black)
            )
        )
}

@Composable
fun rememberNovelaColors(): NovelaColors {
    val scheme = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    return deriveNovelaColors(scheme = scheme, systemDark = systemDark)
}

internal fun deriveNovelaColors(
    scheme: ColorScheme,
    systemDark: Boolean = true,
): NovelaColors {
    val bgLum = scheme.background.luminance()
    val isLight = bgLum > 0.5f || (!systemDark && bgLum > 0.35f)
    val isDark = !isLight
    val isAmoled = isDark && bgLum < 0.005f

    val accent = scheme.primary
    val onAccent = if (accent.luminance() > 0.55f) Color(0xFF0E1117) else Color.White

    val surfaceCard = when {
        isAmoled -> Color(0xFF0E1015)
        isDark -> accent.copy(alpha = 0.04f).compositeOver(scheme.surfaceContainer)
        else -> scheme.surfaceContainerLowest
    }
    val surfaceSubtle = when {
        isAmoled -> Color(0xFF13161E)
        isDark -> scheme.onSurface.copy(alpha = 0.055f).compositeOver(scheme.surface)
        else -> scheme.onSurface.copy(alpha = 0.045f).compositeOver(scheme.surface)
    }
    val surfaceElevated = when {
        isAmoled -> Color(0xFF171B25)
        isDark -> scheme.surfaceContainerHigh
        else -> scheme.surfaceContainerLow
    }
    val surfaceFloating = when {
        isAmoled -> Color(0xFF1A1F2B)
        isDark -> scheme.surfaceContainerHigh
        else -> Color(0xFFFFFFFF)
    }

    val hairlineColor = scheme.outlineVariant.copy(alpha = if (isDark) 0.38f else 0.48f)
    val starGold = if (isDark) Color(0xFFFFBC1F) else Color(0xFFD98A00)
    val favoriteCrimson = if (isDark) Color(0xFFFF3B5C) else Color(0xFFE11D48)
    val success = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    return NovelaColors(
        background = scheme.background,
        surfaceSubtle = surfaceSubtle,
        surfaceCard = surfaceCard,
        surfaceElevated = surfaceElevated,
        surfaceFloating = surfaceFloating,
        elevatedSurface = surfaceElevated,
        glassSurface = surfaceFloating.copy(alpha = if (isDark) 0.92f else 0.95f),
        glassBorder = scheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.08f),
        borderSubtle = hairlineColor,
        borderStrong = scheme.outline.copy(alpha = if (isDark) 0.55f else 0.65f),
        hairline = hairlineColor,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        textMuted = scheme.onSurfaceVariant.copy(alpha = 0.78f),
        accent = accent,
        onAccent = onAccent,
        accentSoft = accent.copy(alpha = if (isDark) 0.16f else 0.11f),
        accentLine = accent.copy(alpha = if (isDark) 0.42f else 0.30f),
        accentGradient = Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = if (isDark) 0.24f else 0.14f),
                Color.Transparent,
            )
        ),
        coverScrim = Brush.verticalGradient(
            0.40f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.80f),
        ),
        badgeBackground = accent,
        badgeText = onAccent,
        starGold = starGold,
        starGoldSoft = starGold.copy(alpha = if (isDark) 0.18f else 0.13f),
        favoriteCrimson = favoriteCrimson,
        favoriteSoft = favoriteCrimson.copy(alpha = if (isDark) 0.18f else 0.12f),
        coverBadgeBg = Color(0xBF0B0E14),
        coverBadgeBorder = Color.White.copy(alpha = 0.14f),
        success = success,
        warning = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
        danger = scheme.error,
        scrimBottom = Color.Black.copy(alpha = 0.78f),
        isDark = isDark,
        isLight = isLight,
    )
}

val LocalNovelaColors = staticCompositionLocalOf {
    NovelaColors(
        background = Color(0xFF0E1117),
        surfaceSubtle = Color(0xFF151821),
        surfaceCard = Color(0xFF171A24),
        surfaceElevated = Color(0xFF1C202C),
        surfaceFloating = Color(0xFF232837),
        elevatedSurface = Color(0xFF1C202C),
        glassSurface = Color(0xEA232837),
        glassBorder = Color(0x1AFFFFFF),
        borderSubtle = Color(0x24FFFFFF),
        borderStrong = Color(0x45FFFFFF),
        hairline = Color(0x24FFFFFF),
        textPrimary = Color(0xFFF2F4F8),
        textSecondary = Color(0xFFB3B9C6),
        textMuted = Color(0xFF8C94A6),
        accent = Color(0xFF4F7CFF),
        onAccent = Color.White,
        accentSoft = Color(0x294F7CFF),
        accentLine = Color(0x664F7CFF),
        accentGradient = Brush.verticalGradient(listOf(Color(0x384F7CFF), Color.Transparent)),
        coverScrim = Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xCC000000)),
        badgeBackground = Color(0xFF4F7CFF),
        badgeText = Color.White,
        starGold = Color(0xFFFFBC1F),
        starGoldSoft = Color(0x2EFFBC1F),
        favoriteCrimson = Color(0xFFFF3B5C),
        favoriteSoft = Color(0x2EFF3B5C),
        coverBadgeBg = Color(0xBF0B0E14),
        coverBadgeBorder = Color(0x24FFFFFF),
        success = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFEF4444),
        scrimBottom = Color(0xC7000000),
        isDark = true,
        isLight = false,
    )
}

object NovelaTheme {
    val colors: NovelaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNovelaColors.current
}
