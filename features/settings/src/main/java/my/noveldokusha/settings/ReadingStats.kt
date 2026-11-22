package my.noveldokusha.settings

import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Counters for the top of the Settings screen. Everything in here is a number read from
 * the database the app already fills — an invented "level" or a "reading time" the app
 * never measures would be a decoration over a lie, so neither exists.
 */
data class ReadingStats(
    val libraryBooks: Int = 0,
    val chaptersRead: Int = 0,
    val streakDays: Int = 0,
)

/**
 * Consecutive local days that end today or yesterday, counted from chapter-open
 * timestamps. A streak that survived only until yesterday evening is already stale, which
 * is why "yesterday" is included: the counter must not punish someone for not reading at
 * 23:59.
 */
internal fun readingStreakDays(
    timestampsMillis: List<Long>,
    nowMillis: Long = System.currentTimeMillis(),
): Int {
    if (timestampsMillis.isEmpty()) return 0
    val zones = TimeZone.getDefault()
    fun dayIndex(millis: Long): Long = TimeUnit.MILLISECONDS.toDays(millis + zones.getOffset(millis))

    val days = timestampsMillis.map { dayIndex(it) }.distinct().sortedDescending()
    val today = dayIndex(nowMillis)
    // Count only from the most recent day if it is today or yesterday.
    val start = days.firstOrNull() ?: return 0
    if (today - start > 1) return 0
    var streak = 1
    for (i in 1 until days.size) {
        if (days[i - 1] - days[i] == 1L) streak++ else break
    }
    return streak
}

/**
 * -1 means "not read yet": the hub shows an ellipsis instead of a confident 0 while the
 * first database read is still in flight.
 */
internal fun Int.displayCount(): String = if (this < 0) "…" else toString()
