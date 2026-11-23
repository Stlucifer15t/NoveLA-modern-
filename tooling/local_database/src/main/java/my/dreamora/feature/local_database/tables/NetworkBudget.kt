package my.dreamora.feature.local_database.tables

/*
 * Table definitions for the For You recommender. Ported from Novery
 * (https://github.com/1Finn2me/Novery, GPL-3.0); see NOTICE.md.
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Daily request budget per source, so For You does not hammer a site. */
@Entity(
    tableName = "network_budget",
    indices = [Index(value = ["providerName", "date"], unique = true)]
)
data class NetworkBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerName: String,
    val date: Long,
    val requestCount: Int = 0,
    val failedCount: Int = 0,
    val inCooldown: Boolean = false,
    val cooldownUntil: Long = 0,
    val lastRequestAt: Long = 0,
    val consecutiveFailures: Int = 0
)

/** One step of a discovery walk, so the walk does not loop back on itself. */
@Entity(
    tableName = "discovery_chains",
    indices = [Index(value = ["sessionId"])]
)
data class DiscoveryChainEntity(
    @PrimaryKey
    val novelUrl: String,
    val sessionId: String,
    val depth: Int,
    val discoveredAt: Long = System.currentTimeMillis()
)
