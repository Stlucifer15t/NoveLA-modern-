package my.noveldokusha.settings.foryou

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.getOrNull
import my.noveldokusha.data.ScraperRepository
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.mappers.mapToBookMetadata
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import javax.inject.Inject
import javax.inject.Singleton

/** One suggestion: the book, the source it came from, and whether the shelf already holds it. */
data class ForYouBook(
    val metadata: BookMetadata,
    val sourceName: String,
    val inLibrary: Boolean,
) {
    val key: String get() = metadata.url
}

/** What one pass produced, so the UI can tell "nothing matched" from "nothing answered". */
data class ForYouBatch(
    val books: List<ForYouBook>,
    val sourcesTouched: Int,
    val sourcesAnswered: Int,
) {
    val noSources: Boolean get() = sourcesTouched == 0
    val allFailed: Boolean get() = sourcesTouched > 0 && sourcesAnswered == 0
}

/**
 * Builds the "For you" rail from the sources that are actually installed: the same catalog
 * and search calls the Finder makes, run per source and merged. No mock data, no bundled
 * source list — if nothing is installed, the rail says so.
 *
 * Two limits, both deliberate. [MAX_SOURCES] keeps a twelve-cover rail from firing a request
 * at every source a user has ever added (the Finder is where the full fan-out belongs), and
 * only page 0 is ever asked for.
 */
@Singleton
class ForYouRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scraperRepository: ScraperRepository,
) {

    suspend fun fetch(genres: List<NovelaGenre>, libraryUrls: Set<String>): ForYouBatch =
        withContext(Dispatchers.IO) {
            val sources = installedSources()
            if (sources.isEmpty()) return@withContext ForYouBatch(emptyList(), 0, 0)
            val picked = sources.take(MAX_SOURCES)
            val terms = genres.flatMap { it.terms }.distinct().take(MAX_TERMS)
            val results = coroutineScope {
                picked.map { source ->
                    async { source to query(source, terms) }
                }.awaitAll()
            }
            val answered = results.count { (_, books) -> books.isNotEmpty() }
            val books = interleave(results)
                .map { (source, raw) -> raw.mapToBookMetadata() to source }
                .map { (meta, source) ->
                    ForYouBook(
                        metadata = meta,
                        sourceName = source.resolveName(context),
                        inLibrary = meta.url in libraryUrls,
                    )
                }
                .filter { it.metadata.title.isNotBlank() && it.metadata.url.isNotBlank() }
                .distinctBy { it.key }
            ForYouBatch(
                books = books,
                sourcesTouched = picked.size,
                sourcesAnswered = answered,
            )
        }

    private suspend fun installedSources(): List<SourceInterface.Catalog> = try {
        scraperRepository.sourcesCatalogListFlow()
            .first()
            .map { it.catalog }
            .filter { !it.isLocalSource }
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * With genres chosen, each source is searched for them; without, its front page is taken.
     * A source that is down, mid-login, or throws on an empty query contributes nothing — it
     * must not blank the whole rail.
     */
    private suspend fun query(
        source: SourceInterface.Catalog,
        terms: List<String>,
    ): List<BookResult> {
        val out = ArrayList<BookResult>()
        val queries = if (terms.isEmpty()) listOf(null) else terms
        for (term in queries) {
            val page: PagedList<BookResult>? = try {
                if (term == null) source.getCatalogList(0).getOrNull()
                else source.getCatalogSearch(0, term).getOrNull()
            } catch (e: Exception) {
                null
            } catch (e: Throwable) {
                // A plugin can die with anything, including non-Exception errors.
                null
            }
            page?.list?.let { out += it }
            if (out.size >= PER_SOURCE_LIMIT) break
        }
        return out.take(PER_SOURCE_LIMIT)
    }

    /**
     * Round-robin, not concatenation: appended one source after another, the first chatty
     * source would fill the rail and the rest would never be seen.
     */
    private fun interleave(
        buckets: List<Pair<SourceInterface.Catalog, List<BookResult>>>,
    ): List<Pair<SourceInterface.Catalog, BookResult>> {
        val out = ArrayList<Pair<SourceInterface.Catalog, BookResult>>()
        val width = buckets.maxOfOrNull { it.second.size } ?: 0
        for (i in 0 until width) {
            for ((source, books) in buckets) {
                books.getOrNull(i)?.let { out += source to it }
            }
        }
        return out
    }

    companion object {
        const val MAX_SOURCES = 6
        const val MAX_TERMS = 2
        const val PER_SOURCE_LIMIT = 8
    }
}
