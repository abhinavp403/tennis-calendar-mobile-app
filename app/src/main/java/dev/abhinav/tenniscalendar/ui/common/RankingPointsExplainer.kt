package dev.abhinav.tenniscalendar.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import java.time.Year

// Tier colors for the level names in the points table (per the feature spec).
private val GrandSlamViolet = Color(0xFFA78BFA)
private val ThousandAmber = Color(0xFFF59E0B)
private val FiveHundredBlue = Color(0xFF3B82F6)
private val TwoFiftyGrey = Color(0xFF8B8FA8)

/** A small circular "i" button, e.g. next to a dialog title. */
@Composable
fun InfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = "How ranking points work",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class PointsRow(val level: String, val color: Color, val points: List<Int>)

private fun atpRows() = listOf(
    PointsRow("Grand Slam", GrandSlamViolet, listOf(2000, 1300, 800, 400, 200)),
    PointsRow("Masters 1000", ThousandAmber, listOf(1000, 650, 400, 200, 100)),
    PointsRow("500", FiveHundredBlue, listOf(500, 330, 200, 100, 50)),
    PointsRow("250", TwoFiftyGrey, listOf(250, 165, 100, 50, 25)),
)

private fun wtaRows() = listOf(
    PointsRow("Grand Slam", GrandSlamViolet, listOf(2000, 1300, 780, 430, 240)),
    PointsRow("WTA 1000", ThousandAmber, listOf(1000, 650, 390, 215, 120)),
    PointsRow("500", FiveHundredBlue, listOf(500, 325, 195, 108, 60)),
    PointsRow("250", TwoFiftyGrey, listOf(250, 163, 98, 54, 30)),
)

@Composable
fun RankingPointsExplainerDialog(tour: Tour, onDismiss: () -> Unit) {
    val accent = TennisTokens.tourAccent(tour)
    val body = MaterialTheme.typography.bodyMedium
    val onSurface = MaterialTheme.colorScheme.onSurface
    FullScreenDialog(
        title = "How ranking points work",
        subtitle = "${tour.label} singles",
        subtitleColor = accent,
        onDismiss = onDismiss,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Section("THE BASICS")
            Text(
                "Players earn points for how far they get in each tournament. Bigger tournaments " +
                    "are worth more, and going further is worth more. You get the points for the " +
                    "round you reach — losing in the final still earns the runner-up points.",
                style = body, color = onSurface,
            )

            Section("POINTS PER ROUND")
            PointsTable(if (tour == Tour.ATP) atpRows() else wtaRows())
            Spacer(Modifier.height(8.dp))
            Text(
                "W = champion · F = runner-up · SF / QF = lost in the semis / quarters · " +
                    "R16 = lost in the round of 16. Earlier rounds earn less. The season-ending " +
                    "${tour.label} Finals is worth up to 1,500 for an unbeaten champion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Section("ONLY THE BEST RESULTS COUNT")
            Text(
                "A player's total is their best 18 results. The four Grand Slams and the biggest " +
                    "1000-level events are mandatory — they count even if the player skips them " +
                    "(as zero), unless injured. The rest are filled by their best other results.",
                style = body, color = onSurface,
            )

            Section("RANKINGS vs. THE RACE")
            Text(
                buildAnnotatedString {
                    bold("Rankings"); append(" cover the last 52 weeks. Points \"drop off\" a year ")
                    append("after they're earned, so a player must defend last year's results to ")
                    append("keep their ranking. "); bold("The Race"); append(" uses the same points but ")
                    append("only counts the current year — everyone starts at zero in January. The ")
                    append("top 8 at the end of the season qualify for the Finals.")
                },
                style = body, color = onSurface,
            )

            Section("IN THIS APP")
            Text(
                buildAnnotatedString {
                    append("The Rankings and Race views show official points. ")
                    bold("Player Stats (YTD)")
                    append(" uses a simpler score: it only counts titles and runner-ups in tour ")
                    append("events shown on this calendar, so it won't match official totals.")
                },
                style = body, color = onSurface,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.bold(text: String) =
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PointsTable(rows: List<PointsRow>) {
    val headers = listOf("LEVEL", "W", "F", "SF", "QF", "R16")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            headers.forEachIndexed { i, h ->
                Text(
                    h,
                    modifier = Modifier.weight(if (i == 0) 1.7f else 1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                )
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(
                    row.level,
                    modifier = Modifier.weight(1.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = row.color,
                )
                row.points.forEachIndexed { i, pts ->
                    Text(
                        pts.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
