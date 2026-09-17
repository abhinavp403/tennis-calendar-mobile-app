package dev.abhinav.tenniscalendar.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.abhinav.tenniscalendar.MainActivity
import dev.abhinav.tenniscalendar.R
import dev.abhinav.tenniscalendar.model.Tournament

/** Builds and posts "tournament complete" notifications. */
class TournamentNotifier(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tournament results",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Notifies you when a tournament finishes." }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun notifyCompleted(tournament: Tournament) {
        if (!canPost()) return

        val champion = tournament.winnerFull ?: tournament.winner ?: return
        val runnerUp = tournament.runnerUpFull ?: tournament.runnerUp
        val text = buildString {
            append(champion)
            if (!runnerUp.isNullOrBlank()) append(" def. ").append(runnerUp)
            tournament.score?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_TOURNAMENT_ID, tournament.id)
            data = "tenniscalendar://tournament/${tournament.id}".toUri()
        }
        val pending = PendingIntent.getActivity(
            context,
            tournament.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎾 ${tournament.name} — champion crowned")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // Tag by the (unique) tournament id so distinct tournaments never share a notification slot;
        // a shared int id would risk hashCode collisions silently overwriting one another.
        NotificationManagerCompat.from(context).notify(tournament.id, NOTIFICATION_ID, notification)
    }

    private fun canPost(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    companion object {
        const val CHANNEL_ID = "tournament_results"
        private const val NOTIFICATION_ID = 1
    }
}
