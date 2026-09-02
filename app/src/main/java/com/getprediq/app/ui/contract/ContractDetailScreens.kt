package com.getprediq.app.ui.contract

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.data.PlanDto
import com.getprediq.app.data.v2.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

@Composable
fun PredictionDetailContractScreen(
    state: PrediqContractState,
    onBack: () -> Unit,
    onFollow: (V2DecisionCard) -> Unit,
    onTeam: (String) -> Unit,
    onLeague: (String) -> Unit,
    onSources: () -> Unit,
) {
    val data = state.prediction
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 42.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PrediqHeader(title = "Analysis", showBack = true, onBack = onBack, actionIcon = Icons.Outlined.Share) }
        if (data == null) {
            item {
                if (state.busy) LoadingState("Building the decision view…")
                else EmptyState("Analysis unavailable", state.error ?: "This prediction could not be loaded.", Icons.Outlined.ErrorOutline)
            }
            return@LazyColumn
        }
        val card = data.decision
        item {
            TeamLine(card.event)
            Spacer(Modifier.height(8.dp))
            BrightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(card.decision.label, decisionTone(card.decision.code), true)
                    Spacer(Modifier.weight(1f))
                    Text(humanize(card.pick.market), color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
                }
                Text(card.pick.label ?: "PredIQ view", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${card.chance.percent ?: "–"}%", color = Lime, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.padding(bottom = 5.dp)) {
                        Text(card.chance.label, color = Color.White.copy(alpha = .9f), fontWeight = FontWeight.SemiBold)
                        card.chance.simple?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(card.evidence.label, "purple", true)
                    StatusPill(card.risk.label, if (card.risk.level == "high") "bad" else "neutral", true)
                    if (card.value.available) StatusPill(card.value.label, "good", true)
                }
                card.freshness.updatedAt?.let { Text("Updated ${compactTime(it)}", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = { onFollow(card) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PurpleDeep),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(if (card.followState.following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (card.followState.following) "Following" else "Follow this match")
                }
            }
        }
        if (data.reasons.isNotEmpty()) item { InsightListCard("Why this stands out", Icons.Outlined.AutoAwesome, data.reasons.map { it.label }, Green) }
        if (data.watchOuts.isNotEmpty()) item { InsightListCard("Watch out", Icons.Outlined.WarningAmber, data.watchOuts.map { it.label }, Amber) }
        if (data.value.available) {
            item {
                WhiteCard {
                    SectionHeading("Price & value")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("PredIQ chance", "${card.chance.percent ?: "–"}%", Modifier.weight(1f))
                        StatBox("Market chance", "${data.value.marketPercent ?: "–"}%", Modifier.weight(1f))
                        StatBox("Edge", data.value.edgePoints?.let { "${if (it >= 0) "+" else ""}${it.toInt()} pts" } ?: "–", Modifier.weight(1f), GreenDeep)
                    }
                    data.value.currentOdds?.let { Text("Current price ${String.format("%.2f", it)}", color = Ink, fontWeight = FontWeight.SemiBold) }
                    data.value.marketSignal?.let { signal ->
                        val opening = signal.double("opening_odds")
                        val current = signal.double("current_odds")
                        if (opening != null || current != null) {
                            Text("Market ${opening?.let { String.format("%.2f", it) } ?: "–"} → ${current?.let { String.format("%.2f", it) } ?: "–"}", color = Muted)
                        }
                    }
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("How PredIQ sees the game")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Home", percent(data.outlook.homeWin), Modifier.weight(1f))
                    StatBox("Draw", percent(data.outlook.draw), Modifier.weight(1f))
                    StatBox("Away", percent(data.outlook.awayWin), Modifier.weight(1f))
                }
                if (data.outlook.expectedGoals.home != null || data.outlook.expectedGoals.away != null) {
                    Text("Expected goals", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${data.outlook.expectedGoals.home?.let { String.format("%.1f", it) } ?: "–"}  –  ${data.outlook.expectedGoals.away?.let { String.format("%.1f", it) } ?: "–"}",
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        if (data.alternatives.isNotEmpty()) {
            item { SectionHeading("Other supported angles") }
            items(data.alternatives, key = { "alt-${it.rank}-${it.pick.label}" }) { alt ->
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(Color(0xFFEDE9FE), CircleShape), contentAlignment = Alignment.Center) {
                            Text(alt.rank.toString(), color = PurpleDeep, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alt.pick.label ?: humanize(alt.pick.market), color = Ink, fontWeight = FontWeight.Bold)
                            if (alt.value.available) Text(alt.value.label, color = GreenDeep, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${alt.chance.percent ?: "–"}%", color = PurpleDeep, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        if (data.timeline.isNotEmpty()) {
            item { SectionHeading("What changed") }
            item {
                WhiteCard {
                    data.timeline.take(8).forEachIndexed { index, change ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(34.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(if (change.type == "price") Icons.Outlined.ShowChart else Icons.Outlined.Update, null, tint = Purple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(change.title, color = Ink, fontWeight = FontWeight.SemiBold)
                                change.detail?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                                if (change.chanceBefore?.percent != null || change.chanceAfter?.percent != null) {
                                    Text("${change.chanceBefore?.percent ?: "–"}% → ${change.chanceAfter?.percent ?: "–"}%", color = PurpleDeep, fontWeight = FontWeight.Bold)
                                }
                                Text(compactTime(change.at), color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (index < data.timeline.take(8).lastIndex) HorizontalDivider(color = Hairline)
                    }
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Team & lineup context")
                val homeId = card.event.participants.home.id
                val awayId = card.event.participants.away.id
                InfoRow(Icons.Outlined.Shield, card.event.participants.home.name, "Team research", onClick = homeId?.let { { onTeam(it) } })
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.Shield, card.event.participants.away.name, "Team research", onClick = awayId?.let { { onTeam(it) } })
                card.event.competition.id?.let { id ->
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.EmojiEvents, card.event.competition.name ?: "Competition", "League intelligence", onClick = { onLeague(id) })
                }
                data.lineup.boolean("confirmed")?.let { confirmed ->
                    HorizontalDivider(color = Hairline)
                    InfoRow(
                        Icons.Outlined.Groups,
                        if (confirmed) "Lineups confirmed" else "Lineups pending",
                        if (confirmed) "PredIQ has the confirmed starting XIs" else "This assessment may change when lineups arrive",
                    )
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("PredIQ history")
                Text(data.similarCalls.label, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${data.similarCalls.settled} settled calls in ${data.similarCalls.range ?: "this range"}", color = Muted)
                HorizontalDivider(color = Hairline)
                Text("This competition & market", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("${percent(data.prediqRecord.hitRate)} · ${data.prediqRecord.settled} settled", color = Ink, fontWeight = FontWeight.Bold)
            }
        }
        item {
            WhiteCard(onClick = onSources) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, null, tint = Purple)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Evidence & sources", color = Ink, fontWeight = FontWeight.Bold)
                        Text("${data.evidence.label} · ${data.evidence.sourcesCount ?: 0} sources · ${data.evidence.signalsCount} signals", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun InsightListCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, rows: List<String>, tone: Color) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tone)
            Spacer(Modifier.width(8.dp))
            Text(title, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        rows.take(5).forEach { row ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, null, tint = tone, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Spacer(Modifier.width(8.dp))
                Text(row, color = Ink, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = PurpleDeep) {
    Column(modifier.background(IvoryDeep, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun LiveMatchDetailScreen(card: V2LiveCard?, onBack: () -> Unit, onOpenFull: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFF07150E)),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White) }
                Text("Live match", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (card == null) {
            item { Text("This live game is no longer available.", color = Color.White) }
            return@LazyColumn
        }
        item {
            TeamLine(card.event, dark = true)
            Spacer(Modifier.height(14.dp))
            BrightCard(brush = LiveBrush) {
                StatusPill(card.decision.label, decisionTone(card.decision.code), true)
                Text(card.pick.label ?: "Live PredIQ view", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${card.currentChance.percent ?: card.chance.percent ?: "–"}%", color = Lime, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(10.dp))
                    Text(card.currentChance.label, color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(bottom = 6.dp))
                }
                if (card.originalChance?.percent != null) {
                    Text("From ${card.originalChance.percent}% · ${card.change.label} ${card.change.points?.toInt() ?: 0} pts", color = Color.White.copy(alpha = .85f))
                }
                card.reasons.firstOrNull()?.let { Text("Why it changed · ${it.label}", color = Color.White) }
                if (card.predictionId != null) {
                    Button(onClick = { onOpenFull(card.predictionId) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GreenDeep)) {
                        Text("Open full analysis")
                    }
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Live status")
                InfoRow(Icons.Outlined.Bolt, "Analysis quality", humanize(card.analysisQuality))
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.Verified, "Evidence", card.evidence.label)
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.WarningAmber, "Risk", card.risk.label)
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.Update, "Updated", compactTime(card.freshness.updatedAt))
            }
        }
    }
}

@Composable
fun ResultReviewContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val data = state.resultReview
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Result review", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Opening the published record…") else EmptyState("Result unavailable", state.error ?: "This result could not be loaded.") }
            return@LazyColumn
        }
        val card = data.originalPrediction
        item {
            val resultBrush = when (data.actualResult.outcome) {
                "won" -> LiveBrush
                "lost" -> Brush.linearGradient(listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D)))
                else -> HeroBrush
            }
            BrightCard(brush = resultBrush) {
                StatusPill(humanize(data.actualResult.outcome), when (data.actualResult.outcome) { "won" -> "good"; "lost" -> "bad"; else -> "neutral" }, true)
                Text(card.pick.label ?: "PredIQ pick", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                TeamLine(card.event, dark = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatDark("Published chance", "${card.chance.percent ?: "–"}%", Modifier.weight(1f))
                    StatDark("Price at time", card.value.currentOdds?.let { String.format("%.2f", it) } ?: "–", Modifier.weight(1f))
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Original prediction")
                Text("This is the immutable first-publication record. Later model updates do not rewrite it.", color = Muted)
                InfoRow(Icons.Outlined.Schedule, "Published", data.publicationContext.string("published_at")?.let(::compactTime) ?: "–")
                InfoRow(Icons.Outlined.Groups, "Lineup state", humanize(data.publicationContext.string("lineup_state")))
            }
        }
        item {
            WhiteCard {
                SectionHeading("Closing market")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Closing price", data.closingMarket.closingOdds?.let { String.format("%.2f", it) } ?: "–", Modifier.weight(1f))
                    val clv = data.closingMarket.clvPrice
                    StatBox("Closing value", clv?.let { "${if (it >= 0) "+" else ""}${(it * 100).toInt()}%" } ?: "–", Modifier.weight(1f), if ((clv ?: 0.0) >= 0) GreenDeep else Red)
                }
            }
        }
        item { WhiteCard { InfoRow(Icons.Outlined.VerifiedUser, "Record integrity", "Published forecast preserved for audit", "Verified", tone = Green) } }
    }
}

@Composable
private fun StatDark(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White.copy(alpha = .12f), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Text(label, color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SearchContractScreen(state: PrediqContractState, onBack: () -> Unit, onQuery: (String) -> Unit, onOpen: (V2SearchResult) -> Unit) {
    var query by remember(state.searchQuery) { mutableStateOf(state.searchQuery) }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Search", showBack = true, onBack = onBack) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onQuery(it) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Search teams, players or leagues") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.length < 2) item { EmptyState("Search PredIQ research", "Type at least two letters to find a team, player or competition.", Icons.Outlined.Search) }
        else if (state.search?.results.isNullOrEmpty()) item { EmptyState("No matches found", "Try a shorter or different name.", Icons.Outlined.SearchOff) }
        else items(state.search!!.results, key = { "${it.type}-${it.id}" }) { result ->
            WhiteCard(onClick = { onOpen(result) }) {
                InfoRow(
                    when (result.type) { "team" -> Icons.Outlined.Shield; "player" -> Icons.Outlined.Person; else -> Icons.Outlined.EmojiEvents },
                    result.name,
                    listOfNotNull(humanize(result.type), result.subtitle).joinToString(" · "),
                )
            }
        }
    }
}

@Composable
fun TeamDetailContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val data = state.team
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Team research", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Loading team intelligence…") else EmptyState("Team unavailable", state.error ?: "PredIQ could not load this team.") }
            return@LazyColumn
        }
        val name = data.team.string("name") ?: "Team"
        item {
            BrightCard(brush = LiveBrush) {
                EntityAvatar(name, true, Modifier.size(58.dp))
                Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(listOfNotNull(data.team.string("country"), humanize(data.team.string("sport"))).filter { it.isNotBlank() }.joinToString(" · "), color = Color.White.copy(alpha = .8f))
                Text("${data.matchesCount} matches in intelligence profile", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (data.profile.isNotEmpty()) item { JsonSummaryCard("Current form & profile", data.profile) }
        item {
            WhiteCard {
                SectionHeading("PredIQ record")
                Text(percent(data.prediqRecord.hitRate), color = GreenDeep, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("${data.prediqRecord.wins} won · ${data.prediqRecord.settled} settled involving this team", color = Muted)
            }
        }
        if (data.upcoming.isNotEmpty()) {
            item { SectionHeading("Upcoming matches") }
            items(data.upcoming) { upcoming -> WhiteCard { UpcomingRowDetail(upcoming) } }
        }
        if (data.squadSummary.isNotEmpty()) item { JsonSummaryCard("Squad & availability", data.squadSummary, 8) }
    }
}

@Composable
fun PlayerDetailContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val data = state.player
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Player research", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Loading player intelligence…") else EmptyState("Player unavailable", state.error ?: "PredIQ could not load this player.") }
            return@LazyColumn
        }
        val name = data.player.string("name") ?: "Player"
        item {
            BrightCard {
                EntityAvatar(name, true, Modifier.size(64.dp))
                Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    listOfNotNull(data.player.string("team"), data.player.string("primary_team"), data.player.string("position"), data.player.string("nationality")).distinct().filter { it.isNotBlank() }.joinToString(" · "),
                    color = Color.White.copy(alpha = .82f),
                )
                val signal = data.currentSignal.string("label") ?: data.currentSignal.string("level")
                if (!signal.isNullOrBlank()) StatusPill(signal, "good", true)
            }
        }
        if (data.headlineStats.isNotEmpty()) item { JsonSummaryCard("Current stats", data.headlineStats, 10) }
        if (data.signals.isNotEmpty()) item { InsightListCard("Current signals", Icons.Outlined.Insights, data.signals, Purple) }
        if (data.recentActivity.isNotEmpty()) item { JsonArraySummaryCard("Recent activity", data.recentActivity, 8) }
        if (data.dataQuality.isNotEmpty()) item { JsonSummaryCard("Data quality", data.dataQuality, 8) }
    }
}

