package dev.abhinav.tenniscalendar.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhinav.tenniscalendar.data.Settings
import dev.abhinav.tenniscalendar.data.SettingsRepository
import dev.abhinav.tenniscalendar.data.ThemeMode
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.work.TournamentSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val appContext: Context,
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<Settings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setDefaultTour(tour: Tour) = viewModelScope.launch { repository.setDefaultTour(tour) }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
        if (enabled) TournamentSync.schedule(appContext, settings.value.slotTimes)
        else TournamentSync.cancel(appContext)
    }

    fun setSlotTime(index: Int, time: LocalTime) = viewModelScope.launch {
        repository.setSlotTime(index, time)
        if (settings.value.notificationsEnabled) {
            val slots = settings.value.slotTimes.toMutableList().apply { this[index] = time }
            TournamentSync.schedule(appContext, slots)
        }
    }
}
