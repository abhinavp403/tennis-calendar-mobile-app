package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.work.TournamentSync
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TournamentSyncScheduleTest {

    private val slot0130 = LocalTime.of(1, 30)
    private val slot0930 = LocalTime.of(9, 30)

    private fun utc(h: Int, m: Int) =
        ZonedDateTime.of(2026, 1, 1, h, m, 0, 0, ZoneOffset.UTC)

    private fun minutesUntil(target: LocalTime, h: Int, m: Int) =
        TournamentSync.millisUntil(target, utc(h, m)) / 60_000

    @Test
    fun `delay to 0130 later same day`() {
        assertEquals(90, minutesUntil(slot0130, 0, 0)) // 00:00 -> 01:30
    }

    @Test
    fun `delay to 0930 later same day`() {
        assertEquals(7 * 60 + 30, minutesUntil(slot0930, 2, 0)) // 02:00 -> 09:30
    }

    @Test
    fun `after the slot wraps to next day`() {
        assertEquals(24 * 60, minutesUntil(slot0130, 1, 30)) // exactly on 01:30 -> next day 01:30
        assertEquals(23 * 60 + 30, minutesUntil(slot0930, 10, 0)) // 10:00 -> next day 09:30
    }
}