@Composable
fun CompetitionDetailContractScreen(state: PrediqContractState, onBack: () -> Unit, onOpenDecision: (String) -> Unit) {
    val data = state.competition
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "League intelligence", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Loading competition intelligence…") else EmptyState("Competition unavailable", state.error ?: "PredIQ could not load this competition.") }
            return@LazyColumn
        }
        val name = data.competition.string("name") ?: "Competition"
        item {
            BrightCard {
                Icon(Icons.Outlined.EmojiEvents, null, tint = Lime, modifier = Modifier.size(42.dp))
                Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(data.competition.string("country") ?: "", color = Color.White.copy(alpha = .8f))
                Text(
                    if (data.trackRecord.isEmpty()) "Track record will appear as results settle" else "${data.trackRecord.size} performance slices available",
                    color = Color.White.copy(alpha = .82f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (data.outlook.isNotEmpty()) item { JsonSummaryCard("League outlook", data.outlook) }
        if (data.trends.isNotEmpty()) item { JsonSummaryCard("Current trends", data.trends) }
        if (data.prediqStrengths.isNotEmpty()) item { JsonArraySummaryCard("PredIQ strengths", data.prediqStrengths) }
        if (data.trackRecord.isNotEmpty()) item { JsonArraySummaryCard("PredIQ record", data.trackRecord, 10) }
        if (data.todayOpportunities.isNotEmpty()) {
            item { SectionHeading("Today's opportunities") }
            items(data.todayOpportunities, key = { it.id }) { card ->
                val ref = card.predictionId ?: card.publishedForecastId ?: card.id
                DecisionCard(card, onOpen = { if (ref.isNotBlank()) onOpenDecision(ref) })
            }
        }
        if (data.teams.isNotEmpty()) item { JsonArraySummaryCard("Teams", data.teams, 20) }
    }
}

@Composable
fun FollowingContractScreen(
    state: PrediqContractState,
    onBack: () -> Unit,
    onUnfollow: (String) -> Unit,
    onUpdate: (String, V2FollowAlerts) -> Unit,
) {
    val follows = state.follows?.follows.orEmpty()
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Following", subtitle = "Matches, teams, players and leagues", showBack = true, onBack = onBack) }
        if (follows.isEmpty()) item { EmptyState("Nothing followed yet", "Tap Follow on a match, team, player or league to monitor meaningful changes.", Icons.Outlined.BookmarkAdd) }
        else items(follows, key = { it.id }) { follow ->
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Color(0xFFEDE9FE), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bookmark, null, tint = Purple) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(follow.entityLabel ?: follow.entityKey, color = Ink, fontWeight = FontWeight.Bold)
                        Text(humanize(follow.entityType), color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onUnfollow(follow.id) }) { Icon(Icons.Outlined.DeleteOutline, "Unfollow", tint = Red) }
                }
                HorizontalDivider(color = Hairline)
                FollowAlertToggle("Prediction changes", follow.alerts.predictionChanges) { onUpdate(follow.id, follow.alerts.copy(predictionChanges = it)) }
                FollowAlertToggle("Lineups", follow.alerts.lineup) { onUpdate(follow.id, follow.alerts.copy(lineup = it)) }
                FollowAlertToggle("Live changes", follow.alerts.live) { onUpdate(follow.id, follow.alerts.copy(live = it)) }
                FollowAlertToggle("Result", follow.alerts.result) { onUpdate(follow.id, follow.alerts.copy(result = it)) }
                FollowAlertToggle("Team news", follow.alerts.teamNews) { onUpdate(follow.id, follow.alerts.copy(teamNews = it)) }
            }
        }
    }
}

