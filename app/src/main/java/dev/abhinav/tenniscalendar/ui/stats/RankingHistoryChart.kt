package dev.abhinav.tenniscalendar.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.abhinav.tenniscalendar.domain.RankingHistory
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("MMM d")

/**
 * Rank through the season: #1 at the top, the bottom of the list (#[RankingHistory.History.depth])
 * at the bottom. Snapshots are placed by date, so the monthly and bi-weekly keys space correctly.
 * Snapshots where the player is outside the list break the line and are marked on the baseline.
 */
@Composable
fun RankingHistoryChart(history: RankingHistory.History, tour: Tour, modifier: Modifier = Modifier) {
    val accent = TennisTokens.tourAccent(tour)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val measurer = rememberTextMeasurer()

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Text(summary(history), style = MaterialTheme.typography.bodyMedium)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 12.dp)
                .semantics { contentDescription = describe(history) },
        ) {
            val points = history.points
            if (points.isEmpty()) return@Canvas
            val depth = history.depth.coerceAtLeast(2)

            val axisWidth = 28.dp.toPx()
            val bottomLabels = 18.dp.toPx()
            val left = axisWidth
            val right = size.width - 6.dp.toPx()
            val top = 6.dp.toPx()
            val bottom = size.height - bottomLabels

            val first = points.first().date
            val span = ChronoUnit.DAYS.between(first, points.last().date).coerceAtLeast(1)
            fun x(i: Int) = left + (right - left) *
                ChronoUnit.DAYS.between(first, points[i].date).toFloat() / span
            fun y(rank: Int) = top + (bottom - top) * (rank - 1).toFloat() / (depth - 1)

            // Horizontal guides at #1, the midpoint and the bottom of the list.
            listOf(1, (depth + 1) / 2, depth).distinct().forEach { rank ->
                drawLine(
                    grid, Offset(left, y(rank)), Offset(right, y(rank)), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
                drawLabel(measurer, "#$rank", labelStyle, Offset(0f, y(rank)), alignEnd = false)
            }
            // First and last snapshot month along the bottom.
            drawLabel(measurer, first.format(MONTH_FORMAT), labelStyle, Offset(left, size.height - bottomLabels / 2), alignEnd = false)
            if (points.size > 1) {
                drawLabel(measurer, points.last().date.format(MONTH_FORMAT), labelStyle, Offset(right, size.height - bottomLabels / 2), alignEnd = true)
            }

            // Line between consecutive ranked snapshots; a gap where the player dropped out.
            for (i in 1 until points.size) {
                val a = points[i - 1].rank ?: continue
                val b = points[i].rank ?: continue
                drawLine(accent, Offset(x(i - 1), y(a)), Offset(x(i), y(b)), 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            points.forEachIndexed { i, p ->
                if (p.rank != null) {
                    drawCircle(accent, 4.dp.toPx(), Offset(x(i), y(p.rank)))
                } else {
                    drawCircle(labelColor, 2.5.dp.toPx(), Offset(x(i), bottom))
                }
            }
        }
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    anchor: Offset,
    alignEnd: Boolean,
) {
    val layout = measurer.measure(text, style)
    val dx = if (alignEnd) anchor.x - layout.size.width else anchor.x
    drawText(layout, topLeft = Offset(dx, anchor.y - layout.size.height / 2f))
}

/** e.g. "Now #2 · Best #1 (Jan 31) · Top 20 in 7 of 8 snapshots". */
private fun summary(history: RankingHistory.History): String = buildString {
    append(history.current?.let { "Now #$it" } ?: "Now outside top ${history.depth}")
    history.best?.let { append(" · Best #${it.rank} (${it.date.format(DAY_FORMAT)})") }
    append(" · Top ${history.depth} in ${history.snapshotsRanked} of ${history.points.size} snapshots")
}

private fun describe(history: RankingHistory.History): String =
    "Ranking history: " + history.points.joinToString("; ") { p ->
        "${p.date.format(DAY_FORMAT)} " + (p.rank?.let { "#$it" } ?: "outside top ${history.depth}")
    }
