package dev.abhinav.tenniscalendar

import android.Manifest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dev.abhinav.tenniscalendar.ui.calendar.CalendarScreen
import dev.abhinav.tenniscalendar.ui.theme.TennisCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge without the APIs deprecated in Android 15 (API 35): draw behind the system
        // bars and only control the bar-icon contrast. Bar colors are left to the system
        // (transparent on API 35+), so we never call the deprecated
        // Window.setStatusBarColor / setNavigationBarColor. Insets are consumed by the Scaffold in
        // CalendarScreen.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TennisCalendarTheme {
                val darkTheme = isSystemInDarkTheme()
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                RequestNotificationPermission()
                CalendarScreen()
            }
        }
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
}
