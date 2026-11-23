package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.dreamora.core.foryou.recommendation.SynopsisTagExtractor
import my.dreamora.core.foryou.recommendation.TagNormalizer
import my.dreamora.feature.local_database.DAOs.RecommendationDao
import my.dreamora.feature.local_database.tables.DiscoveredNovelEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds tags inferred from a candidate's title and synopsis when its source supplied few or
 * none. Local only: no network.
 */
@Singleton
class TagEnhancementManager @Inject constructor(
    private val recommendationDao: RecommendationDao,
) {

    data class EnhancementResult(
        val novelsProcessed: Int,
        val novelsEnhanced: Int,
        val tagsAdded: Int,
        val errors: Int,
    )

    data class TagCoverageStats(
        val totalNovels: Int,
        val withTags: Int,
        val withSynopsis: Int,
        val tagCoveragePercent: Float,
        val topTags: List<Pair<TagNormalizer.TagCategory, Int>>,
    )

    private fun DiscoveredNovelEntity.storedTags(): List<String> = tags

    private fun inferredTagNames(novel: DiscoveredNovelEntity): List<String> {
        val titleTags = SynopsisTagExtractor.extractFromTitle(novel.name)
        val synopsisTags = if (novel.synopsis.isNullOrBlank()) {
            emptySet()
        } else {
            SynopsisTagExtractor.extractTags(novel.synopsis, maxTags = 8)
        }
        return (titleTags + synopsisTags).take(10).toSet().map { TagNormalizer.getDisplayName(it) }
    }

    /** Enhances candidates that have fewer than [minTagThreshold] tags. */
    suspend fun enhanceNovelsWithSynopsis(
        minTagThreshold: Int = 3,
        forceReprocess: Boolean = false,
    ): EnhancementResult = withContext(Dispatchers.IO) {
        var processed = 0
        var enhanced = 0
        var added = 0
        var errors = 0
        for (novel in recommendationDao.getAllDiscoveredNovels()) {
            processed++
            try {
                val existing = novel.storedTags()
                if (!forceReprocess && existing.size >= minTagThreshold) continue
                val existingLower = existing.map { it.lowercase() }.toSet()
                val newTags = inferredTagNames(novel).filter { it.lowercase() !in existingLower }
                if (newTags.isEmpty()) continue
                recommendationDao.insertDiscoveredNovel(
                    novel.copy(
                        tagsString = (existing + newTags).joinToString(","),
                        lastVerifiedAt = System.currentTimeMillis(),
                    ),
                )
                enhanced++
                added += newTags.size
            } catch (e: Exception) {
                errors++
            }
        }
        EnhancementResult(processed, enhanced, added, errors)
    }

    suspend fun getTagCoverageStats(): TagCoverageStats = withContext(Dispatchers.IO) {
        val all = recommendationDao.getAllDiscoveredNovels()
        var withTags = 0
        var withSynopsis = 0
        val counts = mutableMapOf<TagNormalizer.TagCategory, Int>()
        for (novel in all) {
            val tags = novel.storedTags()
            if (tags.isNotEmpty()) {
                withTags++
                for (tag in TagNormalizer.normalizeAll(tags)) {
                    counts[tag] = (counts[tag] ?: 0) + 1
                }
            }
            if (!novel.synopsis.isNullOrBlank()) withSynopsis++
        }
        TagCoverageStats(
            totalNovels = all.size,
            withTags = withTags,
            withSynopsis = withSynopsis,
            tagCoveragePercent = if (all.isNotEmpty()) withTags.toFloat() / all.size * 100f else 0f,
            topTags = counts.entries.sortedByDescending { it.value }.take(20).map { it.key to it.value },
        )
    }

    /** Enhances one candidate. Returns true when a tag was added. */
    suspend fun enhanceSingleNovel(novelUrl: String): Boolean = withContext(Dispatchers.IO) {
        val novel = recommendationDao.getDiscoveredNovel(novelUrl) ?: return@withContext false
        val existing = novel.storedTags()
        val existingLower = existing.map { it.lowercase() }.toSet()
        val newTags = inferredTagNames(novel).filter { it.lowercase() !in existingLower }
        if (newTags.isEmpty()) return@withContext false
        recommendationDao.insertDiscoveredNovel(
            novel.copy(
                tagsString = (existing + newTags).joinToString(","),
                lastVerifiedAt = System.currentTimeMillis(),
            ),
        )
        true
    }
}
