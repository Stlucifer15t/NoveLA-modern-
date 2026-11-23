package my.dreamora.features.foryou

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import my.dreamora.R
import my.dreamora.core.PagedList
import my.dreamora.core.foryou.DreamoraGenre
import my.dreamora.core.foryou.parseRatingScore
import my.dreamora.core.Response
import my.dreamora.core.getOrNull
import my.dreamora.data.ScraperRepository
import my.dreamora.feature.local_database.BookMetadata
import my.dreamora.mappers.mapToBookMetadata
import my.dreamora.scraper.SourceInterface
import my.dreamora.scraper.domain.BookResult
import javax.inject.Inject
import javax.inject.Singleton

/** The three rails the For You tab switches between, and the source types each one reads. */
enum class ForYouContentType(
    val scope: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Novel("novel", R.string.for_you_tab_novel, R.drawable.ic_content_type_novel),
    Manga("manga", R.string.for_you_tab_manga, R.drawable.ic_content_type_manga),
    Video("video", R.string.for_you_tab_video, R.drawable.ic_content_type_video),
    ;

    /**
     * The Finder's rule, kept identical: a source with no content_type label is a novel
     * source, so "novel" must match it; manga and video only match sources that declare
     * themselves as such.
     */
    fun matches(sourceContentType: String): Boolean = when (this) {
        Novel -> sourceContentType == "novel" || sourceContentType.isEmpty()
        else -> sourceContentType == scope
    }
}

/**
 * One suggestion. Every field came from a source response or from this device's database, and
 * [genre] is not an inference either: it is the lane whose query returned the book, i.e. the
 * piece of the source's answer to "isekai" that this row is.
 */
data class ForYouBook(
    val metadata: BookMetadata,
    val sourceName: String,
    val inLibrary: Boolean,
    /**
     * A title from the reader's own history that shares a word with this one. This is the
     * only "because you like this" claim the screen is allowed to make — there is no vector
     * model behind it, and pretending otherwise would be decoration over a guess.
     */
    val matchedHistoryTitle: String?,
    /** The picked genre whose query returned this book; null = the source's front page. */
    val genre: DreamoraGenre? = null,
) {
    val key: String get() = metadata.url

    /** The source's own score on a 0..10 scale, or null when it did not state one. */
    val ratingScore: Double? get() = parseRatingScore(metadata.rating)
}

/** A genre the tab asks the sources about, and the words it asks with. */
data class ForYouLane(
    val slug: String,
    val terms: List<String>,
) {
    companion object {
        /** The pass that asks for nothing — the source's own front page. */
        val Catalog = ForYouLane(slug = "", terms = emptyList())

        fun forGenres(genres: List<DreamoraGenre>): List<ForYouLane> =
            genres.map { ForYouLane(it.slug, it.terms) }
    }
}

/** One row of the result pool, with its source and the lane that produced it. */
data class ForYouHit(
    val source: SourceInterface.Catalog,
    val book: BookResult,
    val lane: ForYouLane,
)

/**
 * A sideways rail. [genre] is null for the pseudo-rail built from the sources' own ratings,
 * which is why the label is resolved by the caller rather than stored here.
 */
data class ForYouRail(
    val genre: DreamoraGenre?,
    val books: List<ForYouBook>,
)

/** Where one source's pager stands. Empty terms means "read its front page". */
data class ForYouCursor(
    val termIndex: Int = 0,
    val page: Int = 0,
    val exhausted: Boolean = false,
)

data class ForYouStep(
    /** Already mapped to metadata and normalized, so keys match the shelf's keys. */
    val books: List<BookResult>,
    val cursor: ForYouCursor,
    val answered: Boolean,
)

/** A (source, lane) pair, which is what a cursor is actually kept for. */
data class ForYouSlot(val sourceId: String, val lane: ForYouLane) {
    companion object {
        fun key(sourceId: String, lane: ForYouLane): String = "$sourceId#${lane.slug}"
    }
}

/** The sources this device can read for one type, with the bound/fallback choice made. */
data class ForYouSources(
    val all: List<SourceInterface.Catalog>,
    /** What a fetch pass will hit: the bound source alone, or a bounded fan-out. */
    val active: List<SourceInterface.Catalog>,
    val bound: SourceInterface.Catalog?,
    val boundWasForced: Boolean,
) {
    val isEmpty: Boolean get() = all.isEmpty()
    val sourceCount: Int get() = active.size
}

