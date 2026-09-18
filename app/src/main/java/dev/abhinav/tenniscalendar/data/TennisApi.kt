package dev.abhinav.tenniscalendar.data

import dev.abhinav.tenniscalendar.model.PlayersResponse
import dev.abhinav.tenniscalendar.model.RaceResponse
import dev.abhinav.tenniscalendar.model.RankingsResponse
import dev.abhinav.tenniscalendar.model.TournamentsResponse
import retrofit2.http.GET

/**
 * Reads the two public Gist files directly (raw HTTPS GET, no auth). Base URL is the raw Gist
 * root; see [dev.abhinav.tenniscalendar.di.AppModule].
 */
interface TennisApi {
    @GET("tournaments.json")
    suspend fun getTournaments(): TournamentsResponse

    @GET("rankings.json")
    suspend fun getRankings(): RankingsResponse

    @GET("players.json")
    suspend fun getPlayers(): PlayersResponse

    @GET("race.json")
    suspend fun getRace(): RaceResponse

    companion object {
        const val BASE_URL =
            "https://gist.githubusercontent.com/abhinavp403/c75d3f961da94fdeed16cdbd8e2ec08e/raw/"
    }
}
