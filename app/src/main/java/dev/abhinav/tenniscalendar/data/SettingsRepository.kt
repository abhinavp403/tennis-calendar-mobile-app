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
    val slot1: LocalTime = LocalTime.of(1, 30),
    val slot2: LocalTime = LocalTime.of(9, 30),
) {
    /** The notification check times (UTC), in order. */
    val slotTimes: List<LocalTime> get() = listOf(slot1, slot2)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val tourKey = stringPreferencesKey("default_tour")
    private val slot1Key = intPreferencesKey("slot1_minutes")
    private val slot2Key = intPreferencesKey("slot2_minutes")

    val settings: Flow<Settings> = context.settingsDataStore.data.map { p ->
        Settings(
            themeMode = p[themeKey]?.let { enumOrNull<ThemeMode>(it) } ?: ThemeMode.SYSTEM,
            notificationsEnabled = p[notificationsKey] ?: true,
            defaultTour = p[tourKey]?.let { enumOrNull<Tour>(it) } ?: Tour.ATP,
            slot1 = p[slot1Key]?.let(::minutesToTime) ?: LocalTime.of(1, 30),
            slot2 = p[slot2Key]?.let(::minutesToTime) ?: LocalTime.of(9, 30),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[themeKey] = mode.name }

    suspend fun setNotificationsEnabled(enabled: Boolean) = edit { it[notificationsKey] = enabled }

    suspend fun setDefaultTour(tour: Tour) = edit { it[tourKey] = tour.name }

    suspend fun setSlotTimes(slot1: LocalTime, slot2: LocalTime) = edit {
        it[slot1Key] = slot1.toSecondOfDay() / 60
        it[slot2Key] = slot2.toSecondOfDay() / 60
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private fun minutesToTime(minuteOfDay: Int): LocalTime =
        LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60)

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
        runCatching { enumValueOf<E>(name) }.getOrNull()
}
