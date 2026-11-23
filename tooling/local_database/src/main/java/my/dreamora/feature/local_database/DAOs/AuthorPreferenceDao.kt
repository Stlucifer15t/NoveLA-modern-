package my.dreamora.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import my.dreamora.feature.local_database.tables.AuthorPreferenceEntity

/** Author affinity rows for For You. Ported from Novery (GPL-3.0). */
@Dao
interface AuthorPreferenceDao {

    @Query("SELECT * FROM author_preferences WHERE authorNormalized = :authorNormalized")
    suspend fun getAuthor(authorNormalized: String): AuthorPreferenceEntity?

    @Query("SELECT * FROM author_preferences ORDER BY affinityScore DESC")
    suspend fun getAllAuthors(): List<AuthorPreferenceEntity>

    @Query("SELECT * FROM author_preferences ORDER BY affinityScore DESC")
    fun observeAllAuthors(): Flow<List<AuthorPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(author: AuthorPreferenceEntity)

    @Update
    suspend fun updateAuthor(author: AuthorPreferenceEntity)

    @Query("DELETE FROM author_preferences WHERE authorNormalized = :authorNormalized")
    suspend fun deleteAuthor(authorNormalized: String)

    @Query("DELETE FROM author_preferences")
    suspend fun clearAllAuthors()

    @Query(
        """
        SELECT * FROM author_preferences
        WHERE affinityScore >= :minScore
        ORDER BY affinityScore DESC
        LIMIT :limit
        """
    )
    suspend fun getTopAuthors(minScore: Int = 600, limit: Int = 10): List<AuthorPreferenceEntity>

    @Query(
        """
        SELECT * FROM author_preferences
        WHERE affinityScore >= 800 AND novelsCompleted >= 1
        ORDER BY affinityScore DESC, novelsCompleted DESC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteAuthors(limit: Int = 5): List<AuthorPreferenceEntity>

    @Query(
        """
        SELECT * FROM author_preferences
        WHERE affinityScore >= 600 AND novelsRead >= 1
        ORDER BY affinityScore DESC
        LIMIT :limit
        """
    )
    suspend fun getLikedAuthors(limit: Int = 20): List<AuthorPreferenceEntity>

    @Query(
        """
        SELECT * FROM author_preferences
        WHERE novelsDropped >= 2
        AND novelsDropped > (novelsRead * 0.5)
        ORDER BY novelsDropped DESC
        """
    )
    suspend fun getDislikedAuthors(): List<AuthorPreferenceEntity>

    @Query(
        """
        SELECT * FROM author_preferences
        WHERE displayName LIKE '%' || :query || '%'
        OR authorNormalized LIKE '%' || :query || '%'
        ORDER BY affinityScore DESC
        LIMIT :limit
        """
    )
    suspend fun searchAuthors(query: String, limit: Int = 20): List<AuthorPreferenceEntity>

    @Query("SELECT COUNT(*) FROM author_preferences")
    suspend fun getAuthorCount(): Int

    @Query(
        """
        SELECT * FROM author_preferences
        WHERE novelUrlsInLibrary != ''
        ORDER BY updatedAt DESC
        """
    )
    suspend fun getAuthorsWithNovelsInLibrary(): List<AuthorPreferenceEntity>

    @Query("SELECT SUM(totalReadingTimeSeconds) FROM author_preferences")
    suspend fun getTotalReadingTime(): Long?

    @Query("SELECT SUM(novelsRead) FROM author_preferences")
    suspend fun getTotalNovelsRead(): Int?

    @Query("SELECT AVG(affinityScore) FROM author_preferences WHERE novelsRead >= 1")
    suspend fun getAverageAffinityScore(): Float?

    @Query(
        """
        UPDATE author_preferences SET
            novelsRead = novelsRead + 1,
            affinityScore = CASE
                WHEN :scoreDelta > 0 THEN MIN(affinityScore + :scoreDelta, 1000)
                ELSE MAX(affinityScore + :scoreDelta, 0)
            END,
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
        """
    )
    suspend fun incrementNovelsRead(
        authorNormalized: String,
        scoreDelta: Int = 50,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE author_preferences SET
            novelsCompleted = novelsCompleted + 1,
            affinityScore = MIN(affinityScore + :scoreBoost, 1000),
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
        """
    )
    suspend fun incrementCompleted(
        authorNormalized: String,
        scoreBoost: Int = 100,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE author_preferences SET
            novelsDropped = novelsDropped + 1,
            affinityScore = MAX(affinityScore - :scorePenalty, 0),
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
        """
    )
    suspend fun incrementDropped(
        authorNormalized: String,
        scorePenalty: Int = 100,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE author_preferences SET
            totalChaptersRead = totalChaptersRead + :chapters,
            totalReadingTimeSeconds = totalReadingTimeSeconds + :seconds,
            affinityScore = MIN(affinityScore + :scoreBoost, 1000),
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
        """
    )
    suspend fun updateReadingProgress(
        authorNormalized: String,
        chapters: Int,
        seconds: Long,
        scoreBoost: Int = 10,
        timestamp: Long = System.currentTimeMillis()
    )
}
