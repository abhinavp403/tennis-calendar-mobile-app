package dev.abhinav.tenniscalendar.ui.race

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhinav.tenniscalendar.domain.RaceStandings
import dev.abhinav.tenniscalendar.model.RacePlayer
import dev.abhinav.tenniscalendar.model.RaceResult
import dev.abhinav.tenniscalendar.model.RaceTour
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import dev.abhinav.tenniscalendar.ui.common.InfoButton
import dev.abhinav.tenniscalendar.ui.common.RankingPointsExplainerDialog
import dev.abhinav.tenniscalendar.ui.common.countryFlag
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter

private val Gold = Color(0xFFFBBF24)
private val GapGreen = Color(0xFF34D399)
private val GapRed = Color(0xFFF87171)
private val CutoffColor = Color(0xFFEF4444)
private val RANK_W = 30.dp
private val POINTS_W = 66.dp
private val GAP_W = 58.dp

private fun fmt(n: Int): String = "%,d".format(n)

private fun formatAsOf(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d")) }.getOrDefault(iso)

@Composable
fun RaceDialog(
    race: RaceTour?,
    finalsLine: String?,
    tour: Tour,
    onDismiss: () -> Unit,
) {
    var showExplainer by remember { mutableStateOf(false) }
    val accent = TennisTokens.tourAccent(tour)
    val players = race?.players.orEmpty()

    val topN = if (players.isEmpty()) "" else "Top ${players.size} "
    val asOf = race?.asOf?.let { formatAsOf(it) }
    val subtitle = "$topN${tour.label} Singles" + (asOf?.let { " · as of $it" } ?: "")

    FullScreenDialog(
        title = "🏁 Race to the Finals",
        subtitle = subtitle,
        onDismiss = onDismiss,
        titleTrailing = { InfoButton { showExplainer = true } },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 640.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                if (finalsLine != null) {
                    Text(
                        finalsLine,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (players.isEmpty()) {
                    Text(
                        "Race standings aren't available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                } else {
                    ColumnHeaders()
                    val leader = RaceStandings.leaderPoints(players)
                    val cutoffAfter = RaceStandings.cutoffAfterIndex(players)
                    var expandedRank by remember { mutableStateOf<Int?>(null) }
                    LazyColumn(Modifier.weight(1f)) {
                        itemsIndexed(players, key = { _, p -> p.rank }) { index, player ->
                            RaceRow(
                                player = player,
                                players = players,
                                leaderPoints = leader,
                                tour = tour,
                                accent = accent,
                                expanded = expandedRank == player.rank,
                                onToggle = {
                                    expandedRank = if (expandedRank == player.rank) null else player.rank
                                },
                            )
                            if (index == cutoffAfter) CutoffLine()
                        }
                    }
                    Text(
                        "Top 8 qualify. GAP shows each player's cushion over #9, or the points still " +
                            "needed to reach #8. Exception: a ${Year.now().value} Grand Slam champion " +
                            "ranked 8–20 takes the 8th spot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }

    if (showExplainer) {
        RankingPointsExplainerDialog(tour) { showExplainer = false }
    }
}

@Composable
private fun ColumnHeaders() {
    val grey = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.labelSmall
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#", Modifier.width(RANK_W), style = style, color = grey)
        Text("PLAYER", Modifier.weight(1f), style = style, color = grey)
        Text("POINTS", Modifier.width(POINTS_W), style = style, color = grey, textAlign = TextAlign.End)
        Text("GAP", Modifier.width(GAP_W), style = style, color = grey, textAlign = TextAlign.End)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaceRow(
    player: RacePlayer,
    players: List<RacePlayer>,
    leaderPoints: Int,
    tour: Tour,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val topEight = player.rank <= RaceStandings.CUTOFF
    val hasResults = !player.results.isNullOrEmpty()
    val dimmed = !topEight && !expanded
    val grey = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (hasResults) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(vertical = 10.dp)
                .alpha(if (dimmed) 0.6f else 1f),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                player.rank.toString(),
                modifier = Modifier.width(RANK_W),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (topEight) accent else grey,
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val flag = countryFlag(player.country)
                    if (flag.isNotBlank()) Text("$flag ", style = MaterialTheme.typography.bodyLarge)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(player.full.ifBlank { player.name }) } },
                        state = rememberTooltipState(),
                    ) {
                        Text(
                            player.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (topEight) FontWeight.Bold else FontWeight.Medium,
                            color = if (topEight) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (player.qualified) {
                        Spacer(Modifier.width(8.dp))
                        QualifiedPill()
                    }
                }
                MetaLine(player, hasResults, expanded, accent, grey)
                Spacer(Modifier.height(6.dp))
                PointsBar(
                    fraction = if (leaderPoints > 0) player.points.toFloat() / leaderPoints else 0f,
                    color = when {
                        player.qualified -> Gold
                        topEight -> accent
                        else -> grey.copy(alpha = 0.4f)
                    },
                )
            }
            Text(
                fmt(player.points),
                modifier = Modifier.width(POINTS_W),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            GapLabel(RaceStandings.gap(players, player), topEight)
        }
        if (expanded && player.results != null) {
            BreakdownPanel(player.results, tour)
        }
    }
}

@Composable
private fun MetaLine(
    player: RacePlayer,
    hasResults: Boolean,
    expanded: Boolean,
    accent: Color,
    grey: Color,
) {
    val tournaments = player.tournaments ?: return
    val titles = player.titles ?: 0
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = grey)) {
                append("$tournaments event${if (tournaments == 1) "" else "s"}")
            }
            if (titles > 0) {
                withStyle(SpanStyle(color = grey)) { append(" · ") }
                withStyle(SpanStyle(color = Gold)) {
                    append("$titles title${if (titles == 1) "" else "s"}")
                }
            }
            if (hasResults) {
                withStyle(SpanStyle(color = if (expanded) accent else grey)) {
                    append(" · breakdown ${if (expanded) "▴" else "▾"}")
                }
            }
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun QualifiedPill() {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Gold.copy(alpha = 0.15f))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text("QUALIFIED", style = MaterialTheme.typography.labelSmall, color = Gold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PointsBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}

@Composable
private fun GapLabel(gap: Int?, topEight: Boolean) {
    val (text, color) = when {
        gap == null -> "" to Color.Unspecified
        topEight -> "+${fmt(gap)}" to GapGreen
        else -> "−${fmt(-gap)}" to GapRed
    }
    Text(
        text,
        modifier = Modifier.width(GAP_W),
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End,
        color = color,
    )
}

@Composable
private fun CutoffLine() {
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = CutoffColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
        Text(
            "QUALIFICATION CUTOFF",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CutoffColor,
            modifier = Modifier.background(background).padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun BreakdownPanel(results: List<RaceResult>, tour: Tour) {
    val groups = RaceStandings.breakdown(results, wta = tour == Tour.WTA)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = RANK_W, bottom = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        // Larger gap between groups; rows within a group stay tight (see the inner Column).
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        groups.forEach { group ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        group.label,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        fmt(group.subtotal),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                group.results.forEach { ResultRow(it) }
            }
        }
    }
}

@Composable
private fun ResultRow(result: RaceResult) {
    val isWin = result.round == "W"
    val isSkipped = result.round == "A"
    val roundColor = when (result.round) {
        "W" -> Gold
        "F" -> Color(0xFFE5E7EB)
        "SF" -> Color(0xFFC4C4D4)
        "QF" -> Color(0xFF9CA3AF)
        else -> Color(0xFF6B7280)
    }
    Row(
        Modifier.fillMaxWidth().alpha(if (isSkipped) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            result.event,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isWin) "🏆 W" else if (isSkipped) "Skipped" else result.round,
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isWin) FontWeight.Bold else FontWeight.Normal,
            color = roundColor,
            textAlign = TextAlign.End,
        )
        Text(
            fmt(result.points),
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            color = if (isWin) Gold else MaterialTheme.colorScheme.onSurface,
        )
    }
}
