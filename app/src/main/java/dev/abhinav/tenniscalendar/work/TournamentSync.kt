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
 * Schedules the tournament-results check to run shortly after the data source refreshes. The Gist is
 * rebuilt by a GitHub Action at 01:00 and 09:00 UTC, so we check ~30 min later, twice a day.
 *
 * Each slot is a self-sustaining daily [PeriodicWorkRequest] whose first run is aligned to the target
 * time via an initial delay; WorkManager keeps repeating it, so a single failed/interrupted run never
 * breaks the schedule.
 */
object TournamentSync {

    /** UTC times to run — 30 min after each Gist refresh (01:00 / 09:00 UTC) — with unique names. */
    private val SLOTS = listOf(
        LocalTime.of(1, 30) to "tournament-sync-0130",
        LocalTime.of(9, 30) to "tournament-sync-0930",
    )

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        for ((time, name) in SLOTS) {
            val request = PeriodicWorkRequestBuilder<TournamentSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(millisUntil(time), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
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
