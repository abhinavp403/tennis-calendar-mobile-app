package dev.abhinav.tenniscalendar.widget

import dev.abhinav.tenniscalendar.domain.RankingKeys
import dev.abhinav.tenniscalendar.domain.WeekWindow
import dev.abhinav.tenniscalendar.model.RankingPlayer
import dev.abhinav.tenniscalendar.model.TennisData
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Everything the home-screen widget renders, derived purely from cached [TennisData] so the widget
 * never touches the network: this week's events (with the final result once it's in) and the
 * current world #1 from the latest ranking snapshot.
 */
data class WidgetSummary(
    val tour: Tour,
    val weekLabel: String,
    val events: List<Event>,
    val topPlayer: RankingPlayer?,
) {
    data class Event(
        val id: String,
        val name: String,
        val level: Int,
        /** "J. Sinner def. C. Alcaraz · 6-4 6-2" once completed, otherwise "Final Sun, Oct 12". */
        val line: String,
        val completed: Boolean,
    )

    companion object {
        private val FINAL_DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d")

        fun from(data: TennisData, tour: Tour, today: LocalDate = LocalDate.now()): WidgetSummary {
            val week = WeekWindow.containing(today)
            val events = week.tournamentsIn(data.tournaments.forTour(tour))
                // Biggest events first so a narrow widget still shows the headline tournament.
                .sortedByDescending { it.level }
                .map { it.toEvent() }
            return WidgetSummary(
                tour = tour,
                weekLabel = week.label,
                events = events,
                topPlayer = currentNumberOne(data, tour),
            )
        }

        private fun Tournament.toEvent(): Event {
            val line = if (isCompleted) {
                buildString {
                    append(winner).append(" def. ").append(runnerUp)
                    score?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                }
            } else {
                val finalDay = runCatching { LocalDate.parse(end).format(FINAL_DAY_FORMAT) }.getOrNull()
                if (finalDay != null) "Final $finalDay" else location
            }
            return Event(id, name, level, line, isCompleted)
        }

        /** Rank 1 in the most recent snapshot (bi-weekly or legacy monthly key). */
        private fun currentNumberOne(data: TennisData, tour: Tour): RankingPlayer? {
            val snapshots = data.rankings.forTour(tour)
            val latest = runCatching { RankingKeys.sortedSnapshots(snapshots.keys).lastOrNull() }
                .getOrNull() ?: return null
            return snapshots[latest.key]?.minByOrNull { it.rank }
        }
    }
}
