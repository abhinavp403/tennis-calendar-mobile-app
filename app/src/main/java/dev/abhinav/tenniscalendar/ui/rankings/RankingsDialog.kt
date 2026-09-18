package dev.abhinav.tenniscalendar.ui.rankings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.calendar.RankingMovement
import dev.abhinav.tenniscalendar.ui.calendar.RankingRow
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import dev.abhinav.tenniscalendar.ui.common.InfoButton
import dev.abhinav.tenniscalendar.ui.common.RankingPointsExplainerDialog
import dev.abhinav.tenniscalendar.ui.common.countryFlag

private val UpGreen = Color(0xFF4ADE80)
private val DownRed = Color(0xFFF87171)

@Composable
fun RankingsDialog(
    rankings: List<RankingRow>,
    tour: Tour,
    snapshotLabel: String?,
    onDismiss: () -> Unit,
) {
    var showExplainer by remember { mutableStateOf(false) }
    FullScreenDialog(
        title = "${tour.label} Rankings",
        subtitle = snapshotLabel?.let { "Snapshot · $it" } ?: "Top 20",
        onDismiss = onDismiss,
        titleTrailing = { InfoButton { showExplainer = true } },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(rankings, key = { it.rank.toString() + it.name }) { row ->
                RankingItem(row)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    if (showExplainer) {
        RankingPointsExplainerDialog(tour) { showExplainer = false }
    }
}

@Composable
private fun RankingItem(row: RankingRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.rank.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("${countryFlag(row.country)} ", style = MaterialTheme.typography.bodyLarge)
        Text(
            row.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MovementLabel(row.movement)
        Spacer(Modifier.width(12.dp))
        Text(
            row.points.toString(),
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MovementLabel(movement: RankingMovement) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val up = if (dark) UpGreen else Color(0xFF16A34A)
    val down = if (dark) DownRed else Color(0xFFDC2626)
    val (text, color) = when (movement) {
        is RankingMovement.Up -> "▲${movement.places}" to up
        is RankingMovement.Down -> "▼${movement.places}" to down
        RankingMovement.Same -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
        RankingMovement.New -> "NEW" to MaterialTheme.colorScheme.primary
    }
    Text(
        text,
        modifier = Modifier.width(44.dp),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}
