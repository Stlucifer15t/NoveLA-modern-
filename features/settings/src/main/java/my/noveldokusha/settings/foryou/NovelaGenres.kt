package my.noveldokusha.settings.foryou

import androidx.annotation.StringRes
import my.noveldokusha.strings.R

/**
 * The genres the first-open sheet offers.
 *
 * Deliberately a fixed list rather than a union of the installed sources' vocabularies: a
 * Lua or Legado source exposes whatever its own `getFilterList` happens to define, so a
 * genre list built from them would change every time a source is added, and the picker
 * would ask about taxonomies most sources cannot answer. What we do instead is honest —
 * the chosen label's words are used as query terms, and the ranking is keyword matching on
 * titles, not a similarity score we cannot compute.
 */
enum class NovelaGenre(
    val slug: String,
    @param:StringRes val labelRes: Int,
    val terms: List<String>,
) {
    Action("action", R.string.genre_action, listOf("action", "fighting")),
    Adventure("adventure", R.string.genre_adventure, listOf("adventure", "exploration")),
    Comedy("comedy", R.string.genre_comedy, listOf("comedy", "humor")),
    Drama("drama", R.string.genre_drama, listOf("drama")),
    Fantasy("fantasy", R.string.genre_fantasy, listOf("fantasy", "magic")),
    Historical("historical", R.string.genre_historical, listOf("historical", "history")),
    Horror("horror", R.string.genre_horror, listOf("horror", "horror novel")),
    MartialArts("martial-arts", R.string.genre_martial_arts, listOf("martial arts", "wuxia")),
    Romance("romance", R.string.genre_romance, listOf("romance")),
    SciFi("sci-fi", R.string.genre_sci_fi, listOf("sci-fi", "science fiction")),
    SliceOfLife("slice-of-life", R.string.genre_slice_of_life, listOf("slice of life")),
    Tragedy("tragedy", R.string.genre_tragedy, listOf("tragedy", "drama")),
    ;

    companion object {
        fun fromSlugs(slugs: Collection<String>): List<NovelaGenre> =
            entries.filter { it.slug in slugs }
    }
}
