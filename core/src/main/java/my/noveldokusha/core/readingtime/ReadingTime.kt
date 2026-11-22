package my.noveldokusha.core.readingtime

import my.noveldokusha.core.appPreferences.AppPreferences
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Reading time that the app actually measured: the time a chapter was open in the reader,
 * from the moment the reader came to the foreground until it left it. It starts counting
 * from the version that ships this code; nothing before it is estimated.
 *
 * The log is `day index -> book url -> milliseconds`, kept in preferences the same way the
 * per-book reader settings are kept, so it needs no database migration.
 */
internal const val READING_LOG_KEEP_DAYS = 400L

/** Local calendar day as a number of days since the epoch in [zone]. */
fun localDayIndex(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    // The offset goes in before the division, so a day starts at local midnight, not UTC's.
    TimeUnit.MILLISECONDS.toDays(millis + zone.getOffset(millis))

/** Local midnight that ends the day containing [millis]. */
internal fun nextLocalMidnight(millis: Long, zone: TimeZone): Long =
    Calendar.getInstance(zone).run {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

/**
 * Splits `[startMs, endMs)` at local midnights and returns milliseconds per day index.
 * A session that crosses midnight is credited to both days, not to the day it began on.
 */
fun splitReadingByDay(
    startMs: Long,
    endMs: Long,
    zone: TimeZone = TimeZone.getDefault(),
): Map<Long, Long> {
    val perDay = linkedMapOf<Long, Long>()
    var cursor = startMs
    while (cursor < endMs) {
        val dayEnd = nextLocalMidnight(cursor, zone).coerceAtLeast(cursor + 1)
        val until = minOf(dayEnd, endMs)
        val day = localDayIndex(cursor, zone)
        perDay[day] = (perDay[day] ?: 0L) + (until - cursor)
        cursor = until
    }
    return perDay
}

/** Day index of the Monday that starts the week containing [nowMillis]. */
fun weekStartDayIndex(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val dayOfWeek = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        .get(Calendar.DAY_OF_WEEK) // Sunday = 1 … Saturday = 7
    val sinceMonday = (dayOfWeek + 5) % 7 // Monday = 0, Sunday = 6
    return localDayIndex(nowMillis, zone) - sinceMonday
}

/** Day index of the first day of the month containing [nowMillis]. */
fun monthStartDayIndex(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val dayOfMonth = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        .get(Calendar.DAY_OF_MONTH)
    return localDayIndex(nowMillis, zone) - (dayOfMonth - 1)
}

/**
 * What the Reading Stats panel shows, computed from the log. Every value is a sum of
 * measured milliseconds; nothing is scaled or projected.
 */
data class ReadingPeriods(
    val todayMs: Long,
    val weekMs: Long,
    val monthMs: Long,
    val allTimeMs: Long,
    /** Seven values, Monday first, measured milliseconds per day of the current week. */
    val weekDailyMs: List<Long>,
    /** Days with any measured reading inside the current week (up to today). */
    val daysWithReadingWeek: Int,
    /** Days with any measured reading inside the current month (up to today). */
    val daysWithReadingMonth: Int,
    /** Milliseconds per book, all time, largest first. */
    val perBookMs: Map<String, Long>,
)

fun summarizeReadingLog(
    log: Map<String, Map<String, Long>>,
    nowMillis: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ReadingPeriods {
    val today = localDayIndex(nowMillis, zone)
    val weekStart = weekStartDayIndex(nowMillis, zone)
    val monthStart = monthStartDayIndex(nowMillis, zone)

    var todayMs = 0L
    var weekMs = 0L
    var monthMs = 0L
    var allTimeMs = 0L
    val weekDaily = LongArray(7)
    val weekDays = HashSet<Long>()
    val monthDays = HashSet<Long>()
    val perBook = HashMap<String, Long>()

    for ((dayKey, books) in log) {
        val day = dayKey.toLongOrNull() ?: continue
        for ((bookUrl, ms) in books) {
            if (ms <= 0L) continue
            allTimeMs += ms
            perBook[bookUrl] = (perBook[bookUrl] ?: 0L) + ms
            if (day == today) todayMs += ms
            if (day in weekStart..today) {
                weekMs += ms
                weekDays += day
            }
            if (day in monthStart..today) {
                monthMs += ms
                monthDays += day
            }
            val slot = day - weekStart
            if (slot in 0L..6L) weekDaily[slot.toInt()] += ms
        }
    }

    return ReadingPeriods(
        todayMs = todayMs,
        weekMs = weekMs,
        monthMs = monthMs,
        allTimeMs = allTimeMs,
        weekDailyMs = weekDaily.toList(),
        daysWithReadingWeek = weekDays.size,
        daysWithReadingMonth = monthDays.size,
        perBookMs = perBook.entries.sortedByDescending { it.value }.associate { it.key to it.value },
    )
}

/** Adds one measured session to the log and drops days older than the keep window. */
fun AppPreferences.recordReadingTime(bookUrl: String, startMs: Long, endMs: Long) {
    if (bookUrl.isBlank() || endMs <= startMs) return
    val updated = READING_TIME_LOG.value.toMutableMap()
    for ((day, ms) in splitReadingByDay(startMs, endMs)) {
        val key = day.toString()
        val books = (updated[key] ?: emptyMap()).toMutableMap()
        books[bookUrl] = (books[bookUrl] ?: 0L) + ms
        updated[key] = books
    }
    val oldest = localDayIndex(endMs) - READING_LOG_KEEP_DAYS
    updated.keys.removeAll { (it.toLongOrNull() ?: Long.MIN_VALUE) < oldest }
    READING_TIME_LOG.value = updated
}

/**
 * Tracks one reader screen. Call [attach] once the book is known, [resume] from onResume and
 * [pause] from onPause. Only foreground time with a book attached is recorded.
 */
class ReadingTimer(private val appPreferences: AppPreferences) {
    private var bookUrl: String? = null
    private var startedAt: Long? = null

    fun attach(bookUrl: String) {
        this.bookUrl = bookUrl
    }

    fun resume(nowMillis: Long = System.currentTimeMillis()) {
        if (startedAt == null && !bookUrl.isNullOrBlank()) startedAt = nowMillis
    }

    fun pause(nowMillis: Long = System.currentTimeMillis()) {
        val start = startedAt ?: return
        startedAt = null
        val book = bookUrl ?: return
        appPreferences.recordReadingTime(book, start, nowMillis)
    }
}
