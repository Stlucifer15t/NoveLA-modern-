package my.noveldokusha.settings

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.core.appPreferences.AppPreferences

/**
 * The reader's own preferences, exposed to the Settings screen.
 *
 * Every field here is the very same `Preference.state(...)` object the reader panel writes
 * (see `ReaderActivity`), not a copy — so a value changed on this screen is what the reader
 * reads on the next chapter, and the two screens cannot drift apart. Ranges and value formats
 * match `StyleSettingDialog` for the same reason: two sliders for one number should not offer
 * two different ranges.
 */
class ReadingPreferences(appPreferences: AppPreferences, scope: CoroutineScope) {

    val fontSize: MutableState<Float> = appPreferences.READER_FONT_SIZE.state(scope)
    val lineHeight: MutableState<Float> = appPreferences.READER_LINE_HEIGHT.state(scope)
    val paragraphSpacing: MutableState<Float> = appPreferences.READER_PARAGRAPH_SPACING.state(scope)
    val marginLeft: MutableState<Float> = appPreferences.READER_MARGIN_LEFT.state(scope)
    val marginRight: MutableState<Float> = appPreferences.READER_MARGIN_RIGHT.state(scope)
    val justify: MutableState<Boolean> = appPreferences.READER_TEXT_JUSTIFY.state(scope)
    val hyphenation: MutableState<Boolean> = appPreferences.READER_TEXT_HYPHENATION.state(scope)
    val paragraphIndent: MutableState<Boolean> = appPreferences.READER_PARAGRAPH_INDENT.state(scope)
    val firstLetterBold: MutableState<Boolean> =
        appPreferences.READER_PARAGRAPH_FIRST_LETTER_BOLD.state(scope)
    val textSmooth: MutableState<Boolean> = appPreferences.READER_TEXT_SMOOTH.state(scope)
    val keepScreenOn: MutableState<Boolean> = appPreferences.READER_KEEP_SCREEN_ON.state(scope)
    val fullScreen: MutableState<Boolean> = appPreferences.READER_FULL_SCREEN.state(scope)
    val singleTapOpensControls: MutableState<Boolean> =
        appPreferences.READER_SINGLE_TAP_TO_OPEN_SETTINGS.state(scope)
    val selectableText: MutableState<Boolean> = appPreferences.READER_SELECTABLE_TEXT.state(scope)
    val prefetchChapters: MutableState<Int> = appPreferences.READER_PAGE_PREFETCH_COUNT.state(scope)

    /** Clamped here instead of in the UI, so the slider and a restored value can't disagree. */
    fun setFontSize(value: Float) {
        fontSize.value = value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
    }

    /**
     * The reader keeps left and right separately (it exposes two sliders); this screen offers
     * one control, because that is what a reader wants, and writes both. If the two differ,
     * the wider one is shown so the handle starts where the text actually stops.
     */
    val sideMargins: Float
        get() = maxOf(marginLeft.value, marginRight.value)

    fun setSideMargins(value: Float) {
        val v = value.coerceIn(0f, MAX_MARGIN)
        marginLeft.value = v
        marginRight.value = v
    }

    companion object {
        const val MIN_FONT_SIZE = 8f
        const val MAX_FONT_SIZE = 32f
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 2.5f
        const val MAX_MARGIN = 100f
    }
}
