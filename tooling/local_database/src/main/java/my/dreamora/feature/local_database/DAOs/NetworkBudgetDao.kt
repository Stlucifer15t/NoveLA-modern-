package my.dreamora.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import my.dreamora.feature.local_database.tables.DiscoveryChainEntity
import my.dreamora.feature.local_database.tables.NetworkBudgetEntity

/** Per-source request budget and discovery-walk bookkeeping. Ported from Novery (GPL-3.0). */
@Dao
interface NetworkBudgetDao {

    @Query("SELECT * FROM network_budget WHERE providerName = :providerName AND date = :date")
    suspend fun getBudget(providerName: String, date: Long): NetworkBudgetEntity?

    @Query("SELECT * FROM network_budget WHERE date = :date")
    suspend fun getAllBudgetsForDay(date: Long): List<NetworkBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: NetworkBudgetEntity)

    @Query(
        """
        UPDATE network_budget SET
            requestCount = requestCount + 1,
            lastRequestAt = :timestamp
        WHERE providerName = :providerName AND date = :date
        """
    )
    suspend fun incrementRequestCount(providerName: String, date: Long, timestamp: Long)

    @Query(
        """
        UPDATE network_budget SET
            failedCount = failedCount + 1,
            consecutiveFailures = consecutiveFailures + 1,
            inCooldown = 1,
            cooldownUntil = :cooldownUntil
        WHERE providerName = :providerName AND date = :date
        """
    )
    suspend fun recordFailure(providerName: String, date: Long, cooldownUntil: Long)

    @Query(
        """
        UPDATE network_budget SET
            consecutiveFailures = 0,
            inCooldown = 0,
            cooldownUntil = 0
        WHERE providerName = :providerName AND date = :date
        """
    )
    suspend fun clearCooldown(providerName: String, date: Long)

    @Query("SELECT SUM(requestCount) FROM network_budget WHERE date = :date")
    suspend fun getTotalRequestsForDay(date: Long): Int?

    @Query("DELETE FROM network_budget WHERE date < :threshold")
    suspend fun deleteOldBudgets(threshold: Long)

    @Query("SELECT * FROM discovery_chains WHERE novelUrl = :novelUrl AND sessionId = :sessionId")
    suspend fun getChainEntry(novelUrl: String, sessionId: String): DiscoveryChainEntity?

    @Query("SELECT MAX(depth) FROM discovery_chains WHERE sessionId = :sessionId")
    suspend fun getMaxDepthForSession(sessionId: String): Int?

    @Query("SELECT COUNT(*) FROM discovery_chains WHERE sessionId = :sessionId")
    suspend fun getChainSizeForSession(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainEntry(entry: DiscoveryChainEntity)

    @Query("DELETE FROM discovery_chains WHERE sessionId = :sessionId")
    suspend fun clearChainSession(sessionId: String)

    @Query("DELETE FROM discovery_chains WHERE discoveredAt < :threshold")
    suspend fun deleteOldChains(threshold: Long)

    @Query("SELECT * FROM network_budget ORDER BY date DESC, providerName ASC")
    fun observeAllBudgets(): Flow<List<NetworkBudgetEntity>>
}
