package my.noveldokusha.settings

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.appPreferences.ListLayoutMode

/**
 * The Layout screen's own view of the layout preferences.
 *
 * Same pattern as [ReadingPreferences]: every field is the very Preference object the tab it
 * configures reads, so this screen is another door into those values rather than a second copy
 * that can disagree with the first. [forYouGridColumns] keeps an explicit "auto" (0) because
 * the For You grid sizes itself to the screen by default; the shared books counter is clamped
 * to the 2..6 range the library's own sheet uses, so the two screens cannot offer two ranges
 * for one number.
 */
class LayoutPreferences(appPreferences: AppPreferences, scope: CoroutineScope) {

    val forYouLayoutMode: MutableState<ListLayoutMode> =
        appPreferences.FOR_YOU_LIST_LAYOUT_MODE.state(scope)
    val forYouGridColumns: MutableState<Int> = appPreferences.FOR_YOU_GRID_COLUMNS.state(scope)
    val booksLayoutMode: MutableState<ListLayoutMode> =
        appPreferences.BOOKS_LIST_LAYOUT_MODE.state(scope)
    val booksGridColumns: MutableState<Int> = appPreferences.BOOKS_GRID_COLUMNS.state(scope)
    val greyOutReadChapters: MutableState<Boolean> =
        appPreferences.GREY_OUT_READ_CHAPTERS.state(scope)

    fun setForYouGridColumns(value: Int) {
        forYouGridColumns.value = if (value <= AUTO_COLUMNS) AUTO_COLUMNS
        else value.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    fun setBooksGridColumns(value: Int) {
        booksGridColumns.value = value.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    companion object {
        /** "Let the screen decide" — only the For You grid offers it. */
        const val AUTO_COLUMNS = 0
        const val MIN_GRID_COLUMNS = 2
        const val MAX_GRID_COLUMNS = 6
        val GRID_COLUMN_CHOICES = listOf(AUTO_COLUMNS, 2, 3, 4, 5, 6)
    }
}
