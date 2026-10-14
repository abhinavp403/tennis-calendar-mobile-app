package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.model.RacePlayer
import dev.abhinav.tenniscalendar.model.RaceResponse
import dev.abhinav.tenniscalendar.model.RaceTour
import dev.abhinav.tenniscalendar.model.RankingPlayer
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.work.RaceAlertLogic
import dev.abhinav.tenniscalendar.work.RankingAlertLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AlertLogicTest {

    private fun top(vararg names: String) =
        names.mapIndexed { i, n -> RankingPlayer(i + 1, n, "X", 10_000 - i * 100) }

    private val base = top("J. Sinner", "C. Alcaraz", "A. Zverev", "T. Fritz", "N. Djokovic",
        "J. Draper", "A. de Minaur", "L. Musetti", "H. Rune", "B. Shelton", "T. Paul")

    // --- Rankings ---

    @Test
    fun `first run records the latest snapshot without notifying`() {
        val outcome = RankingAlertLogic.decide(Tour.ATP, mapOf("2026-07-13" to base), lastSeenKey = null)
        assertEquals("2026-07-13", outcome.latestKey)
        assertNull(outcome.change)
    }

    @Test
    fun `same snapshot notifies nothing`() {
        val outcome = RankingAlertLogic.decide(Tour.ATP, mapOf("2026-07-13" to base), "2026-07-13")
        assertNull(outcome.change)
    }

    @Test
    fun `new number one is reported with the previous one`() {
        val swapped = top("C. Alcaraz", "J. Sinner", "A. Zverev", "T. Fritz", "N. Djokovic",
            "J. Draper", "A. de Minaur", "L. Musetti", "H. Rune", "B. Shelton")
        val change = RankingAlertLogic.decide(
            Tour.ATP, mapOf("2026-07-13" to base, "2026-07-27" to swapped), "2026-07-13",
        ).change!!
        assertEquals("C. Alcaraz", change.newNumberOne?.name)
        assertEquals("J. Sinner", change.previousNumberOne)
        assertEquals(emptyList<RankingPlayer>(), change.enteredTop)
    }

    @Test
    fun `players entering the top 10 are reported in rank order`() {
        // T. Paul (11th) moves up to 9th, H. Rune drops out.
        val next = top("J. Sinner", "C. Alcaraz", "A. Zverev", "T. Fritz", "N. Djokovic",
            "J. Draper", "A. de Minaur", "L. Musetti", "T. Paul", "B. Shelton", "H. Rune")
        val change = RankingAlertLogic.decide(
            Tour.ATP, mapOf("2026-07-13" to base, "2026-07-27" to next), "2026-07-13",
        ).change!!
        assertNull(change.newNumberOne)
        assertEquals(listOf("T. Paul" to 9), change.enteredTop.map { it.name to it.rank })
    }

    @Test
    fun `reshuffle inside the top 10 is not notable`() {
        val reshuffled = top("J. Sinner", "A. Zverev", "C. Alcaraz", "T. Fritz", "N. Djokovic",
            "J. Draper", "A. de Minaur", "L. Musetti", "H. Rune", "B. Shelton")
        val outcome = RankingAlertLogic.decide(
            Tour.ATP, mapOf("2026-07-13" to base, "2026-07-27" to reshuffled), "2026-07-13",
        )
        assertEquals("2026-07-27", outcome.latestKey)
        assertNull(outcome.change)
    }

    @Test
    fun `compares against the last seen snapshot when several arrived at once`() {
        val mid = top("C. Alcaraz", "J. Sinner") // #1 changed and changed back in between
        val outcome = RankingAlertLogic.decide(
            Tour.ATP, mapOf("2026-07-13" to base, "2026-07-20" to mid, "2026-07-27" to base), "2026-07-13",
        )
        assertNull(outcome.change)
    }

    @Test
    fun `older feed than what was seen does nothing and keeps the seen key`() {
        val outcome = RankingAlertLogic.decide(Tour.ATP, mapOf("2026-07-13" to base), "2026-07-27")
        assertEquals("2026-07-27", outcome.latestKey)
        assertNull(outcome.change)
    }

    @Test
    fun `short base snapshot reports no top-10 entrants`() {
        val truncated = top("J. Sinner", "C. Alcaraz", "A. Zverev") // partial feed rebuild
        val outcome = RankingAlertLogic.decide(
            Tour.ATP, mapOf("2026-07-13" to truncated, "2026-07-27" to base), "2026-07-13",
        )
        assertNull(outcome.change) // #1 unchanged, and ranks 4–10 aren't "new"
    }

    // --- Race ---

    @Test
    fun `missing asOf falls back to the current year for the season`() {
        val race = RaceResponse(
            atp = RaceTour(asOf = null, players = listOf(RacePlayer(rank = 1, name = "J. Sinner", qualified = true))),
        )
        assertEquals(
            listOf("atp:2027:j. sinner"),
            RaceAlertLogic.qualifiers(race, today = LocalDate.of(2027, 10, 1)).map { it.id },
        )
    }

    @Test
    fun `qualifier ids are scoped to tour and season`() {
        val race = RaceResponse(
            atp = RaceTour(
                asOf = "2026-10-05",
                players = listOf(
                    RacePlayer(rank = 1, name = "J. Sinner", qualified = true),
                    RacePlayer(rank = 2, name = "C. Alcaraz", qualified = false),
                ),
            ),
            wta = RaceTour(asOf = "2026-10-05", players = listOf(RacePlayer(rank = 1, name = "A. Sabalenka", qualified = true))),
        )
        assertEquals(
            listOf("atp:2026:j. sinner", "wta:2026:a. sabalenka"),
            RaceAlertLogic.qualifiers(race).map { it.id },
        )
        assertEquals(1, RaceAlertLogic.qualifiedCount(race, Tour.ATP))
    }
}
