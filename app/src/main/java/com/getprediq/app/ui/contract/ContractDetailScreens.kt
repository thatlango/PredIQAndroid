package com.getprediq.app.ui.contract

import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.data.AccountFeatureRepository
import com.getprediq.app.data.PaymentHistoryItem
import com.getprediq.app.data.PlanDto
import com.getprediq.app.data.v2.*
import com.getprediq.app.data.v3.V3SlateCard
import com.getprediq.app.ui.CompetitionMark
import com.getprediq.app.ui.PlayerHeadshot
import com.getprediq.app.ui.TeamCrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.LocalDate

@Composable
fun PredictionDetailContractScreen(
    state: PrediqContractState,
    onBack: () -> Unit,
    onFollow: (V2DecisionCard) -> Unit,
    onTeam: (String) -> Unit,
    onLeague: (String) -> Unit,
    onSources: () -> Unit,
    onV3Event: (String) -> Unit,
) {
    val context = LocalContext.current
    val data = state.prediction
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 42.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PrediqHeader(title = "Analysis", showBack = true, onBack = onBack, actionIcon = Icons.Outlined.Share, onAction = {
                val share = state.prediction?.share
                val text = share?.text?.takeIf { it.isNotBlank() } ?: state.prediction?.decision?.let { card -> "PredIQ: ${card.pick.label ?: "Prediction"} · ${card.chance.percent ?: "–"}% — ${card.event.participants.home.name} vs ${card.event.participants.away.name}" } ?: return@PrediqHeader
                val fullText = share?.url?.takeIf { it.isNotBlank() }?.let { "$text\n$it" } ?: text
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, fullText) }, "Share PredIQ analysis"))
            })
        }
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
                    Button(onClick = { if (card.event.id.isNotBlank()) onV3Event(card.event.id) }, enabled = card.event.id.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                        Icon(Icons.Outlined.AutoGraph, null); Spacer(Modifier.width(7.dp)); Text("Open multi-market intelligence")
                    }
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
fun LiveMatchDetailScreen(card: V2LiveCard?, onBack: () -> Unit, onOpenFull: (String) -> Unit, onOpenV3: (String) -> Unit) {
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
                    Button(onClick = { onOpenFull(card.predictionId) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GreenDeep)) { Text("Open full analysis") }
                }
                OutlinedButton(onClick = { onOpenV3(card.event.id) }, enabled = card.event.id.isNotBlank(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Outlined.AutoGraph, null); Spacer(Modifier.width(7.dp)); Text("Markets & reference odds")
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
                SectionHeading("What happened")
                val actual = data.actualResult.actual
                val homeScore = actual?.double("home_score")?.toInt()
                val awayScore = actual?.double("away_score")?.toInt()
                if (homeScore != null || awayScore != null) {
                    Text("${card.event.participants.home.name} ${homeScore ?: "–"}  –  ${awayScore ?: "–"} ${card.event.participants.away.name}", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    actual?.double("total_goals")?.let { Text("${it.toInt()} total goals", color = Muted) }
                } else Text("The settled outcome is preserved even where detailed match-event data is unavailable.", color = Muted)
                data.actualResult.settledAt?.let { Text("Settled ${friendlyDateTime(it)}", color = Muted, style = MaterialTheme.typography.bodySmall) }
                Text(if (data.actualResult.outcome == "won") "The published call landed. PredIQ keeps the original thesis and price visible for audit." else if (data.actualResult.outcome == "lost") "The published call did not land. PredIQ keeps the loss and original thesis visible instead of rewriting history." else "This call is not yet graded as a win or loss.", color = if (data.actualResult.outcome == "won") GreenDeep else if (data.actualResult.outcome == "lost") Red else Muted, fontWeight = FontWeight.SemiBold)
            }
        }
        if (card.reasons.isNotEmpty()) item { InsightListCard("Original reasons", Icons.Outlined.HistoryEdu, card.reasons.map { it.label }, if (data.actualResult.outcome == "won") Green else Purple) }
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
    var type by rememberSaveable { mutableStateOf("all") }
    val allResults = state.search?.results.orEmpty()
    val visible = if (type == "all") allResults else allResults.filter { it.type == type }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Search", subtitle = "Teams, players and competitions", showBack = true, onBack = onBack) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onQuery(it) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = if (query.isNotBlank()) {{ IconButton(onClick = { query = ""; onQuery("") }) { Icon(Icons.Outlined.Close, "Clear") } }} else null,
                placeholder = { Text("Search teams, players or leagues") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.length >= 2 && allResults.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("all", "team", "player", "competition")) { item ->
                        val count = if (item == "all") allResults.size else allResults.count { it.type == item }
                        FilterChip(selected = type == item, onClick = { type = item }, label = { Text("${if (item == "all") "All" else humanize(item)}${if (count > 0) "  $count" else ""}") })
                    }
                }
            }
        }
        if (query.length < 2) item { EmptyState("Search PredIQ research", "Type at least two letters to find a team, player or competition.", Icons.Outlined.Search) }
        else if (visible.isEmpty()) item { EmptyState("No matches found", if (type == "all") "Try a shorter or different name." else "No ${humanize(type).lowercase()} matches in these results.", Icons.Outlined.SearchOff) }
        else {
            listOf("team", "player", "competition").forEach { group ->
                val rows = visible.filter { it.type == group }
                if (rows.isNotEmpty()) {
                    item { SectionHeading(when (group) { "team" -> "Teams"; "player" -> "Players"; else -> "Leagues & competitions" }) }
                    items(rows, key = { "${it.type}-${it.id}" }) { result ->
                        WhiteCard(onClick = { onOpen(result) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (result.type) {
                                    "team" -> TeamCrest(result.name, result.sportCode ?: result.sport ?: "football", size = 46.dp)
                                    "player" -> PlayerHeadshot(result.name, result.sportCode ?: result.sport ?: "football", size = 46.dp)
                                    else -> CompetitionMark(result.name, result.sportCode ?: result.sport ?: "football", size = 46.dp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(result.name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(listOfNotNull(result.subtitle, result.sportCode?.let(::humanize), result.sport?.let(::humanize)).distinct().joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamDetailContractScreen(state: PrediqContractState, onBack: () -> Unit, onFollowEntity: (String, String, String) -> Unit) {
    val data = state.team
    var tab by rememberSaveable { mutableStateOf("overview") }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Team research", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Loading team intelligence…") else EmptyState("Team unavailable", state.error ?: "PredIQ could not load this team.") }
            return@LazyColumn
        }
        val name = data.team.string("name") ?: "Team"
        val sport = data.team.string("sport") ?: "football"
        val overall = data.profile["overall"] as? JsonObject
        val home = data.profile["home"] as? JsonObject
        val away = data.profile["away"] as? JsonObject
        val overall20 = data.profile["overall_20"] as? JsonObject
        val competitions = data.profile["competitions"] as? JsonObject
        item {
            BrightCard(brush = LiveBrush) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamCrest(name, sport, size = 66.dp, dark = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text(listOfNotNull(data.team.string("country"), humanize(sport)).filter { it.isNotBlank() }.joinToString(" · "), color = Color.White.copy(alpha = .8f))
                        Text("${data.matchesCount} matches observed", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                overall?.string("form")?.let { StatusPill("Form $it", "good", true) }
                val entityId = data.team.string("id").orEmpty()
                val following = state.follows?.follows.orEmpty().any { it.entityType == "team" && it.entityKey == entityId }
                Button(onClick = { if (!following && entityId.isNotBlank()) onFollowEntity("team", entityId, name) }, enabled = entityId.isNotBlank() && !following, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GreenDeep, disabledContainerColor = Color.White.copy(alpha = .18f), disabledContentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Icon(if (following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text(if (following) "Following team" else "Follow team")
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("overview", "form", "stats", "squad", "matches")) { section -> FilterChip(selected = tab == section, onClick = { tab = section }, label = { Text(humanize(section)) }) }
            }
        }
        when (tab) {
            "overview" -> {
                item {
                    WhiteCard {
                        SectionHeading("Current form")
                        overall?.let { profile ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatBox("PPG", profile.double("ppg")?.let { "%.2f".format(it) } ?: "–", Modifier.weight(1f))
                                StatBox("Goals/game", profile.double("gf_pg")?.let { "%.2f".format(it) } ?: "–", Modifier.weight(1f), GreenDeep)
                                StatBox("Concede/game", profile.double("ga_pg")?.let { "%.2f".format(it) } ?: "–", Modifier.weight(1f), Amber)
                            }
                            profile.string("form")?.let { Text(it.replace(" ", "   "), color = GreenDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
                        } ?: Text("Form summary is still being built.", color = Muted)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TeamVenueCard("Home", home, Modifier.weight(1f))
                        TeamVenueCard("Away", away, Modifier.weight(1f))
                    }
                }
                item {
                    WhiteCard {
                        SectionHeading("PredIQ record")
                        Text(percent(data.prediqRecord.hitRate), color = GreenDeep, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("${data.prediqRecord.wins} won · ${data.prediqRecord.settled} settled involving this team", color = Muted)
                    }
                }
                if (data.upcoming.isNotEmpty()) {
                    item { SectionHeading("Next matches") }
                    items(data.upcoming.take(3)) { upcoming -> WhiteCard { UpcomingRowDetail(upcoming) } }
                }
            }
            "form" -> {
                item { TeamProfileCard("Last 10", overall) }
                overall20?.let { item { TeamProfileCard("Last 20", it) } }
                if (!competitions.isNullOrEmpty()) {
                    item { SectionHeading("By competition") }
                    competitions.entries.forEach { (competition, value) ->
                        val profile = value as? JsonObject
                        if (profile != null) item { TeamProfileCard(competition, profile) }
                    }
                }
            }
            "stats" -> {
                item {
                    WhiteCard {
                        SectionHeading("Team indicators")
                        overall?.let { profile ->
                            InfoRow(Icons.Outlined.SportsSoccer, "Goals scored / game", profile.double("gf_pg")?.let { "%.2f".format(it) } ?: "–")
                            InfoRow(Icons.Outlined.Shield, "Goals conceded / game", profile.double("ga_pg")?.let { "%.2f".format(it) } ?: "–")
                            InfoRow(Icons.Outlined.SwapHoriz, "BTTS rate", profile.double("btts_rate")?.let(::percent) ?: "–")
                            InfoRow(Icons.Outlined.TrendingUp, "Over 2.5 rate", profile.double("over25_rate")?.let(::percent) ?: "–")
                            InfoRow(Icons.Outlined.Leaderboard, "Points / game", profile.double("ppg")?.let { "%.2f".format(it) } ?: "–")
                        }
                        data.profile.double("rest_days")?.let { InfoRow(Icons.Outlined.Bedtime, "Rest days", it.toInt().toString()) }
                    }
                }
            }
            "squad" -> {
                if (data.squadSummary.isNotEmpty()) item { JsonSummaryCard("Squad & availability", data.squadSummary, 12) }
                else item { EmptyState("Squad layer is limited", "PredIQ will show squad and availability detail when the data source exposes it.", Icons.Outlined.Groups) }
            }
            "matches" -> {
                if (data.upcoming.isEmpty()) item { EmptyState("No upcoming matches", "Future fixtures will appear when scheduled.", Icons.Outlined.CalendarMonth) }
                else items(data.upcoming) { upcoming -> WhiteCard { UpcomingRowDetail(upcoming) } }
            }
        }
    }
}

@Composable
private fun TeamVenueCard(label: String, profile: JsonObject?, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Muted, fontWeight = FontWeight.Bold)
            Text(profile?.double("ppg")?.let { "%.1f PPG".format(it) } ?: "–", color = PurpleDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(profile?.string("form") ?: "Form unavailable", color = GreenDeep, style = MaterialTheme.typography.bodySmall)
            Text("${profile?.double("gf_pg")?.let { "%.1f".format(it) } ?: "–"} scored · ${profile?.double("ga_pg")?.let { "%.1f".format(it) } ?: "–"} conceded", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TeamProfileCard(title: String, profile: JsonObject?) {
    if (profile == null) { EmptyState(title, "No form profile is available for this slice."); return }
    WhiteCard {
        SectionHeading(title)
        profile.string("form")?.let { Text(it.replace(" ", "   "), color = GreenDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("PPG", profile.double("ppg")?.let { "%.2f".format(it) } ?: "–", Modifier.weight(1f))
            StatBox("GF", profile.double("gf_pg")?.let { "%.2f".format(it) } ?: "–", Modifier.weight(1f), GreenDeep)
            StatBox("GA", profile.double("ga_pg")?.let { "%.2f".format(it) } ?: "–", Modifier.weight(1f), Amber)
        }
        Text("${profile.double("wins")?.toInt() ?: 0}W · ${profile.double("draws")?.toInt() ?: 0}D · ${profile.double("losses")?.toInt() ?: 0}L", color = Muted)
    }
}

@Composable
fun PlayerDetailContractScreen(state: PrediqContractState, onBack: () -> Unit, onFollowEntity: (String, String, String) -> Unit) {
    val data = state.player
    var tab by rememberSaveable { mutableStateOf("overview") }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Player research", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Loading player intelligence…") else EmptyState("Player unavailable", state.error ?: "PredIQ could not load this player.") }
            return@LazyColumn
        }
        val name = data.player.string("name") ?: "Player"
        val sport = data.player.string("sport") ?: "football"
        item {
            BrightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerHeadshot(name, sport, size = 70.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text(listOfNotNull(data.player.string("team"), data.player.string("position"), data.player.string("nationality")).filter { it.isNotBlank() }.joinToString(" · "), color = Color.White.copy(alpha = .82f))
                    }
                }
                val signal = data.currentSignal.string("label") ?: data.currentSignal.string("level")
                val index = data.currentSignal.double("index")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!signal.isNullOrBlank()) StatusPill(signal, if ((index ?: 0.0) >= 60) "good" else "warn", true)
                    index?.let { StatusPill("Form ${it.toInt()}/100", "purple", true) }
                }
                val entityId = data.player.string("id").orEmpty()
                val following = state.follows?.follows.orEmpty().any { it.entityType == "player" && it.entityKey == entityId }
                Button(onClick = { if (!following && entityId.isNotBlank()) onFollowEntity("player", entityId, name) }, enabled = entityId.isNotBlank() && !following, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PurpleDeep, disabledContainerColor = Color.White.copy(alpha = .18f), disabledContentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Icon(if (following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text(if (following) "Following player" else "Follow player")
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("overview", "stats", "matches", "profile")) { section -> FilterChip(selected = tab == section, onClick = { tab = section }, label = { Text(humanize(section)) }) }
            }
        }
        when (tab) {
            "overview" -> {
                item {
                    WhiteCard {
                        SectionHeading("Current form")
                        val index = data.currentSignal.double("index")
                        Text(data.currentSignal.string("label") ?: "Current signal", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        index?.let {
                            LinearProgressIndicator(progress = { (it / 100.0).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = if (it >= 60) Green else Purple, trackColor = IvoryDeep)
                            Text("PredIQ form signal ${it.toInt()}/100", color = Muted)
                        }
                    }
                }
                if (data.signals.isNotEmpty()) item { InsightListCard("Current signals", Icons.Outlined.Insights, data.signals, Purple) }
                if (data.headlineStats.isNotEmpty()) item { PlayerHeadlineStatsCard(data.headlineStats) }
            }
            "stats" -> {
                if (data.headlineStats.isEmpty()) item { EmptyState("Stats are still limited", "PredIQ will add headline numbers as source coverage improves.", Icons.Outlined.Analytics) }
                else item { PlayerHeadlineStatsCard(data.headlineStats, expanded = true) }
            }
            "matches" -> {
                if (data.recentActivity.isEmpty()) item { EmptyState("No recent recorded activity", "Recent event-level observations will appear here.", Icons.Outlined.EventNote) }
                else items(data.recentActivity) { row ->
                    WhiteCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SportsSoccer, null, tint = Purple) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(row.string("team") ?: data.player.string("team") ?: "Recorded activity", color = Ink, fontWeight = FontWeight.Bold)
                                Text(listOfNotNull(row.string("competition"), row.string("at")?.let(::friendlyDateTime)).joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            row.double("minute")?.let { Text("${it.toInt()}'", color = PurpleDeep, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }
            "profile" -> {
                item {
                    WhiteCard {
                        SectionHeading("Player profile")
                        InfoRow(Icons.Outlined.Shield, "Club", data.player.string("team") ?: "Not set")
                        InfoRow(Icons.Outlined.Sports, "Position", data.player.string("position") ?: "Not set")
                        InfoRow(Icons.Outlined.Public, "Nationality", data.player.string("nationality") ?: "Not set")
                        data.player.double("age")?.let { InfoRow(Icons.Outlined.Cake, "Age", it.toInt().toString()) }
                    }
                }
                if (data.dataQuality.isNotEmpty()) item { JsonSummaryCard("Data quality", data.dataQuality, 8) }
            }
        }
    }
}

@Composable
private fun PlayerHeadlineStatsCard(stats: JsonObject, expanded: Boolean = false) {
    WhiteCard {
        SectionHeading(if (expanded) "Stats & source context" else "Current stats")
        stats.entries.take(if (expanded) 16 else 6).forEachIndexed { index, (key, value) ->
            val row = value as? JsonObject
            val rawValue = row?.double("value")
            val display = rawValue?.let { if (kotlin.math.abs(it - it.toInt()) < .001) it.toInt().toString() else "%.2f".format(it) } ?: (value as? JsonPrimitive)?.contentOrNull ?: "–"
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(humanize(key), color = Ink, fontWeight = FontWeight.SemiBold)
                    row?.let { meta ->
                        val context = listOfNotNull(meta.string("competition"), meta.string("season"), meta.string("source")?.let(::humanize)).joinToString(" · ")
                        if (context.isNotBlank()) Text(context, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(display, color = PurpleDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            if (index < stats.entries.take(if (expanded) 16 else 6).lastIndex) HorizontalDivider(color = Hairline)
        }
    }
}

@Composable
fun CompetitionDetailContractScreen(state: PrediqContractState, onBack: () -> Unit, onOpenDecision: (String) -> Unit, onFollowEntity: (String, String, String) -> Unit) {
    val data = state.competition
    var tab by rememberSaveable { mutableStateOf("overview") }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "League intelligence", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Loading competition intelligence…") else EmptyState("Competition unavailable", state.error ?: "PredIQ could not load this competition.") }
            return@LazyColumn
        }
        val name = data.competition.string("name") ?: "Competition"
        val sport = data.competition.string("sport") ?: "football"
        val leagueTeams = state.research?.teamsInTodayPicks.orEmpty().filter { team ->
            val comps = team.profile["competitions"] as? JsonObject
            comps?.keys?.any { it.equals(name, true) } == true
        }
        item {
            BrightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompetitionMark(name, sport, size = 54.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text(listOfNotNull(data.competition.string("country"), humanize(sport)).filter { it.isNotBlank() }.joinToString(" · "), color = Color.White.copy(alpha = .8f))
                    }
                }
                data.outlook.string("insight")?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha = .86f), maxLines = 3, overflow = TextOverflow.Ellipsis) }
                val entityId = data.competition.string("id").orEmpty()
                val following = state.follows?.follows.orEmpty().any { it.entityType == "competition" && it.entityKey == entityId }
                Button(onClick = { if (!following && entityId.isNotBlank()) onFollowEntity("competition", entityId, name) }, enabled = entityId.isNotBlank() && !following, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PurpleDeep, disabledContainerColor = Color.White.copy(alpha = .18f), disabledContentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Icon(if (following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text(if (following) "Following league" else "Follow league")
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("overview", "teams", "stats", "markets")) { section -> FilterChip(selected = tab == section, onClick = { tab = section }, label = { Text(humanize(section)) }) }
            }
        }
        when (tab) {
            "overview" -> {
                item {
                    WhiteCard {
                        SectionHeading("League outlook")
                        val insight = data.outlook.string("insight")
                        Text(insight ?: "PredIQ is building the current competition outlook from available league and team evidence.", color = Ink, fontWeight = if (insight != null) FontWeight.SemiBold else FontWeight.Normal)
                        data.outlook.string("valid_until")?.let { Text("Valid until ${friendlyDateTime(it)}", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (data.trends.isNotEmpty()) item { LeagueTrendCard(data.trends) }
                if (data.todayOpportunities.isNotEmpty()) {
                    item { SectionHeading("Today's opportunities") }
                    items(data.todayOpportunities.take(4), key = { it.id }) { card ->
                        val ref = card.predictionId ?: card.publishedForecastId ?: card.id
                        DecisionCard(card, onOpen = { if (ref.isNotBlank()) onOpenDecision(ref) })
                    }
                }
            }
            "teams" -> {
                if (leagueTeams.isEmpty() && data.teams.isEmpty()) item { EmptyState("Team list is still limited", "PredIQ currently shows teams when they are in the active research set. Full table coverage will appear as the competition contract expands.", Icons.Outlined.Groups) }
                else {
                    if (leagueTeams.isNotEmpty()) items(leagueTeams, key = { it.id }) { team ->
                        WhiteCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamCrest(team.name, team.sport.ifBlank { sport }, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) { Text(team.name, color = Ink, fontWeight = FontWeight.Bold); Text("${team.matchesCount} matches observed", color = Muted, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    if (data.teams.isNotEmpty()) item { JsonArraySummaryCard("Competition teams", data.teams, 30) }
                }
            }
            "stats" -> {
                if (data.trends.isEmpty()) item { EmptyState("League stats are limited", "PredIQ will populate competition-level trends as source coverage grows.", Icons.Outlined.Analytics) }
                else item { LeagueTrendCard(data.trends, expanded = true) }
            }
            "markets" -> {
                if (data.prediqStrengths.isNotEmpty()) {
                    item { SectionHeading("PredIQ strengths") }
                    items(data.prediqStrengths) { row ->
                        WhiteCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(humanize(row.string("market")), color = Ink, fontWeight = FontWeight.Bold); Text("${row.double("settled")?.toInt() ?: 0} settled", color = Muted) }
                                Text(row.double("hit_rate")?.let(::percent) ?: "–", color = GreenDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                if (data.trackRecord.isNotEmpty()) item { JsonArraySummaryCard("Track record", data.trackRecord, 12) }
                if (data.prediqStrengths.isEmpty() && data.trackRecord.isEmpty()) item { EmptyState("No market has enough settled history yet", "PredIQ waits for a meaningful sample before calling a league-market strength.", Icons.Outlined.QueryStats) }
            }
        }
    }
}

@Composable
private fun LeagueTrendCard(trends: JsonObject, expanded: Boolean = false) {
    WhiteCard {
        SectionHeading(if (expanded) "Competition statistics" else "Current trends")
        val preferred = listOf("avg_goals", "home_win_rate", "draw_rate", "btts_rate", "over25_rate", "avg_corners", "avg_cards", "favourite_win_rate", "market_sample")
        preferred.filter { it in trends }.take(if (expanded) 12 else 5).forEachIndexed { index, key ->
            val value = trends.double(key)
            val display = when {
                value == null -> jsonText(key, trends[key])
                key.endsWith("_rate") -> percent(value)
                key == "market_sample" -> value.toInt().toString()
                else -> "%.2f".format(value)
            }
            InfoRow(
                when (key) { "avg_goals" -> Icons.Outlined.SportsSoccer; "avg_corners" -> Icons.Outlined.Flag; "avg_cards" -> Icons.Outlined.Style; "home_win_rate" -> Icons.Outlined.Home; else -> Icons.Outlined.QueryStats },
                humanize(key), display,
            )
            if (index < preferred.filter { it in trends }.take(if (expanded) 12 else 5).lastIndex) HorizontalDivider(color = Hairline)
        }
    }
}

@Composable
fun FollowingContractScreen(
    state: PrediqContractState,
    onBack: () -> Unit,
    onUnfollow: (String) -> Unit,
    onUpdate: (String, V2FollowAlerts) -> Unit,
    onOpen: (V2Follow) -> Unit,
) {
    val follows = state.follows?.follows.orEmpty()
    var tab by rememberSaveable { mutableStateOf("all") }
    val visible = follows.filter { follow ->
        when (tab) {
            "matches" -> follow.entityType in setOf("event", "prediction")
            "teams" -> follow.entityType == "team"
            "players" -> follow.entityType == "player"
            "leagues" -> follow.entityType == "competition"
            else -> true
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Following", subtitle = "Control what PredIQ watches for you", showBack = true, onBack = onBack) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all", "matches", "teams", "players", "leagues")) { item ->
                    val count = follows.count { f -> when (item) { "matches" -> f.entityType in setOf("event","prediction"); "teams" -> f.entityType == "team"; "players" -> f.entityType == "player"; "leagues" -> f.entityType == "competition"; else -> true } }
                    FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text("${humanize(item)}  $count") })
                }
            }
        }
        if (visible.isEmpty()) item { EmptyState("Nothing followed here yet", "Open a match, team, player or league and tap Follow.", Icons.Outlined.BookmarkAdd) }
        else items(visible, key = { it.id }) { follow ->
            WhiteCard(onClick = { onOpen(follow) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (follow.entityType) { "team" -> Icons.Outlined.Shield; "player" -> Icons.Outlined.Person; "competition" -> Icons.Outlined.EmojiEvents; else -> Icons.Outlined.SportsSoccer }
                    Box(Modifier.size(44.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Purple) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(follow.entityLabel ?: follow.entityKey, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(when (follow.entityType) { "event", "prediction" -> "Match / prediction"; "competition" -> "League"; else -> humanize(follow.entityType) }, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onUnfollow(follow.id) }) { Icon(Icons.Outlined.DeleteOutline, "Unfollow", tint = Red) }
                }
                HorizontalDivider(color = Hairline)
                FollowAlertToggle("Prediction changes", follow.alerts.predictionChanges) { onUpdate(follow.id, follow.alerts.copy(predictionChanges = it)) }
                FollowAlertToggle("Lineups", follow.alerts.lineup) { onUpdate(follow.id, follow.alerts.copy(lineup = it)) }
                FollowAlertToggle("Live changes", follow.alerts.live) { onUpdate(follow.id, follow.alerts.copy(live = it)) }
                FollowAlertToggle("Result", follow.alerts.result) { onUpdate(follow.id, follow.alerts.copy(result = it)) }
                FollowAlertToggle("Team news", follow.alerts.teamNews) { onUpdate(follow.id, follow.alerts.copy(teamNews = it)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onOpen(follow) }) { Text("Open"); Icon(Icons.Outlined.ChevronRight, null) } }
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
    val context = LocalContext.current
    val repo = remember { AccountFeatureRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val quietPrefs = remember { context.getSharedPreferences("prediq_quiet_hours", android.content.Context.MODE_PRIVATE) }
    val current = state.notifications
    var settings by remember(current) { mutableStateOf(current) }
    var quietEnabled by remember { mutableStateOf(quietPrefs.getBoolean("enabled", false)) }
    var quietStart by remember { mutableStateOf(quietPrefs.getString("start", "22:00") ?: "22:00") }
    var quietEnd by remember { mutableStateOf(quietPrefs.getString("end", "07:00") ?: "07:00") }
    var availableLeagues by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedLeagues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var leagueBusy by remember { mutableStateOf(true) }
    var saveBusy by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { repo.leagueAlerts() }
            .onSuccess { availableLeagues = it.available; selectedLeagues = it.leagues.toSet(); leagueBusy = false }
            .onFailure { leagueBusy = false }
    }
    fun validTime(text: String): Boolean = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(text)
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Notifications", subtitle = "Only the changes that matter", showBack = true, onBack = onBack) }
        if (settings == null) {
            item { LoadingState("Loading notification preferences…") }
            return@LazyColumn
        }
        item {
            BrightCard {
                Icon(Icons.Outlined.NotificationsActive, null, tint = Lime, modifier = Modifier.size(38.dp))
                Text("Your attention is part of the product.", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Use push for urgent intelligence; keep longer account messages on email or WhatsApp.", color = Color.White.copy(alpha = .8f))
            }
        }
        item {
            WhiteCard {
                SectionHeading("Channels")
                ToggleRow("Push notifications", "Recommended for live and lineup changes", settings!!.pushEnabled) { settings = settings!!.copy(pushEnabled = it) }
                HorizontalDivider(color = Hairline)
                ToggleRow("WhatsApp", "Account and longer intelligence messages when available", settings!!.whatsappEnabled) { settings = settings!!.copy(whatsappEnabled = it) }
                HorizontalDivider(color = Hairline)
                ToggleRow("Email", "Longer updates and account notices", settings!!.emailEnabled) { settings = settings!!.copy(emailEnabled = it) }
                HorizontalDivider(color = Hairline)
                ToggleRow("SMS", "Use only where the service supports SMS delivery", settings!!.smsEnabled) { settings = settings!!.copy(smsEnabled = it) }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Pick & analysis")
                ToggleRow("Daily top picks", null, settings!!.alerts.dailyPicks) { settings = settings!!.copy(alerts = settings!!.alerts.copy(dailyPicks = it)) }
                ToggleRow("Prediction changes & new opportunities", null, settings!!.alerts.liveChanges) { settings = settings!!.copy(alerts = settings!!.alerts.copy(liveChanges = it)) }
                ToggleRow("Lineups confirmed", null, settings!!.alerts.lineupChanges) { settings = settings!!.copy(alerts = settings!!.alerts.copy(lineupChanges = it)) }
                ToggleRow("Results & settlement", null, settings!!.alerts.results) { settings = settings!!.copy(alerts = settings!!.alerts.copy(results = it)) }
                ToggleRow("Subscription notices", null, settings!!.alerts.subscription) { settings = settings!!.copy(alerts = settings!!.alerts.copy(subscription = it)) }
                Text("Team news alerts are controlled per team/match in Following, so they do not interrupt you globally.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            WhiteCard {
                SectionHeading("Quiet hours")
                ToggleRow("Pause non-urgent phone alerts", "Device-side quiet hours", quietEnabled) { quietEnabled = it }
                if (quietEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(quietStart, { quietStart = it.take(5) }, label = { Text("From") }, supportingText = { Text("24h time") }, singleLine = true, isError = !validTime(quietStart), modifier = Modifier.weight(1f))
                        OutlinedTextField(quietEnd, { quietEnd = it.take(5) }, label = { Text("Until") }, supportingText = { Text("24h time") }, singleLine = true, isError = !validTime(quietEnd), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("League alerts")
                if (leagueBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                else if (availableLeagues.isEmpty()) Text("No league-specific alert catalogue is available right now.", color = Muted)
                else {
                    Text("Choose leagues that deserve their own notification stream.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableLeagues.take(40), key = { it }) { league ->
                            FilterChip(
                                selected = league in selectedLeagues,
                                onClick = { selectedLeagues = if (league in selectedLeagues) selectedLeagues - league else selectedLeagues + league },
                                label = { Text(league, maxLines = 1) },
                            )
                        }
                    }
                    Text(if (selectedLeagues.isEmpty()) "No league-specific alerts selected" else "Following ${selectedLeagues.size} league alert${if (selectedLeagues.size == 1) "" else "s"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        saveMessage?.let { item { Text(it, color = GreenDeep, fontWeight = FontWeight.SemiBold) } }
        item {
            Button(
                onClick = {
                    if (quietEnabled && (!validTime(quietStart) || !validTime(quietEnd))) return@Button
                    quietPrefs.edit().putBoolean("enabled", quietEnabled).putString("start", quietStart).putString("end", quietEnd).apply()
                    saveBusy = true; saveMessage = null
                    scope.launch {
                        runCatching { repo.updateLeagueAlerts(selectedLeagues.sorted()) }
                        onSave(settings!!)
                        saveBusy = false; saveMessage = "Preferences saved"
                    }
                },
                enabled = !saveBusy && (!quietEnabled || (validTime(quietStart) && validTime(quietEnd))),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { if (saveBusy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Save preferences") }
        }
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
    onMarket: (String) -> Unit,
    onChance: (String) -> Unit,
    onValue: (String) -> Unit,
    onStatus: (String) -> Unit,
    onReset: () -> Unit,
) {
    val options = state.today?.filterOptions ?: state.live?.filterOptions ?: V2FilterOptions()
    val decisionCards = state.today?.let { it.topPicks + it.waiting }.orEmpty()
    val liveCards = state.live?.let { it.following + it.opportunities + it.games }.orEmpty()
    val markets = (decisionCards.mapNotNull { it.pick.market } + liveCards.mapNotNull { it.pick.market }).filter { it.isNotBlank() }.distinct().sorted()
    val statuses = (decisionCards.map { it.decision.code } + liveCards.map { it.decision.code }).filter { it.isNotBlank() }.distinct()
    val values = (decisionCards.map { it.value.status } + liveCards.map { it.value.status }).filter { it.isNotBlank() && it != "unpriced" }.distinct()
    val activeCount = listOf(state.selectedSport, state.selectedCompetition, state.selectedMarket, state.selectedChanceBand, state.selectedValueFilter, state.selectedStatusFilter).count { it.isNotBlank() } + if (state.followingOnly) 1 else 0
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Ivory) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 720.dp).navigationBarsPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Filters", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text(if (activeCount == 0) "Show what matters" else "$activeCount active filter${if (activeCount == 1) "" else "s"}", color = Muted) }
                    TextButton(onClick = onReset, enabled = activeCount > 0) { Text("Reset") }
                }
            }
            item {
                Text("Sports", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedSport.isBlank(), onClick = { onSport("") }, label = { Text("All") }) }
                    items(options.sports, key = { it.code }) { sport ->
                        FilterChip(selected = state.selectedSport == sport.code, onClick = { onSport(sport.code) }, label = { Text("${sport.label.ifBlank { humanize(sport.code) }}  ${sport.events}") })
                    }
                }
            }
            val competitionOptions = options.competitions.filter { state.selectedSport.isBlank() || it.sport == state.selectedSport }
            if (competitionOptions.isNotEmpty()) item {
                Text("Leagues", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedCompetition.isBlank(), onClick = { onCompetition("") }, label = { Text("All leagues") }) }
                    items(competitionOptions.take(40), key = { "${it.sport}-${it.name}" }) { competition ->
                        FilterChip(selected = state.selectedCompetition == competition.name, onClick = { onCompetition(competition.name) }, label = { Text(competition.name, maxLines = 1) })
                    }
                }
            }
            if (markets.isNotEmpty()) item {
                Text("Markets", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedMarket.isBlank(), onClick = { onMarket("") }, label = { Text("All markets") }) }
                    items(markets) { market -> FilterChip(selected = state.selectedMarket == market, onClick = { onMarket(market) }, label = { Text(humanize(market)) }) }
                }
            }
            item {
                Text("Chance", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("" to "All", "80+" to "80%+", "70-79" to "70–79%", "60-69" to "60–69%", "<60" to "<60%")) { (value, label) -> FilterChip(selected = state.selectedChanceBand == value, onClick = { onChance(value) }, label = { Text(label) }) }
                }
            }
            if (values.isNotEmpty()) item {
                Text("Value", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedValueFilter.isBlank(), onClick = { onValue("") }, label = { Text("All") }) }
                    items(values) { value -> FilterChip(selected = state.selectedValueFilter == value, onClick = { onValue(value) }, label = { Text(humanize(value)) }) }
                }
            }
            if (statuses.isNotEmpty()) item {
                Text("PredIQ status", color = Ink, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedStatusFilter.isBlank(), onClick = { onStatus("") }, label = { Text("All") }) }
                    items(statuses) { status -> FilterChip(selected = state.selectedStatusFilter == status, onClick = { onStatus(status) }, label = { Text(humanize(status)) }) }
                }
            }
            item {
                WhiteCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Bookmarks, null, tint = Purple)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text("Following only", color = Ink, fontWeight = FontWeight.SemiBold); Text("Limit this view to things you asked PredIQ to watch", color = Muted, style = MaterialTheme.typography.bodySmall) }
                        Switch(state.followingOnly, onCheckedChange = onFollowing)
                    }
                }
            }
            item { Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Show results") } }
        }
    }
}

@Composable
fun UpcomingContractScreen(state: PrediqContractState, onBack: () -> Unit, onOpenV3: (String) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("today") }
    val today = LocalDate.now()
    val v3 = state.v3Slate?.cards.orEmpty()
    val future = v3.filter { card ->
        val date = localEventDate(card.event.startsAt) ?: return@filter false
        when (tab) {
            "tomorrow" -> date == today.plusDays(1)
            "week" -> !date.isBefore(today) && !date.isAfter(today.plusDays(7))
            else -> date == today
        }
    }
    val v2Today = state.today?.upcoming.orEmpty()
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PrediqHeader(title = "Upcoming", subtitle = "Plan what deserves your attention", showBack = true, onBack = onBack) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("today" to "Today", "tomorrow" to "Tomorrow", "week" to "This week")) { (value, label) ->
                    FilterChip(selected = tab == value, onClick = { tab = value }, label = { Text(label) })
                }
            }
        }
        if (tab == "today" && v2Today.isNotEmpty()) {
            items(v2Today) { row -> WhiteCard { UpcomingRowDetail(row) } }
        } else if (future.isEmpty()) {
            item { EmptyState("No upcoming events in this view", if (state.v3Slate == null) "PredIQ is still loading the wider slate." else "Try another date window.", Icons.Outlined.CalendarMonth) }
        } else {
            items(future, key = { it.event.id }) { card -> V3UpcomingCard(card) { onOpenV3(card.event.id) } }
        }
        item {
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = Purple)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Want an alert for a specific match?", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Open the match and tap Follow to choose what PredIQ should watch.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun V3UpcomingCard(card: V3SlateCard, onOpen: () -> Unit) {
    WhiteCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamCrest(card.event.home, card.event.sportCode, size = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${card.event.home} vs ${card.event.away}", color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${card.event.competition} · ${friendlyDateTime(card.event.startsAt)}", color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill(humanize(card.status), decisionTone(card.status))
        }
        card.primary?.let { primary ->
            HorizontalDivider(color = Hairline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(primary.selectionLabel, color = PurpleDeep, fontWeight = FontWeight.Bold); Text(humanize(primary.marketLabel.ifBlank { primary.marketKey }), color = Muted, style = MaterialTheme.typography.bodySmall) }
                Text("${(primary.probability * 100).toInt()}%", color = GreenDeep, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun localEventDate(raw: String?): LocalDate? = runCatching {
    OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
}.getOrNull()

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContractScreen(state: PrediqContractState, onBack: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AccountFeatureRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val profile = state.account?.profile
    var editing by remember { mutableStateOf(false) }
    var name by remember(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var country by remember(profile?.id) { mutableStateOf(profile?.country ?: "UG") }
    var currency by remember(profile?.id) { mutableStateOf(profile?.currency ?: "UGX") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Profile & settings", subtitle = "Your PredIQ display and regional preferences", showBack = true, onBack = onBack) }
        if (profile == null) item { LoadingState() }
        else {
            item {
                BrightCard {
                    EntityAvatar(profile.name ?: profile.email, true, Modifier.size(68.dp))
                    Text(profile.name ?: "PredIQ member", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(profile.email, color = Color.White.copy(alpha = .8f))
                    Button(onClick = { editing = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PurpleDeep)) { Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit profile") }
                }
            }
            item {
                WhiteCard {
                    SectionHeading("Region")
                    InfoRow(Icons.Outlined.Public, "Country", profile.country ?: "Not set")
                    InfoRow(Icons.Outlined.Payments, "Currency", profile.currency ?: "Not set")
                    InfoRow(Icons.Outlined.Schedule, "Timezone", state.notifications?.timezone ?: "Africa/Kampala")
                }
            }
            item {
                WhiteCard {
                    SectionHeading("Account & security")
                    InfoRow(Icons.Outlined.AccountCircle, "Tuku identity", "Shared sign-in across the Tuku estate")
                    Text("Password recovery and security controls stay with your Tuku identity so PredIQ does not create a second account system.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (editing && profile != null) {
        ModalBottomSheet(onDismissRequest = { if (!busy) editing = false }, containerColor = Ivory) {
            Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit profile", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(country, { country = it.uppercase().filter(Char::isLetter).take(2) }, label = { Text("Country code") }, supportingText = { Text("e.g. UG") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(currency, { currency = it.uppercase().filter(Char::isLetter).take(3) }, label = { Text("Currency") }, supportingText = { Text("e.g. UGX") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                error?.let { Text(it, color = Red) }
                Button(
                    onClick = {
                        if (country.length != 2 || currency.length != 3) { error = "Use a 2-letter country code and a 3-letter currency code."; return@Button }
                        busy = true; error = null
                        scope.launch {
                            runCatching { repo.updateProfile(name, country, currency) }
                                .onSuccess { busy = false; editing = false; onRefresh() }
                                .onFailure { busy = false; error = it.message ?: "Profile could not be saved" }
                        }
                    },
                    enabled = !busy && country.length == 2 && currency.length == 3,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Save profile") }
            }
        }
    }
}

@Composable
fun HelpContractScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf("basics") }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Help & methodology", subtitle = "Understand the signal before acting on it", showBack = true, onBack = onBack) }
        item {
            BrightCard(brush = HeroBrush) {
                Icon(Icons.Outlined.Psychology, null, tint = Lime, modifier = Modifier.size(38.dp))
                Text("PredIQ is decision support, not certainty.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Chance is likelihood. Evidence is how well the estimate is supported. Value only appears when a usable market comparison exists.", color = Color.White.copy(alpha = .84f))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("basics", "method", "responsible", "support")) { item ->
                    FilterChip(selected = section == item, onClick = { section = item }, label = { Text(when(item) { "method" -> "Method"; "responsible" -> "Play safely"; else -> humanize(item) }) })
                }
            }
        }
        when (section) {
            "basics" -> item {
                WhiteCard {
                    SectionHeading("Reading a PredIQ call")
                    InfoRow(Icons.Outlined.Percent, "Chance", "Estimated likelihood of the stated outcome. It is never a guarantee.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Verified, "Evidence", "How much relevant, reliable information supports the estimate.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.WarningAmber, "Risk", "How fragile the call is to uncertainty, context or model disagreement.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.ShowChart, "Reference odds", "A neutral market benchmark for comparison, not an executable bookmaker offer.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.AutoAwesome, "PredIQ view", "The single user-facing assessment produced after the intelligence layer reconciles the inputs.")
                }
            }
            "method" -> item {
                WhiteCard {
                    SectionHeading("How the intelligence layer works")
                    Text("PredIQ normalises independent sports, team, player, competition, context and market signals before promoting a call. The Android app shows the consolidated assessment rather than raw feeds.", color = Ink)
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.FactCheck, "Published record", "First-publication calls remain visible after settlement so later updates cannot rewrite the track record.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.SwapVert, "Safer / higher return", "Alternatives are shown only when the intelligence contract supports them.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.AccountTree, "Odds Builder", "Target odds and risk appetite guide the builder; combined probability is an approximation and legs are not independent guarantees.")
                }
            }
            "responsible" -> item {
                WhiteCard {
                    SectionHeading("Use PredIQ responsibly")
                    InfoRow(Icons.Outlined.MoneyOff, "Set a budget", "Decide what you can afford to lose before placing any bet.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Replay, "Do not chase", "A previous loss does not make the next outcome more likely to win.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Timer, "Take breaks", "If gambling stops feeling recreational, stop and step away.")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Block, "No certainty language", "PredIQ probabilities and tickets are analytical estimates, not guaranteed returns.")
                }
            }
            else -> item {
                WhiteCard {
                    SectionHeading("Support & feedback")
                    InfoRow(Icons.Outlined.Language, "PredIQ website", "Open getprediq.site", onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://getprediq.site")))
                    })
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Feedback, "Send feedback", "Share a problem, idea or screenshot through an app on your phone", onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "PredIQ Android feedback")
                            putExtra(Intent.EXTRA_TEXT, "PredIQ Android feedback:\n\n")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send PredIQ feedback"))
                    })
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Security, "Account security", "Password recovery and identity security are managed by Tuku sign-in.")
                }
            }
        }
    }
}

@Composable
fun PlanContractScreen(plans: List<PlanDto>, state: PrediqContractState, onBack: () -> Unit, onChoose: (PlanDto) -> Unit) {
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
            WhiteCard(onClick = { onChoose(plan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.WorkspacePremium, null, tint = Purple) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(plan.name, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${plan.durationDays} days full access", color = Muted)
                    }
                    Text("UGX ${"%,d".format(plan.priceUgx)}", color = PurpleDeep, fontWeight = FontWeight.ExtraBold)
                }
                Button(onClick = { onChoose(plan) }, modifier = Modifier.fillMaxWidth()) { Text(if (state.account?.membership?.fullAccess == true) "Extend access" else "Choose plan") }
            }
        }
    }
}

@Composable
fun PaymentsContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AccountFeatureRepository(context.applicationContext) }
    var history by remember { mutableStateOf<List<PaymentHistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var historyError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { repo.paymentHistory() }
            .onSuccess { history = it.payments; loading = false }
            .onFailure { historyError = it.message ?: "Payment history could not load"; loading = false }
    }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Payments & receipts", subtitle = "Your confirmed and pending PredIQ transactions", showBack = true, onBack = onBack) }
        item {
            BrightCard {
                Text("ACCESS & BILLING", color = Lime, fontWeight = FontWeight.Bold)
                Text(state.account?.membership?.planName ?: "PredIQ access", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(state.account?.membership?.daysRemaining?.let { "$it days remaining" } ?: humanize(state.account?.membership?.state), color = Color.White.copy(alpha = .82f))
            }
        }
        val summary = state.account?.paymentsSummary ?: JsonObject(emptyMap())
        if (summary.isNotEmpty()) item { JsonSummaryCard("Payment summary", summary, 10) }
        item { SectionHeading("Transaction history") }
        if (loading) item { LoadingState("Loading transactions…") }
        historyError?.let { message -> item { EmptyState("Payment history unavailable", message, Icons.Outlined.ReceiptLong) } }
        if (!loading && historyError == null && history.isEmpty()) item { EmptyState("No payments yet", "Confirmed and pending transactions will appear here.", Icons.Outlined.ReceiptLong) }
        items(history, key = { it.id }) { payment ->
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background((if (payment.status.equals("settled", true) || payment.status.equals("completed", true)) Green else Purple).copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (payment.status.equals("settled", true) || payment.status.equals("completed", true)) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule, null, tint = if (payment.status.equals("settled", true) || payment.status.equals("completed", true)) GreenDeep else Purple)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(payment.planName.ifBlank { humanize(payment.planCode) }, color = Ink, fontWeight = FontWeight.Bold)
                        Text(friendlyDateTime(payment.settledAt ?: payment.createdAt), color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("UGX ${"%,d".format(payment.amountUgx)}", color = Ink, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(humanize(payment.status), if (payment.status.lowercase() in setOf("settled","completed","successful")) "good" else if (payment.status.lowercase() in setOf("failed","cancelled")) "bad" else "warn")
                    payment.provider.takeIf { it.isNotBlank() }?.let { StatusPill(humanize(it)) }
                }
                payment.providerReference?.takeIf { it.isNotBlank() }?.let { Text("Reference $it", color = Muted, style = MaterialTheme.typography.bodySmall) }
                payment.failureReason?.takeIf { it.isNotBlank() }?.let { Text(it, color = Red, style = MaterialTheme.typography.bodySmall) }
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
                .joinToString(" · ") { "${humanize(it.key)} ${jsonText(it.key, it.value)}" }
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
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Text(humanize(key), color = Muted, modifier = Modifier.weight(.42f).padding(end = 12.dp), maxLines = 2)
            Text(jsonText(key, element), color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(.58f), maxLines = 4, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
        }
        if (index < rows.lastIndex) HorizontalDivider(color = Hairline)
    }
}

private fun jsonText(key: String, element: JsonElement?): String {
    val primitive = element as? JsonPrimitive ?: return if (element == null) "–" else element.toString()
    val raw = primitive.contentOrNull?.takeUnless { it.equals("null", ignoreCase = true) }?.trim().orEmpty()
    if (raw.isBlank()) return "–"
    return if (key.endsWith("_at") || key in setOf("as_of", "valid_until", "generated_at", "updated_at", "expires_at")) friendlyDateTime(raw) else raw
}

private fun jsonText(element: JsonElement?): String = jsonText("", element)

fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
