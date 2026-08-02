package dev.abhinav.tenniscalendar

import android.app.Application
import dev.abhinav.tenniscalendar.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TennisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TennisApp)
            modules(appModule)
        }
    }
}
