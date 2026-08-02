package dev.abhinav.tenniscalendar.ui.calendar

import dev.abhinav.tenniscalendar.data.DataError
import dev.abhinav.tenniscalendar.domain.PlayerStat
import dev.abhinav.tenniscalendar.model.Surface
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import java.time.YearMonth

/**
 * Season-range helpers. The actual bounds are derived from the tournament data (see
 * [CalendarViewModel]) and carried in [CalendarUiState], so the calendar auto-adjusts if the Gist
 * ever spans another year — nothing here is hardcoded to a season.
 */
object CalendarBounds {
    fun clamp(month: YearMonth, min: YearMonth, max: YearMonth): YearMonth = when {
        month.isBefore(min) -> min
        month.isAfter(max) -> max
        else -> month
    }

    fun allMonths(min: YearMonth, max: YearMonth): List<YearMonth> = buildList {
        var m = min
        while (!m.isAfter(max)) {
            add(m)
            m = m.plusMonths(1)
        }
    }

    fun years(min: YearMonth, max: YearMonth): List<Int> = (min.year..max.year).toList()

    fun monthsInYear(year: Int, min: YearMonth, max: YearMonth): List<YearMonth> =
        allMonths(min, max).filter { it.year == year }
}

/** Movement of a player between the active ranking snapshot and the previous one. */
sealed interface RankingMovement {
    data object New : RankingMovement
    data object Same : RankingMovement
    data class Up(val places: Int) : RankingMovement
    data class Down(val places: Int) : RankingMovement
}

data class RankingRow(
    val rank: Int,
    val name: String,
    val country: String,
    val points: Int,
    val movement: RankingMovement,
)

data class CalendarUiState(
    // Transient UI-only
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: DataError? = null,
    val isOffline: Boolean = false,
    // Editable input
    val tour: Tour = Tour.ATP,
    val month: YearMonth = YearMonth.now(),
    val surfaceFilter: Surface? = null,
    // Season range, derived from the tournament dates once data loads.
    val minMonth: YearMonth = YearMonth.now(),
    val maxMonth: YearMonth = YearMonth.now(),
    val canGoPrev: Boolean = true,
    val canGoNext: Boolean = true,
    // Persisted snapshot (derived once per input change, kept here so the UI stays dumb)
    val monthTournaments: List<Tournament> = emptyList(),
    val leaderboard: List<PlayerStat> = emptyList(),
    /** Full-season YTD leaderboard (latest data), independent of the displayed month — for player search. */
    val searchLeaderboard: List<PlayerStat> = emptyList(),
    val rankings: List<RankingRow> = emptyList(),
    val rankingSnapshotLabel: String? = null,
) {
    val hasData: Boolean get() = !isLoading && error == null
}
