package dev.abhinav.tenniscalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Month grid. Tournaments render on their END-date cell, color-coded by tier (as in the web app). */
@Composable
fun CalendarGrid(
    month: YearMonth,
    tournaments: List<Tournament>,
    today: LocalDate,
    accentColor: Color,
    onTournamentClick: (Tournament) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byEndDay: Map<Int, List<Tournament>> = tournaments
        .mapNotNull { t -> runCatching { LocalDate.parse(t.end) }.getOrNull()?.let { it to t } }
        .filter { YearMonth.from(it.first) == month }
        .groupBy({ it.first.dayOfMonth }, { it.second })

    val firstOfMonth = month.atDay(1)
    // Monday-first grid.
    val leadingBlanks = (firstOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                val dow = DayOfWeek.of(((i + 0) % 7) + 1) // Monday..Sunday
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // Weeks share the remaining vertical space equally so the grid fills the screen.
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (row in 0 until rows) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingBlanks + 1
                        if (dayNumber in 1..daysInMonth) {
                            DayCell(
                                day = dayNumber,
                                isToday = today == month.atDay(dayNumber),
                                accentColor = accentColor,
                                tournaments = byEndDay[dayNumber].orEmpty(),
                                onTournamentClick = onTournamentClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    accentColor: Color,
    tournaments: List<Tournament>,
    onTournamentClick: (Tournament) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isToday) Modifier.border(
                    1.5.dp, accentColor, RoundedCornerShape(8.dp),
                ) else Modifier
            )
            .padding(4.dp),
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))

        // Chips share the cell's remaining height equally, so a single tournament fills the space
        // and multiple ones stay the same size as each other.
        val maxChips = 4
        val shown = if (tournaments.size <= maxChips) tournaments else tournaments.take(maxChips - 1)
        val extra = tournaments.size - shown.size
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            shown.forEach { t ->
                MiniTournamentChip(
                    t,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { onTournamentClick(t) },
                )
            }
            if (extra > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+$extra",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTournamentChip(tournament: Tournament, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = TennisTokens.tierColor(tournament.level, isSystemInDarkTheme())
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.25f))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = TennisTokens.abbreviation(tournament.name),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
