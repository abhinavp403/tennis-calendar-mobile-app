package dev.abhinav.tenniscalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhinav.tenniscalendar.data.ConnectivityObserver
import dev.abhinav.tenniscalendar.data.DataError
import dev.abhinav.tenniscalendar.data.DataResult
import dev.abhinav.tenniscalendar.data.TennisRepository
import dev.abhinav.tenniscalendar.domain.NameMatch
import dev.abhinav.tenniscalendar.domain.PlayerStat
import dev.abhinav.tenniscalendar.domain.PlayerStats
import dev.abhinav.tenniscalendar.domain.RankingKeys
import dev.abhinav.tenniscalendar.model.Surface
import dev.abhinav.tenniscalendar.model.TennisData
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarViewModel(
    private val repository: TennisRepository,
    private val connectivity: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var data: TennisData? = null

    // Cache for the per-(tour, month) heavy derivation, so cheap changes (e.g. surface filter) don't
    // rebuild the leaderboards and rankings.
    private data class Heavy(
        val leaderboard: List<PlayerStat>,
        val searchLeaderboard: List<PlayerStat>,
        val rankings: List<RankingRow>,
        val snapshotLabel: String?,
    )
    private var heavyKey: Pair<Tour, YearMonth>? = null
    private var heavyCache: Heavy? = null

    init {
        viewModelScope.launch {
            // Show the bundled/cached seed immediately so the app never sits on an empty spinner
            // (offline, slow networks, or store-review crawlers), then refresh from the network.
            if (data == null) {
                repository.loadLocal()?.let { seed ->
                    data = seed
                    heavyKey = null
                    _uiState.update { recompute(it.copy(isLoading = false)) }
                }
            }
            fetch()
        }
        observeConnectivity()
    }

    /**
     * @param showLoading blanks the screen with a spinner. Only used when there is nothing to show
     * yet; a refresh keeps the current data on screen.
     */
    fun load(showLoading: Boolean = true) {
        _uiState.update {
            if (showLoading) it.copy(isLoading = true, error = null) else it.copy(isRefreshing = true)
        }
        viewModelScope.launch { fetch() }
    }

    /** Re-fetch without blanking the screen (used by the offline banner and connectivity regain). */
    fun refresh() = load(showLoading = false)

    /** Fetch from the network and apply the result. Guarded so no throwable can leave the UI hung. */
    private suspend fun fetch() {
        val result = try {
            repository.load()
        } catch (t: Throwable) {
            DataResult.Failure(DataError.UNKNOWN)
        }
        when (result) {
            is DataResult.Success -> {
                data = result.data
                heavyKey = null // fresh data invalidates the derivation cache
                _uiState.update {
                    recompute(it.copy(isLoading = false, isRefreshing = false, error = null, isOffline = result.fromCache))
                }
            }
            is DataResult.Failure -> _uiState.update {
                // Keep whatever we're already showing (seed/cache); only error out with nothing to show.
                if (data != null) it.copy(isLoading = false, isRefreshing = false, isOffline = true)
                else it.copy(isLoading = false, isRefreshing = false, error = result.error)
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val current = _uiState.value
                // When the network returns and we're on stale/cached data or an error, try again.
                if (online && (current.isOffline || current.error != null)) refresh()
            }
        }
    }

    fun selectTour(tour: Tour) = _uiState.update { recompute(it.copy(tour = tour)) }

    // recompute() clamps the month into the derived season range, so these can advance freely.
    fun nextMonth() = _uiState.update { recompute(it.copy(month = it.month.plusMonths(1))) }

    fun previousMonth() = _uiState.update { recompute(it.copy(month = it.month.minusMonths(1))) }

    fun goToToday() = _uiState.update { recompute(it.copy(month = YearMonth.now())) }

    fun setMonth(month: YearMonth) = _uiState.update { recompute(it.copy(month = month)) }

    fun setSurfaceFilter(surface: Surface?) =
        _uiState.update { recompute(it.copy(surfaceFilter = surface)) }

    /** Full season tournament list for the current tour (for search and the champions wall). */
    fun seasonTournaments(): List<Tournament> =
        data?.forTourList(uiState.value.tour) ?: emptyList()

    private fun TennisData.forTourList(tour: Tour) = tournaments.forTour(tour)

    /** Recomputes all derived slices from the current selections. Pure given [data]. */
    private fun recompute(input: CalendarUiState): CalendarUiState {
        val d = data ?: return input

        // Derive the season range from the tournament dates (both tours), snapped to full years so
        // empty trailing/leading months (e.g. December with no events) are still shown.
        val allDates = d.tournaments.all()
            .flatMap { listOfNotNull(parseDate(it.start), parseDate(it.end)) }
        val minMonth = allDates.minOrNull()?.let { YearMonth.of(it.year, 1) } ?: input.month
        val maxMonth = allDates.maxOrNull()?.let { YearMonth.of(it.year, 12) } ?: input.month
        val state = input.copy(
            month = CalendarBounds.clamp(input.month, minMonth, maxMonth),
            minMonth = minMonth,
            maxMonth = maxMonth,
        )

        val tourList = d.tournaments.forTour(state.tour)

        // Depends on tour + month + surface — always cheap, always recomputed.
        val monthTournaments = tourList
            .filter { endsInMonth(it, state.month) }
            .filter { state.surfaceFilter == null || it.surfaceEnum == state.surfaceFilter }
            .sortedWith(compareByDescending<Tournament> { it.level }.thenBy { it.name })

        // The leaderboards and rankings depend only on (tour, month) — reuse the cache when those
        // are unchanged so a surface-filter tap doesn't rebuild them.
        val key = state.tour to state.month
        val heavy = heavyCache?.takeIf { heavyKey == key }
            ?: computeHeavy(d, state.tour, state.month).also { heavyCache = it; heavyKey = key }

        return state.copy(
            monthTournaments = monthTournaments,
            leaderboard = heavy.leaderboard,
            searchLeaderboard = heavy.searchLeaderboard,
            rankings = heavy.rankings,
            rankingSnapshotLabel = heavy.snapshotLabel,
            canGoPrev = state.month.isAfter(minMonth),
            canGoNext = state.month.isBefore(maxMonth),
        )
    }

    private fun computeHeavy(d: TennisData, tour: Tour, month: YearMonth): Heavy {
        val tourList = d.tournaments.forTour(tour)

        // Enrich the (tournament-derived) leaderboard with each player's country. players.json (from
        // the Gist) is the authoritative source; the rankings feed is a fallback for any name it
        // misses. Both keyed on the accent-folded abbreviated name.
        val playerCountries = d.players.forTour(tour)
            .entries.associate { (name, country) -> NameMatch.fold(name) to country }
        val rankingCountries = buildMap {
            d.rankings.forTour(tour).values.forEach { snapshot ->
                snapshot.forEach { player -> put(NameMatch.fold(player.name), player.country) }
            }
        }
        // Lenient fallback: index country by surname so an entry added under a different spelling or
        // abbreviation (e.g. "Damir Džumhur" vs "D. Džumhur") still resolves. Only surnames that map
        // to a single country are kept — ambiguous ones (shared surname, different countries) are
        // dropped so we never show a wrong flag.
        val countryBySurname: Map<String, String> = run {
            val groups = HashMap<String, MutableSet<String>>()
            fun add(name: String, country: String?) {
                val c = country?.trim().orEmpty()
                if (c.isNotEmpty()) groups.getOrPut(NameMatch.surname(name)) { mutableSetOf() }.add(c)
            }
            d.players.forTour(tour).forEach { (name, country) -> add(name, country) }
            d.rankings.forTour(tour).values.forEach { snap -> snap.forEach { add(it.name, it.country) } }
            groups.filterValues { it.size == 1 }.mapValues { it.value.first() }
        }
        fun countryOf(name: String): String? {
            val folded = NameMatch.fold(name)
            return playerCountries[folded]?.ifBlank { null }
                ?: rankingCountries[folded]?.ifBlank { null }
                ?: countryBySurname[NameMatch.surname(name)]?.ifBlank { null }
        }

        val leaderboard = PlayerStats.leaderboard(tourList, month)
            .map { it.copy(country = countryOf(it.name)) }

        // Player search always uses the latest full-season standings, regardless of the month on
        // screen — so we aggregate through the most recent month that has any completed result.
        val latestMonth = tourList
            .filter { it.isCompleted }
            .mapNotNull { parseDate(it.end)?.let { d2 -> YearMonth.from(d2) } }
            .maxOrNull() ?: month
        val searchLeaderboard = PlayerStats.leaderboard(tourList, latestMonth)
            .map { it.copy(country = countryOf(it.name)) }

        val (rankings, snapshotLabel) = buildRankings(d, tour, month)
        return Heavy(leaderboard, searchLeaderboard, rankings, snapshotLabel)
    }

    private fun buildRankings(
        d: TennisData,
        tour: Tour,
        month: YearMonth,
    ): Pair<List<RankingRow>, String?> {
        val map = d.rankings.forTour(tour)
        val (active, previous) = RankingKeys.activeAndPrevious(map.keys, month)
        active ?: return emptyList<RankingRow>() to null

        val prevRanks: Map<String, Int> =
            previous?.let { map[it.key] }?.associate { it.name to it.rank } ?: emptyMap()

        val rows = (map[active.key] ?: emptyList())
            .sortedBy { it.rank }
            .take(20)
            .map { player ->
                val prev = prevRanks[player.name]
                val movement = when {
                    previous == null || prev == null -> RankingMovement.New
                    prev == player.rank -> RankingMovement.Same
                    prev > player.rank -> RankingMovement.Up(prev - player.rank)
                    else -> RankingMovement.Down(player.rank - prev)
                }
                RankingRow(player.rank, player.name, player.country, player.points, movement)
            }
        return rows to active.date.format(SNAPSHOT_FORMAT)
    }

    private fun endsInMonth(t: Tournament, month: YearMonth): Boolean {
        val end = parseDate(t.end) ?: return false
        return YearMonth.from(end) == month
    }

    private fun parseDate(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()

    private companion object {
        val SNAPSHOT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    }
}
