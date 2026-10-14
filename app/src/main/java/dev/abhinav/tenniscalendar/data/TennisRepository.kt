package dev.abhinav.tenniscalendar.data

import dev.abhinav.tenniscalendar.model.PlayersResponse
import dev.abhinav.tenniscalendar.model.RaceResponse
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
    private val assets: AssetDataSource,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
    /**
     * Called after a fresh payload is written to the cache (e.g. to re-render the home widget).
     * Must not block: load() returns only after it, so slow work belongs on another scope.
     */
    private val onCacheUpdated: () -> Unit = {},
) {
    /**
     * Local-only load (no network): the disk cache if present, otherwise the bundled asset seed.
     * Lets the UI show content instantly on launch while the network refresh runs. Never fails as
     * long as the bundled seed parses, so the app is never stuck on an empty loading screen.
     */
    suspend fun loadLocal(): TennisData? = withContext(ioDispatcher) {
        (cache.read() ?: assets.read())?.let { parseOrNull(it) }
    }

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
            // race.json is optional too: a missing/broken file must not fail the whole load.
            val race: RaceResponse? = try {
                api.getRace()
            } catch (e: IOException) {
                null
            } catch (e: HttpException) {
                null
            } catch (e: kotlinx.serialization.SerializationException) {
                null
            }
            cache.save(
                json.encodeToString(TournamentsResponse.serializer(), tournaments),
                json.encodeToString(RankingsResponse.serializer(), rankings),
                json.encodeToString(PlayersResponse.serializer(), players),
                race?.let { json.encodeToString(RaceResponse.serializer(), it) },
            )
            runCatching { onCacheUpdated() }
            DataResult.Success(TennisData(tournaments, rankings, players, race), fromCache = false)
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

    // Network failed: fall back to the disk cache, then the bundled asset seed, so there is almost
    // always something to show. Only a genuinely unreadable seed yields a Failure.
    private fun fallBackToCache(errorIfNoCache: DataError): DataResult<TennisData> {
        val raw = cache.read() ?: assets.read() ?: return DataResult.Failure(errorIfNoCache)
        return parseOrNull(raw)
            ?.let { DataResult.Success(it, fromCache = true) }
            ?: DataResult.Failure(errorIfNoCache)
    }

    private fun parseOrNull(cached: TennisCache.Cached): TennisData? = runCatching {
        TennisData(
            tournaments = json.decodeFromString(TournamentsResponse.serializer(), cached.tournaments),
            rankings = json.decodeFromString(RankingsResponse.serializer(), cached.rankings),
            players = cached.players
                ?.let { json.decodeFromString(PlayersResponse.serializer(), it) }
                ?: PlayersResponse(),
            race = cached.race?.let {
                runCatching { json.decodeFromString(RaceResponse.serializer(), it) }.getOrNull()
            },
        )
    }.getOrNull()
}