@Composable
private fun FollowAlertToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Ink)
        Switch(checked, onCheckedChange = onChange)
    }
}

@Composable
fun NotificationPreferencesContractScreen(
    state: PrediqContractState,
    onBack: () -> Unit,
    onSave: (V2NotificationSettings) -> Unit,
) {
    val current = state.notifications
    var settings by remember(current) { mutableStateOf(current) }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Notifications", subtitle = "Only the changes that matter", showBack = true, onBack = onBack) }
        if (settings == null) {
            item { LoadingState("Loading notification preferences…") }
            return@LazyColumn
        }
        item {
            WhiteCard {
                ToggleRow("Push notifications", "Recommended for live and lineup changes", settings!!.pushEnabled) { settings = settings!!.copy(pushEnabled = it) }
                HorizontalDivider(color = Hairline)
                ToggleRow("Email", "Longer updates and account notices", settings!!.emailEnabled) { settings = settings!!.copy(emailEnabled = it) }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Pick & analysis")
                ToggleRow("Daily top picks", null, settings!!.alerts.dailyPicks) { settings = settings!!.copy(alerts = settings!!.alerts.copy(dailyPicks = it)) }
                ToggleRow("Live / prediction changes", null, settings!!.alerts.liveChanges) { settings = settings!!.copy(alerts = settings!!.alerts.copy(liveChanges = it)) }
                ToggleRow("Lineups confirmed", null, settings!!.alerts.lineupChanges) { settings = settings!!.copy(alerts = settings!!.alerts.copy(lineupChanges = it)) }
                ToggleRow("Results & settlement", null, settings!!.alerts.results) { settings = settings!!.copy(alerts = settings!!.alerts.copy(results = it)) }
                ToggleRow("Subscription notices", null, settings!!.alerts.subscription) { settings = settings!!.copy(alerts = settings!!.alerts.copy(subscription = it)) }
            }
        }
        item { Button(onClick = { onSave(settings!!) }, modifier = Modifier.fillMaxWidth()) { Text("Save preferences") } }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersContractSheet(
    state: PrediqContractState,
    onClose: () -> Unit,
    onSport: (String) -> Unit,
    onCompetition: (String) -> Unit,
    onFollowing: (Boolean) -> Unit,
) {
    val options = state.today?.filterOptions ?: state.live?.filterOptions ?: V2FilterOptions()
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Ivory) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Filters", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Sport", color = Ink, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.selectedSport.isBlank(), onClick = { onSport("") }, label = { Text("All") }) }
                items(options.sports) { sport -> FilterChip(selected = state.selectedSport == sport, onClick = { onSport(sport) }, label = { Text(humanize(sport)) }) }
            }
            if (options.competitions.isNotEmpty()) {
                Text("Competition", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedCompetition.isBlank(), onClick = { onCompetition("") }, label = { Text("All") }) }
                    items(options.competitions.take(30)) { competition ->
                        FilterChip(selected = state.selectedCompetition == competition, onClick = { onCompetition(competition) }, label = { Text(competition, maxLines = 1) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Following only", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold)
                Switch(state.followingOnly, onCheckedChange = onFollowing)
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Show results") }
        }
    }
}

