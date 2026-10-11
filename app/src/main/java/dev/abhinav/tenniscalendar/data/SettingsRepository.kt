package dev.abhinav.tenniscalendar.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.abhinav.tenniscalendar.model.Tour
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User-configurable app settings, with sensible defaults. Notification slot times are UTC. */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultTour: Tour = Tour.ATP,
    /** The notification check times (UTC), in order. Always [DEFAULT_SLOTS].size entries. */
    val slotTimes: List<LocalTime> = DEFAULT_SLOTS,
) {
    companion object {
        /** Default background check times (UTC): every six hours. */
        val DEFAULT_SLOTS: List<LocalTime> = listOf(
            LocalTime.of(1, 0),
            LocalTime.of(7, 0),
            LocalTime.of(13, 0),
            LocalTime.of(19, 0),
        )
    }
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val tourKey = stringPreferencesKey("default_tour")
    // slot1/slot2 keep their original key names so earlier customizations carry over.
    private val slotKeys = Settings.DEFAULT_SLOTS.indices.map { intPreferencesKey("slot${it + 1}_minutes") }

    val settings: Flow<Settings> = context.settingsDataStore.data.map { p ->
        Settings(
            themeMode = p[themeKey]?.let { enumOrNull<ThemeMode>(it) } ?: ThemeMode.SYSTEM,
            notificationsEnabled = p[notificationsKey] ?: true,
            defaultTour = p[tourKey]?.let { enumOrNull<Tour>(it) } ?: Tour.ATP,
            slotTimes = slotKeys.mapIndexed { i, key ->
                p[key]?.let(::minutesToTime) ?: Settings.DEFAULT_SLOTS[i]
            },
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[themeKey] = mode.name }

    suspend fun setNotificationsEnabled(enabled: Boolean) = edit { it[notificationsKey] = enabled }

    suspend fun setDefaultTour(tour: Tour) = edit { it[tourKey] = tour.name }

    /** Sets the check time (UTC) at [index] (0-based), leaving the other slots unchanged. */
    suspend fun setSlotTime(index: Int, time: LocalTime) = edit {
        it[slotKeys[index]] = time.toSecondOfDay() / 60
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private fun minutesToTime(minuteOfDay: Int): LocalTime =
        LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60)

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
        runCatching { enumValueOf<E>(name) }.getOrNull()
}
