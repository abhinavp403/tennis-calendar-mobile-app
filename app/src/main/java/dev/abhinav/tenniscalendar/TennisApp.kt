package dev.abhinav.tenniscalendar

import android.app.Application
import dev.abhinav.tenniscalendar.data.SettingsRepository
import dev.abhinav.tenniscalendar.di.appModule
import dev.abhinav.tenniscalendar.notifications.TournamentNotifier
import dev.abhinav.tenniscalendar.widget.TennisWidget
import dev.abhinav.tenniscalendar.work.TournamentSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TennisApp : Application(), KoinComponent {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TennisApp)
            modules(appModule)
        }
        val notifier: TournamentNotifier by inject()
        notifier.ensureChannel()
        // Align the background schedule with the saved settings (respects the on/off toggle and
        // the configured slot times). WorkManager de-dupes by unique name, so this is idempotent.
        val settings: SettingsRepository by inject()
        appScope.launch {
            runCatching {
                val current = settings.settings.first()
                if (current.notificationsEnabled) {
                    TournamentSync.schedule(this@TennisApp, current.slotTimes)
                } else {
                    TournamentSync.cancel(this@TennisApp)
                }
            }
        }
        // The home widget shows the default tour, so re-render it when that setting changes.
        appScope.launch {
            runCatching {
                settings.settings.map { it.defaultTour }.distinctUntilChanged().drop(1)
                    .collect { TennisWidget.refresh(this@TennisApp) }
            }
        }
    }
}
