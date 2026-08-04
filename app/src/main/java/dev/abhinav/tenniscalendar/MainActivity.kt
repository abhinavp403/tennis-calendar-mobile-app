package dev.abhinav.tenniscalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
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
                CalendarScreen()
            }
        }
    }
}