@Composable
fun UpcomingContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val upcoming = state.today?.upcoming.orEmpty()
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PrediqHeader(title = "Upcoming", subtitle = "Today’s scheduled events", showBack = true, onBack = onBack) }
        if (upcoming.isEmpty()) item { EmptyState("No upcoming events in this view", "Adjust your filters to see more events.", Icons.Outlined.CalendarMonth) }
        else items(upcoming) { row -> WhiteCard { UpcomingRowDetail(row) } }
    }
}

@Composable
private fun UpcomingRowDetail(item: V2Upcoming) {
    TeamLine(item.event)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(compactTime(item.event.startsAt), color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        StatusPill(item.decision.label, decisionTone(item.decision.code))
    }
    item.decision.reason?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
}

@Composable
fun EvidenceSourceContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val evidence = state.prediction?.evidence
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Evidence & sources", showBack = true, onBack = onBack) }
        if (evidence == null) {
            item { EmptyState("Evidence unavailable", "This decision does not currently expose source detail.") }
            return@LazyColumn
        }
        item {
            BrightCard {
                Text(evidence.label, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("${evidence.sourcesCount ?: 0} sources · ${evidence.signalsCount} signals", color = Color.White.copy(alpha = .8f))
                StatusPill(if (evidence.lineupState == "confirmed") "Lineups confirmed" else "Lineups ${humanize(evidence.lineupState)}", if (evidence.lineupState == "confirmed") "good" else "warn", true)
            }
        }
        if (evidence.signals.isEmpty()) item { EmptyState("Source detail is limited", "PredIQ has retained the evidence strength while source-level detail is unavailable.", Icons.Outlined.Verified) }
        else items(evidence.signals) { signal ->
            WhiteCard {
                InfoRow(
                    Icons.Outlined.Verified,
                    signal.string("evidence_type") ?: signal.string("source_code") ?: "Evidence",
                    signal.string("entity_type") ?: signal.string("entity_name"),
                    signal.string("observed_at")?.let(::compactTime),
                )
                signal.string("summary")?.let { Text(it, color = Muted) }
            }
        }
    }
}

