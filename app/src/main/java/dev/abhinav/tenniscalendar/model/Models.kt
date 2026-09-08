package dev.abhinav.tenniscalendar.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The two tours. String value matches the JSON top-level keys and the ranking maps. */
enum class Tour(val key: String, val label: String) {
    ATP("atp", "ATP"),
    WTA("wta", "WTA");

    val other: Tour get() = if (this == ATP) WTA else ATP
}

/** Court surfaces present in the data. [raw] matches the JSON string exactly. */
enum class Surface(val raw: String, val label: String) {
    HARD("Hard", "Hard"),
    INDOOR_HARD("Indoor Hard", "Indoor"),
    CLAY("Clay", "Clay"),
    GRASS("Grass", "Grass");

    companion object {
        fun fromRaw(raw: String): Surface? = entries.firstOrNull { it.raw == raw }
    }
}

@Serializable
data class Tournament(
    val id: String,
    val name: String,
    val level: Int,
    val start: String,
    val end: String,
    val location: String,
    val surface: String,
    val logo: String,
    val winner: String? = null,
    @SerialName("runner_up") val runnerUp: String? = null,
    val score: String? = null,
    @SerialName("winner_full") val winnerFull: String? = null,
    @SerialName("runner_up_full") val runnerUpFull: String? = null,
) {
    /** A tournament counts as completed once both finalists are recorded. */
    val isCompleted: Boolean get() = !winner.isNullOrBlank() && !runnerUp.isNullOrBlank()

    val surfaceEnum: Surface? get() = Surface.fromRaw(surface)
}

@Serializable
data class TournamentsResponse(
    val atp: List<Tournament> = emptyList(),
    val wta: List<Tournament> = emptyList(),
) {
    fun forTour(tour: Tour): List<Tournament> = if (tour == Tour.ATP) atp else wta

    /** Both tours combined. */
    fun all(): List<Tournament> = atp + wta
}

@Serializable
data class RankingPlayer(
    val rank: Int,
    val name: String,
    val country: String,
    val points: Int,
    val movement: String? = null,
)

/**
 * rankings.json is `{ atp: { key -> Player[] }, wta: { key -> Player[] } }` where keys are a mix of
 * legacy `YYYY-MM` (monthly) and bi-weekly `YYYY-MM-DD` snapshots.
 */
@Serializable
data class RankingsResponse(
    val atp: Map<String, List<RankingPlayer>> = emptyMap(),
    val wta: Map<String, List<RankingPlayer>> = emptyMap(),
) {
    fun forTour(tour: Tour): Map<String, List<RankingPlayer>> = if (tour == Tour.ATP) atp else wta
}

/**
 * Player nationality, sourced from the Gist so it can be edited without code changes:
 * `{ atp: { "C. Alcaraz": "ESP", … }, wta: { … } }`, keyed by abbreviated name → IOC code.
 */
@Serializable
data class PlayersResponse(
    val atp: Map<String, String> = emptyMap(),
    val wta: Map<String, String> = emptyMap(),
) {
    fun forTour(tour: Tour): Map<String, String> = if (tour == Tour.ATP) atp else wta
}

/** The payloads bundled so the repository can hand the UI a single consistent snapshot. */
data class TennisData(
    val tournaments: TournamentsResponse,
    val rankings: RankingsResponse,
    val players: PlayersResponse = PlayersResponse(),
)
