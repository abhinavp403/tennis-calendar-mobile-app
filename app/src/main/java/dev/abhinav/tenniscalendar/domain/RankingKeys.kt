package dev.abhinav.tenniscalendar.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Normalizes the mixed ranking-snapshot keys from `rankings.json`. Ported from the web
 * `utils/rankingKeys.js`: keys are either legacy monthly `YYYY-MM` (treated as that month's last
 * day) or bi-weekly `YYYY-MM-DD` (a Monday). Used to pick the latest snapshot within a displayed
 * month and the immediately preceding one for movement arrows.
 */
object RankingKeys {

    data class Snapshot(val key: String, val date: LocalDate)

    /** `YYYY-MM` -> last day of month; `YYYY-MM-DD` -> that exact date. */
    fun normalize(key: String): LocalDate =
        if (key.length == 7) YearMonth.parse(key).atEndOfMonth() else LocalDate.parse(key)

    fun sortedSnapshots(keys: Collection<String>): List<Snapshot> =
        keys.map { Snapshot(it, normalize(it)) }.sortedBy { it.date }

    /**
     * The active snapshot for [displayedMonth] and the one immediately before it.
     *
     * Active = the latest snapshot falling inside the displayed month; if the month has none, the
     * most recent snapshot on or before the month's end (carried forward). Previous = the snapshot
     * directly before the active one, regardless of month.
     */
    fun activeAndPrevious(
        keys: Collection<String>,
        displayedMonth: YearMonth,
    ): Pair<Snapshot?, Snapshot?> {
        val sorted = sortedSnapshots(keys)
        if (sorted.isEmpty()) return null to null

        val monthStart = displayedMonth.atDay(1)
        val monthEnd = displayedMonth.atEndOfMonth()

        val active = sorted.lastOrNull { !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }
            ?: sorted.lastOrNull { !it.date.isAfter(monthEnd) }
            ?: return null to null

        val idx = sorted.indexOfFirst { it.key == active.key }
        val previous = sorted.getOrNull(idx - 1)
        return active to previous
    }
}
