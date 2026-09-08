package dev.abhinav.tenniscalendar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.notificationDataStore by preferencesDataStore(name = "notifications")

/**
 * Remembers which tournaments the background sync has already accounted for, so we notify only on
 * the transition to completed — once per tournament, and never for tournaments that were already
 * finished when notifications were first enabled.
 */
class NotificationPrefs(private val context: Context) {

    private val seenKey = stringSetPreferencesKey("seen_completed_ids")
    private val initializedKey = booleanPreferencesKey("initialized")

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
}
