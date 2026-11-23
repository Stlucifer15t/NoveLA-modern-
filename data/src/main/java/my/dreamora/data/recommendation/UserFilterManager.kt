package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import my.dreamora.core.foryou.recommendation.TagNormalizer.TagCategory
import my.dreamora.core.foryou.recommendation.model.NovelVector
import my.dreamora.feature.local_database.DAOs.UserFilterDao
import my.dreamora.feature.local_database.tables.BlockedAuthorEntity
import my.dreamora.feature.local_database.tables.HiddenNovelEntity
import my.dreamora.feature.local_database.tables.HideReason
import my.dreamora.feature.local_database.tables.TagFilterType
import my.dreamora.feature.local_database.tables.UserTagFilterEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The reader's blocks, boosts and hidden books, applied to For You candidates.
 */
@Singleton
class UserFilterManager @Inject constructor(
    private val filterDao: UserFilterDao,
) {

    data class FilterState(
        val blockedTags: Set<TagCategory> = emptySet(),
        val boostedTags: Set<TagCategory> = emptySet(),
        val hiddenNovelUrls: Set<String> = emptySet(),
        /** Normalized author names */
        val blockedAuthors: Set<String> = emptySet(),
    )

    suspend fun getFilterState(): FilterState = withContext(Dispatchers.IO) {
        FilterState(
            blockedTags = filterDao.getBlockedTags().toTagCategories(),
            boostedTags = filterDao.getBoostedTags().toTagCategories(),
            hiddenNovelUrls = filterDao.getHiddenNovelUrls().toSet(),
            blockedAuthors = filterDao.getBlockedAuthors().toSet(),
        )
    }

    /** True when the candidate must not be shown at all. */
    fun shouldFilter(novel: NovelVector, filterState: FilterState): Boolean {
        if (novel.url in filterState.hiddenNovelUrls) return true
        val author = novel.authorNormalized
        if (author != null && author in filterState.blockedAuthors) return true
        if (novel.tags.any { it in filterState.blockedTags }) return true
        return false
    }

    /** 1.0 for no boost, up to 1.3 when several boosted tags match. */
    fun calculateBoostFactor(novel: NovelVector, filterState: FilterState): Float {
        return when (novel.tags.count { it in filterState.boostedTags }) {
            0 -> 1.0f
            1 -> 1.1f
            2 -> 1.2f
            else -> 1.3f
        }
    }

    fun observeTagFilters(): Flow<Map<TagCategory, TagFilterType>> =
        filterDao.observeAllTagFilters().map { filters ->
            filters.mapNotNull { entity ->
                val tag = runCatching { TagCategory.valueOf(entity.tag) }.getOrNull()
                val type = runCatching { TagFilterType.valueOf(entity.filterType) }.getOrNull()
                if (tag != null && type != null) tag to type else null
            }.toMap()
        }

    suspend fun setTagFilter(tag: TagCategory, filterType: TagFilterType) =
        withContext(Dispatchers.IO) {
            if (filterType == TagFilterType.NEUTRAL) {
                filterDao.removeTagFilter(tag.name)
            } else {
                filterDao.setTagFilter(UserTagFilterEntity(tag = tag.name, filterType = filterType.name))
            }
        }

    suspend fun blockTag(tag: TagCategory) = setTagFilter(tag, TagFilterType.BLOCKED)
    suspend fun boostTag(tag: TagCategory) = setTagFilter(tag, TagFilterType.BOOSTED)
    suspend fun clearTagFilter(tag: TagCategory) = setTagFilter(tag, TagFilterType.NEUTRAL)

    suspend fun getBlockedTags(): Set<TagCategory> = withContext(Dispatchers.IO) {
        filterDao.getBlockedTags().toTagCategories()
    }

    suspend fun getBoostedTags(): Set<TagCategory> = withContext(Dispatchers.IO) {
        filterDao.getBoostedTags().toTagCategories()
    }

    fun observeHiddenNovels(): Flow<List<HiddenNovelEntity>> = filterDao.observeHiddenNovels()

    suspend fun hideNovel(
        novelUrl: String,
        novelName: String,
        reason: HideReason = HideReason.NOT_INTERESTED,
    ) = withContext(Dispatchers.IO) {
        filterDao.hideNovel(HiddenNovelEntity(novelUrl = novelUrl, novelName = novelName, reason = reason.name))
    }

    suspend fun unhideNovel(novelUrl: String) = withContext(Dispatchers.IO) {
        filterDao.unhideNovel(novelUrl)
    }

    suspend fun isNovelHidden(novelUrl: String): Boolean = withContext(Dispatchers.IO) {
        filterDao.isNovelHidden(novelUrl)
    }

    fun observeBlockedAuthors(): Flow<List<BlockedAuthorEntity>> = filterDao.observeBlockedAuthors()

    suspend fun blockAuthor(authorNormalized: String, displayName: String) = withContext(Dispatchers.IO) {
        filterDao.blockAuthor(BlockedAuthorEntity(authorNormalized = authorNormalized, displayName = displayName))
    }

    suspend fun unblockAuthor(authorNormalized: String) = withContext(Dispatchers.IO) {
        filterDao.unblockAuthor(authorNormalized)
    }

    suspend fun clearAllFilters() = withContext(Dispatchers.IO) {
        filterDao.clearAllTagFilters()
        filterDao.clearAllHiddenNovels()
        filterDao.clearAllBlockedAuthors()
    }

    private fun List<String>.toTagCategories(): Set<TagCategory> =
        mapNotNull { runCatching { TagCategory.valueOf(it) }.getOrNull() }.toSet()
}
