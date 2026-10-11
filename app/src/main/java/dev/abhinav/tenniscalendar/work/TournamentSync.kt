package dev.abhinav.tenniscalendar.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the tournament-results check. By default it runs four times a day, at 01:00, 07:00,
 * 13:00 and 19:00 UTC (see [dev.abhinav.tenniscalendar.data.Settings.DEFAULT_SLOTS]); the times
 * are user-configurable in Settings. Each slot is a self-sustaining daily [PeriodicWorkRequest] whose first run is aligned to
 * the target via an initial delay, so a single failed/interrupted run never breaks the schedule.
 */
object TournamentSync {

    private const val PREFIX = "tournament-sync-"

    /** Upper bound on slot count, used to cancel stale slots when the user reduces them. */
    private const val MAX_SLOTS = 4

    /** Names used before slots were configurable; cancelled on (re)schedule to avoid duplicates. */
    private val LEGACY_NAMES = listOf("tournament-sync-0130", "tournament-sync-0930")

    fun schedule(context: Context, slots: List<LocalTime>) {
        val workManager = WorkManager.getInstance(context)
        LEGACY_NAMES.forEach(workManager::cancelUniqueWork)
        // Cancel any slots beyond the ones we're about to (re)schedule.
        for (i in slots.size until MAX_SLOTS) workManager.cancelUniqueWork(PREFIX + i)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        slots.forEachIndexed { i, time ->
            val request = PeriodicWorkRequestBuilder<TournamentSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(millisUntil(time), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(PREFIX + i, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    /** Stop all scheduled checks (notifications turned off). */
    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context)
        LEGACY_NAMES.forEach(workManager::cancelUniqueWork)
        repeat(MAX_SLOTS) { workManager.cancelUniqueWork(PREFIX + it) }
    }

    /** Milliseconds from [now] until the next occurrence of [target] in UTC. Pure, for testing. */
    fun millisUntil(target: LocalTime, now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Long {
        val today = now.toLocalDate()
        val next = listOf(today, today.plusDays(1))
            .map { ZonedDateTime.of(it, target, ZoneOffset.UTC) }
            .first { it.isAfter(now) }
        return Duration.between(now, next).toMillis()
    }
}
