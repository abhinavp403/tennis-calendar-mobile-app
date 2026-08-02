package dev.abhinav.tenniscalendar.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import dev.abhinav.tenniscalendar.ui.common.TournamentBadge
import dev.abhinav.tenniscalendar.ui.common.TournamentDetailSheet
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens

@Composable
fun MonthSummaryDialog(
    monthLabel: String,
    completed: List<Tournament>,
    tour: Tour,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<Tournament?>(null) }
    FullScreenDialog(title = "Month Results", subtitle = monthLabel, onDismiss = onDismiss) {
        if (completed.isEmpty()) {
            EmptyState("No completed tournaments this month.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(completed, key = { it.id }) { t ->
                    ResultRow(t, tour) { selected = t }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
    // Detail opens on top of this dialog, so the list stays put behind it.
    selected?.let { TournamentDetailSheet(it, tour) { selected = null } }
}

@Composable
fun ChampionsWallDialog(
    completed: List<Tournament>,
    tour: Tour,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<Tournament?>(null) }
    val sorted = completed.sortedWith(
        compareByDescending<Tournament> { it.level }.thenBy { it.end },
    )
    FullScreenDialog(
        title = "Champions Wall",
        subtitle = "${tour.label} · ${completed.size} titles",
        onDismiss = onDismiss,
    ) {
        if (sorted.isEmpty()) {
            EmptyState("No champions yet.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(sorted, key = { it.id }) { t ->
                    ResultRow(t, tour) { selected = t }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
    selected?.let { TournamentDetailSheet(it, tour) { selected = null } }
}

@Composable
private fun ResultRow(t: Tournament, tour: Tour, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TournamentBadge(t, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "🏆 ${t.winnerFull ?: t.winner ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val runnerUp = t.runnerUpFull ?: t.runnerUp
            if (!runnerUp.isNullOrBlank()) {
                Text(
                    "🥈 $runnerUp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            TennisTokens.levelLabel(t.level, tour),
            style = MaterialTheme.typography.labelSmall,
            color = TennisTokens.tierColor(t.level, androidx.compose.foundation.isSystemInDarkTheme()),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
