package my.dreamora.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * `activeDays` is the one stats number that depends on where a day is cut, so it is pinned
 * here: two chapters opened an hour apart across midnight are two days, and three opened on
 * one calendar day are one day no matter how far apart in that day they were.
 *
 * The boundary matters because `TimeUnit.toDays` divides by 24 hours from the epoch — which is
 * UTC midnight, not the reader's. `dayIndex` adds the zone offset first, and these tests run in
 * a fixed zone so the arithmetic is checked rather than inherited from the build machine.
 */
class ActiveDaysTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun stamp(day: Int, hour: Int, minute: Int = 0): Long {
        val cal = GregorianCalendar(utc)
        cal.set(2026, Calendar.JANUARY, 1 + day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** JUnit wants void test methods, so the helper is generic instead of `() -> Int`. */
    private fun <T> withUtc(block: () -> T): T {
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(utc)
        try {
            return block()
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `no stamps is no days`() {
        withUtc { assertEquals(0, activeDays(emptyList())) }
    }

    @Test
    fun `three stamps on one calendar day count once`() {
        withUtc {
            val sameDay = listOf(stamp(0, 6), stamp(0, 13), stamp(0, 23, 59))
            assertEquals(1, activeDays(sameDay))
        }
    }

    @Test
    fun `an hour apart across midnight is two days`() {
        withUtc { assertEquals(2, activeDays(listOf(stamp(3, 23, 40), stamp(4, 0, 20)))) }
    }

    @Test
    fun `unsorted and duplicated stamps do not inflate the count`() {
        withUtc {
            val stamps = listOf(stamp(7, 9), stamp(6, 21), stamp(7, 22), stamp(6, 21))
            assertEquals(2, activeDays(stamps))
        }
    }

    @Test
    fun `day index follows the local zone rather than utc midnight`() {
        val previous = TimeZone.getDefault()
        // In UTC+11, 13:00 UTC on the 2nd is already 00:00 local on the 3rd, and 00:30 UTC on
        // the 3rd is still the 3rd locally: one day for the reader. UTC's own arithmetic calls
        // them two, which is exactly the mistake the offset in dayIndex exists to avoid.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+11:00"))
        try {
            assertEquals(dayIndex(stamp(2, 0, 30)), dayIndex(stamp(1, 13)))
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}
