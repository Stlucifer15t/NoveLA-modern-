package my.dreamora.settings

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
    /** Books on the shelf whose source says they are finished, or whose every chapter is read. */
    val booksFinished: Int = 0,
    /** Distinct local days with a chapter open — the history table, not a guess. */
    val daysActive: Int = 0,
    /** Video titles on the shelf (Book.contentType = video). */
    val moviesInLibrary: Int = 0,
    /** Episodes the player reported a position for, i.e. actually opened. */
    val episodesWatched: Int = 0,
    /** Seconds watched, summed from the player's own position, capped at the episode length. */
    val watchSeconds: Long = 0,
    /** Measured reading time (chapter open in the reader). -1 while the first read is in flight. */
    val readSecondsToday: Long = -1,
    val readSecondsThisWeek: Long = -1,
    val readSecondsThisMonth: Long = -1,
    val readSecondsAllTime: Long = -1,
    /** Seven values, Monday first: measured seconds for each day of the current week. */
    val weekDailySeconds: List<Long> = emptyList(),
    /** Days inside the current week / month with any measured reading. */
    val daysWithReadingWeek: Int = 0,
    val daysWithReadingMonth: Int = 0,
    /** Top books by measured reading time, largest first. */
    val mostRead: List<MostReadBook> = emptyList(),
)

/** A book in the Most Read list. [sourceName] is null for local books or unknown sources. */
data class MostReadBook(
    val bookUrl: String,
    val title: String,
    val coverUrl: String,
    val sourceName: String?,
    val seconds: Long,
)

/**
 * Minutes, for the one place that shows a duration rather than a count. A negative total means
 * "the first read has not finished", which the screen renders as an ellipsis, exactly as the
 * integer counters above do.
 */
internal fun ReadingStats.watchMinutes(): Long = if (watchSeconds < 0) -1 else watchSeconds / 60

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
 * How many separate days carry a timestamp, which is the "days with reading" counter on the
 * stats screen. Day boundaries are the device's own, so a chapter opened at 00:20 counts to the
 * day the reader considers it, not to UTC's.
 */
internal fun activeDays(timestampsMillis: List<Long>): Int =
    timestampsMillis.mapTo(HashSet()) { dayIndex(it) }.size

/**
 * The local calendar day a timestamp falls on. The offset is added before the division because
 * `toDays` alone would cut the day at UTC midnight.
 */
internal fun dayIndex(millis: Long): Long =
    TimeUnit.MILLISECONDS.toDays(millis + TimeZone.getDefault().getOffset(millis))

/**
 * -1 means "not read yet": the hub shows an ellipsis instead of a confident 0 while the
 * first database read is still in flight.
 */
internal fun Int.displayCount(): String = if (this < 0) "…" else toString()
