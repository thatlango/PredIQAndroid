package com.getprediq.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
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
import com.getprediq.app.data.TeamIntelligenceSummary
import com.getprediq.app.ui.theme.PrediqBackground
import com.getprediq.app.ui.theme.PrediqBlue
import com.getprediq.app.ui.theme.PrediqMuted
import com.getprediq.app.ui.theme.PrediqSurfaceLow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    var mode by rememberSaveable { mutableStateOf("teams") }
    var sport by rememberSaveable { mutableStateOf("football") }
    var playerQuery by rememberSaveable { mutableStateOf("") }
    var teamQuery by rememberSaveable { mutableStateOf("") }
    var squadTeam by rememberSaveable { mutableStateOf("") }
    var compareA by rememberSaveable { mutableStateOf("") }
    var compareB by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(mode, sport) {
        when (mode) {
            "players" -> if (state.players.isEmpty()) vm.searchPlayers(sport, playerQuery)
            "teams" -> {
                if (state.teams.isEmpty()) vm.loadTeams(sport, state.selectedCompetition)
                if (state.leagueProfiles.isEmpty()) vm.loadLeagueProfiles(sport)
            }
        }
    }

    val visibleTeams = state.teams.filter { teamQuery.isBlank() || it.teamName.contains(teamQuery, ignoreCase = true) }
    val teamA = state.teams.firstOrNull { it.teamName == compareA }
    val teamB = state.teams.firstOrNull { it.teamName == compareB }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Explore", style = MaterialTheme.typography.headlineMedium)
                Text("Team, player and squad evidence behind PredIQ decisions.", color = PrediqMuted)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(PrediqSurfaceLow, RoundedCornerShape(14.dp)).padding(4.dp)) {
                ExploreSegment("Teams", mode == "teams", Modifier.weight(1f)) { mode = "teams" }
                ExploreSegment("Players", mode == "players", Modifier.weight(1f)) { mode = "players" }
                ExploreSegment("Squad", mode == "squad", Modifier.weight(1f)) { mode = "squad" }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("football" to "Football", "cricket" to "Cricket", "baseball" to "Baseball").forEach { (value, label) ->
                    FilterChip(selected = sport == value, onClick = { sport = value }, label = { Text(label) }, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }

        when (mode) {
            "teams" -> {
                item {
                    OutlinedTextField(
                        value = teamQuery,
                        onValueChange = { teamQuery = it },
                        label = { Text("Search teams") },
                        placeholder = { Text("Club or national team") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.leagueProfiles.isNotEmpty()) {
                    item { PrediqSectionTitle("League intelligence") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.leagueProfiles.take(2).forEach { league ->
                                Surface(Modifier.weight(1f), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(league.competition, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                        Text("${league.matchesCount} matches observed", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                                        league.asOf?.let { Text("Updated ${it.take(10)}", color = PrediqMuted, style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                    }
                }
                state.exploreError?.let { error -> item { StateCard("Team intelligence could not refresh", error, error = true, action = "Retry", onAction = { vm.loadTeams(sport, state.selectedCompetition) }) } }
                if (state.exploreBusy && state.teams.isEmpty()) item { StateCard("Loading team intelligence…", "PredIQ is resolving observed form, competition and scoring context.") }
                state.teamDetail?.let { detail -> item { TeamIntelligenceCard(detail) } }

                if (state.teams.size >= 2) {
                    item { PrediqSectionTitle("Compare teams") }
                    item {
                        PrediqCard(Modifier.fillMaxWidth()) {
                            Text("Side-by-side evidence", style = MaterialTheme.typography.titleMedium)
                            Text("Choose any two teams from the backend intelligence set. Comparison uses observed evidence only.", color = PrediqMuted)
                            TeamPicker("Team A", compareA, state.teams) { compareA = it }
                            TeamPicker("Team B", compareB, state.teams) { compareB = it }
                            if (teamA != null && teamB != null) TeamComparison(teamA, teamB)
                        }
                    }
                }

                if (visibleTeams.isNotEmpty()) {
                    item { PrediqSectionTitle("Teams") }
                    items(visibleTeams.take(80), key = { it.teamName }) { team ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { vm.loadTeam(team.teamName, sport) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(team.teamName, fontWeight = FontWeight.SemiBold)
                                    Text("${team.matchesCount} matches observed", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(profileHeadline(team.profile), color = PrediqBlue, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            "players" -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }

            else -> {
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
}

@Composable
private fun ExploreSegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 44.dp), color = if (selected) Color.White else Color.Transparent, shape = RoundedCornerShape(11.dp), shadowElevation = if (selected) 1.dp else 0.dp) { Box(contentAlignment = Alignment.Center) { Text(label, color = if (selected) PrediqBlue else PrediqMuted, fontWeight = FontWeight.SemiBold) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamPicker(label: String, selected: String, teams: List<TeamIntelligenceSummary>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.take(80).forEach { team -> DropdownMenuItem(text = { Text(team.teamName) }, onClick = { onSelected(team.teamName); expanded = false }) }
        }
    }
}

@Composable
private fun TeamComparison(a: TeamIntelligenceSummary, b: TeamIntelligenceSummary) {
    HorizontalDivider()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CompareArrows, null, tint = PrediqBlue)
        Spacer(Modifier.width(8.dp))
        Text("Observed comparison", style = MaterialTheme.typography.titleMedium)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ComparisonColumn(a, Modifier.weight(1f))
        ComparisonColumn(b, Modifier.weight(1f))
    }
}

@Composable
private fun ComparisonColumn(team: TeamIntelligenceSummary, modifier: Modifier = Modifier) {
    Surface(modifier, color = PrediqSurfaceLow, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(team.teamName, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text("${team.matchesCount} matches", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
            metric(team.profile, "win_rate")?.let { Text("Win rate ${formatRate(it)}", color = PrediqBlue, style = MaterialTheme.typography.labelMedium) }
            metric(team.profile, "goals_for_per_match")?.let { Text("Goals ${"%.2f".format(it)} / match", style = MaterialTheme.typography.bodySmall) }
            metric(team.profile, "goals_against_per_match")?.let { Text("Concedes ${"%.2f".format(it)} / match", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun TeamIntelligenceCard(team: TeamIntelligenceSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TEAM INTELLIGENCE", color = PrediqBlue, style = MaterialTheme.typography.labelSmall)
            Text(team.teamName, style = MaterialTheme.typography.headlineSmall)
            Text("${team.matchesCount} matches observed${team.asOf?.let { " · updated ${it.take(10)}" } ?: ""}", color = PrediqMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamMetric("Win rate", metric(team.profile, "win_rate")?.let(::formatRate) ?: "—", Modifier.weight(1f))
                TeamMetric("Goals / match", metric(team.profile, "goals_for_per_match")?.let { "%.2f".format(it) } ?: "—", Modifier.weight(1f))
                TeamMetric("Concedes", metric(team.profile, "goals_against_per_match")?.let { "%.2f".format(it) } ?: "—", Modifier.weight(1f))
            }
            profileCompetitions(team.profile).takeIf { it.isNotBlank() }?.let { Text("Competitions: $it", color = PrediqMuted, style = MaterialTheme.typography.bodySmall) }
            Text("PredIQ surfaces observed team evidence and preserves uncertainty when coverage is limited.", color = PrediqMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TeamMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = PrediqSurfaceLow, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(10.dp)) { Text(label, color = PrediqMuted, style = MaterialTheme.typography.labelSmall); Text(value, fontWeight = FontWeight.Bold) } }
}

private fun metric(profile: JsonObject, key: String): Double? = profile[key]?.jsonPrimitive?.doubleOrNull
private fun formatRate(value: Double): String = if (value <= 1.0) "${(value * 100).toInt()}%" else "${value.toInt()}%"
private fun profileHeadline(profile: JsonObject): String = metric(profile, "win_rate")?.let { "${formatRate(it)} wins" } ?: "View evidence"
private fun profileCompetitions(profile: JsonObject): String = runCatching { profile["competitions"]?.jsonObject?.keys?.take(4)?.joinToString(" · ").orEmpty() }.getOrDefault("")

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
