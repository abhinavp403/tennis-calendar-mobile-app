package dev.abhinav.tenniscalendar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhinav.tenniscalendar.data.Settings
import dev.abhinav.tenniscalendar.data.SettingsRepository
import dev.abhinav.tenniscalendar.data.ThemeMode
import dev.abhinav.tenniscalendar.ui.calendar.CalendarScreen
import dev.abhinav.tenniscalendar.ui.theme.TennisCalendarTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    // The tournament id from a tapped notification, surfaced to Compose to open its detail sheet.
    private var pendingTournamentId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingTournamentId = intent?.getStringExtra(EXTRA_TOURNAMENT_ID)
        // Edge-to-edge without the APIs deprecated in Android 15 (API 35): draw behind the system
        // bars and only control the bar-icon contrast. Bar colors are left to the system
        // (transparent on API 35+), so we never call the deprecated
        // Window.setStatusBarColor / setNavigationBarColor. Insets are consumed by the Scaffold in
        // CalendarScreen.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val settings by koinInject<SettingsRepository>().settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            TennisCalendarTheme(darkTheme = darkTheme) {
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                RequestNotificationPermission()
                CalendarScreen(
                    notificationTournamentId = pendingTournamentId,
                    onNotificationConsumed = { pendingTournamentId = null },
                )
            }
        }
    }

    // Tapping a notification while the activity is already running arrives here (single-top).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTournamentId = intent.getStringExtra(EXTRA_TOURNAMENT_ID)
    }

    /** Ask for POST_NOTIFICATIONS once on launch (Android 13+) so results notifications can show. */
    @androidx.compose.runtime.Composable
    private fun RequestNotificationPermission() {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_TOURNAMENT_ID = "tournament_id"
    }
}