@Composable
fun PerformanceBreakdownContractScreen(state: PrediqContractState, market: String, onBack: () -> Unit) {
    val summary = state.resultsSummary
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = humanize(market), subtitle = "Performance breakdown", showBack = true, onBack = onBack) }
        if (summary == null) {
            item { LoadingState() }
            return@LazyColumn
        }
        val slice = summary.byMarket.firstOrNull { it.market == market }
        item {
            BrightCard {
                Text("${percent(slice?.hitRate)} landed", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("${slice?.settled ?: 0} settled predictions in the selected period", color = Color.White.copy(alpha = .8f))
            }
        }
        item { SectionHeading("By chance") }
        items(summary.calibration.reversed()) { band ->
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(band.range, color = Ink, fontWeight = FontWeight.Bold)
                        Text("${band.settled} settled", color = Muted)
                    }
                    Text(band.observedRate?.let { "${it.toInt()}% landed" } ?: "–", color = GreenDeep, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun ProfileContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val profile = state.account?.profile
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Profile & region", showBack = true, onBack = onBack) }
        if (profile == null) item { LoadingState() }
        else item {
            WhiteCard {
                EntityAvatar(profile.name ?: profile.email, modifier = Modifier.size(64.dp))
                Text(profile.name ?: "PredIQ member", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(profile.email, color = Muted)
                InfoRow(Icons.Outlined.Public, "Country", profile.country ?: "Not set")
                InfoRow(Icons.Outlined.Payments, "Currency", profile.currency ?: "Not set")
                InfoRow(Icons.Outlined.Schedule, "Timezone", state.notifications?.timezone ?: "Africa/Kampala")
            }
        }
    }
}

@Composable
fun HelpContractScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Help & methodology", showBack = true, onBack = onBack) }
        item {
            BrightCard {
                Text("PredIQ is decision support, not certainty.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Chance tells you likelihood. Evidence tells you how well-supported the estimate is. Value only appears when market pricing is actually available.", color = Color.White.copy(alpha = .82f))
            }
        }
        item {
            WhiteCard {
                InfoRow(Icons.Outlined.AutoAwesome, "How PredIQ works", "Multiple signals are normalised into a single assessment")
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.Percent, "Understanding chance", "Percentages are estimates, not guarantees")
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.ShowChart, "Understanding value", "PredIQ only claims value when a valid market comparison exists")
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.VerifiedUser, "Transparent results", "Published picks remain visible after settlement")
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.HealthAndSafety, "Responsible gambling", "Set limits, avoid chasing losses, and stop when gambling stops being fun")
            }
        }
    }
}

