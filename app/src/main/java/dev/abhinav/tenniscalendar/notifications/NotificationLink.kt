package dev.abhinav.tenniscalendar.notifications

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.abhinav.tenniscalendar.MainActivity
import dev.abhinav.tenniscalendar.model.Tour

/** Where a tapped notification (or widget row) should take the user inside the app. */
sealed interface NotificationLink {
    data class Tournament(val id: String) : NotificationLink
    data class Rankings(val tour: Tour) : NotificationLink
    data class Race(val tour: Tour) : NotificationLink

    /** An intent that opens [MainActivity] on this link; the data URI keeps PendingIntents distinct. */
    fun toIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        when (val link = this@NotificationLink) {
            is Tournament -> {
                putExtra(MainActivity.EXTRA_TOURNAMENT_ID, link.id)
                data = "tenniscalendar://tournament/${link.id}".toUri()
            }
            is Rankings -> {
                putExtra(EXTRA_SCREEN, SCREEN_RANKINGS)
                putExtra(EXTRA_TOUR, link.tour.key)
                data = "tenniscalendar://rankings/${link.tour.key}".toUri()
            }
            is Race -> {
                putExtra(EXTRA_SCREEN, SCREEN_RACE)
                putExtra(EXTRA_TOUR, link.tour.key)
                data = "tenniscalendar://race/${link.tour.key}".toUri()
            }
        }
    }

    companion object {
        private const val EXTRA_SCREEN = "open_screen"
        private const val EXTRA_TOUR = "open_tour"
        private const val SCREEN_RANKINGS = "rankings"
        private const val SCREEN_RACE = "race"

        fun fromIntent(intent: Intent?): NotificationLink? {
            intent ?: return null
            intent.getStringExtra(MainActivity.EXTRA_TOURNAMENT_ID)?.let { return Tournament(it) }
            val tour = intent.getStringExtra(EXTRA_TOUR)
                ?.let { key -> Tour.entries.firstOrNull { it.key == key } } ?: return null
            return when (intent.getStringExtra(EXTRA_SCREEN)) {
                SCREEN_RANKINGS -> Rankings(tour)
                SCREEN_RACE -> Race(tour)
                else -> null
            }
        }
    }
}
