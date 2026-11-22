package my.noveldokusha.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * The streak is the only number on the Settings screen that is computed rather than counted,
 * so it is the one worth pinning down: it must survive a day missed by hours, and it must not
 * claim a streak across a real gap.
 */
class ReadingStreakTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun dayOffsetMillis(days: Int, hour: Int = 12): Long {
        val cal = GregorianCalendar(utc)
        cal.set(2026, Calendar.JANUARY, 1 + days, hour, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun streak(stamps: List<Long>, today: Int): Int {
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(utc)
        try {
            return readingStreakDays(stamps, nowMillis = dayOffsetMillis(today))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun countsConsecutiveDaysEndingToday() {
        val stamps = listOf(dayOffsetMillis(8), dayOffsetMillis(9), dayOffsetMillis(10))
        assertEquals(3, streak(stamps, today = 10))
    }

    @Test
    fun streakThatEndedYesterdayStillCounts() {
        // Read on the 8th and 9th, nothing yet on the 10th: the run is alive, not stale.
        val stamps = listOf(dayOffsetMillis(8), dayOffsetMillis(9))
        assertEquals(2, streak(stamps, today = 10))
    }

    @Test
    fun aRunEndingTheDayBeforeYesterdayIsOver() {
        val stamps = listOf(dayOffsetMillis(7), dayOffsetMillis(8))
        assertEquals(0, streak(stamps, today = 10))
    }

    @Test
    fun realGapResetsTheStreak() {
        val stamps = listOf(dayOffsetMillis(4), dayOffsetMillis(5))
        assertEquals(0, streak(stamps, today = 10))
    }

    @Test
    fun sameDayReadTwiceIsOneDay() {
        val stamps = listOf(dayOffsetMillis(10, 8), dayOffsetMillis(10, 22))
        assertEquals(1, streak(stamps, today = 10))
    }

    @Test
    fun noHistoryIsNoStreak() {
        assertEquals(0, streak(emptyList(), today = 10))
    }
}
