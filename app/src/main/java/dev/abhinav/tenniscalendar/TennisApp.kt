package dev.abhinav.tenniscalendar

import android.app.Application
import dev.abhinav.tenniscalendar.di.appModule
import dev.abhinav.tenniscalendar.notifications.TournamentNotifier
import dev.abhinav.tenniscalendar.work.TournamentSync
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TennisApp : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TennisApp)
            modules(appModule)
        }
        runCatching {
            val notifier: TournamentNotifier by inject()
            notifier.ensureChannel()
            TournamentSync.schedule(this)
        }
    }
}
