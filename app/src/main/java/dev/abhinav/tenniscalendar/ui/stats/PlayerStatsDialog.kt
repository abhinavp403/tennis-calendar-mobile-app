package dev.abhinav.tenniscalendar.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import dev.abhinav.tenniscalendar.domain.PlayerStat
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import dev.abhinav.tenniscalendar.ui.common.countryFlag

@Composable
fun PlayerStatsDialog(
    leaderboard: List<PlayerStat>,
    tour: Tour,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<PlayerStat?>(null) }

    FullScreenDialog(
        title = "Player Stats",
        subtitle = "${tour.label} · Year to date",
        onDismiss = onDismiss,
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header stays pinned; only the rows below it scroll.
            LeaderboardHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(leaderboard, key = { "${it.rank}-${it.name}" }) { player ->
                    LeaderboardRow(player) { selected = player }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    selected?.let { player ->
        PlayerProfileDialog(player = player, tour = tour, onDismiss = { selected = null })
    }
}

@Composable
private fun LeaderboardHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("Rank", RANK_W, TextAlign.Start)
        HeaderCell("Player", NAME_W, TextAlign.Start)
        HeaderCell("Wins", WINS_W, TextAlign.End)
        HeaderCell("Runner-up", RU_W, TextAlign.End)
        HeaderCell("Total", TOTAL_W, TextAlign.End)
        HeaderCell("Points", PTS_W, TextAlign.End)
    }
}

// Shared column weights so the pinned header aligns with the scrolling rows.
private const val RANK_W = 0.11f
private const val NAME_W = 0.28f
private const val WINS_W = 0.13f
private const val RU_W = 0.20f
private const val TOTAL_W = 0.13f
private const val PTS_W = 0.15f

@Composable
private fun LeaderboardRow(player: PlayerStat, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyCell(player.rank.toString(), RANK_W, TextAlign.Start, muted = true)
        val flag = player.country?.let { countryFlag(it) }.orEmpty()
        Text(
            text = if (flag.isBlank()) player.name else "$flag  ${player.name}",
            modifier = Modifier.weight(NAME_W),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BodyCell(player.wins.toString(), WINS_W, TextAlign.End)
        BodyCell(player.runnerUp.toString(), RU_W, TextAlign.End)
        BodyCell(player.total.toString(), TOTAL_W, TextAlign.End)
        BodyCell(
            player.points.toString(),
            PTS_W,
            TextAlign.End,
            bold = true,
            accent = true,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(
    text: String,
    weight: Float,
    align: TextAlign,
    bold: Boolean = false,
    accent: Boolean = false,
    muted: Boolean = false,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = align,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = when {
            accent -> MaterialTheme.colorScheme.primary
            muted -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
    )
}
