package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.domain.RankingHistory
import dev.abhinav.tenniscalendar.model.RankingPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RankingHistoryTest {

    private fun p(rank: Int, name: String) = RankingPlayer(rank, name, "X", 10_000 - rank * 100)

    private val snapshots = mapOf(
        // Deliberately out of order and mixing monthly / bi-weekly keys.
        "2026-07-13" to listOf(p(1, "J. Sinner"), p(2, "C. Alcaraz")),
        "2026-01" to listOf(p(1, "C. Alcaraz"), p(2, "J. Sinner")),
        "2026-03" to listOf(p(1, "C. Alcaraz"), p(2, "A. Zverev")),
        "2026-07-27" to listOf(p(1, "J. Sinner"), p(2, "C. Alcaraz")),
    )

    @Test
    fun `points are in date order with gaps where the player is outside the list`() {
        val history = RankingHistory.forPlayer(snapshots, "J. Sinner")!!
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 27),
            ),
            history.points.map { it.date },
        )
        assertEquals(listOf(2, null, 1, 1), history.points.map { it.rank })
        assertEquals(2, history.depth)
    }

    @Test
    fun `current, best and ranked count`() {
        val history = RankingHistory.forPlayer(snapshots, "J. Sinner")!!
        assertEquals(1, history.current)
        assertEquals(LocalDate.of(2026, 7, 13), history.best?.date) // first time at #1
        assertEquals(3, history.snapshotsRanked)
    }

    @Test
    fun `name match ignores accents and case`() {
        val withAccent = mapOf("2026-01" to listOf(p(5, "J. Lehečka")))
        assertEquals(5, RankingHistory.forPlayer(withAccent, "j. lehecka")?.current)
    }

    @Test
    fun `surname fallback resolves a differently spelled name`() {
        val feed = mapOf("2026-01" to listOf(p(3, "Jannik Sinner"), p(4, "C. Alcaraz")))
        assertEquals(3, RankingHistory.forPlayer(feed, "J. Sinner")?.current)
    }

    @Test
    fun `surname fallback needs the same initial`() {
        val feed = mapOf("2026-01" to listOf(p(3, "M. Zverev")))
        assertNull(RankingHistory.forPlayer(feed, "A. Zverev"))
    }

    @Test
    fun `surname fallback skips an ambiguous snapshot`() {
        val feed = mapOf(
            "2026-01" to listOf(p(3, "Alexander Zverev"), p(9, "Andrei Zverev")),
            "2026-02" to listOf(p(4, "Alexander Zverev")),
        )
        // January has two "A. … Zverev"s, so it's a gap rather than a guess; February resolves.
        assertEquals(listOf(null, 4), RankingHistory.forPlayer(feed, "A. Zverev")?.points?.map { it.rank })
    }

    @Test
    fun `exact match wins over the fallback`() {
        val feed = mapOf("2026-01" to listOf(p(2, "Alexander Zverev"), p(7, "A. Zverev")))
        assertEquals(7, RankingHistory.forPlayer(feed, "A. Zverev")?.current)
    }

    @Test
    fun `player never ranked gives null`() {
        assertNull(RankingHistory.forPlayer(snapshots, "N. Nobody"))
    }
}
