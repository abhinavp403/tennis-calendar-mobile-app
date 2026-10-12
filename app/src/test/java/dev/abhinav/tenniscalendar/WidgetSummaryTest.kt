package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.domain.WeekWindow
import dev.abhinav.tenniscalendar.model.RankingPlayer
import dev.abhinav.tenniscalendar.model.RankingsResponse
import dev.abhinav.tenniscalendar.model.TennisData
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.model.TournamentsResponse
import dev.abhinav.tenniscalendar.widget.WidgetSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetSummaryTest {

    // Wednesday; the week is Mon Oct 5 – Sun Oct 11, 2026.
    private val today = LocalDate.of(2026, 10, 7)

    private fun t(
        id: String,
        start: String,
        end: String,
        level: Int = 250,
        winner: String? = null,
        runnerUp: String? = null,
        score: String? = null,
    ) = Tournament(
        id = id, name = id, level = level, start = start, end = end, location = "City, X",
        surface = "Hard", logo = "", winner = winner, runnerUp = runnerUp, score = score,
    )

    private fun data(atp: List<Tournament>, rankings: Map<String, List<RankingPlayer>> = emptyMap()) =
        TennisData(TournamentsResponse(atp = atp), RankingsResponse(atp = rankings))

    @Test
    fun `week window runs Monday to Sunday`() {
        val week = WeekWindow.containing(today)
        assertEquals(LocalDate.of(2026, 10, 5), week.start)
        assertEquals(LocalDate.of(2026, 10, 11), week.end)
        assertEquals("Oct 5 – Oct 11", week.label)
    }

    @Test
    fun `only events overlapping this week, biggest tier first`() {
        val summary = WidgetSummary.from(
            data(
                listOf(
                    t("before", "2026-09-21", "2026-09-27"),
                    t("small", "2026-10-05", "2026-10-11", level = 250),
                    t("masters", "2026-10-01", "2026-10-12", level = 1000),
                    t("after", "2026-10-12", "2026-10-18"),
                )
            ),
            Tour.ATP,
            today,
        )
        assertEquals(listOf("masters", "small"), summary.events.map { it.id })
    }

    @Test
    fun `completed event shows the final result, pending shows the final day`() {
        val summary = WidgetSummary.from(
            data(
                listOf(
                    t("done", "2026-10-05", "2026-10-11", winner = "J. Sinner", runnerUp = "C. Alcaraz", score = "6-4 6-2"),
                    t("live", "2026-10-05", "2026-10-11", level = 500),
                )
            ),
            Tour.ATP,
            today,
        )
        val live = summary.events.first { it.id == "live" }
        val done = summary.events.first { it.id == "done" }
        assertFalse(live.completed)
        assertEquals("Final Sun, Oct 11", live.line)
        assertTrue(done.completed)
        assertEquals("J. Sinner def. C. Alcaraz · 6-4 6-2", done.line)
    }

    @Test
    fun `number one comes from the latest snapshot across key formats`() {
        val rankings = mapOf(
            "2026-09" to listOf(RankingPlayer(1, "C. Alcaraz", "ESP", 12000)),
            "2026-10-05" to listOf(
                RankingPlayer(2, "C. Alcaraz", "ESP", 11500),
                RankingPlayer(1, "J. Sinner", "ITA", 11800),
            ),
        )
        val summary = WidgetSummary.from(data(emptyList(), rankings), Tour.ATP, today)
        assertEquals("J. Sinner", summary.topPlayer?.name)
        assertTrue(summary.events.isEmpty())
    }

    @Test
    fun `no rankings means no number one`() {
        assertNull(WidgetSummary.from(data(emptyList()), Tour.ATP, today).topPlayer)
    }
}
