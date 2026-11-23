package my.dreamora.data.recommendation

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import my.dreamora.feature.local_database.DAOs.NetworkBudgetDao
import my.dreamora.feature.local_database.tables.DiscoveryChainEntity
import my.dreamora.feature.local_database.tables.NetworkBudgetEntity
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NetworkBudgetManager"

/**
 * Keeps For You's background requests to each source within a daily budget, a minimum gap
 * between requests, and an exponential cooldown after failures.
 *
 * Budgets are keyed by source id. Novery's per-site table is keyed by site names that do not
 * exist in this app, so every source uses [defaultBudget] until a source-specific entry is
 * added to [providerBudgets].
 */
@Singleton
class NetworkBudgetManager @Inject constructor(
    private val budgetDao: NetworkBudgetDao,
) {

    /** Per-source budget configuration. */
    data class ProviderBudget(
        /** Maximum requests per day for discovery / recommendation */
        val dailyDiscoveryLimit: Int,
        /** Maximum requests per day for user-initiated actions */
        val dailyUserLimit: Int,
        /** Minimum delay between requests in ms */
        val minRequestDelayMs: Long,
        /** Base cooldown after a failure; doubled per consecutive failure */
        val baseCooldownMs: Long,
        /** Maximum cooldown time */
        val maxCooldownMs: Long,
    )

    private val providerBudgets: Map<String, ProviderBudget> = emptyMap()

    private val defaultBudget = ProviderBudget(
        dailyDiscoveryLimit = 100,
        dailyUserLimit = 300,
        minRequestDelayMs = 1000,
        baseCooldownMs = 60_000,
        maxCooldownMs = 3_600_000,
    )

    /** Discovery chain limits */
    object ChainLimits {
        const val MAX_DEPTH = 2
        const val MAX_NOVELS_PER_SESSION = 50
        const val MAX_RELATED_PER_NOVEL = 5
    }

    enum class RequestType {
        /** Background discovery for recommendations */
        DISCOVERY,

        /** User-initiated (browse, read, search) */
        USER,
    }

    private val budgetMutex = Mutex()

    /** In-memory last-request time per source. Kept for parity with Novery's pacing. */
    private val lastRequestTimes = mutableMapOf<String, Long>()
    private val requestTimeMutex = Mutex()

    /**
     * Whether a request to [providerName] may go out now.
     * @return Pair of (canRequest, waitTimeMs when it cannot)
     */
    suspend fun canMakeRequest(
        providerName: String,
        requestType: RequestType = RequestType.USER,
    ): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        budgetMutex.withLock {
            val today = currentEpochDay()
            val budget = getOrCreateBudget(providerName, today)
            val config = providerBudget(providerName)

            if (budget.inCooldown && System.currentTimeMillis() < budget.cooldownUntil) {
                return@withContext false to (budget.cooldownUntil - System.currentTimeMillis())
            }
            if (budget.inCooldown && System.currentTimeMillis() >= budget.cooldownUntil) {
                budgetDao.clearCooldown(providerName, today)
            }

            val limit = when (requestType) {
                RequestType.DISCOVERY -> config.dailyDiscoveryLimit
                RequestType.USER -> config.dailyUserLimit
            }
            if (budget.requestCount >= limit) {
                return@withContext false to millisUntilMidnight()
            }

            val sinceLast = System.currentTimeMillis() - budget.lastRequestAt
            if (sinceLast < config.minRequestDelayMs) {
                return@withContext false to (config.minRequestDelayMs - sinceLast)
            }
            true to 0L
        }
    }

    /** Requests left today for [providerName]. */
    suspend fun getRemainingBudget(
        providerName: String,
        requestType: RequestType = RequestType.USER,
    ): Int = withContext(Dispatchers.IO) {
        val budget = budgetDao.getBudget(providerName, currentEpochDay())
        val config = providerBudget(providerName)
        val limit = when (requestType) {
            RequestType.DISCOVERY -> config.dailyDiscoveryLimit
            RequestType.USER -> config.dailyUserLimit
        }
        (limit - (budget?.requestCount ?: 0)).coerceAtLeast(0)
    }

    /** Today's budget state for every source that has made a request today. */
    suspend fun getAllRemainingBudgets(): Map<String, BudgetStatus> = withContext(Dispatchers.IO) {
        budgetDao.getAllBudgetsForDay(currentEpochDay()).associate { row ->
            val config = providerBudget(row.providerName)
            row.providerName to BudgetStatus(
                used = row.requestCount,
                discoveryLimit = config.dailyDiscoveryLimit,
                userLimit = config.dailyUserLimit,
                inCooldown = row.inCooldown,
                cooldownUntil = row.cooldownUntil,
            )
        }
    }

    /** Records one request that went out to [providerName]. */
    suspend fun recordRequest(providerName: String) = withContext(Dispatchers.IO) {
        budgetMutex.withLock {
            val today = currentEpochDay()
            val now = System.currentTimeMillis()
            if (budgetDao.getBudget(providerName, today) != null) {
                budgetDao.incrementRequestCount(providerName, today, now)
            } else {
                budgetDao.insertBudget(
                    NetworkBudgetEntity(
                        providerName = providerName,
                        date = today,
                        requestCount = 1,
                        lastRequestAt = now,
                    ),
                )
            }
        }
        requestTimeMutex.withLock {
            lastRequestTimes[providerName] = System.currentTimeMillis()
        }
    }

    /** Records a rate limit or failure and starts an exponential cooldown. */
    suspend fun recordFailure(providerName: String) = withContext(Dispatchers.IO) {
        budgetMutex.withLock {
            val today = currentEpochDay()
            val config = providerBudget(providerName)
            val existing = budgetDao.getBudget(providerName, today)
            val failures = (existing?.consecutiveFailures ?: 0) + 1
            val cooldownMs = (config.baseCooldownMs * (1L shl failures.coerceAtMost(5)))
                .coerceAtMost(config.maxCooldownMs)
            val cooldownUntil = System.currentTimeMillis() + cooldownMs
            if (existing != null) {
                budgetDao.recordFailure(providerName, today, cooldownUntil)
            } else {
                budgetDao.insertBudget(
                    NetworkBudgetEntity(
                        providerName = providerName,
                        date = today,
                        requestCount = 1,
                        failedCount = 1,
                        consecutiveFailures = 1,
                        inCooldown = true,
                        cooldownUntil = cooldownUntil,
                        lastRequestAt = System.currentTimeMillis(),
                    ),
                )
            }
            Log.w(TAG, "$providerName failed; cooldown ${cooldownMs}ms (failures: $failures)")
        }
    }

    /**
     * Waits for a request slot. Gives up when the wait is longer than a minute, so a
     * background pass never sits on one source.
     */
    suspend fun waitForSlot(providerName: String): Boolean = withContext(Dispatchers.IO) {
        repeat(10) {
            val (canRequest, waitMs) = canMakeRequest(providerName, RequestType.DISCOVERY)
            if (canRequest) return@withContext true
            if (waitMs > 60_000) return@withContext false
            if (waitMs > 0) delay(waitMs)
        }
        false
    }

    fun createSessionId(): String = UUID.randomUUID().toString()

    suspend fun canDiscoverMore(sessionId: String, currentDepth: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (currentDepth >= ChainLimits.MAX_DEPTH) return@withContext false
            budgetDao.getChainSizeForSession(sessionId) < ChainLimits.MAX_NOVELS_PER_SESSION
        }

    suspend fun isAlreadyDiscovered(novelUrl: String, sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            budgetDao.getChainEntry(novelUrl, sessionId) != null
        }

    suspend fun recordDiscovery(novelUrl: String, sessionId: String, depth: Int) =
        withContext(Dispatchers.IO) {
            budgetDao.insertChainEntry(
                DiscoveryChainEntity(novelUrl = novelUrl, sessionId = sessionId, depth = depth),
            )
        }

    suspend fun clearSession(sessionId: String) = withContext(Dispatchers.IO) {
        budgetDao.clearChainSession(sessionId)
    }

    /** Drops budget rows older than a week and chain rows older than an hour. */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        budgetDao.deleteOldBudgets(currentEpochDay() - 7)
        budgetDao.deleteOldChains(System.currentTimeMillis() - 60 * 60 * 1000L)
    }

    private fun providerBudget(providerName: String): ProviderBudget =
        providerBudgets[providerName] ?: defaultBudget

    private suspend fun getOrCreateBudget(providerName: String, date: Long): NetworkBudgetEntity =
        budgetDao.getBudget(providerName, date) ?: NetworkBudgetEntity(
            providerName = providerName,
            date = date,
        ).also { budgetDao.insertBudget(it) }

    private fun currentEpochDay(): Long = LocalDate.now().toEpochDay()

    private fun millisUntilMidnight(): Long {
        val midnight = (currentEpochDay() + 1) * 24 * 60 * 60 * 1000L
        return midnight - System.currentTimeMillis()
    }
}

/** Today's budget state for one source, for display. */
data class BudgetStatus(
    val used: Int,
    val discoveryLimit: Int,
    val userLimit: Int,
    val inCooldown: Boolean,
    val cooldownUntil: Long,
) {
    val discoveryRemaining: Int get() = (discoveryLimit - used).coerceAtLeast(0)
    val userRemaining: Int get() = (userLimit - used).coerceAtLeast(0)
    val discoveryPercentUsed: Float get() = (used.toFloat() / discoveryLimit).coerceIn(0f, 1f)
}
