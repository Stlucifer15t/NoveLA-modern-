package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.dreamora.core.foryou.parseRatingScore
import my.dreamora.core.getOrNull
import my.dreamora.feature.local_database.DAOs.RecommendationDao
import my.dreamora.feature.local_database.tables.DiscoveredNovelEntity
import my.dreamora.scraper.SourceInterface
import my.dreamora.scraper.domain.BookResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Maintains the candidate pool that For You scores. The pool is filled from catalog pages the
 * tab already loads, and enriched with genres and synopses from each source's own
 * `getBookGenres` / `getBookDescription`. Every enrichment request is charged to
 * [NetworkBudgetManager].
 */
@Singleton
class DiscoveryManager @Inject constructor(
    private val recommendationDao: RecommendationDao,
    private val networkBudgetManager: NetworkBudgetManager,
) {

    data class DiscoveryConfig(
        /** Below this many candidates the pool is considered thin. */
        val minPoolSize: Int = 50,
        /** Books enriched per source per pass. */
        val enrichPerPass: Int = 10,
        /** A book that failed enrichment is not retried for this long. */
        val retryAfterMs: Long = 24L * 60 * 60 * 1000,
    )

    private val config = DiscoveryConfig()

    suspend fun needsSeeding(): Boolean = withContext(Dispatchers.IO) {
        recommendationDao.getDiscoveredNovelCount() < config.minPoolSize
    }

    suspend fun getPoolSize(): Int = withContext(Dispatchers.IO) {
        recommendationDao.getDiscoveredNovelCount()
    }

    suspend fun getPoolSizeByProvider(): Map<String, Int> = withContext(Dispatchers.IO) {
        recommendationDao.getDiscoveredProviders().associateWith { id ->
            recommendationDao.getDiscoveredNovelCountByProvider(id)
        }
    }

    /**
     * Stores one catalog page as candidates for [sourceId]. Genres and synopsis already known
     * for a book are kept, so a later browse never wipes what enrichment learned.
     *
     * @return the number of books stored
     */
    suspend fun cacheCatalogPage(
        sourceId: String,
        books: List<BookResult>,
        source: String = "browse",
    ): Int = withContext(Dispatchers.IO) {
        val usable = books.filter { it.url.isNotBlank() && it.title.isNotBlank() }
        if (usable.isEmpty()) return@withContext 0
        val prior = recommendationDao.getDiscoveredNovelsByProvider(sourceId).associateBy { it.url }
        val entities = usable.map { book ->
            val old = prior[book.url]
            val rating = parseRatingScore(book.rating.orEmpty())
                ?.let { (it * 100).roundToInt() }
                ?: old?.rating
            DiscoveredNovelEntity(
                url = book.url,
                name = book.title,
                apiName = sourceId,
                posterUrl = book.coverImageUrl.ifBlank { old?.posterUrl },
                rating = rating,
                tagsString = old?.tagsString,
                status = old?.status,
                synopsis = book.description.ifBlank { old?.synopsis }?.take(500),
                author = book.author?.takeIf { it.isNotBlank() } ?: old?.author,
                source = source,
            )
        }
        recommendationDao.insertDiscoveredNovels(entities)
        entities.size
    }

    /**
     * Fetches genres and synopsis for candidates from [source] that have no tags yet.
     * Each source call waits for and is recorded against the discovery budget. Books whose
     * attempt returned nothing are not retried for [DiscoveryConfig.retryAfterMs].
     *
     * @return the number of books that gained at least one tag or synopsis
     */
    suspend fun enrichCandidates(
        source: SourceInterface.Catalog,
        limit: Int = config.enrichPerPass,
    ): Int = withContext(Dispatchers.IO) {
        val sourceId = source.id
        val now = System.currentTimeMillis()
        val pending = recommendationDao.getDiscoveredNovelsByProvider(sourceId)
            .filter { it.tagsString.isNullOrBlank() && now - it.lastVerifiedAt >= config.retryAfterMs }
            .take(limit)
        var enriched = 0
        for (entity in pending) {
            if (!networkBudgetManager.waitForSlot(sourceId)) break

            val genres = source.getBookGenres(entity.url).getOrNull().orEmpty()
            networkBudgetManager.recordRequest(sourceId)

            var description: String? = null
            if (entity.synopsis.isNullOrBlank() && networkBudgetManager.waitForSlot(sourceId)) {
                description = source.getBookDescription(entity.url).getOrNull()
                networkBudgetManager.recordRequest(sourceId)
            }

            val tags = genres.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val gotSomething = tags.isNotEmpty() || !description.isNullOrBlank()
            recommendationDao.insertDiscoveredNovel(
                entity.copy(
                    tagsString = tags.joinToString(",").takeIf { it.isNotEmpty() },
                    synopsis = description?.takeIf { it.isNotBlank() }?.take(500) ?: entity.synopsis,
                    lastVerifiedAt = now,
                ),
            )
            if (gotSomething) enriched++
        }
        enriched
    }

    /** Drops candidates not seen for 30 days and prunes the budget tables. */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        recommendationDao.deleteOldDiscoveredNovels(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        networkBudgetManager.cleanup()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        recommendationDao.clearAllDiscoveredNovels()
    }
}
