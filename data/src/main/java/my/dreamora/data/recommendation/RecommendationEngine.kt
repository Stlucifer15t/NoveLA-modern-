package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import my.dreamora.core.foryou.recommendation.NovelScorer
import my.dreamora.core.foryou.recommendation.SimilarityCalculator
import my.dreamora.core.foryou.recommendation.SynopsisTagExtractor
import my.dreamora.core.foryou.recommendation.TagNormalizer
import my.dreamora.core.foryou.recommendation.TagNormalizer.TagCategory
import my.dreamora.core.foryou.recommendation.model.AuthorAffinity
import my.dreamora.core.foryou.recommendation.model.NovelVector
import my.dreamora.core.foryou.recommendation.model.Recommendation
import my.dreamora.core.foryou.recommendation.model.RecommendationGroup
import my.dreamora.core.foryou.recommendation.model.RecommendationType
import my.dreamora.core.foryou.recommendation.model.ScoreBreakdown
import my.dreamora.core.foryou.recommendation.model.TrackedBook
import my.dreamora.core.foryou.recommendation.model.TrackedStatus
import my.dreamora.core.foryou.recommendation.model.UserTasteProfile
import my.dreamora.data.LibraryBooksRepository
import my.dreamora.feature.local_database.DAOs.RecommendationDao
import my.dreamora.feature.local_database.tables.Book
import my.dreamora.feature.local_database.tables.DiscoveredNovelEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Knobs for one generation pass. */
data class RecommendationConfig(
    val maxPerGroup: Int = 10,
    val minScore: Float = 0.25f,
    val includeCrossSource: Boolean = true,
    val preferredSource: String? = null,
    /** Fraction of a group that may repeat books already shown in an earlier group. */
    val allowedOverlapRatio: Float = 0.2f,
    /** Source ids to draw from. Null means every source in the pool. */
    val allowedSources: Set<String>? = null,
    /** Display name per source id, used in reasons such as "Popular on …". */
    val sourceNames: Map<String, String> = emptyMap(),
)

