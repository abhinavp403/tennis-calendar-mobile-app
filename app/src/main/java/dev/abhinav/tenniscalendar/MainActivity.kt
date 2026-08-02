package dev.abhinav.tenniscalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.abhinav.tenniscalendar.ui.calendar.CalendarScreen
import dev.abhinav.tenniscalendar.ui.theme.TennisCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TennisCalendarTheme {
                CalendarScreen()
            }
        }
    }
}
