package dev.abhinav.tenniscalendar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.abhinav.tenniscalendar.data.DataResult
import dev.abhinav.tenniscalendar.data.NotificationPrefs
import dev.abhinav.tenniscalendar.data.Settings
import dev.abhinav.tenniscalendar.data.SettingsRepository
import dev.abhinav.tenniscalendar.data.TennisRepository
import dev.abhinav.tenniscalendar.model.TennisData
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.notifications.TournamentNotifier
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Periodic background fetch that notifies on newly-completed tournaments, ranking changes (new #1,
 * top-10 entrants) and Race to the Finals qualifiers. Dependencies are pulled
 * from Koin's global context so the default WorkManager factory can instantiate the worker.
 */
class TournamentSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: TennisRepository by inject()
    private val prefs: NotificationPrefs by inject()
    private val notifier: TournamentNotifier by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result = try {
        val result = repository.load()
        // Only a genuine fresh fetch should drive notifications; on a network miss, retry later.
        if (result !is DataResult.Success || result.fromCache) {
            Result.retry()
        } else {
            val completed = result.data.tournaments.all().filter { it.isCompleted }
            val completedIds = completed.map { it.id }

            val outcome = TournamentSyncLogic.decide(
                completedIds = completedIds,
                seen = prefs.seenIds(),
                initialized = prefs.isInitialized(),
            )
            if (outcome.seedOnly) {
                // First run: record what's already finished without back-notifying the whole season.
                prefs.seedInitial(completedIds)
            } else {
                val byId = completed.associateBy { it.id }
                outcome.toNotify.forEach { id -> byId[id]?.let { notifier.notifyCompleted(it) } }
                prefs.markSeen(outcome.toNotify)
            }
            // Independent extras: a failure in one must not cost the tournament alerts above.
            val current = runCatching { settings.settings.first() }.getOrDefault(Settings())
            runCatching { checkRankings(result.data, current.rankingAlerts) }
            runCatching { checkRace(result.data, current.raceAlerts) }
            Result.success()
        }
    } catch (t: Throwable) {
        Result.retry()
    }

    /**
     * A new rankings snapshot: notify on a new #1 or top-10 entrants. The seen snapshot is always
     * advanced, even with the alert switched off, so turning it on later doesn't replay old changes.
     */
    private suspend fun checkRankings(data: TennisData, notify: Boolean) {
        Tour.entries.forEach { tour ->
            val outcome = RankingAlertLogic.decide(tour, data.rankings.forTour(tour), prefs.lastRankingSnapshot(tour))
            if (notify) outcome.change?.let(notifier::notifyRankingChange)
            outcome.latestKey?.let { prefs.setLastRankingSnapshot(tour, it) }
        }
    }

    /** Newly qualified Finals players, grouped into one notification per tour. */
    private suspend fun checkRace(data: TennisData, notify: Boolean) {
        val race = data.race ?: return // optional feed missing: leave state untouched
        val qualifiers = RaceAlertLogic.qualifiers(race)
        val outcome = TournamentSyncLogic.decide(
            completedIds = qualifiers.map { it.id },
            seen = prefs.raceSeenIds(),
            initialized = prefs.isRaceInitialized(),
        )
        if (outcome.seedOnly) {
            prefs.seedRace(qualifiers.map { it.id })
            return
        }
        if (notify) {
            qualifiers.filter { it.id in outcome.toNotify }
                .groupBy { it.tour }
                .forEach { (tour, newOnes) ->
                    notifier.notifyRaceQualified(
                        tour,
                        newOnes.map { it.player }.sortedBy { it.rank },
                        RaceAlertLogic.qualifiedCount(race, tour),
                    )
                }
        }
        prefs.markRaceSeen(outcome.toNotify)
    }
}
