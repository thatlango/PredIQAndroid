package com.getprediq.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqUiState
import com.getprediq.app.PrediqViewModel
import com.getprediq.app.data.PlayerDetail
import com.getprediq.app.data.SquadDepthResponse
import com.getprediq.app.ui.theme.PrediqBackground
import com.getprediq.app.ui.theme.PrediqBlue
import com.getprediq.app.ui.theme.PrediqMuted
import com.getprediq.app.ui.theme.PrediqSurfaceLow

@Composable
fun ExploreScreen(state: PrediqUiState, vm: PrediqViewModel) {
    if (!vm.fullAccess) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(PrediqBackground),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Explore", style = MaterialTheme.typography.headlineMedium)
                    Text("The deeper evidence behind PredIQ calls.", color = PrediqMuted)
                }
            }
            item {
                StateCard(
                    "Explore needs Full Access",
                    if (state.account == null) "Sign in, then activate a plan to open league, team, player, squad and comparison intelligence." else "Activate a PredIQ plan from Account to open league, team, player, squad and comparison intelligence.",
                )
            }
        }
        return
    }
    var mode by rememberSaveable { mutableStateOf("players") }
    var sport by rememberSaveable { mutableStateOf("football") }
    var playerQuery by rememberSaveable { mutableStateOf("") }
    var squadTeam by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(mode) { if (mode == "players" && state.players.isEmpty()) vm.searchPlayers(sport, playerQuery) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Explore", style = MaterialTheme.typography.headlineMedium)
                Text("Players and squad context behind the match view.", color = PrediqMuted)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(PrediqSurfaceLow, RoundedCornerShape(14.dp)).padding(4.dp)) {
                ExploreSegment("Players", mode == "players", Modifier.weight(1f)) { mode = "players" }
                ExploreSegment("Squad", mode == "squad", Modifier.weight(1f)) { mode = "squad" }
            }
        }
        if (mode == "players") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("football" to "Football", "cricket" to "Cricket", "baseball" to "Baseball").forEach { (value,label) ->
                            FilterChip(selected = sport == value, onClick = { sport = value; vm.searchPlayers(sport, playerQuery) }, label = { Text(label) }, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp))
                        }
                    }
                    OutlinedTextField(value = playerQuery, onValueChange = { playerQuery = it }, label = { Text("Search player") }, placeholder = { Text("Name, nationality or position") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { vm.searchPlayers(sport, playerQuery) }, enabled = !state.exploreBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)) { Icon(Icons.Outlined.PersonSearch, null); Spacer(Modifier.width(8.dp)); Text("Search") }
                }
            }
            state.exploreError?.let { error -> item { StateCard("Explore could not refresh", error, error = true, action = "Retry", onAction = { vm.searchPlayers(sport, playerQuery) }) } }
            if (state.exploreBusy && state.players.isEmpty()) item { StateCard("Loading players…", "PredIQ is resolving the player layer.") }
            state.playerDetail?.let { detail -> item { PlayerIntelligenceCard(detail) } }
            if (state.players.isNotEmpty()) {
                item { Text("Players", style = MaterialTheme.typography.titleLarge) }
                items(state.players.take(50), key = { it.id }) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { vm.loadPlayer(player.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(player.name, fontWeight = FontWeight.SemiBold); Text(listOfNotNull(player.position, player.nationality).joinToString(" · ").ifBlank { player.sportCode.replaceFirstChar(Char::uppercase) }, color = PrediqMuted, style = MaterialTheme.typography.bodySmall) }
                            Column(horizontalAlignment = Alignment.End) { player.headline?.let { Text(it, color = PrediqBlue, fontWeight = FontWeight.SemiBold) }; Text("${player.statRows} signals", color = PrediqMuted, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = squadTeam, onValueChange = { squadTeam = it }, label = { Text("Football team") }, placeholder = { Text("e.g. FC Barcelona") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { vm.loadSquad(squadTeam) }, enabled = !state.exploreBusy && squadTeam.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)) { Icon(Icons.Outlined.Groups, null); Spacer(Modifier.width(8.dp)); Text("Open squad view") }
                }
            }
            state.exploreError?.let { error -> item { StateCard("Squad could not refresh", error, error = true, action = "Retry", onAction = { vm.loadSquad(squadTeam) }) } }
            if (state.exploreBusy && state.squadDepth == null) item { StateCard("Building squad view…", "PredIQ is checking observed player and lineup evidence.") }
            state.squadDepth?.let { squad -> item { SquadDepthCard(squad) } }
        }
    }
}