/**
 * The For You data path. It is deliberately small: installed sources, their catalog/search
 * pagers, and the shelf/history tables. The app has no reading-time or tag-affinity store, so
 * nothing here pretends to score a book beyond the two facts it can hold up.
 */
@Singleton
class ForYouEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scraperRepository: ScraperRepository,
) {

    /**
     * Reactive on purpose. Installing a source in Extensions makes this flow re-emit, so the
     * tab fills in on its own; the old failure mode — "I installed an extension and For You
     * stayed empty until I restarted the app" — came from reading the list exactly once.
     */
    fun sourcesFlow(): Flow<List<SourceInterface.Catalog>> =
        scraperRepository.sourcesCatalogListFlow()
            .map { items -> items.map { it.catalog }.filter { !it.isLocalSource } }

    fun resolve(
        sources: List<SourceInterface.Catalog>,
        type: ForYouContentType,
        boundId: String,
    ): ForYouSources {
        val ofType = sources.filter { type.matches(it.contentType) }
        val explicit = boundId.takeIf { it.isNotEmpty() }?.let { id -> ofType.find { it.id == id } }
        // No explicit choice yet: prefer the source provisioning aimed for, so a fresh
        // install with WebNovel lands bound without anybody configuring it.
        val hinted = ofType.firstOrNull { DEFAULT_HINT in sourceName(it).lowercase() }
        val bound = explicit ?: hinted
        return ForYouSources(
            all = ofType,
            active = when {
                bound != null -> listOf(bound)
                else -> ofType.take(MAX_FANOUT)
            },
            bound = bound,
            boundWasForced = explicit != null,
        )
    }

    /**
     * One pager step across every (source, lane) pair, in parallel. Results are interleaved
     * round-robin rather than concatenated: appended one after another, the first chatty slot
     * fills the tab and the rest are never seen.
     *
     * Paging a lane per lane is also what makes the sideways rails possible at all. One pass
     * over the sources with the words of every genre glued into a single list asks one question
     * at a time and retires the source after its first answer, so the tab would only ever have
     * data for one genre on screen. With a slot per genre, the first pass already has an
     * isekai shelf, a romance shelf and a catalog shelf — at the cost of one page request per
     * slot, which is the price of rails that say something true.
     */
    suspend fun step(
        sources: List<SourceInterface.Catalog>,
        cursors: Map<String, ForYouCursor>,
        lanes: List<ForYouLane>,
    ): Pair<Map<String, ForYouCursor>, List<ForYouHit>> = withContext(Dispatchers.IO) {
        val slots = sources.flatMap { source -> lanes.map { source to it } }
        val results = coroutineScope {
            slots.map { (source, lane) ->
                async {
                    val key = ForYouSlot.key(source.id, lane)
                    Triple(
                        source,
                        lane,
                        step(source, cursors[key] ?: ForYouCursor(), lane.terms),
                    )
                }
            }.awaitAll()
        }
        val nextCursors = results.associate { (source, lane, step) ->
            ForYouSlot.key(source.id, lane) to step.cursor
        }
        val width = results.maxOfOrNull { (_, _, step) -> step.books.size } ?: 0
        val merged = ArrayList<ForYouHit>(width * results.size)
        for (i in 0 until width) {
            for ((source, lane, step) in results) {
                step.books.getOrNull(i)?.let {
                    merged += ForYouHit(source = source, book = it, lane = lane)
                }
            }
        }
        nextCursors to merged
    }

    /**
     * One source's pager. When a genre word runs out the cursor moves to the next word, and
     * only after the last word ends is the source marked finished — so "Load more" keeps
     * working while never paging past what the source itself reports.
     */
    private suspend fun step(
        source: SourceInterface.Catalog,
        cursor: ForYouCursor,
        terms: List<String>,
    ): ForYouStep {
        if (cursor.exhausted) return ForYouStep(emptyList(), cursor, answered = false)
        val term = terms.getOrNull(cursor.termIndex)
        val response: Response<PagedList<BookResult>>? = try {
            if (term == null) source.getCatalogList(cursor.page)
            else source.getCatalogSearch(cursor.page, term)
        } catch (e: CancellationException) {
            // Never swallow a cancel: a swallowed one keeps paging for a pass nobody waits for.
            throw e
        } catch (e: Exception) {
            null
        }
        val page = response?.getOrNull()
            ?: return ForYouStep(
                books = emptyList(),
                // One failed step retires that source for this pass — leaving it "not
                // exhausted" would let Load more promise forever on a dead source.
                cursor = cursor.copy(exhausted = true),
                answered = false,
            )
        val next = when {
            !page.isLastPage -> cursor.copy(page = cursor.page + 1)
            terms.isEmpty() -> cursor.copy(exhausted = true)
            cursor.termIndex + 1 >= terms.size -> cursor.copy(exhausted = true)
            else -> ForYouCursor(termIndex = cursor.termIndex + 1, page = 0)
        }
        return ForYouStep(
            books = page.list.take(PER_STEP),
            cursor = next,
            answered = true,
        )
    }

    fun decorate(
        raw: List<ForYouHit>,
        libraryUrls: Set<String>,
        historyTitles: List<String>,
    ): List<ForYouBook> = raw
        .map { hit ->
            val meta = hit.book.mapToBookMetadata()
            ForYouBook(
                metadata = meta,
                sourceName = sourceName(hit.source),
                inLibrary = meta.url in libraryUrls,
                matchedHistoryTitle = historyTitles.firstOrNull {
                    sharedTitleWord(it, meta.title) != null
                },
                genre = hit.lane.slug.takeIf { it.isNotEmpty() }?.let { slug ->
                    DreamoraGenre.fromSlugs(listOf(slug)).firstOrNull()
                },
            )
        }
        .filter { it.metadata.title.isNotBlank() && it.metadata.url.isNotBlank() }
        .distinctBy { it.key }
        // No sorting here: the view pushes shelf books to the end of a fresh pass
        // only, because re-sorting on "Load more" would move rows mid-read.
        // What is already on the shelf goes last instead of being hidden: hiding it can leave
        // the tab looking broken when a source only carries books already read.

    fun sourceName(source: SourceInterface.Catalog): String = source.resolveName(context)

    companion object {
        /** The source first-run provisioning aims to bind. */
        const val DEFAULT_HINT = "webnovel"
        const val MAX_FANOUT = 8
        const val PER_STEP = 12
    }
}