@Composable
fun PlanContractScreen(plans: List<PlanDto>, state: PrediqContractState, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Plan & subscription", showBack = true, onBack = onBack) }
        state.account?.membership?.let { membership ->
            item {
                BrightCard {
                    Text(membership.planName ?: "PredIQ access", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(membership.daysRemaining?.let { "$it days remaining" } ?: humanize(membership.state), color = Color.White.copy(alpha = .82f))
                    StatusPill(if (membership.fullAccess) "Full access" else humanize(membership.state), if (membership.fullAccess) "good" else "warn", true)
                }
            }
        }
        if (plans.isEmpty()) item { EmptyState("Plans are loading", "Available subscription options will appear here.", Icons.Outlined.WorkspacePremium) }
        else items(plans, key = { it.code }) { plan ->
            WhiteCard {
                Text(plan.name, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("UGX ${"%,d".format(plan.priceUgx)} · ${plan.durationDays} days", color = Muted)
                Text("Checkout continues through the existing PredIQ payment flow.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PaymentsContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Payments & receipts", showBack = true, onBack = onBack) }
        item {
            WhiteCard {
                SectionHeading("Payment summary")
                val summary = state.account?.paymentsSummary ?: JsonObject(emptyMap())
                if (summary.isEmpty()) Text("No payment summary is available yet.", color = Muted)
                else JsonEntries(summary, 10)
            }
        }
    }
}

@Composable
private fun JsonSummaryCard(title: String, value: JsonObject, maxItems: Int = 6) {
    WhiteCard {
        SectionHeading(title)
        JsonEntries(value, maxItems)
    }
}

@Composable
private fun JsonArraySummaryCard(title: String, rows: List<JsonObject>, maxItems: Int = 8) {
    WhiteCard {
        SectionHeading(title)
        rows.take(maxItems).forEachIndexed { index, row ->
            val titleText = row.string("name") ?: row.string("label") ?: row.string("market") ?: row.string("team") ?: "Item ${index + 1}"
            Text(titleText, color = Ink, fontWeight = FontWeight.SemiBold)
            val secondary = row.entries
                .filterNot { it.key in setOf("name", "label", "market", "team") }
                .filter { it.value !is JsonObject && it.value !is JsonArray }
                .take(3)
                .joinToString(" · ") { "${humanize(it.key)} ${jsonText(it.value)}" }
            if (secondary.isNotBlank()) Text(secondary, color = Muted, style = MaterialTheme.typography.bodySmall)
            if (index < rows.take(maxItems).lastIndex) HorizontalDivider(color = Hairline)
        }
    }
}

@Composable
private fun JsonEntries(value: JsonObject, maxItems: Int) {
    val rows = value.entries.filter { (_, element) -> element !is JsonObject && element !is JsonArray }.take(maxItems)
    if (rows.isEmpty()) {
        Text("No simple summary is available yet.", color = Muted)
        return
    }
    rows.forEachIndexed { index, (key, element) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(humanize(key), color = Muted, modifier = Modifier.weight(1f))
            Text(jsonText(element), color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (index < rows.lastIndex) HorizontalDivider(color = Hairline)
    }
}

private fun jsonText(element: JsonElement?): String = when (element) {
    null -> "–"
    is JsonPrimitive -> element.content
    else -> element.toString()
}

fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.content
fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
