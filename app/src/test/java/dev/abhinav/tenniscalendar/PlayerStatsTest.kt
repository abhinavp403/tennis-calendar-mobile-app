package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.domain.PlayerStats
import dev.abhinav.tenniscalendar.model.Tournament
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class PlayerStatsTest {

    private fun t(
        id: String,
        level: Int,
        end: String,
        surface: String = "Hard",
        winner: String? = null,
        runnerUp: String? = null,
        winnerFull: String? = null,
        runnerUpFull: String? = null,
    ) = Tournament(
        id = id, name = id, level = level, start = end, end = end,
        location = "X", surface = surface, logo = "$id.png",
        winner = winner, runnerUp = runnerUp, score = "6-0 6-0",
        winnerFull = winnerFull, runnerUpFull = runnerUpFull,
    )

    @Test
    fun `champion and runner-up points follow the level table`() {
        val list = listOf(
            t("gs", 2000, "2026-06-07", winner = "A. Zverev", runnerUp = "F. Cobolli",
                winnerFull = "Alexander Zverev", runnerUpFull = "Flavio Cobolli"),
        )
        val board = PlayerStats.leaderboard(list, YearMonth.of(2026, 6))
        val z = board.first { it.name == "A. Zverev" }
        val c = board.first { it.name == "F. Cobolli" }
        assertEquals(2000, z.points)
        assertEquals(1300, c.points)
        assertEquals(1, z.wins)
        assertEquals(1, c.runnerUp)
        assertEquals("Alexander Zverev", z.fullName)
    }

    @Test
    fun `a Grand Slam champion outranks a stack of 250 titles (Zverev above Cobolli)`() {
        val list = mutableListOf(
            t("gs", 2000, "2026-06-07", winner = "A. Zverev", runnerUp = "S. Someone"),
        )
        // Cobolli wins four 250s: 4 * 250 = 1000 < 2000.
        repeat(4) { i ->
            list += t("c$i", 250, "2026-0${i + 1}-10", winner = "F. Cobolli", runnerUp = "R. Rival")
        }
        val board = PlayerStats.leaderboard(list, YearMonth.of(2026, 6))
        val zRank = board.first { it.name == "A. Zverev" }.rank
        val cRank = board.first { it.name == "F. Cobolli" }.rank
        assertTrue("Zverev ($zRank) should rank above Cobolli ($cRank)", zRank < cRank)
    }

    @Test
    fun `equal points share a dense rank`() {
        val list = listOf(
            t("a", 500, "2026-01-10", winner = "P. One", runnerUp = "Z. LoserA"),
            t("b", 500, "2026-02-10", winner = "P. Two", runnerUp = "Z. LoserB"),
        )
        val board = PlayerStats.leaderboard(list, YearMonth.of(2026, 3))
        val one = board.first { it.name == "P. One" }
        val two = board.first { it.name == "P. Two" }
        assertEquals(500, one.points)
        assertEquals(500, two.points)
        assertEquals("champions with equal points share rank 1", one.rank, two.rank)
        assertEquals(1, one.rank)
    }

    @Test
    fun `cumulative is bounded by the displayed month end`() {
        val list = listOf(
            t("jan", 250, "2026-01-10", winner = "P. Jan", runnerUp = "R"),
            t("aug", 250, "2026-08-10", winner = "P. Aug", runnerUp = "R"),
        )
        val throughJune = PlayerStats.leaderboard(list, YearMonth.of(2026, 6))
        assertTrue(throughJune.any { it.name == "P. Jan" })
        assertTrue("August result excluded through June", throughJune.none { it.name == "P. Aug" })
    }
}
