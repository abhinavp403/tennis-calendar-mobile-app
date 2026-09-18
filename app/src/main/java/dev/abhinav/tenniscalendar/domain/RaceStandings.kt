package dev.abhinav.tenniscalendar.domain

import dev.abhinav.tenniscalendar.model.RacePlayer
import dev.abhinav.tenniscalendar.model.RaceResult

/** Pure helpers for the Race to the Finals standings, kept UI-free so they can be unit-tested. */
object RaceStandings {

    /** The top-8 qualification cutoff. */
    const val CUTOFF = 8

    /**
     * A player's GAP, or null when the reference player is missing:
     * - ranks 1–8: cushion over #9 (positive) → `points − #9.points`
     * - ranks 9+: points still needed to reach #8 (negative) → `points − #8.points`
     */
    fun gap(players: List<RacePlayer>, player: RacePlayer): Int? {
        val referenceRank = if (player.rank <= CUTOFF) CUTOFF + 1 else CUTOFF
        val reference = players.firstOrNull { it.rank == referenceRank } ?: return null
        return player.points - reference.points
    }

    /** The leader's points (used to scale the points bars); 0 if there are no players. */
    fun leaderPoints(players: List<RacePlayer>): Int = players.maxOfOrNull { it.points } ?: 0

    /** Index of the last player at or above the cutoff, or null if none needs a cutoff line drawn. */
    fun cutoffAfterIndex(players: List<RacePlayer>): Int? {
        if (players.none { it.rank > CUTOFF }) return null // nobody below the line → no divider
        val idx = players.indexOfLast { it.rank <= CUTOFF }
        return idx.takeIf { it >= 0 }
    }

    data class Group(val key: String, val label: String, val results: List<RaceResult>, val subtotal: Int)

    /**
     * Group a player's results into slam → masters → other, preserving table order within each group
     * and skipping empty groups. [wta] switches the masters label to "WTA 1000".
     */
    fun breakdown(results: List<RaceResult>, wta: Boolean): List<Group> {
        val labels = listOf(
            "slam" to "GRAND SLAMS",
            "masters" to if (wta) "WTA 1000" else "MASTERS 1000",
            "other" to "BEST OTHER RESULTS",
        )
        return labels.mapNotNull { (key, label) ->
            val group = results.filter { it.group == key }
            if (group.isEmpty()) null
            else Group(key, label, group, group.sumOf { it.points })
        }
    }
}
