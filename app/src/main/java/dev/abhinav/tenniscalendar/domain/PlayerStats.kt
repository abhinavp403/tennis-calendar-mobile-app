package dev.abhinav.tenniscalendar.domain

import dev.abhinav.tenniscalendar.model.Surface
import dev.abhinav.tenniscalendar.model.Tournament
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Level-weighted year-to-date player statistics. Ported from the web app's `utils/playerStats.js`:
 * champion/runner-up points by tier, per-player aggregation over completed finals, and a dense rank
 * where equal point totals share a rank.
 */
object PlayerStats {

    val CHAMPION_POINTS = mapOf(
        2000 to 2000,
        1500 to 1500,
        1000 to 1000,
        500 to 500,
        250 to 250,
    )

    val RUNNER_UP_POINTS = mapOf(
        2000 to 1300,
        1500 to 1000,
        1000 to 650,
        500 to 330,
        250 to 165,
    )

    /**
     * All completed tournaments from season start through the end of [displayedMonth], keyed off the
     * tournament end date. Equivalent to the web `cumulativeCompleted(tourList, currentMonth)`.
     */
    fun cumulativeCompleted(tourList: List<Tournament>, displayedMonth: YearMonth): List<Tournament> {
        val monthEnd = displayedMonth.atEndOfMonth()
        return tourList.filter { t ->
            t.isCompleted && runCatching { LocalDate.parse(t.end) }
                .getOrNull()?.let { !it.isAfter(monthEnd) } == true
        }
    }

    /** Computes the YTD leaderboard for [tourList] up to and including [displayedMonth]. */
    fun leaderboard(tourList: List<Tournament>, displayedMonth: YearMonth): List<PlayerStat> {
        val acc = LinkedHashMap<String, MutableStat>()

        fun bucket(abbrev: String, full: String?): MutableStat =
            acc.getOrPut(abbrev) { MutableStat(name = abbrev, fullName = full ?: abbrev) }.also {
                // Prefer a real full name if a later result supplies one.
                if (it.fullName == it.name && !full.isNullOrBlank()) it.fullName = full
            }

        for (t in cumulativeCompleted(tourList, displayedMonth)) {
            val winner = t.winner ?: continue
            val runnerUp = t.runnerUp ?: continue
            val surface = t.surfaceEnum

            bucket(winner, t.winnerFull).apply {
                points += CHAMPION_POINTS[t.level] ?: 0
                wins += 1
                titles += t.name
                if (surface != null) surfaceWins[surface] = (surfaceWins[surface] ?: 0) + 1
            }
            bucket(runnerUp, t.runnerUpFull).apply {
                points += RUNNER_UP_POINTS[t.level] ?: 0
                runnerUp2 += 1
                runnerUps += t.name
                if (surface != null) surfaceFinals[surface] = (surfaceFinals[surface] ?: 0) + 1
            }
        }

        val sorted = acc.values.sortedWith(
            compareByDescending<MutableStat> { it.points }
                .thenByDescending { it.wins }
                .thenByDescending { it.runnerUp2 }
        )

        // Dense rank: equal points share a rank.
        var lastPoints = Int.MIN_VALUE
        var rank = 0
        return sorted.map { s ->
            if (s.points != lastPoints) {
                rank += 1
                lastPoints = s.points
            }
            // A winning surface tally is the primary per-surface breakdown; also fold in final-only
            // appearances so the profile can show finals reached per surface.
            val perSurface = (Surface.entries).associateWith { surf ->
                (s.surfaceWins[surf] ?: 0) + (s.surfaceFinals[surf] ?: 0)
            }.filterValues { it > 0 }
            PlayerStat(
                rank = rank,
                name = s.name,
                fullName = s.fullName,
                points = s.points,
                wins = s.wins,
                runnerUp = s.runnerUp2,
                total = s.wins + s.runnerUp2,
                surfaceWins = s.surfaceWins.toMap(),
                surfaceFinals = perSurface,
                titles = s.titles.toList(),
                runnerUps = s.runnerUps.toList(),
            )
        }
    }

    private class MutableStat(
        val name: String,
        var fullName: String,
        var points: Int = 0,
        var wins: Int = 0,
        var runnerUp2: Int = 0,
        val surfaceWins: MutableMap<Surface, Int> = mutableMapOf(),
        val surfaceFinals: MutableMap<Surface, Int> = mutableMapOf(),
        val titles: MutableList<String> = mutableListOf(),
        val runnerUps: MutableList<String> = mutableListOf(),
    )
}

data class PlayerStat(
    val rank: Int,
    val name: String,
    val fullName: String,
    /** IOC nationality code, sourced from the rankings feed (tournament data has no country). */
    val country: String? = null,
    val points: Int,
    val wins: Int,
    val runnerUp: Int,
    val total: Int,
    /** Titles won per surface. */
    val surfaceWins: Map<Surface, Int>,
    /** Finals reached (wins + runner-ups) per surface. */
    val surfaceFinals: Map<Surface, Int>,
    val titles: List<String>,
    val runnerUps: List<String>,
) {
    /** Win rate across finals contested, as a whole-number percentage. */
    val winPercent: Int get() = if (total == 0) 0 else (wins * 100f / total).roundToInt()

    /** Surface on which the player has won the most titles (ties broken by enum order). */
    val bestSurface: Surface? get() = surfaceWins.maxByOrNull { it.value }?.key
}
