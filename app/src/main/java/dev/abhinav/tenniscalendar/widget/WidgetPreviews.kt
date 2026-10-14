package dev.abhinav.tenniscalendar.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.abhinav.tenniscalendar.BuildConfig
import dev.abhinav.tenniscalendar.model.RankingPlayer
import dev.abhinav.tenniscalendar.model.Tour

/**
 * Widget-picker previews. On Android 15+ the picker shows a preview rendered by the widget's own
 * Compose UI ([TennisWidget.providePreview]) from [SAMPLE]; older versions fall back to the static
 * `previewLayout` (12–14) or `previewImage` (≤11) declared in `tennis_widget_info.xml`.
 */
object WidgetPreviews {

    /** Representative content for the picker, independent of what's cached on the device. */
    val SAMPLE = WidgetSummary(
        tour = Tour.ATP,
        weekLabel = "Oct 5 – Oct 11",
        events = listOf(
            WidgetSummary.Event("preview-1", "Shanghai Masters", 1000, "Final Sun, Oct 18", completed = false),
            WidgetSummary.Event("preview-2", "China Open", 500, "N. Djokovic def. A. de Minaur", completed = true),
            WidgetSummary.Event("preview-3", "Tokyo Open", 500, "C. Alcaraz def. J. Lehečka", completed = true),
        ),
        topPlayer = RankingPlayer(rank = 1, name = "J. Sinner", country = "ITA", points = 11000),
    )

    private const val PREFS = "widget_previews"
    private const val KEY_VERSION = "published_version_code"

    /**
     * Publishes the generated preview once per app version. The platform rate-limits this call, so
     * it's skipped when this version already published, and retried on a later launch if limited.
     */
    suspend fun publishIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, -1) == BuildConfig.VERSION_CODE) return
        val result = runCatching {
            GlanceAppWidgetManager(context).setWidgetPreviews(TennisWidgetReceiver::class)
        }.getOrNull()
        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            prefs.edit().putInt(KEY_VERSION, BuildConfig.VERSION_CODE).apply()
        }
    }
}
