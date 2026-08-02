package dev.abhinav.tenniscalendar.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abhinav.tenniscalendar.domain.PlayerStat
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import dev.abhinav.tenniscalendar.ui.common.countryFlag
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens

@Composable
fun PlayerProfileDialog(player: PlayerStat, tour: Tour, onDismiss: () -> Unit) {
    val flag = player.country?.let { countryFlag(it) }.orEmpty()
    val title = if (flag.isBlank()) player.fullName else "$flag  ${player.fullName}"
    FullScreenDialog(title = title, onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Titles", player.wins.toString(), Modifier.weight(1f))
                StatCard("Finals", player.total.toString(), Modifier.weight(1f))
                StatCard("Finals Win %", "${player.winPercent}%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Points", player.points.toString(), Modifier.weight(1f))
                StatCard("Runner-up", player.runnerUp.toString(), Modifier.weight(1f))
                StatCard(
                    "Best surface",
                    player.bestSurface?.label ?: "—",
                    Modifier.weight(1f),
                    accent = player.bestSurface != null,
                )
            }

            if (player.surfaceFinals.isNotEmpty()) {
                SectionHeader("By surface")
                player.surfaceFinals.entries
                    .sortedByDescending { it.value }
                    .forEach { (surface, finals) ->
                        val wins = player.surfaceWins[surface] ?: 0
                        BreakdownRow(surface.label, "$wins title" + (if (wins == 1) "" else "s") + " · $finals final" + (if (finals == 1) "" else "s"))
                    }
            }

            if (player.titles.isNotEmpty()) {
                SectionHeader("Titles (${player.titles.size})")
                player.titles.forEach { ListLine("🏆  $it") }
            }
            if (player.runnerUps.isNotEmpty()) {
                SectionHeader("Runner-up (${player.runnerUps.size})")
                player.runnerUps.forEach { ListLine("🥈  $it") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ListLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
}
