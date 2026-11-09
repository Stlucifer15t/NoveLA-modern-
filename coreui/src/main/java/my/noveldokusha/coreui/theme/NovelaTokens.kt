package my.noveldokusha.coreui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spatial, shape, sizing, and motion tokens for NoveLA Modern's WebNovel-inspired UI.
 * Superset of all prior tokens so every module compiles cleanly throughout incremental updates.
 */
object NovelaSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 32.dp
    val section: Dp = 28.dp

    val screenH: Dp = 16.dp
    val screenHorizontal: Dp = 16.dp
    val gridHorizontal: Dp = 14.dp
    val gridGap: Dp = 12.dp
    val gridGutter: Dp = 12.dp
    val listGap: Dp = 10.dp
    val cardPadding: Dp = 14.dp
    val navBarClearance: Dp = 104.dp
    val bottomDockSafePadding: Dp = 112.dp
}

object NovelaRadius {
    val xs: Dp = 6.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 26.dp
    val pill: Dp = 100.dp

    val cover: Dp = 10.dp
    val coverHero: Dp = 12.dp
    val badge: Dp = 8.dp
    val card: Dp = 16.dp
    val cardLarge: Dp = 20.dp
    val sheet: Dp = 24.dp

    val coverShape = RoundedCornerShape(cover)
    val cardShape = RoundedCornerShape(card)
    val chipShape = RoundedCornerShape(pill)
    val buttonShape = RoundedCornerShape(pill)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
    val badgeShape = RoundedCornerShape(sm)
}

object NovelaSize {
    const val COVER_ASPECT: Float = 1f / 1.45f
    const val coverAspectRatio: Float = 1f / 1.45f

    val carouselCoverHeight: Dp = 168.dp
    val carouselCoverWidth: Dp = 116.dp
    val minTouchTarget: Dp = 48.dp
    val navBarHeight: Dp = 66.dp
    val bottomBarHeight: Dp = 66.dp
    val navIndicatorHeight: Dp = 3.dp
    val topBarHeight: Dp = 56.dp
    val searchBarHeight: Dp = 44.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
    val iconBadge: Dp = 36.dp
    val hairline: Dp = 0.75.dp
    val coverWidthSm: Dp = 58.dp
    val coverWidthMd: Dp = 92.dp
    val coverWidthHero: Dp = 118.dp
}

object NovelaMotion {
    const val INSTANT: Int = 120
    const val STANDARD: Int = 220
    const val EMPHASIZED: Int = 320
    const val STAGGER: Int = 28

    const val fastMs: Int = 150
    const val mediumMs: Int = 220
    const val slowMs: Int = 320

    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val StandardEasing: Easing = FastOutSlowInEasing
}
