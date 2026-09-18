package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.domain.RaceStandings
import dev.abhinav.tenniscalendar.model.RacePlayer
import dev.abhinav.tenniscalendar.model.RaceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RaceStandingsTest {

    private fun players(vararg points: Int): List<RacePlayer> =
        points.mapIndexed { i, p -> RacePlayer(rank = i + 1, name = "P${i + 1}", points = p) }

    // Ten players, #8 = 2780, #9 = 2509, #1 = 8650.
    private val ten = players(8650, 7950, 7000, 6000, 5000, 4000, 3200, 2780, 2509, 2480)

    @Test
    fun `rank 8 gap is cushion over rank 9`() {
        val p8 = ten.first { it.rank == 8 }
        assertEquals(2780 - 2509, RaceStandings.gap(ten, p8))
    }

    @Test
    fun `rank 9 gap is the negation of rank 8 gap`() {
        val p9 = ten.first { it.rank == 9 }
        assertEquals(2509 - 2780, RaceStandings.gap(ten, p9)) // negative
    }

    @Test
    fun `leader gap measured against rank 9`() {
        val p1 = ten.first { it.rank == 1 }
        assertEquals(8650 - 2509, RaceStandings.gap(ten, p1))
    }

    @Test
    fun `gap is null when the reference player is missing`() {
        val topEightOnly = players(8650, 7950, 7000, 6000, 5000, 4000, 3200, 2780) // no #9
        val p8 = topEightOnly.first { it.rank == 8 }
        assertNull(RaceStandings.gap(topEightOnly, p8)) // needs #9, absent
    }

    @Test
    fun `cutoff sits after the last top-8 player, or is absent with no one below`() {
        assertEquals(7, RaceStandings.cutoffAfterIndex(ten)) // index of rank 8
        assertNull(RaceStandings.cutoffAfterIndex(players(8650, 7950))) // nobody below the line
    }

    @Test
    fun `breakdown keeps group order, skips empty groups, and subtotals sum to points`() {
        val results = listOf(
            RaceResult("US Open", "W", 2000, "slam"),
            RaceResult("Australian Open", "SF", 800, "slam"),
            RaceResult("Indian Wells", "W", 1000, "masters"),
            RaceResult("Some 500", "F", 330, "other"),
        )
        val groups = RaceStandings.breakdown(results, wta = false)
        assertEquals(listOf("slam", "masters", "other"), groups.map { it.key })
        assertEquals(2800, groups[0].subtotal)
        assertEquals(1000, groups[1].subtotal)
        assertEquals(330, groups[2].subtotal)
        assertEquals(2000 + 800 + 1000 + 330, groups.sumOf { it.subtotal })
        assertEquals("MASTERS 1000", groups[1].label)
        assertEquals("WTA 1000", RaceStandings.breakdown(results, wta = true)[1].label)
    }
}
