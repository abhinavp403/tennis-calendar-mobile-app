package dev.abhinav.tenniscalendar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.abhinav.tenniscalendar.data.DataResult
import dev.abhinav.tenniscalendar.data.NotificationPrefs
import dev.abhinav.tenniscalendar.data.TennisRepository
import dev.abhinav.tenniscalendar.notifications.TournamentNotifier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Periodic background fetch that notifies on newly-completed tournaments. Dependencies are pulled
 * from Koin's global context so the default WorkManager factory can instantiate the worker.
 */
class TournamentSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: TennisRepository by inject()
    private val prefs: NotificationPrefs by inject()
    private val notifier: TournamentNotifier by inject()

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
            Result.success()
        }
    } catch (t: Throwable) {
        Result.retry()
    }
}
