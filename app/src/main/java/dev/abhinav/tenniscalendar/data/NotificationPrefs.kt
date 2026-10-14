package dev.abhinav.tenniscalendar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.abhinav.tenniscalendar.model.Tour
import kotlinx.coroutines.flow.first

private val Context.notificationDataStore by preferencesDataStore(name = "notifications")

/**
 * Remembers what the background sync has already accounted for, so we notify only on transitions:
 * a tournament completing, a new ranking snapshot, a player qualifying for the Finals — once each,
 * and never for anything that was already the case when notifications were first enabled.
 */
class NotificationPrefs(private val context: Context) {

    private val seenKey = stringSetPreferencesKey("seen_completed_ids")
    private val initializedKey = booleanPreferencesKey("initialized")
    private val raceSeenKey = stringSetPreferencesKey("race_seen_qualified")
    private val raceInitializedKey = booleanPreferencesKey("race_initialized")
    private fun rankingKey(tour: Tour) = stringPreferencesKey("ranking_last_snapshot_${tour.key}")

    suspend fun isInitialized(): Boolean =
        context.notificationDataStore.data.first()[initializedKey] ?: false

    suspend fun seenIds(): Set<String> =
        context.notificationDataStore.data.first()[seenKey] ?: emptySet()

    /** First run: record everything already completed so we don't back-notify the whole season. */
    suspend fun seedInitial(completedIds: Collection<String>) {
        context.notificationDataStore.edit {
            it[seenKey] = completedIds.toSet()
            it[initializedKey] = true
        }
    }

    suspend fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.notificationDataStore.edit {
            it[seenKey] = (it[seenKey] ?: emptySet()) + ids
        }
    }

    /** The newest ranking snapshot key already accounted for (null before the first check). */
    suspend fun lastRankingSnapshot(tour: Tour): String? =
        context.notificationDataStore.data.first()[rankingKey(tour)]

    suspend fun setLastRankingSnapshot(tour: Tour, key: String) {
        context.notificationDataStore.edit { it[rankingKey(tour)] = key }
    }

    // Race qualification mirrors the tournament seen-set: seed on first run, then notify new ids.
    suspend fun isRaceInitialized(): Boolean =
        context.notificationDataStore.data.first()[raceInitializedKey] ?: false

    suspend fun raceSeenIds(): Set<String> =
        context.notificationDataStore.data.first()[raceSeenKey] ?: emptySet()

    suspend fun seedRace(ids: Collection<String>) {
        context.notificationDataStore.edit {
            it[raceSeenKey] = ids.toSet()
            it[raceInitializedKey] = true
        }
    }

    suspend fun markRaceSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.notificationDataStore.edit {
            it[raceSeenKey] = (it[raceSeenKey] ?: emptySet()) + ids
        }
    }
}
