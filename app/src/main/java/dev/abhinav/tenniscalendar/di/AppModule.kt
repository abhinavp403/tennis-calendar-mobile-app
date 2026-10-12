package dev.abhinav.tenniscalendar.di

import dev.abhinav.tenniscalendar.BuildConfig
import dev.abhinav.tenniscalendar.data.AssetDataSource
import dev.abhinav.tenniscalendar.data.ConnectivityObserver
import dev.abhinav.tenniscalendar.data.NotificationPrefs
import dev.abhinav.tenniscalendar.data.SettingsRepository
import dev.abhinav.tenniscalendar.data.TennisApi
import dev.abhinav.tenniscalendar.data.TennisCache
import dev.abhinav.tenniscalendar.data.TennisRepository
import dev.abhinav.tenniscalendar.notifications.TournamentNotifier
import dev.abhinav.tenniscalendar.ui.calendar.CalendarViewModel
import dev.abhinav.tenniscalendar.ui.settings.SettingsViewModel
import dev.abhinav.tenniscalendar.widget.TennisWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    single<TennisApi> {
        val json: Json = get()
        Retrofit.Builder()
            .baseUrl(TennisApi.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TennisApi::class.java)
    }

    single { TennisCache(get()) }

    single { AssetDataSource(get()) }

    single { ConnectivityObserver(get()) }

    single { NotificationPrefs(get()) }

    single { TournamentNotifier(get()) }

    single { SettingsRepository(get()) }

    single {
        val context = androidContext()
        TennisRepository(get(), get(), get(), get(), Dispatchers.IO) { TennisWidget.refresh(context) }
    }

    viewModel { CalendarViewModel(get(), get(), get()) }

    viewModel { SettingsViewModel(get(), get()) }
}
