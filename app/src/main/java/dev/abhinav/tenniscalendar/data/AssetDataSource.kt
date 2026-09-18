package dev.abhinav.tenniscalendar.data

import android.content.Context

/**
 * The seed dataset bundled in `assets/`, shipped with the app so the calendar shows content on the
 * very first launch — before any network fetch and even fully offline. The network refresh replaces
 * it as soon as it arrives.
 */
class AssetDataSource(private val context: Context) {

    fun read(): TennisCache.Cached? = runCatching {
        TennisCache.Cached(
            tournaments = asset("tournaments.json"),
            rankings = asset("rankings.json"),
            players = runCatching { asset("players.json") }.getOrNull(),
            race = runCatching { asset("race.json") }.getOrNull(),
        )
    }.getOrNull()

    private fun asset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