@Composable
private fun ExploreSegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 44.dp), color = if (selected) Color.White else Color.Transparent, shape = RoundedCornerShape(11.dp), shadowElevation = if (selected) 1.dp else 0.dp) { Box(contentAlignment = Alignment.Center) { Text(label, color = if (selected) PrediqBlue else PrediqMuted, fontWeight = FontWeight.SemiBold) } }
}

@Composable
private fun PlayerIntelligenceCard(player: PlayerDetail) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text("PLAYER", color = PrediqBlue, style = MaterialTheme.typography.labelSmall); Text(player.name, style = MaterialTheme.typography.headlineSmall); Text(listOfNotNull(player.position, player.primaryTeam, player.nationality, player.age?.let { "Age $it" }).joinToString(" · "), color = PrediqMuted) }
                Surface(color = PrediqBlue, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.End) { Text("PERFORMANCE", color = Color.White.copy(alpha=.8f), style = MaterialTheme.typography.labelSmall); Text("${player.performanceIndex}", color = Color.White, style = MaterialTheme.typography.headlineMedium); Text("Coverage ${player.coverageScore}%", color = Color.White.copy(alpha=.8f), style = MaterialTheme.typography.labelSmall) } }
            }
            if (player.signals.isNotEmpty()) { player.signals.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) } }
            if (player.teams.isNotEmpty()) { HorizontalDivider(); Text("Team evidence", style = MaterialTheme.typography.labelLarge); Text(player.teams.take(4).joinToString(" · ") { it.name }, color = PrediqMuted) }
            HorizontalDivider(); Text(if (player.sources.isEmpty()) "Limited source coverage" else "Sources: ${player.sources.joinToString(" · ")}", color = PrediqMuted, style = MaterialTheme.typography.labelSmall); Text(player.disclaimer, color = PrediqMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SquadDepthCard(squad: SquadDepthResponse) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text("SQUAD DEPTH", color = PrediqBlue, style = MaterialTheme.typography.labelSmall); Text(squad.team, style = MaterialTheme.typography.headlineSmall); Text(squad.coverage.message, color = PrediqMuted) }
                Surface(color = PrediqBlue, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.End) { Text("OBSERVED", color = Color.White.copy(alpha=.8f), style = MaterialTheme.typography.labelSmall); Text(squad.depthScore?.toString() ?: "—", color = Color.White, style = MaterialTheme.typography.headlineMedium); Text("${squad.coverage.level} · ${squad.coverage.score}%", color = Color.White.copy(alpha=.8f), style = MaterialTheme.typography.labelSmall) } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { squad.positionGroups.filter { it.name != "Unknown" }.take(4).forEach { group -> Surface(Modifier.weight(1f), color = PrediqSurfaceLow, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(10.dp)) { Text(group.name, color = PrediqMuted, style = MaterialTheme.typography.labelSmall); Text("${group.count}", fontWeight = FontWeight.Bold) } } } }
            HorizontalDivider(); Text("Observed players", style = MaterialTheme.typography.titleMedium)
            squad.players.take(24).forEach { player -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(player.name, fontWeight = FontWeight.Medium); Text(listOfNotNull(player.position, player.nationality).joinToString(" · ").ifBlank { player.positionGroup }, color = PrediqMuted, style = MaterialTheme.typography.bodySmall) }; Text(player.goals?.let { "${it.toInt()} goals" }.orEmpty(), color = PrediqBlue, style = MaterialTheme.typography.labelMedium) } }
            HorizontalDivider(); Text("${squad.coverage.observedPlayers} observed players · ${squad.coverage.lineupEvents} lineup observations", color = PrediqMuted, style = MaterialTheme.typography.labelSmall); Text(squad.methodology, color = PrediqMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}
