package dev.abhinav.tenniscalendar.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_RANGE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val DATE_RANGE_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailSheet(tournament: Tournament, tour: Tour, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TournamentBadge(tournament, size = 56)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        tournament.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        tournament.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TierChip(tournament.level, tour)
                val surface = tournament.surfaceEnum
                if (surface != null) {
                    ColorPill(surface.label, TennisTokens.surfaceColor(surface))
                } else {
                    InfoPill(tournament.surface)
                }
                InfoPill(dateRange(tournament))
            }

            if (tournament.isCompleted) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                ResultRow("Champion", tournament.winnerFull ?: tournament.winner!!, highlight = true)
                Spacer(Modifier.height(10.dp))
                ResultRow("Runner-up", tournament.runnerUpFull ?: tournament.runnerUp!!, highlight = false)
                tournament.score?.let {
                    Spacer(Modifier.height(10.dp))
                    ResultRow("Score", it, highlight = false)
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Upcoming — result not yet available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TierChip(level: Int, tour: Tour) {
    val color = TennisTokens.tierColor(level, androidx.compose.foundation.isSystemInDarkTheme())
    Text(
        text = TennisTokens.levelLabel(level, tour),
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun ColorPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun InfoPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun ResultRow(label: String, value: String, highlight: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun dateRange(t: Tournament): String {
    val start = runCatching { LocalDate.parse(t.start) }.getOrNull()
    val end = runCatching { LocalDate.parse(t.end) }.getOrNull()
    if (start == null || end == null) return ""
    return "${start.format(DATE_RANGE)} – ${end.format(DATE_RANGE_YEAR)}"
}
