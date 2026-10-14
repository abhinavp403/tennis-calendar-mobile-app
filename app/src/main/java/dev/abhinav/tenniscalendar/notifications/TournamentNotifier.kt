package dev.abhinav.tenniscalendar.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.abhinav.tenniscalendar.R
import dev.abhinav.tenniscalendar.model.RacePlayer
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.work.RankingAlertLogic

/** Builds and posts tournament-result, ranking-change and Race qualification notifications. */
class TournamentNotifier(private val context: Context) {

    /** Each alert type has its own channel so it can also be silenced from system settings. */
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            NotificationChannel(CHANNEL_ID, "Tournament results", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Notifies you when a tournament finishes." },
            NotificationChannel(RANKINGS_CHANNEL_ID, "Ranking changes", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "A new world #1 or players entering the top 10." },
            NotificationChannel(RACE_CHANNEL_ID, "Race to the Finals", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Players qualifying for the season-ending Finals." },
        ).forEach(manager::createNotificationChannel)
    }

    fun notifyCompleted(tournament: Tournament) {
        val champion = tournament.winnerFull ?: tournament.winner ?: return
        val runnerUp = tournament.runnerUpFull ?: tournament.runnerUp
        val text = buildString {
            append(champion)
            if (!runnerUp.isNullOrBlank()) append(" def. ").append(runnerUp)
            tournament.score?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
        }
        // Tag by the (unique) tournament id so distinct tournaments never share a notification slot;
        // a shared int id would risk hashCode collisions silently overwriting one another.
        post(
            channel = CHANNEL_ID,
            tag = tournament.id,
            title = "🎾 ${tournament.name} — champion crowned",
            text = text,
            link = NotificationLink.Tournament(tournament.id),
        )
    }

    fun notifyRankingChange(change: RankingAlertLogic.Change) {
        val tour = change.tour.label
        val entered = change.enteredTop.joinToString(", ") { "${it.name} (#${it.rank})" }
        val newOne = change.newNumberOne
        val title = if (newOne != null) "🏆 New $tour world #1" else "📈 $tour top ${RankingAlertLogic.TOP} update"
        val text = buildString {
            if (newOne != null) {
                append(newOne.name).append(" is the new world #1")
                change.previousNumberOne?.let { append(", taking over from ").append(it) }
                append('.')
            }
            if (entered.isNotEmpty()) {
                if (isNotEmpty()) append(' ')
                append("Into the top ${RankingAlertLogic.TOP}: ").append(entered).append('.')
            }
        }
        post(
            channel = RANKINGS_CHANNEL_ID,
            // One per tour: a newer snapshot replaces an unread older one.
            tag = "rankings-${change.tour.key}",
            title = title,
            text = text,
            link = NotificationLink.Rankings(change.tour),
        )
    }

    fun notifyRaceQualified(tour: Tour, players: List<RacePlayer>, qualifiedCount: Int) {
        if (players.isEmpty()) return
        val names = players.map { it.full.ifBlank { it.name } }
        val who = when (names.size) {
            1 -> names[0]
            else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
        }
        val verb = if (names.size == 1) "qualifies" else "qualify"
        post(
            channel = RACE_CHANNEL_ID,
            tag = "race-${tour.key}-${names.joinToString("|")}",
            title = "✅ ${tour.label} Finals qualification",
            text = "$who $verb for the ${tour.label} Finals · $qualifiedCount of 8 spots filled.",
            link = NotificationLink.Race(tour),
        )
    }

    private fun post(channel: String, tag: String, title: String, text: String, link: NotificationLink) {
        if (!canPost()) return
        val pending = PendingIntent.getActivity(
            context,
            tag.hashCode(),
            link.toIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(tag, NOTIFICATION_ID, notification)
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
        const val RANKINGS_CHANNEL_ID = "ranking_changes"
        const val RACE_CHANNEL_ID = "race_qualification"
        private const val NOTIFICATION_ID = 1
    }
}
