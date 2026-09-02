package com.getprediq.app.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.data.v2.V2Change
import com.getprediq.app.data.v2.V2ResearchTeam
import kotlinx.serialization.json.JsonObject

@Composable
fun NotificationInboxContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val changes = buildList {
        addAll(state.today?.changes.orEmpty())
        state.live?.changes.orEmpty().forEach { live -> if (none { it.eventId == live.eventId && it.occurredAt == live.occurredAt }) add(live) }
    }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Notifications", subtitle = "Prediction, lineup and live changes", showBack = true, onBack = onBack) }
        if (changes.isEmpty()) item { EmptyState("You're caught up", "Meaningful PredIQ changes will appear here.", Icons.Outlined.NotificationsNone) }
        else items(changes, key = { "${it.eventId}-${it.occurredAt}-${it.type}" }) { change -> NotificationChangeCard(change) }
    }
}

@Composable
private fun NotificationChangeCard(change: V2Change) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val tone = when (change.type) { "strengthened" -> Green; "weakened" -> Red; else -> Purple }
            Box(Modifier.size(42.dp).background(tone.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(when (change.type) { "strengthened" -> Icons.Outlined.TrendingUp; "weakened" -> Icons.Outlined.TrendingDown; else -> Icons.Outlined.Notifications }, null, tint = tone)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(change.title, color = Ink, fontWeight = FontWeight.Bold)
                Text(change.summary ?: humanize(change.type), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (change.oldChance?.percent != null || change.newChance?.percent != null) Text("${change.oldChance?.percent ?: "–"}% → ${change.newChance?.percent ?: "–"}%", color = tone, fontWeight = FontWeight.SemiBold)
            }
            Text(compactTime(change.occurredAt).takeLast(5), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TeamCompareContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val teams = state.research?.teamsInTodayPicks.orEmpty()
    var left by remember(teams) { mutableStateOf(teams.getOrNull(0)) }
    var right by remember(teams) { mutableStateOf(teams.getOrNull(1)) }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Compare teams", subtitle = "Simple side-by-side evidence", showBack = true, onBack = onBack) }
        if (teams.size < 2) { item { EmptyState("More team data needed", "Comparison becomes available when at least two teams are in the current research set.", Icons.Outlined.CompareArrows) }; return@LazyColumn }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TeamSelector(left, teams.filter { it.id != right?.id }, Modifier.weight(1f)) { left = it }
                TeamSelector(right, teams.filter { it.id != left?.id }, Modifier.weight(1f)) { right = it }
            }
        }
        val l = left; val r = right
        if (l != null && r != null) {
            item {
                BrightCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { EntityAvatar(l.name, true, Modifier.size(54.dp)); Text(l.name, color = Color.White, fontWeight = FontWeight.Bold) }
                        Text("VS", color = Color.White.copy(alpha = .7f), fontWeight = FontWeight.ExtraBold)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { EntityAvatar(r.name, true, Modifier.size(54.dp)); Text(r.name, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item {
                WhiteCard {
                    SectionHeading("Profile comparison")
                    val keys = (l.profile.keys + r.profile.keys).distinct().filter { key -> l.profile[key]?.let { it !is JsonObject } == true || r.profile[key]?.let { it !is JsonObject } == true }.take(8)
                    if (keys.isEmpty()) Text("No directly comparable headline metrics are available yet.", color = Muted)
                    keys.forEach { key ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(l.profile[key]?.toString()?.trim('"') ?: "–", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                            Text(humanize(key), Modifier.weight(1.2f), color = Muted, style = MaterialTheme.typography.bodySmall)
                            Text(r.profile[key]?.toString()?.trim('"') ?: "–", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Hairline)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSelector(selected: V2ResearchTeam?, choices: List<V2ResearchTeam>, modifier: Modifier, onSelect: (V2ResearchTeam) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }, modifier) {
        OutlinedTextField(selected?.name ?: "Choose team", {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) }, shape = RoundedCornerShape(16.dp))
        ExposedDropdownMenu(open, { open = false }) {
            choices.forEach { team -> DropdownMenuItem(text = { Text(team.name) }, onClick = { onSelect(team); open = false }) }
        }
    }
}

@Composable
fun ReferralContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val affiliate = state.account?.affiliateSummary ?: JsonObject(emptyMap())
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Refer & earn", showBack = true, onBack = onBack) }
        item {
            BrightCard(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF65A30D), Color(0xFF16A34A)))) {
                Icon(Icons.Outlined.GroupAdd, null, tint = Color.White, modifier = Modifier.size(38.dp))
                Text("Share PredIQ with people who will use it responsibly.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Referral rewards are shown only when the backend confirms settled eligible payments.", color = Color.White.copy(alpha = .8f))
            }
        }
        item {
            WhiteCard {
                SectionHeading("Referral summary")
                if (affiliate.isEmpty()) Text("No referral summary is available yet.", color = Muted)
                else affiliate.entries.take(12).forEach { (key, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(humanize(key), Modifier.weight(1f), color = Muted); Text(value.toString().trim('"'), color = Ink, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun OnboardingContractScreen(
    onDone: () -> Unit,
    onSearchTeams: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var football by rememberSaveable { mutableStateOf(true) }
    var basketball by rememberSaveable { mutableStateOf(false) }
    var tennis by rememberSaveable { mutableStateOf(false) }
    var alerts by rememberSaveable { mutableStateOf(true) }
    val titles = listOf("What sports do you follow?", "Pick teams or leagues", "Stay updated your way", "You're ready")
    Column(Modifier.fillMaxSize().background(Ivory).padding(22.dp).systemBarsPadding(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Step ${step + 1} of 4", color = PurpleDeep, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth(), color = Purple)
        Text(titles[step], color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        when (step) {
            0 -> {
                Text("Choose what interests you. You can change this later.", color = Muted)
                InterestToggle("Football", Icons.Outlined.SportsSoccer, football) { football = it }
                InterestToggle("Basketball", Icons.Outlined.SportsBasketball, basketball) { basketball = it }
                InterestToggle("Tennis", Icons.Outlined.SportsTennis, tennis) { tennis = it }
                Text("Sport interests currently shape this onboarding experience; match/team follows are the persisted backend preference.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            1 -> {
                EmptyState("Follow what matters", "Search PredIQ research and follow specific teams, players, competitions or matches. Those follows are saved to your account.", Icons.Outlined.BookmarkAdd, "Search teams & leagues", onSearchTeams)
            }
            2 -> {
                WhiteCard { ToggleRowOnboarding("Prediction changes", alerts) { alerts = it }; ToggleRowOnboarding("Lineups confirmed", alerts) { alerts = it }; ToggleRowOnboarding("Live opportunities", alerts) { alerts = it }; ToggleRowOnboarding("Results settled", alerts) { alerts = it } }
            }
            else -> {
                BrightCard { Icon(Icons.Outlined.CheckCircle, null, tint = Lime, modifier = Modifier.size(42.dp)); Text("All set.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text("PredIQ will keep the interface simple while the intelligence layer does the heavy work.", color = Color.White.copy(alpha = .82f)) }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = { if (step < 3) step++ else onDone() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (step < 3) "Continue" else "Start exploring") }
        if (step > 0) TextButton(onClick = { step-- }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}

@Composable
private fun InterestToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Purple) }; Spacer(Modifier.width(12.dp)); Text(label, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Checkbox(checked, onCheckedChange = onChange) }
    }
}

@Composable
private fun ToggleRowOnboarding(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = Ink); Switch(checked, onCheckedChange = onChange) }
}
