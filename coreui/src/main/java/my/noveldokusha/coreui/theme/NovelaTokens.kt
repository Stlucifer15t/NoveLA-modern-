package my.noveldokusha.coreui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * NoveLA design tokens.
 *
 * Single source of truth for spacing, corner radii, sizing and motion.
 * These are additive: nothing here replaces existing theme values, so screens
 * that have not been redesigned yet keep working unchanged.
 */

/** Vertical + horizontal rhythm. Use these instead of magic dp literals. */
object NovelaSpacing {
    /** 2dp — hairline tweaks. */
    val xxs: Dp = 2.dp

    /** 4dp — tight pairs (badge to text). */
    val xs: Dp = 4.dp

    /** 8dp — related elements inside one block. */
    val sm: Dp = 8.dp

    /** 12dp — grid gutters, list item padding. */
    val md: Dp = 12.dp

    /** 16dp — standard screen horizontal padding. */
    val lg: Dp = 16.dp

    /** 20dp — section separation. */
    val xl: Dp = 20.dp

    /** 24dp — major block separation. */
    val xxl: Dp = 24.dp

    /** 32dp — hero/empty-state breathing room. */
    val huge: Dp = 32.dp

    /** Screen edge padding used by every redesigned screen. */
    val screenH: Dp = 16.dp

    /** Gap between book cards in a grid. */
    val gridGap: Dp = 12.dp

    /** Vertical gap between stacked cards in a list. */
    val listGap: Dp = 10.dp

    /** Bottom padding so content clears the floating navigation bar. */
    val navBarClearance: Dp = 96.dp
}

/** Corner radii. Larger + softer than stock M3 for the reading-first look. */
object NovelaRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 6.dp
    val md: Dp = 10.dp
    val lg: Dp = 14.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp

    /** Fully rounded; large value rather than a percentage. */
    val pill: Dp = 100.dp

    /** Book cover corner radius. */
    val cover: Dp = 8.dp

    val coverShape = RoundedCornerShape(cover)
    val cardShape = RoundedCornerShape(lg)
    val chipShape = RoundedCornerShape(pill)
    val buttonShape = RoundedCornerShape(md)
    val sheetShape = RoundedCornerShape(topStart = xxl, topEnd = xxl)
    val badgeShape = RoundedCornerShape(sm)
}

/** Fixed dimensions shared by book presentation. */
object NovelaSize {
    /** Width / height ratio of a book cover. Tall enough to read a title thumbnail. */
    const val COVER_ASPECT: Float = 1f / 1.45f

    /** Height of a compact book cover in a horizontal carousel. */
    val carouselCoverHeight: Dp = 168.dp

    val carouselCoverWidth: Dp = 116.dp

    /** Minimum touch target. */
    val minTouchTarget: Dp = 48.dp

    /** Height of the floating bottom navigation bar. */
    val navBarHeight: Dp = 64.dp

    /** Thickness of a bottom navigation indicator. */
    val navIndicatorHeight: Dp = 3.dp
}

/**
 * Motion tokens.
 *
 * Every animation in the redesign should reference one of these so the whole
 * app moves with a single personality: quick, decelerating, never bouncy.
 */
object NovelaMotion {
    /** Micro feedback: ripples, icon toggles. */
    const val INSTANT = 120

    /** Standard enter/exit for content. */
    const val STANDARD = 220

    /** Larger surfaces: sheets, screen transitions. */
    const val EMPHASIZED = 320

    /** Staggered list item reveal step. */
    const val STAGGER = 28

    /** Decelerating curve for things entering. */
    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Accelerating curve for things leaving. */
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Shared default. */
    val StandardEasing: Easing = FastOutSlowInEasing
}
