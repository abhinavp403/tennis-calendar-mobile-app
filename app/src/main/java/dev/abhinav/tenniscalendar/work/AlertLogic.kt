package dev.abhinav.tenniscalendar.work

import dev.abhinav.tenniscalendar.domain.NameMatch
import dev.abhinav.tenniscalendar.domain.RankingKeys
import dev.abhinav.tenniscalendar.model.RacePlayer
import dev.abhinav.tenniscalendar.model.RaceResponse
import dev.abhinav.tenniscalendar.model.RankingPlayer
import dev.abhinav.tenniscalendar.model.Tour
import java.time.LocalDate

/**
 * Pure decision logic for ranking-change alerts. A new snapshot is compared with the one we last
 * saw; only a new world #1 or players entering the top 10 are worth a notification.
 */
object RankingAlertLogic {

    const val TOP = 10

    data class Change(
        val tour: Tour,
        val snapshotKey: String,
        /** The new #1, or null if #1 is unchanged. */
        val newNumberOne: RankingPlayer?,
        val previousNumberOne: String?,
        /** Players in the new top [TOP] who weren't in it before, by rank. */
        val enteredTop: List<RankingPlayer>,
    )

    data class Outcome(
        /** Snapshot key to remember as seen (null if the feed has no snapshots). */
        val latestKey: String?,
        /** What to notify about, or null for nothing notable / first run. */
        val change: Change?,
    )

    fun decide(tour: Tour, snapshots: Map<String, List<RankingPlayer>>, lastSeenKey: String?): Outcome {
        val sorted = runCatching { RankingKeys.sortedSnapshots(snapshots.keys) }.getOrNull()
        val latest = sorted?.lastOrNull() ?: return Outcome(null, null)
        // First run records the current snapshot without notifying; same key means nothing new.
        if (lastSeenKey == null || latest.key == lastSeenKey) return Outcome(latest.key, null)
        val lastSeenDate = runCatching { RankingKeys.normalize(lastSeenKey) }.getOrNull()
        if (lastSeenDate != null && !latest.date.isAfter(lastSeenDate)) return Outcome(lastSeenKey, null)

        // Compare against what we last saw if it's still in the feed, else the snapshot before latest.
        val baseKey = lastSeenKey.takeIf { it in snapshots } ?: sorted.getOrNull(sorted.size - 2)?.key
            ?: return Outcome(latest.key, null)
        val now = snapshots[latest.key].orEmpty().sortedBy { it.rank }
        val before = snapshots[baseKey].orEmpty().sortedBy { it.rank }

        val oldOne = before.firstOrNull()
        val newOne = now.firstOrNull()
        val numberOneChanged = oldOne != null && newOne != null &&
            NameMatch.fold(oldOne.name) != NameMatch.fold(newOne.name)

        val beforeTop = before.filter { it.rank <= TOP }.map { NameMatch.fold(it.name) }.toSet()
        // A base list shorter than the top N can't say who was already in it, so skip entrants.
        val entered = if (before.size < TOP) emptyList() else
            now.filter { it.rank <= TOP && NameMatch.fold(it.name) !in beforeTop }

        val change = if (numberOneChanged || entered.isNotEmpty()) {
            Change(
                tour = tour,
                snapshotKey = latest.key,
                newNumberOne = newOne.takeIf { numberOneChanged },
                previousNumberOne = oldOne?.name?.takeIf { numberOneChanged },
                enteredTop = entered,
            )
        } else null
        return Outcome(latest.key, change)
    }
}

/**
 * Race to the Finals qualification alerts. Each qualified player gets a stable id scoped to tour
 * and season, so the seen-set logic of [TournamentSyncLogic] applies and next season starts fresh.
 */
object RaceAlertLogic {

    data class Qualifier(val id: String, val tour: Tour, val player: RacePlayer)

    /** [today] supplies the season when the feed's `asOf` is missing, so ids never lose their year. */
    fun qualifiers(race: RaceResponse, today: LocalDate = LocalDate.now()): List<Qualifier> = Tour.entries.flatMap { tour ->
        val raceTour = race.forTour(tour) ?: return@flatMap emptyList()
        val season = raceTour.asOf?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
            ?: today.year.toString()
        raceTour.players
            .filter { it.qualified && it.name.isNotBlank() }
            .map { Qualifier("${tour.key}:$season:${NameMatch.fold(it.name)}", tour, it) }
    }

    /** Qualified players per tour in the current feed (for "n of 8 spots" copy). */
    fun qualifiedCount(race: RaceResponse, tour: Tour): Int =
        race.forTour(tour)?.players?.count { it.qualified } ?: 0
}
