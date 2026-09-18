package dev.abhinav.tenniscalendar.data

import android.content.Context
import java.io.File

/**
 * Persists the last successful raw JSON payloads to the app's files dir so the calendar can launch
 * offline. Mirrors the web app, which caches the last good `dataSource` response.
 */
class TennisCache(context: Context) {
    private val dir: File = context.filesDir
    private val tournamentsFile = File(dir, "tournaments.json")
    private val rankingsFile = File(dir, "rankings.json")
    private val playersFile = File(dir, "players.json")
    private val raceFile = File(dir, "race.json")

    /**
     * Raw cached JSON. [players] and [race] may be null for caches written before those files
     * existed, or when the (optional) race feed was unavailable.
     */
    data class Cached(
        val tournaments: String,
        val rankings: String,
        val players: String?,
        val race: String? = null,
    )

    fun save(tournamentsJson: String, rankingsJson: String, playersJson: String, raceJson: String?) {
        runCatching {
            tournamentsFile.writeText(tournamentsJson)
            rankingsFile.writeText(rankingsJson)
            playersFile.writeText(playersJson)
            if (raceJson != null) raceFile.writeText(raceJson) else raceFile.delete()
        }
    }

    /** Returns the cached payloads, or null if the required files are missing. */
    fun read(): Cached? {
        if (!tournamentsFile.exists() || !rankingsFile.exists()) return null
        return runCatching {
            Cached(
                tournaments = tournamentsFile.readText(),
                rankings = rankingsFile.readText(),
                players = playersFile.takeIf { it.exists() }?.readText(),
                race = raceFile.takeIf { it.exists() }?.readText(),
            )
        }.getOrNull()
    }
}
