package dev.abhinav.tenniscalendar.ui.calendar

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhinav.tenniscalendar.R
import dev.abhinav.tenniscalendar.data.DataError
import dev.abhinav.tenniscalendar.model.Surface
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.ui.common.TournamentDetailSheet
import dev.abhinav.tenniscalendar.ui.rankings.RankingsDialog
import dev.abhinav.tenniscalendar.ui.search.PlayerSearchDialog
import dev.abhinav.tenniscalendar.ui.search.TournamentSearchDialog
import dev.abhinav.tenniscalendar.ui.settings.SettingsScreen
import dev.abhinav.tenniscalendar.ui.stats.PlayerStatsDialog
import dev.abhinav.tenniscalendar.ui.summary.ChampionsWallDialog
import dev.abhinav.tenniscalendar.ui.summary.MonthSummaryDialog
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class Overlay { STATS, RANKINGS, MONTH, CHAMPIONS, TOURNAMENT_SEARCH, PLAYER_SEARCH }

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Overlay + picker survive config changes; the transient detail sheet is cheap to reopen.
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }
    var sheetTournament by remember { mutableStateOf<Tournament?>(null) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_tennis_ball),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tennis Calendar",
                            fontWeight = FontWeight.Bold,
                            color = TennisTokens.tourAccent(state.tour),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { overlay = Overlay.CHAMPIONS }) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Champions wall")
                    }
                    IconButton(onClick = { overlay = Overlay.TOURNAMENT_SEARCH }) {
                        Icon(Icons.Default.Search, contentDescription = "Find tournament")
                    }
                    IconButton(onClick = { overlay = Overlay.PLAYER_SEARCH }) {
                        Icon(Icons.Default.PersonSearch, contentDescription = "Find player")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null -> ErrorState(state.error!!, viewModel::load, Modifier.padding(padding))
            else -> CalendarContent(
                state = state,
                onSelectTour = viewModel::selectTour,
                onPrevMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToday = viewModel::goToToday,
                onSurface = viewModel::setSurfaceFilter,
                onTournamentClick = { sheetTournament = it },
                onOpenOverlay = { overlay = it },
                onMonthPicker = { showMonthPicker = true },
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )
        }
    }

    sheetTournament?.let { t ->
        TournamentDetailSheet(t, state.tour) { sheetTournament = null }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = state.month,
            min = state.minMonth,
            max = state.maxMonth,
            accent = TennisTokens.tourAccent(state.tour),
            onSelect = { viewModel.setMonth(it); showMonthPicker = false },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }

    when (overlay) {
        Overlay.STATS -> PlayerStatsDialog(state.leaderboard, state.tour) { overlay = null }
        Overlay.RANKINGS -> RankingsDialog(state.rankings, state.tour, state.rankingSnapshotLabel) { overlay = null }
        Overlay.MONTH -> {
            val monthCompleted = remember(state.tour, state.month) {
                viewModel.seasonTournaments().filter {
                    it.isCompleted && runCatching { YearMonth.from(LocalDate.parse(it.end)) == state.month }.getOrDefault(false)
                }
            }
            MonthSummaryDialog(
                monthLabel = state.month.format(MONTH_YEAR),
                completed = monthCompleted,
                tour = state.tour,
                onDismiss = { overlay = null },
            )
        }
        Overlay.CHAMPIONS -> {
            val champions = remember(state.tour) { viewModel.seasonTournaments().filter { it.isCompleted } }
            ChampionsWallDialog(
                completed = champions,
                tour = state.tour,
                onDismiss = { overlay = null },
            )
        }
        Overlay.TOURNAMENT_SEARCH -> {
            val all = remember(state.tour) { viewModel.seasonTournaments() }
            TournamentSearchDialog(
                all = all,
                tour = state.tour,
                onDismiss = { overlay = null },
            )
        }
        Overlay.PLAYER_SEARCH -> PlayerSearchDialog(state.searchLeaderboard, state.tour) { overlay = null }
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(
    state: CalendarUiState,
    onSelectTour: (Tour) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSurface: (Surface?) -> Unit,
    onTournamentClick: (Tournament) -> Unit,
    onOpenOverlay: (Overlay) -> Unit,
    onMonthPicker: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = TennisTokens.tourAccent(state.tour)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val controls: @Composable () -> Unit = {
        if (state.isOffline) OfflineBanner(onRefresh)
        Spacer(Modifier.height(8.dp))
        TourToggle(state.tour, onSelectTour)
        Spacer(Modifier.height(10.dp))
        SurfaceChips(state.surfaceFilter, onSurface)
        Spacer(Modifier.height(10.dp))
        MonthNav(
            month = state.month,
            accent = accent,
            canGoPrev = state.canGoPrev,
            canGoNext = state.canGoNext,
            onPrev = onPrevMonth,
            onNext = onNextMonth,
            onToday = onToday,
            onMonthPicker = onMonthPicker,
        )
        Spacer(Modifier.height(10.dp))
        QuickActions(onOpenOverlay, accent)
    }
    val grid: @Composable (Modifier) -> Unit = { m ->
        CalendarGrid(
            month = state.month,
            tournaments = state.monthTournaments,
            today = LocalDate.now(),
            accentColor = accent,
            onTournamentClick = onTournamentClick,
            modifier = m,
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (landscape) {
            // Wide + short: controls in a left panel, calendar fills the rest.
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                Column(
                    Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    controls()
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.width(12.dp))
                PullableGrid(Modifier.weight(1f).fillMaxHeight()) { h ->
                    grid(Modifier.fillMaxWidth().height(h))
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            ) {
                controls()
                Spacer(Modifier.height(12.dp))
                PullableGrid(Modifier.weight(1f).fillMaxWidth()) { h ->
                    grid(Modifier.fillMaxWidth().height(h))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Hosts the (non-scrolling) calendar grid inside a viewport-height vertical scroll so pull-to-refresh
 * receives the drag gesture, while the grid still fills the available space exactly (zero scroll range).
 */
@Composable
private fun PullableGrid(modifier: Modifier, content: @Composable (androidx.compose.ui.unit.Dp) -> Unit) {
    BoxWithConstraints(modifier) {
        val viewportHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            content(viewportHeight)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TourToggle(tour: Tour, onSelect: (Tour) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Tour.entries.forEachIndexed { index, t ->
            val accent = TennisTokens.tourAccent(t)
            SegmentedButton(
                selected = tour == t,
                onClick = { onSelect(t) },
                shape = SegmentedButtonDefaults.itemShape(index, Tour.entries.size),
                icon = {}, // no selected checkmark
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = accent,
                    activeContentColor = Color.White,
                    activeBorderColor = accent,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(t.label, fontWeight = if (tour == t) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurfaceChips(selected: Surface?, onSelect: (Surface?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        Surface.entries.forEach { s ->
            val color = TennisTokens.surfaceColor(s)
            FilterChip(
                selected = selected == s,
                onClick = { onSelect(if (selected == s) null else s) },
                label = { Text(s.label) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.22f),
                    selectedLabelColor = color,
                    selectedLeadingIconColor = color,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == s,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = color,
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthNav(
    month: YearMonth,
    accent: Color,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onMonthPicker: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            month.format(MONTH_YEAR),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                // Tap or long-press to jump to any month in the season.
                .combinedClickable(onClick = onMonthPicker, onLongClick = onMonthPicker)
                .padding(vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(onClick = onToday) { Text("Today", color = accent, fontWeight = FontWeight.SemiBold) }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthPickerDialog(
    current: YearMonth,
    min: YearMonth,
    max: YearMonth,
    accent: Color,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val years = CalendarBounds.years(min, max)
    var yearIndex by remember { mutableStateOf(years.indexOf(current.year).coerceAtLeast(0)) }
    val selectedYear = years[yearIndex]
    val months = CalendarBounds.monthsInYear(selectedYear, min, max)
    var monthIndex by remember { mutableStateOf(months.indexOf(current).coerceAtLeast(0)) }
    val safeMonthIndex = monthIndex.coerceIn(0, months.lastIndex)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                // Month reel — recreated when the year changes so it re-centers.
                key(selectedYear) {
                    WheelPicker(
                        items = months.map { it.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
                        initialIndex = safeMonthIndex,
                        accent = accent,
                        onSettled = { monthIndex = it },
                        modifier = Modifier.weight(1.4f),
                    )
                }
                // Year reel.
                WheelPicker(
                    items = years.map { it.toString() },
                    initialIndex = yearIndex,
                    accent = accent,
                    onSettled = { yearIndex = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onSelect(months[safeMonthIndex]) }) {
                    Text("Done", color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** iOS-style spinning reel: the selected value sits in the center band, neighbors peek and fade. */
@Composable
private fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    accent: Color,
    onSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val itemHeight = 44.dp
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(state)

    LaunchedEffect(Unit) { state.scrollToItem(initialIndex.coerceIn(0, items.lastIndex)) }

    val centerIndex by remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                initialIndex.coerceIn(0, items.lastIndex)
            } else {
                val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo.minByOrNull { kotlin.math.abs((it.offset + it.size / 2f) - mid) }!!.index
            }
        }
    }
    // Report only after a real scroll gesture settles (ignore the initial false state).
    LaunchedEffect(state) {
        var wasScrolling = false
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (wasScrolling && !scrolling) onSettled(centerIndex)
            wasScrolling = scrolling
        }
    }

    Box(
        modifier.height(itemHeight * 3),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.12f)),
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { i, label ->
                val selected = i == centerIndex
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        modifier = Modifier.alpha(if (selected) 1f else 0.4f),
                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActions(onOpen: (Overlay) -> Unit, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionChip("Player Stats", Icons.Default.Leaderboard, accent) { onOpen(Overlay.STATS) }
        ActionChip("Rankings", Icons.AutoMirrored.Filled.TrendingUp, accent) { onOpen(Overlay.RANKINGS) }
        ActionChip("Month Results", Icons.Default.CalendarMonth, accent) { onOpen(Overlay.MONTH) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onRetry)
            .padding(vertical = 8.dp),
    ) {
        Text(
            "⚠ Offline — showing cached data · tap to retry",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFF59E0B),
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(error: DataError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (error) {
        DataError.NO_CONNECTION_NO_CACHE -> "No connection and no cached data yet.\nConnect and retry."
        DataError.SERVER -> "The server returned an error."
        DataError.SERIALIZATION -> "Could not read the data."
        DataError.UNKNOWN -> "Something went wrong."
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