/**
 * The best-rated of what the sources actually returned, normalized score first and source text
 * second. A score below 6/10 is not a recommendation, so a thin pass simply yields no rail
 * rather than a rail that says "highest rated" about mediocre books.
 */
internal fun List<ForYouBook>.highestRated(limit: Int = 10): List<ForYouBook> =
    asSequence()
        .mapNotNull { book -> parseRatingScore(book.metadata.rating)?.let { book to it } }
        .filter { (_, score) -> score >= 6.0 }
        .sortedByDescending { (_, score) -> score }
        .distinctBy { it.first.key }
        .take(limit)
        .map { it.first }
        .toList()

/**
 * The sideways rails: one per picked genre, in the order the genres were picked, holding
 * exactly the books that genre's query returned. A genre that came back with fewer than
 * [minItems] answers gets no rail — a row of one cover is noise that looks like a bug, and
 * "Isekai" with nothing in it is worse than no "Isekai" at all.
 */
internal fun List<ForYouBook>.railsFor(
    genres: List<DreamoraGenre>,
    minItems: Int = 3,
): List<ForYouRail> =
    genres.mapNotNull { genre ->
        val books = filter { it.genre == genre }
        if (books.size >= minItems) ForYouRail(genre = genre, books = books) else null
    }

private val STOP_WORDS = setOf("the", "and", "with", "from", "into", "that", "this", "book", "vol", "chapter")

/** The one word two titles share, or null. Letters and digits only, nothing shorter than 4. */
internal fun sharedTitleWord(left: String, right: String): String? {
    fun words(text: String): Set<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .asSequence()
            .filter { it.length >= 4 && it !in STOP_WORDS }
            .toSet()

    val a = words(left)
    if (a.isEmpty()) return null
    return words(right).firstOrNull { it in a }
}
