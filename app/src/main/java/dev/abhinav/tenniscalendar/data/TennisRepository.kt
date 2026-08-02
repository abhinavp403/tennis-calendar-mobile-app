package dev.abhinav.tenniscalendar.data

import dev.abhinav.tenniscalendar.model.PlayersResponse
import dev.abhinav.tenniscalendar.model.RankingsResponse
import dev.abhinav.tenniscalendar.model.TennisData
import dev.abhinav.tenniscalendar.model.TournamentsResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Single source of truth for tennis data. Fetches fresh from the Gist on every load, writes the
 * last good payload to disk, and transparently falls back to that cache when the network fails —
 * matching the web app's offline behavior. This is the error boundary: no Retrofit/OkHttp
 * exception type ever escapes; callers see [DataResult] with a domain [DataError].
 */
class TennisRepository(
    private val api: TennisApi,
    private val cache: TennisCache,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun load(): DataResult<TennisData> = withContext(ioDispatcher) {
        try {
            val tournaments = api.getTournaments()
            val rankings = api.getRankings()
            // players.json is optional: a 404/parse issue must not fail the whole load, so it falls
            // back to empty (flags then come only from the rankings feed).
            val players = try {
                api.getPlayers()
            } catch (e: IOException) {
                PlayersResponse()
            } catch (e: HttpException) {
                PlayersResponse()
            } catch (e: kotlinx.serialization.SerializationException) {
                PlayersResponse()
            }
            cache.save(
                json.encodeToString(TournamentsResponse.serializer(), tournaments),
                json.encodeToString(RankingsResponse.serializer(), rankings),
                json.encodeToString(PlayersResponse.serializer(), players),
            )
            DataResult.Success(TennisData(tournaments, rankings, players), fromCache = false)
        } catch (e: IOException) {
            fallBackToCache(DataError.NO_CONNECTION_NO_CACHE)
        } catch (e: HttpException) {
            fallBackToCache(DataError.SERVER)
        } catch (e: kotlinx.serialization.SerializationException) {
            fallBackToCache(DataError.SERIALIZATION)
        } catch (e: Exception) {
            fallBackToCache(DataError.UNKNOWN)
        }
    }

    private fun fallBackToCache(errorIfNoCache: DataError): DataResult<TennisData> {
        val cached = cache.read() ?: return DataResult.Failure(errorIfNoCache)
        return runCatching {
            val tournaments = json.decodeFromString(TournamentsResponse.serializer(), cached.tournaments)
            val rankings = json.decodeFromString(RankingsResponse.serializer(), cached.rankings)
            val players = cached.players
                ?.let { json.decodeFromString(PlayersResponse.serializer(), it) }
                ?: PlayersResponse()
            DataResult.Success(TennisData(tournaments, rankings, players), fromCache = true)
        }.getOrElse { DataResult.Failure(errorIfNoCache) }
    }
}
