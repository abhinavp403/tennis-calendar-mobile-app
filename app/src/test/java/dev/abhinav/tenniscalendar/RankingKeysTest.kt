package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.domain.RankingKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RankingKeysTest {

    @Test
    fun `monthly key normalizes to last day of month`() {
        assertEquals(LocalDate.of(2026, 2, 28), RankingKeys.normalize("2026-02"))
        assertEquals(LocalDate.of(2026, 6, 30), RankingKeys.normalize("2026-06"))
    }

    @Test
    fun `bi-weekly key normalizes to the exact date`() {
        assertEquals(LocalDate.of(2026, 7, 13), RankingKeys.normalize("2026-07-13"))
    }

    @Test
    fun `active is latest within displayed month, previous is the one before`() {
        val keys = listOf("2026-01", "2026-06", "2026-07-13")
        val (active, previous) = RankingKeys.activeAndPrevious(keys, YearMonth.of(2026, 7))
        assertEquals("2026-07-13", active?.key)
        assertEquals("2026-06", previous?.key)
    }

    @Test
    fun `months with no own snapshot carry forward the most recent prior one`() {
        val keys = listOf("2026-06", "2026-07-13")
        val (active, _) = RankingKeys.activeAndPrevious(keys, YearMonth.of(2026, 9))
        assertEquals("2026-07-13", active?.key)
    }

    @Test
    fun `no snapshot before the month yields null`() {
        val keys = listOf("2026-06")
        val (active, _) = RankingKeys.activeAndPrevious(keys, YearMonth.of(2026, 1))
        assertNull(active)
    }
}
