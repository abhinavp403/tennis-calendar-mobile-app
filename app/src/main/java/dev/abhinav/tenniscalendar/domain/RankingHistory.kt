package dev.abhinav.tenniscalendar.domain

import dev.abhinav.tenniscalendar.model.RankingPlayer
import java.time.LocalDate

/**
 * A player's rank across every ranking snapshot of the season, for the profile chart. The feed only
 * lists the top [History.depth] players per snapshot, so a snapshot where the player is missing is
 * "outside the top N" (a null rank), not an error.
 */
object RankingHistory {

    data class Point(val date: LocalDate, val rank: Int?, val points: Int?)

    data class History(val points: List<Point>, val depth: Int) {
        private val ranked get() = points.filter { it.rank != null }

        /** Rank in the most recent snapshot, or null if outside the top [depth]. */
        val current: Int? get() = points.lastOrNull()?.rank

        /** Best (lowest) rank of the season and the first snapshot it was reached. */
        val best: Point? get() = ranked.minByOrNull { it.rank!! }

        val snapshotsRanked: Int get() = ranked.size
    }

    /**
     * History for [name] (abbreviated, e.g. "J. Sinner", matched accent-insensitively) across
     * [snapshots] keyed as in rankings.json. Null if the player never appears in any snapshot.
     */
    fun forPlayer(snapshots: Map<String, List<RankingPlayer>>, name: String): History? {
        val sorted = runCatching { RankingKeys.sortedSnapshots(snapshots.keys) }.getOrNull() ?: return null
        val points = sorted.map { snap ->
            val entry = find(snapshots[snap.key].orEmpty(), name)
            Point(snap.date, entry?.rank, entry?.points)
        }
        if (points.none { it.rank != null }) return null
        val depth = snapshots.values.maxOfOrNull { it.size } ?: 0
        return History(points, depth)
    }

    /**
     * Exact (accent-folded) name first. Failing that, a lenient match so a spelling difference
     * between feeds ("J. Sinner" vs "Jannik Sinner") still resolves: same surname and same first
     * initial, and only when exactly one player in the snapshot qualifies — so "A. Zverev" never
     * picks up "M. Zverev", and an ambiguous surname gives no match rather than a wrong one.
     */
    private fun find(snapshot: List<RankingPlayer>, name: String): RankingPlayer? {
        val target = NameMatch.fold(name)
        snapshot.firstOrNull { NameMatch.fold(it.name) == target }?.let { return it }

        val surname = NameMatch.surname(name)
        val initial = target.firstOrNull()
        return snapshot
            .filter { NameMatch.surname(it.name) == surname && NameMatch.fold(it.name).firstOrNull() == initial }
            .singleOrNull()
    }
}
