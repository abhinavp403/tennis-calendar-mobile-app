package dev.abhinav.tenniscalendar.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.abhinav.tenniscalendar.MainActivity
import dev.abhinav.tenniscalendar.data.SettingsRepository
import dev.abhinav.tenniscalendar.data.TennisRepository
import dev.abhinav.tenniscalendar.model.TennisData
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.notifications.NotificationLink
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.NumberFormat

/**
 * Home-screen widget: this week's events (with finals results once in) and the current world #1
 * for one tour. Each placed instance starts on the user's default tour and remembers its own
 * choice once the tour chip is tapped ([ToggleTourAction]). Reads only the local cache / bundled
 * seed — refreshed whenever the repository saves fresh data (see [TennisWidget.refresh]) and on the
 * system update period.
 */
class TennisWidget : GlanceAppWidget(), KoinComponent {

    private val repository: TennisRepository by inject()
    private val settings: SettingsRepository by inject()

    private data class Snapshot(val data: TennisData?, val defaultTour: Tour)

    private suspend fun loadSnapshot() = Snapshot(
        data = runCatching { repository.loadLocal() }.getOrNull(),
        defaultTour = runCatching { settings.settings.first().defaultTour }.getOrDefault(Tour.ATP),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadSnapshot()
        provideContent {
            val prefs = currentState<Preferences>()
            // A running session isn't re-provided on update, so reload whenever refresh() bumps
            // the stamp — otherwise a live widget would keep showing the data it started with.
            val snapshot by produceState(initial, prefs[DATA_STAMP]) {
                if (prefs[DATA_STAMP] != null) value = loadSnapshot()
            }
            val tour = prefs[SELECTED_TOUR]?.let { key -> Tour.entries.firstOrNull { it.key == key } }
                ?: snapshot.defaultTour
            val summary = snapshot.data?.let { WidgetSummary.from(it, tour) }
            GlanceTheme {
                WidgetContent(context, summary)
            }
        }
    }

    /** The widget-picker preview (Android 15+), drawn by the same UI from sample data. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme {
                WidgetContent(context, WidgetPreviews.SAMPLE)
            }
        }
    }

    companion object {
        /** Per-instance tour picked via the chip; absent means "follow the default tour". */
        internal val SELECTED_TOUR = stringPreferencesKey("selected_tour")
        private val DATA_STAMP = longPreferencesKey("data_stamp")

        /** Re-render every placed instance. Never throws: a widget hiccup must not break data loads. */
        suspend fun refresh(context: Context) {
            runCatching {
                val widget = TennisWidget()
                GlanceAppWidgetManager(context).getGlanceIds(TennisWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { it[DATA_STAMP] = System.currentTimeMillis() }
                    widget.update(context, id)
                }
            }
        }
    }
}

class TennisWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TennisWidget()
}

/** Flips this widget instance to the other tour. */
class ToggleTourAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val current = parameters[CURRENT_TOUR]?.let { key -> Tour.entries.firstOrNull { it.key == key } }
            ?: return
        updateAppWidgetState(context, glanceId) { it[TennisWidget.SELECTED_TOUR] = current.other.key }
        TennisWidget().update(context, glanceId)
    }

    companion object {
        val CURRENT_TOUR = ActionParameters.Key<String>("current_tour")
    }
}

@Composable
private fun WidgetContent(context: Context, summary: WidgetSummary?) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(openApp),
    ) {
        if (summary == null) {
            Message("Open Tennis Calendar to load tournaments")
            return@Column
        }
        Header(summary)
        Spacer(GlanceModifier.height(6.dp))
        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            if (summary.events.isEmpty()) {
                Message("No ${summary.tour.label} tournaments this week")
            } else {
                LazyColumn {
                    items(summary.events, itemId = { it.id.hashCode().toLong() }) { event ->
                        EventRow(event, summary.tour, openTournament(context, event.id))
                    }
                }
            }
        }
        summary.topPlayer?.let { player ->
            Spacer(GlanceModifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#1",
                    style = TextStyle(
                        color = fixedColor(TennisTokens.tourAccent(summary.tour)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = buildString {
                        append(player.name)
                        if (player.country.isNotBlank()) append(" (").append(player.country).append(")")
                        append(" · ").append(NumberFormat.getIntegerInstance().format(player.points))
                        append(" pts")
                    },
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Header(summary: WidgetSummary) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Tour chip: tapping it flips this widget between ATP and WTA.
        Text(
            text = "${summary.tour.label} ⇄",
            style = TextStyle(
                color = fixedColor(Color.White),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .background(fixedColor(TennisTokens.tourAccent(summary.tour)))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .semantics { contentDescription = "Switch to ${summary.tour.other.label}" }
                .clickable(
                    actionRunCallback<ToggleTourAction>(
                        actionParametersOf(ToggleTourAction.CURRENT_TOUR to summary.tour.key)
                    )
                ),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "This week",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = summary.weekLabel,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun EventRow(event: WidgetSummary.Event, tour: Tour, onClick: Action) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(
                    ColorProvider(
                        day = TennisTokens.tierColor(event.level, dark = false),
                        night = TennisTokens.tierColor(event.level, dark = true),
                    )
                ),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "${event.name} · ${TennisTokens.levelLabel(event.level, tour)}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = event.line,
                style = TextStyle(
                    color = if (event.completed) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        modifier = GlanceModifier.padding(vertical = 4.dp),
    )
}

private fun fixedColor(color: Color) = ColorProvider(day = color, night = color)

/** Opens the app on this tournament's detail sheet, same path as a tapped results notification. */
private fun openTournament(context: Context, tournamentId: String): Action =
    actionStartActivity(NotificationLink.Tournament(tournamentId).toIntent(context))
