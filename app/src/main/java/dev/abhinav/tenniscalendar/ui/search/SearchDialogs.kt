package dev.abhinav.tenniscalendar.ui.search

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhinav.tenniscalendar.domain.NameMatch
import dev.abhinav.tenniscalendar.domain.PlayerStat
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.model.Tournament
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import dev.abhinav.tenniscalendar.ui.common.TournamentBadge
import dev.abhinav.tenniscalendar.ui.common.TournamentDetailSheet
import dev.abhinav.tenniscalendar.ui.common.countryFlag
import dev.abhinav.tenniscalendar.ui.stats.PlayerProfileDialog
import dev.abhinav.tenniscalendar.ui.theme.TennisTokens

@Composable
fun TournamentSearchDialog(
    all: List<Tournament>,
    tour: Tour,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Tournament?>(null) }
    val results = remember(query, all) {
        if (query.isBlank()) all
        else all.filter {
            NameMatch.fold(it.name).contains(NameMatch.fold(query)) ||
                NameMatch.fold(it.location).contains(NameMatch.fold(query))
        }
    }
    // Jump back to the top whenever the query changes so a cleared/edited search shows from the top.
    val listState = rememberLazyListState()
    LaunchedEffect(query) { listState.scrollToItem(0) }

    FullScreenDialog(title = "Find Tournament", subtitle = tour.label, onDismiss = onDismiss) {
        SearchField(query, "Tournament or location", TennisTokens.tourAccent(tour)) { query = it }
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            items(results, key = { it.id }) { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selected = t }
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
                            t.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        TennisTokens.levelLabel(t.level, tour),
                        style = MaterialTheme.typography.labelSmall,
                        color = TennisTokens.tierColor(t.level, androidx.compose.foundation.isSystemInDarkTheme()),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    selected?.let { TournamentDetailSheet(it, tour) { selected = null } }
}

@Composable
fun PlayerSearchDialog(
    leaderboard: List<PlayerStat>,
    tour: Tour,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PlayerStat?>(null) }
    val results = remember(query, leaderboard) {
        val matched = if (query.isBlank()) leaderboard
            else leaderboard.filter { NameMatch.matches(query, it.fullName, it.name) }
        matched.sortedBy { NameMatch.fold(it.fullName) } // alphabetical by first name
    }
    val listState = rememberLazyListState()
    LaunchedEffect(query) { listState.scrollToItem(0) }

    FullScreenDialog(title = "Find Player", subtitle = tour.label, onDismiss = onDismiss) {
        SearchField(query, "Player name", TennisTokens.tourAccent(tour)) { query = it }
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            items(results, key = { "${it.rank}-${it.name}" }) { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selected = p }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        p.country?.let { countryFlag(it) }.orEmpty(),
                        modifier = Modifier.width(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${p.wins} titles · ${p.points} pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    selected?.let { PlayerProfileDialog(it, tour) { selected = null } }
}

@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    accent: Color,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = accent.copy(alpha = 0.6f),
            focusedLeadingIconColor = accent,
            focusedLabelColor = accent,
            cursorColor = accent,
        ),
    )
}