/**
 * Turns the reader's library and the cached candidate pool into For You rails. Scores come
 * from [NovelScorer]; rails and their order follow Novery's recommendation engine.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val libraryBooksRepository: LibraryBooksRepository,
    private val recommendationDao: RecommendationDao,
    private val userFilterManager: UserFilterManager,
    private val authorPreferenceManager: AuthorPreferenceManager,
) {

    private data class LibraryEntry(val book: Book, val tracked: TrackedBook)

    /** Rebuilds the taste profile from the current library. */
    suspend fun refreshTasteFromLibrary() = withContext(Dispatchers.IO) {
        userPreferenceManager.recalculateAllPreferences(loadLibrary().map { it.tracked })
    }

    /**
     * Builds the For You rails.
     *
     * @param refreshTaste rebuild the taste profile from the library first
     */
    suspend fun generateRecommendations(
        config: RecommendationConfig = RecommendationConfig(),
        refreshTaste: Boolean = true,
    ): List<RecommendationGroup> = withContext(Dispatchers.IO) {
        if (refreshTaste) refreshTasteFromLibrary()
        val library = loadLibrary()
        val libraryUrls = library.map { it.tracked.url }.toSet()
        val profile = userPreferenceManager.getUserProfile()
        val filter = userFilterManager.getFilterState()
        val likedAuthors = authorPreferenceManager.getLikedAuthors(50)
        val scorer = NovelScorer.create(profile, authorPreferenceManager.getLikedAffinities(50))

        val candidates = loadCandidates(libraryUrls, filter, config.allowedSources)
        if (candidates.isEmpty()) return@withContext emptyList()
        val withGoodData = candidates.filter { it.hasQualityData }

        val pass = Pass(
            config = config,
            profile = profile,
            scorer = scorer,
            filterManager = userFilterManager,
            filter = filter,
            authorNames = likedAuthors.associate { it.authorNormalized to it.displayName },
            authorScores = likedAuthors.associate { it.authorNormalized to it.affinityScore },
        )

        val groups = ArrayList<RecommendationGroup>()
        if (profile.sampleSize >= 1 && withGoodData.isNotEmpty()) {
            pass.forYou(withGoodData)?.let { groups += it }
        }
        pass.fromAuthors(candidates)?.let { groups += it }
        library
            .filter { it.tracked.lastReadEpochMilli > 0 }
            .maxByOrNull { it.tracked.lastReadEpochMilli }
            ?.let { entry -> pass.becauseYouRead(libraryVector(entry), candidates)?.let { groups += it } }
        pass.topRated(candidates)?.let { groups += it }
        if (withGoodData.isNotEmpty()) groups += pass.genreGroups(withGoodData)
        if (config.includeCrossSource) {
            val preferred = config.preferredSource
                ?: candidates.groupingBy { it.providerName }.eachCount().maxByOrNull { it.value }?.key
            if (preferred != null) pass.discover(candidates, preferred)?.let { groups += it }
        }
        groups += pass.providerGroups(candidates)
        pass.newReleases(candidates)?.let { groups += it }
        groups
    }

    /** Books similar to [bookUrl], for a "more like this" list. */
    suspend fun getSimilarTo(bookUrl: String, limit: Int = 10): List<Recommendation> =
        withContext(Dispatchers.IO) {
            val library = loadLibrary()
            val source = library.firstOrNull { it.tracked.url == bookUrl }?.let { libraryVector(it) }
                ?: recommendationDao.getDiscoveredNovel(bookUrl)?.let { discoveredVector(it) }
                ?: return@withContext emptyList()
            val profile = userPreferenceManager.getUserProfile()
            val filter = userFilterManager.getFilterState()
            val candidates = loadCandidates(library.map { it.tracked.url }.toSet() + bookUrl, filter, null)
            val pass = Pass(
                config = RecommendationConfig(),
                profile = profile,
                scorer = NovelScorer.create(profile, authorPreferenceManager.getLikedAffinities(50)),
                filterManager = userFilterManager,
                filter = filter,
                authorNames = emptyMap(),
                authorScores = emptyMap(),
            )
            SimilarityCalculator.findSimilarWithQuality(
                target = source,
                candidates = candidates,
                limit = limit,
                minSimilarity = 0.12f,
            ).map { (v, _) ->
                pass.recommendation(
                    v = v,
                    type = RecommendationType.SIMILAR_TO,
                    breakdown = similarityBreakdown(source, v),
                    sourceUrl = source.url,
                    sourceTitle = source.name,
                )
            }
        }

    private suspend fun loadLibrary(): List<LibraryEntry> =
        libraryBooksRepository.getBooksInLibraryWithContextFlow.first()
            .filter { it.book.inLibrary }
            .map { ctx ->
                // The shelf "Completed" category always means completed. Otherwise an explicit
                // choice from the reader wins, and "" (automatic) falls back to the reading data.
                val chosen = TrackedStatus.entries.firstOrNull { it.name == ctx.book.readingStatus }
                val status = when {
                    ctx.book.completed -> TrackedStatus.COMPLETED
                    chosen != null -> chosen
                    ctx.chaptersReadCount > 0 -> TrackedStatus.READING
                    else -> TrackedStatus.PLAN_TO_READ
                }
                LibraryEntry(
                    book = ctx.book,
                    tracked = TrackedBook(
                        url = ctx.book.url,
                        title = ctx.book.title,
                        tags = ctx.book.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        author = recommendationDao.getDiscoveredNovel(ctx.book.url)?.author?.takeIf { it.isNotBlank() },
                        status = status,
                        chaptersRead = ctx.chaptersReadCount,
                        chapterCount = ctx.chaptersCount,
                        readingTimeSeconds = 0L,
                        lastReadEpochMilli = ctx.book.lastReadEpochTimeMilli,
                    ),
                )
            }

    private suspend fun loadCandidates(
        excludeUrls: Set<String>,
        filter: UserFilterManager.FilterState,
        allowedSources: Set<String>?,
    ): List<NovelVector> {
        val out = LinkedHashMap<String, NovelVector>()
        for (entity in recommendationDao.getAllDiscoveredNovels()) {
            if (entity.url in excludeUrls || entity.url in out) continue
            if (allowedSources != null && entity.apiName !in allowedSources) continue
            val vector = discoveredVector(entity)
            if (!userFilterManager.shouldFilter(vector, filter)) out[entity.url] = vector
        }
        return out.values.toList()
    }

    private fun libraryVector(entry: LibraryEntry): NovelVector = buildVector(
        url = entry.tracked.url,
        name = entry.tracked.title,
        providerName = "",
        rawTags = entry.tracked.tags,
        author = entry.tracked.author,
        rating = null,
        chapterCount = entry.tracked.chapterCount,
        isCompleted = entry.tracked.status == TrackedStatus.COMPLETED,
        synopsis = entry.book.description.ifBlank { null },
        posterUrl = entry.book.coverImageUrl.ifBlank { null },
    )

    private fun discoveredVector(entity: DiscoveredNovelEntity): NovelVector = buildVector(
        url = entity.url,
        name = entity.name,
        providerName = entity.apiName,
        rawTags = entity.tags,
        author = entity.author,
        rating = entity.rating,
        chapterCount = 0,
        isCompleted = entity.status?.lowercase() == "completed",
        synopsis = entity.synopsis,
        posterUrl = entity.posterUrl,
    )

    companion object {
        /** Builds a vector from stored facts. Title tags always count; synopsis tags fill gaps. */
        internal fun buildVector(
            url: String,
            name: String,
            providerName: String,
            rawTags: List<String>,
            author: String?,
            rating: Int?,
            chapterCount: Int,
            isCompleted: Boolean,
            synopsis: String?,
            posterUrl: String?,
        ): NovelVector {
            val normalized = TagNormalizer.normalizeAll(rawTags)
            val titleTags = SynopsisTagExtractor.extractFromTitle(name)
            val tags: Set<TagCategory> = if (normalized.size < 3) {
                normalized + titleTags + SynopsisTagExtractor.extractTags(synopsis, maxTags = 8)
            } else {
                normalized + titleTags
            }
            return NovelVector(
                url = url,
                name = name,
                providerName = providerName,
                tags = tags,
                rawTags = rawTags,
                authorNormalized = NovelVector.normalizeAuthor(author),
                rating = rating,
                chapterCount = chapterCount,
                isCompleted = isCompleted,
                synopsisKeywords = SynopsisTagExtractor.extractContentKeywords(synopsis, maxKeywords = 25),
                titleKeywords = NovelVector.extractTitleKeywords(name),
                posterUrl = posterUrl,
                synopsis = synopsis,
            )
        }

        internal fun similarityBreakdown(source: NovelVector, candidate: NovelVector) = ScoreBreakdown(
            tagSimilarity = SimilarityCalculator.calculateTagSimilarity(source.tags, candidate.tags),
            authorMatch = SimilarityCalculator.calculateAuthorSimilarity(source.authorNormalized, candidate.authorNormalized),
            ratingScore = SimilarityCalculator.calculateRatingSimilarity(source.rating, candidate.rating),
            providerBoost = if (candidate.providerName == source.providerName) 1f else 0f,
            synopsisMatch = SimilarityCalculator.calculateSynopsisSimilarity(source.synopsisKeywords, candidate.synopsisKeywords),
        )
    }
}

