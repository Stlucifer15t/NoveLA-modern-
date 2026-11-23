package my.dreamora.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import my.dreamora.feature.local_database.tables.DiscoveredNovelEntity
import my.dreamora.feature.local_database.tables.UserPreferenceEntity

/** Tag preferences and the discovered-novel cache for For You. Ported from Novery (GPL-3.0). */
@Dao
interface RecommendationDao {

    @Query("SELECT * FROM user_preferences ORDER BY affinityScore DESC")
    suspend fun getAllPreferences(): List<UserPreferenceEntity>

    @Query("SELECT * FROM user_preferences ORDER BY affinityScore DESC")
    fun observeAllPreferences(): Flow<List<UserPreferenceEntity>>

    @Query("SELECT * FROM user_preferences WHERE tag = :tag")
    suspend fun getPreference(tag: String): UserPreferenceEntity?

    @Query("SELECT * FROM user_preferences ORDER BY affinityScore DESC LIMIT :limit")
    suspend fun getTopPreferences(limit: Int): List<UserPreferenceEntity>

    @Query("SELECT * FROM user_preferences WHERE affinityScore >= :minScore ORDER BY affinityScore DESC")
    suspend fun getPreferencesAboveScore(minScore: Int): List<UserPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: List<UserPreferenceEntity>)

    @Query(
        """
        UPDATE user_preferences SET
            affinityScore = :score,
            novelCount = novelCount + :novelDelta,
            chaptersRead = chaptersRead + :chaptersDelta,
            readingTimeSeconds = readingTimeSeconds + :timeDelta,
            completedCount = completedCount + :completedDelta,
            droppedCount = droppedCount + :droppedDelta,
            updatedAt = :updatedAt
        WHERE tag = :tag
        """
    )
    suspend fun updatePreference(
        tag: String,
        score: Int,
        novelDelta: Int = 0,
        chaptersDelta: Int = 0,
        timeDelta: Long = 0,
        completedDelta: Int = 0,
        droppedDelta: Int = 0,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM user_preferences")
    suspend fun clearAllPreferences()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoveredNovel(novel: DiscoveredNovelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoveredNovels(novels: List<DiscoveredNovelEntity>)

    @Query("SELECT * FROM discovered_novels ORDER BY discoveredAt DESC")
    suspend fun getAllDiscoveredNovels(): List<DiscoveredNovelEntity>

    @Query("SELECT * FROM discovered_novels WHERE apiName = :apiName ORDER BY discoveredAt DESC")
    suspend fun getDiscoveredNovelsByProvider(apiName: String): List<DiscoveredNovelEntity>

    @Query(
        """
        SELECT * FROM discovered_novels
        WHERE tagsString IS NOT NULL AND tagsString != ''
        ORDER BY discoveredAt DESC
        """
    )
    suspend fun getDiscoveredNovelsWithTags(): List<DiscoveredNovelEntity>

    @Query("SELECT COUNT(*) FROM discovered_novels")
    suspend fun getDiscoveredNovelCount(): Int

    @Query("SELECT COUNT(*) FROM discovered_novels WHERE apiName = :apiName")
    suspend fun getDiscoveredNovelCountByProvider(apiName: String): Int

    @Query("SELECT DISTINCT apiName FROM discovered_novels")
    suspend fun getDiscoveredProviders(): List<String>

    @Query("SELECT * FROM discovered_novels WHERE url = :url")
    suspend fun getDiscoveredNovel(url: String): DiscoveredNovelEntity?

    @Query("DELETE FROM discovered_novels WHERE discoveredAt < :threshold")
    suspend fun deleteOldDiscoveredNovels(threshold: Long)

    @Query("DELETE FROM discovered_novels WHERE apiName = :apiName")
    suspend fun deleteDiscoveredNovelsByProvider(apiName: String)

    @Query("DELETE FROM discovered_novels")
    suspend fun clearAllDiscoveredNovels()
}
