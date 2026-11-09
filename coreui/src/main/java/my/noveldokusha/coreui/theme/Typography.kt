package my.noveldokusha.coreui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * NoveLA type scale.
 *
 * Sizes stay close to the Material 3 defaults so existing layouts keep their
 * measurements; what changes is weight, tracking and line height, which is what
 * makes the app read as "editorial" instead of "utility".
 *
 * Applied globally through [typography], so every screen picks it up without
 * call-site changes.
 */

private val Default = FontFamily.Default

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun base(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TightLineHeight,
)

val typography = Typography(
    // Hero areas: empty states, big numbers.
    displayLarge = base(52, 58, FontWeight.Bold, (-1.5f)),
    displayMedium = base(42, 48, FontWeight.Bold, (-1f)),
    displaySmall = base(34, 40, FontWeight.Bold, (-0.5f)),

    // Screen and section titles.
    headlineLarge = base(28, 34, FontWeight.Bold, (-0.4f)),
    headlineMedium = base(24, 30, FontWeight.Bold, (-0.3f)),
    headlineSmall = base(21, 27, FontWeight.SemiBold, (-0.2f)),

    // Card titles, dialog titles, app bar titles.
    titleLarge = base(19, 25, FontWeight.SemiBold, (-0.1f)),
    titleMedium = base(16, 22, FontWeight.SemiBold),
    titleSmall = base(14, 19, FontWeight.SemiBold),

    // Reading and descriptive copy.
    bodyLarge = base(16, 24, FontWeight.Normal),
    bodyMedium = base(14, 21, FontWeight.Normal),
    bodySmall = base(12, 17, FontWeight.Normal),

    // Metadata, chips, buttons.
    labelLarge = base(14, 18, FontWeight.Medium, 0.1f),
    labelMedium = base(12, 16, FontWeight.Medium, 0.2f),
    labelSmall = base(11, 14, FontWeight.Medium, 0.3f),
)

/**
 * Extra emphasis styles that the Material scale has no slot for.
 * Book titles in a grid need to be bolder than [titleSmall] but smaller than
 * [titleMedium], which is exactly the gap this fills.
 */
object NovelaType {
    /** Bold 2-line title under a book cover. */
    val bookTitle = base(13, 17, FontWeight.Bold, (-0.1f))

    /** Single-line gray metadata under a book title. */
    val bookMeta = base(11, 15, FontWeight.Normal, 0.1f)

    /** Count badge / pill label. */
    val badge = base(10, 12, FontWeight.Bold, 0.2f)

    /** Section header on a home/list screen. */
    val sectionTitle = base(18, 24, FontWeight.Bold, (-0.2f))

    /** "See all" style trailing action. */
    val sectionAction = base(13, 18, FontWeight.Medium, 0f)

    /** Large ordinal number in ranked lists (1, 2, 3…). */
    val rank = base(15, 18, FontWeight.Black, (-0.5f))
}