/**
 * One generation pass. Holds the shared state every rail needs and tracks which books have
 * already been placed, so a book appears in one rail unless the overlap allowance lets it
 * repeat.
 */
private class Pass(
    private val config: RecommendationConfig,
    private val profile: UserTasteProfile,
    private val scorer: NovelScorer,
    private val filterManager: UserFilterManager,
    private val filter: UserFilterManager.FilterState,
    private val authorNames: Map<String, String>,
    private val authorScores: Map<String, Int>,
) {
    private val used = HashSet<String>()
    private val overlap = (config.maxPerGroup * config.allowedOverlapRatio).toInt()

    private fun sourceName(id: String): String = config.sourceNames[id] ?: id

    /** Unused books first, then up to the overlap allowance of books already placed. */
    private fun pool(candidates: List<NovelVector>): List<NovelVector> {
        val fresh = candidates.filter { it.url !in used }
        val reused = candidates.filter { it.url in used }.take(overlap)
        return fresh + reused
    }

    private fun group(
        type: RecommendationType,
        title: String,
        subtitle: String?,
        recs: List<Recommendation>,
        sourceTitle: String? = null,
    ): RecommendationGroup {
        used += recs.map { it.bookUrl }
        return RecommendationGroup(
            type = type,
            title = title,
            subtitle = subtitle,
            recommendations = recs,
            sourceBookTitle = sourceTitle,
        )
    }

    fun recommendation(
        v: NovelVector,
        type: RecommendationType,
        breakdown: ScoreBreakdown,
        sourceUrl: String? = null,
        sourceTitle: String? = null,
        cross: Boolean = false,
    ): Recommendation {
        val boosted = (breakdown.total * filterManager.calculateBoostFactor(v, filter)).coerceAtMost(1f)
        return Recommendation(
            bookUrl = v.url,
            title = v.name,
            coverUrl = v.posterUrl.orEmpty(),
            sourceId = v.providerName,
            rating = v.rating,
            score = boosted,
            type = type,
            reason = reasonFor(v, type, breakdown, sourceTitle),
            scoreBreakdown = breakdown,
            sourceBookUrl = sourceUrl,
            sourceBookTitle = sourceTitle,
            isCrossSource = cross,
        )
    }

    private fun reasonFor(
        v: NovelVector,
        type: RecommendationType,
        b: ScoreBreakdown,
        sourceTitle: String?,
    ): String {
        val matching = v.tags
            .filter { tag -> profile.preferredTags.any { it.tag == tag } }
            .take(2)
            .map { TagNormalizer.getDisplayName(it) }
        val ratingText = v.rating?.let { "%.1f★".format(it / 200f) }
        val source = sourceName(v.providerName)
        return when (type) {
            RecommendationType.FOR_YOU -> when {
                matching.isNotEmpty() -> "Matches your love of ${matching.joinToString(" & ")}"
                ratingText != null -> "$ratingText on $source"
                else -> "Recommended for you"
            }
            RecommendationType.SIMILAR_TO, RecommendationType.BECAUSE_YOU_READ -> when {
                b.authorMatch > 0.5f -> "Same author"
                b.tagSimilarity > 0.5f && matching.isNotEmpty() -> "Similar ${matching.first()}"
                b.synopsisMatch > 0.3f -> "Similar themes"
                sourceTitle != null -> "Similar vibes"
                else -> "You might like this"
            }
            RecommendationType.TOP_RATED_FOR_YOU -> ratingText ?: "Highly rated"
            RecommendationType.DISCOVER_NEW_SOURCE ->
                if (ratingText != null) "$ratingText on $source" else "Popular on $source"
            RecommendationType.TRENDING_IN_YOUR_GENRES -> ratingText ?: "Trending"
            RecommendationType.NEW_FOR_YOU -> "Ongoing series"
            RecommendationType.FROM_AUTHORS_YOU_LIKE -> "From an author you like"
        }
    }

    fun forYou(candidates: List<NovelVector>): RecommendationGroup? {
        val pool = pool(candidates)
        if (pool.isEmpty()) return null
        val recs = scorer.scoreAndRank(pool, config.preferredSource, config.maxPerGroup * 2)
            .filter { (_, b) -> b.total >= config.minScore }
            .take(config.maxPerGroup)
            .map { (v, b) -> recommendation(v, RecommendationType.FOR_YOU, b) }
        if (recs.isEmpty()) return null
        return group(RecommendationType.FOR_YOU, "For You", "Based on your reading history", recs)
    }

    fun fromAuthors(candidates: List<NovelVector>): RecommendationGroup? {
        val liked = authorScores.filter { it.value >= 600 }.keys
        if (liked.isEmpty()) return null
        val matches = candidates
            .filter { v -> v.url !in used && v.authorNormalized != null && v.authorNormalized in liked }
            .sortedWith(
                compareByDescending<NovelVector> { authorScores[it.authorNormalized] ?: 0 }
                    .thenByDescending { it.rating ?: 0 },
            )
            .take(config.maxPerGroup)
        if (matches.size < 2) return null
        val recs = matches.map { v ->
            val breakdown = ScoreBreakdown(
                authorMatch = (authorScores[v.authorNormalized] ?: 500) / 1000f,
                ratingScore = (v.rating ?: 0) / 1000f,
                tagSimilarity = 0.5f,
                userPreferenceMatch = 0.6f,
            )
            recommendation(v, RecommendationType.FROM_AUTHORS_YOU_LIKE, breakdown).copy(
                reason = "By ${authorNames[v.authorNormalized] ?: v.authorNormalized ?: "Unknown"}",
            )
        }
        val authorCount = matches.mapNotNull { it.authorNormalized }.distinct().size
        val title = when {
            authorCount == 1 -> "More from ${authorNames[matches.first().authorNormalized] ?: ""}"
            authorCount <= 3 -> "From Authors You Love"
            else -> "From Your Favorite Authors"
        }
        return group(
            RecommendationType.FROM_AUTHORS_YOU_LIKE,
            title,
            "Other works by authors you've enjoyed",
            recs,
        )
    }

    fun becauseYouRead(source: NovelVector, candidates: List<NovelVector>): RecommendationGroup? {
        val pool = pool(candidates).filter { it.url != source.url }
        if (pool.isEmpty()) return null
        val similar = SimilarityCalculator.findSimilarWithQuality(
            target = source,
            candidates = pool,
            limit = config.maxPerGroup,
            minSimilarity = 0.12f,
        )
        if (similar.isEmpty()) return null
        val recs = similar.map { (v, _) ->
            recommendation(
                v = v,
                type = RecommendationType.BECAUSE_YOU_READ,
                breakdown = RecommendationEngine.similarityBreakdown(source, v),
                sourceUrl = source.url,
                sourceTitle = source.name,
            )
        }
        val display = if (source.name.length > 25) source.name.take(22) + "..." else source.name
        return group(
            RecommendationType.BECAUSE_YOU_READ,
            "Because you read $display",
            "Similar stories you might enjoy",
            recs,
            sourceTitle = source.name,
        )
    }

    fun topRated(candidates: List<NovelVector>): RecommendationGroup? {
        val pool = pool(candidates.filter { it.rating != null })
            .sortedByDescending { it.rating ?: 0 }
        if (pool.isEmpty()) return null
        val recs = pool.take(config.maxPerGroup).map { v ->
            recommendation(
                v,
                RecommendationType.TOP_RATED_FOR_YOU,
                ScoreBreakdown(ratingScore = (v.rating ?: 0) / 1000f, popularityScore = 0.7f),
            )
        }
        return group(RecommendationType.TOP_RATED_FOR_YOU, "Highest Rated", "Top-rated stories across all sources", recs)
    }

    /** One rail per frequently occurring genre in the pool. */
    fun genreGroups(candidates: List<NovelVector>): List<RecommendationGroup> {
        val counts = HashMap<TagCategory, Int>()
        candidates.forEach { v -> v.tags.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        val popular = counts.entries
            .filter { it.value >= 5 }
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }
        val out = ArrayList<RecommendationGroup>()
        for (genre in popular) {
            val list = candidates
                .filter { genre in it.tags && it.url !in used }
                .sortedByDescending { it.rating ?: 0 }
                .take(config.maxPerGroup)
            if (list.size < 3) continue
            val name = TagNormalizer.getDisplayName(genre)
            val recs = list.map { v ->
                recommendation(
                    v,
                    RecommendationType.TRENDING_IN_YOUR_GENRES,
                    ScoreBreakdown(tagSimilarity = 0.8f, ratingScore = (v.rating ?: 0) / 1000f),
                )
            }
            out += group(RecommendationType.TRENDING_IN_YOUR_GENRES, "Best in $name", "Top $name novels", recs)
        }
        return out
    }

    fun discover(candidates: List<NovelVector>, preferred: String): RecommendationGroup? {
        val list = candidates
            .filter { it.providerName != preferred && it.url !in used }
            .sortedByDescending { it.rating ?: 0 }
            .take(config.maxPerGroup)
        if (list.isEmpty()) return null
        val recs = list.map { v ->
            recommendation(
                v,
                RecommendationType.DISCOVER_NEW_SOURCE,
                ScoreBreakdown(ratingScore = (v.rating ?: 0) / 1000f, popularityScore = 0.6f),
                cross = true,
            )
        }
        return group(RecommendationType.DISCOVER_NEW_SOURCE, "Discover New Sources", "Great novels from other platforms", recs)
    }

    /** Two rails for the two sources with the most candidates. */
    fun providerGroups(candidates: List<NovelVector>): List<RecommendationGroup> {
        val byProvider = candidates.groupBy { it.providerName }
        if (byProvider.size < 2) return emptyList()
        val top = byProvider.entries.sortedByDescending { it.value.size }.take(2)
        val out = ArrayList<RecommendationGroup>()
        for (entry in top) {
            val list = entry.value
                .filter { it.url !in used }
                .sortedWith(compareByDescending<NovelVector> { it.rating ?: 0 }.thenBy { it.name })
                .take(config.maxPerGroup)
            if (list.size < 3) continue
            val name = sourceName(entry.key)
            val recs = list.map { v ->
                recommendation(
                    v,
                    RecommendationType.TRENDING_IN_YOUR_GENRES,
                    ScoreBreakdown(ratingScore = (v.rating ?: 0) / 1000f, providerBoost = 1f, popularityScore = 0.7f),
                )
            }
            out += group(RecommendationType.TRENDING_IN_YOUR_GENRES, "Trending on $name", "Popular novels from $name", recs)
        }
        return out
    }

    fun newReleases(candidates: List<NovelVector>): RecommendationGroup? {
        val list = candidates
            .filter { !it.isCompleted && it.url !in used }
            .sortedByDescending { it.rating ?: 0 }
            .take(config.maxPerGroup)
        if (list.size < 3) return null
        val recs = list.map { v ->
            recommendation(
                v,
                RecommendationType.NEW_FOR_YOU,
                ScoreBreakdown(ratingScore = (v.rating ?: 0) / 1000f, popularityScore = 0.6f),
            )
        }
        return group(RecommendationType.NEW_FOR_YOU, "Ongoing Series", "Active stories still being updated", recs)
    }
}
