package my.noveldokusha.settings

import my.noveldokusha.core.readingtime.localDayIndex
import my.noveldokusha.core.readingtime.splitReadingByDay
import my.noveldokusha.core.readingtime.summarizeReadingLog
import my.noveldokusha.core.readingtime.weekStartDayIndex
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * The reading-time log is only useful if a session is credited to the right day and the
 * panel's periods line up with the calendar. These pin that down in UTC, so the expectations
 * do not depend on the machine running the tests.
 */
class ReadingTimeLogTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun millis(local: String): Long =
        LocalDateTime.parse(local).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val minute = 60_000L

    @Test
    fun session_across_midnight_is_split_between_the_two_days() {
        val start = millis("2026-11-22T23:50")
        val end = millis("2026-11-23T00:20")

        val perDay = splitReadingByDay(start, end, utc)

        assertEquals(2, perDay.size)
        assertEquals(10 * minute, perDay[localDayIndex(start, utc)])
        assertEquals(20 * minute, perDay[localDayIndex(end, utc)])
    }

    @Test
    fun week_starts_on_monday_even_when_today_is_sunday() {
        // 2026-11-22 is a Sunday; its Monday is 2026-11-16, six days earlier.
        val now = millis("2026-11-22T12:00")
        assertEquals(
            localDayIndex(millis("2026-11-16T00:00"), utc),
            weekStartDayIndex(now, utc),
        )
    }

    @Test
    fun summary_counts_today_week_and_month_separately() {
        val now = millis("2026-11-22T12:00")
        val log = mapOf(
            // today (Sunday), book A: 30 min
            localDayIndex(millis("2026-11-22T09:00"), utc).toString() to mapOf("A" to 30 * minute),
            // Monday of this week, book B: 15 min
            localDayIndex(millis("2026-11-16T10:00"), utc).toString() to mapOf("B" to 15 * minute),
            // earlier this month (2nd of November), book A: 60 min
            localDayIndex(millis("2026-11-02T10:00"), utc).toString() to mapOf("A" to 60 * minute),
            // last month, must not reach the month total: 90 min
            localDayIndex(millis("2026-10-31T10:00"), utc).toString() to mapOf("B" to 90 * minute),
        )

        val periods = summarizeReadingLog(log, now, utc)

        assertEquals(30 * minute, periods.todayMs)
        assertEquals(45 * minute, periods.weekMs)
        assertEquals(105 * minute, periods.monthMs)
        assertEquals(195 * minute, periods.allTimeMs)
        assertEquals(15 * minute, periods.weekDailyMs[0])   // Monday
        assertEquals(30 * minute, periods.weekDailyMs[6])   // Sunday = today
        assertEquals(2, periods.daysWithReadingWeek)
        assertEquals(3, periods.daysWithReadingMonth)
    }

    @Test
    fun per_book_totals_are_sorted_largest_first_and_empty_entries_are_ignored() {
        val now = millis("2026-11-22T12:00")
        val today = localDayIndex(now, utc).toString()
        val log = mapOf(today to mapOf("small" to 5 * minute, "big" to 40 * minute, "zero" to 0L))

        val periods = summarizeReadingLog(log, now, utc)

        assertEquals(listOf("big", "small"), periods.perBookMs.keys.toList())
    }

    @Test
    fun empty_log_gives_zero_periods_not_a_crash() {
        val periods = summarizeReadingLog(emptyMap(), millis("2026-11-22T12:00"), utc)
        assertEquals(0L, periods.allTimeMs)
        assertEquals(List(7) { 0L }, periods.weekDailyMs)
    }
}
