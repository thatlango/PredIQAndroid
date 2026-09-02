package com.getprediq.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getprediq.app.PrediqUiState
import com.getprediq.app.PrediqViewModel
import com.getprediq.app.data.*
import com.getprediq.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun TodayScreen(state: PrediqUiState, vm: PrediqViewModel, onAuth: () -> Unit, onMatch: (String) -> Unit, onLeagueWinners: () -> Unit) {
    val rankedAssessments = remember(state.assessments) {
        state.assessments.sortedWith(
            compareByDescending<AssessmentDto> { it.promotable }
                .thenByDescending { it.confidence ?: 0.0 }
                .thenByDescending { it.probability ?: 0.0 }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { DailyIntelligenceHero(state) }
        item { SportChips(primarySports(state.filterOptions.sports), state.selectedSport, vm::selectSport) }
        item {
            Row(Modifier.fillMaxWidth().background(PrediqSurfaceLow, RoundedCornerShape(16.dp)).padding(4.dp)) {
                TodaySegment("Today", state.todayMode == "today", Modifier.weight(1f)) { vm.setTodayMode("today") }
                TodaySegment("Upcoming", state.todayMode == "upcoming", Modifier.weight(1f)) { vm.setTodayMode("upcoming") }
            }
        }

        if (state.todayMode == "today" && state.picks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Highest-conviction calls", style = MaterialTheme.typography.titleLarge)
                    Text("PredIQ only promotes calls that clear its evidence and confidence gates.", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            items(state.picks.take(2), key = { "pick-${it.eventId}" }) { pick ->
                PickFeatureCard(pick) { if (state.account == null) onAuth() else onMatch(pick.eventId) }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLeagueWinners),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0FF)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Color.White, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EmojiEvents, null, tint = PrediqBlue) }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("League intelligence", style = MaterialTheme.typography.titleMedium, color = PrediqBlue, fontWeight = FontWeight.Bold)
                        Text("Title probabilities and competition-specific context", color = Color(0xFF53657A), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = PrediqBlue)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (state.todayMode == "upcoming") "Upcoming intelligence" else "More assessed opportunities", style = MaterialTheme.typography.titleLarge)
                Text("Ranked by promotion status, confidence and model probability—not by fixture popularity.", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        when {
            state.loadingToday -> item { StateCard("Analysing current games…", "PredIQ is combining the latest match, competition, market and team context.") }
            state.todayError != null -> item { StateCard("Could not refresh intelligence", state.todayError, error = true, action = "Retry", onAction = vm::loadToday) }
            !vm.fullAccess -> item { StateCard("Open the full intelligence layer", "Start a trial or activate a plan to see ranked assessments, risks, evidence and match-level analysis.", action = if (state.account == null) "Start trial" else "View Account", onAction = onAuth) }
            rankedAssessments.isEmpty() -> item { StateCard("No strong assessment in this view", "PredIQ will not fill the list with weak or unrelated picks. Try another sport or check Upcoming.") }
            else -> items(rankedAssessments.take(30), key = { it.eventId }) { assessment -> AssessmentCard(assessment) { onMatch(assessment.eventId) } }
        }
    }
}

@Composable
private fun DailyIntelligenceHero(state: PrediqUiState) {
    val promoted = state.assessments.count { it.promotable }
    val analysed = state.assessments.size
    val best = (state.picks.mapNotNull { it.confidence ?: it.probability } + state.assessments.mapNotNull { it.confidence }).maxOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrediqBlue),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(Color.White.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoGraph, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("PREDIQ DAILY BRIEFING", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(greeting(state.account?.user?.displayName), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                SubscriptionPill(state.account?.subscriptionProgress)
            }
            Text(
                if (promoted > 0) "$promoted call${if (promoted == 1) "" else "s"} currently clear the promotion gate. Start there." else "PredIQ is scanning the slate. Strong evidence comes before volume.",
                color = Color.White.copy(alpha = .92f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BriefMetric("STRONG", promoted.toString(), Modifier.weight(1f), highlight = true)
                BriefMetric("ASSESSED", analysed.toString(), Modifier.weight(1f))
                BriefMetric("BEST CONF.", probability(best), Modifier.weight(1f))
            }
            Text("${if (state.selectedSport.isBlank()) "All tracked sports" else prettySport(state.selectedSport)} · probability + confidence + risk + freshness", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BriefMetric(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Surface(modifier = modifier, color = if (highlight) PrediqLiveLime else Color.White.copy(alpha = .11f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(label, color = if (highlight) PrediqLiveInk else Color.White.copy(alpha = .68f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = if (highlight) PrediqLiveInk else Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TodaySegment(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 44.dp), color = if (active) Color.White else Color.Transparent, shape = RoundedCornerShape(12.dp), shadowElevation = if (active) 1.dp else 0.dp) {
        Box(contentAlignment = Alignment.Center) { Text(label, color = if (active) PrediqBlue else PrediqMuted, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun LiveScreen(state: PrediqUiState, vm: PrediqViewModel, onAuth: () -> Unit, onMatch: (String) -> Unit) {
    var bestOnly by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) { delay(300_000); vm.loadLive() }
    }
    val live = state.live
    val filtered = live?.games.orEmpty()
        .filter { (state.selectedSport.isBlank() || it.sportCode == state.selectedSport) && (!bestOnly || it.analysisPromotable) }
        .sortedWith(compareByDescending<LiveGameDto> { it.analysisPromotable }.thenByDescending { it.confidence ?: it.probability ?: 0.0 })

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LiveOverviewHero(live, state.loadingLive, vm::loadLive) }
        item { SportChips(primarySports(state.filterOptions.sports), state.selectedSport, vm::selectSport) }
        item {
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(5.dp)) {
                ToggleSegment("All live", !bestOnly, Modifier.weight(1f)) { bestOnly = false }
                ToggleSegment("Strong calls", bestOnly, Modifier.weight(1f)) { bestOnly = true }
            }
        }

        if (state.liveError != null) {
            item {
                StateCard(
                    if (live != null) "Using last-known live intelligence" else "Live could not refresh",
                    state.liveError,
                    error = live == null,
                    cached = live != null,
                    action = "Retry",
                    onAction = vm::loadLive,
                )
            }
        } else if (live?.state == "cached") {
            item { StateCard("Fresh data is catching up", live.message, cached = true, action = "Refresh", onAction = vm::loadLive) }
        } else if (live?.state == "service_error") {
            item { StateCard("Live is temporarily unavailable", live.message, error = true, action = "Retry", onAction = vm::loadLive) }
        }

        if (state.account == null && (live?.liveCount ?: 0) > 0) {
            item { StateCard("Live intelligence is active", "Sign in to open current probabilities, reasons, risks, movement and match-level analysis.", action = "Sign in", onAction = onAuth) }
        }

        if (vm.fullAccess) {
            if (filtered.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(if (bestOnly) "Strong live calls" else "Live intelligence queue", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (bestOnly) "Only calls clearing PredIQ's promotion gate right now." else "Promoted calls are ranked first; monitored matches follow.",
                                color = PrediqMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(color = PrediqSurfaceLow, shape = CircleShape) { Text(filtered.size.toString(), Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = PrediqBlue, fontWeight = FontWeight.Bold) }
                    }
                }
                items(filtered.take(20), key = { it.eventId }) { game -> LiveMatchCard(game) { onMatch(game.eventId) } }
            } else if (!state.loadingLive && state.liveError == null) {
                item {
                    StateCard(
                        if (bestOnly) "No strong live calls" else "No tracked live games",
                        if (bestOnly) "PredIQ is tracking live fixtures but none currently clear the evidence and confidence gates." else "No tracked event is underway right now. PredIQ will populate this screen automatically when live fixtures begin.",
                    )
                }
            }
        }
    }
}

@Composable
fun ResultsScreen(state: PrediqUiState, vm: PrediqViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ResultsTrustHero(state.resultsDashboard) }
        item { SportChips(primarySports(state.filterOptions.sports), state.selectedSport, vm::selectSport) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Filter the record", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("" to "All", "won" to "Won", "lost" to "Lost", "void" to "Void", "pending" to "Pending")) { pair ->
                        FilterChip(selected = state.resultOutcome == pair.first, onClick = { vm.setResultOutcome(pair.first) }, label = { Text(pair.second) }, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { days -> FilterChip(selected = state.resultDays == days, onClick = { vm.setResultDays(days) }, label = { Text("${days}D") }, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp)) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Graded predictions", style = MaterialTheme.typography.titleLarge)
                Text("Wins, losses and voids remain visible. PredIQ does not remove misses from the record.", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        when {
            state.loadingResults -> item { StateCard("Loading graded predictions…", "PredIQ is reconciling finished events with the original recorded calls.") }
            state.resultError != null -> item { StateCard("Results could not refresh", state.resultError, error = true, action = "Retry", onAction = vm::loadResults) }
            state.results.isEmpty() -> item { StateCard("No results in this view", "Try another time window, sport or outcome filter.") }
            else -> items(state.results, key = { "result-${it.predictionId}-${it.startsAt}" }) { ResultCard(it) }
        }
    }
}

@Composable
private fun ResultsTrustHero(dashboard: ResultsDashboard) {
    val today = dashboard.today
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrediqLiveInk),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FactCheck, null, tint = PrediqLiveLime)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("TRACK RECORD", color = PrediqLiveLime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Accuracy you can audit", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text("Every graded call stays in the record—successful or not.", color = PrediqLiveMuted, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ResultsMetric("30D TOP PICKS", probability(dashboard.topPicks30d.accuracy), "${dashboard.topPicks30d.graded} graded", Modifier.weight(1.15f), highlight = true)
                ResultsMetric("TODAY", "${today.wins}W · ${today.losses}L", "${today.voids} void · ${today.graded} graded", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultsMetric(label: String, value: String, sub: String, modifier: Modifier, highlight: Boolean = false) {
    Surface(modifier = modifier, color = if (highlight) PrediqLiveLime else PrediqLiveCardAlt, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = if (highlight) PrediqLiveInk else PrediqLiveMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, color = if (highlight) PrediqLiveInk else Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(sub, color = if (highlight) PrediqLiveInk.copy(alpha = .7f) else PrediqLiveMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AccountScreen(state: PrediqUiState, vm: PrediqViewModel, onAuth: () -> Unit, onPlan: (PlanDto) -> Unit, onNotifications: () -> Unit, onResponsible: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Column(verticalArrangement=Arrangement.spacedBy(5.dp)){ Text("Start with seven days on us.", style = MaterialTheme.typography.headlineMedium); Text("Everything is included across web and Android.",color=PrediqMuted) } }
        val account = state.account
        if (account == null) {
            item { PrediqCard(Modifier.fillMaxWidth()) { Icon(Icons.Outlined.LockOpen, null, tint = PrediqBlue, modifier = Modifier.size(52.dp)); Text("Seven days of full access", style = MaterialTheme.typography.titleLarge); Text("No limited preview. Create one Tuku account and open every sport, live view, result and intelligence screen.", color = PrediqMuted); Button(onClick = onAuth, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start 7-day trial") }; Text("Plans start at UGX 15,000. Pay now and paid time starts after your trial.",color=PrediqMuted,style=MaterialTheme.typography.bodySmall) } }
        } else {
            item {
                PrediqCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(58.dp).background(PrediqSurfaceLow, CircleShape), contentAlignment = Alignment.Center) { Text(teamInitials(account.user.displayName ?: account.user.email), fontWeight = FontWeight.Bold, color = PrediqBlue) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(account.user.displayName ?: "PredIQ member", style = MaterialTheme.typography.titleLarge); Text(account.user.email, color = PrediqMuted); Text(subscriptionLabel(account), color = PrediqBlue, fontWeight = FontWeight.SemiBold) } }
                    account.subscriptionProgress?.let { progress -> Box(Modifier.fillMaxWidth().height(5.dp).background(PrediqSurfaceLow, CircleShape)) { Box(Modifier.fillMaxWidth(progress.fractionRemaining.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(PrediqBlue, CircleShape)) } }
                }
            }
            if (!account.access.subscriptionBypass) {
                item { PrediqSectionTitle(if (account.subscriptionState == "trial") "Pay now—your trial stays intact" else if (account.access.fullSelections) "Extend Subscription" else "Choose a Plan") }
                items(state.plans, key = { it.code }) { plan -> PlanCard(plan, account.access.fullSelections) { onPlan(plan) } }
                if (!state.paymentCapabilities.mobileMoney) item { StateCard("Mobile money is not active yet", state.paymentCapabilities.message) }
            }
            state.affiliate?.let { affiliate ->
                item { PrediqSectionTitle("Recommend PredIQ") }
                item { PrediqCard(Modifier.fillMaxWidth()) { Text("Earn ${(affiliate.commissionRate*100).toInt()}% for ${affiliate.commissionMonths} months",style=MaterialTheme.typography.titleLarge); Text("Share your link and earn on every settled payment from each referred customer.",color=PrediqMuted); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ SummaryMetric("REFERRED",affiliate.referrals.toString(),"${affiliate.conversions} converted",Modifier.weight(1f)); SummaryMetric("AVAILABLE",ugx(affiliate.availableUgx),"${affiliate.holdingDays}-day hold",Modifier.weight(1f),highlight=true) }; Button(onClick={ val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"Try PredIQ with 7 days of full access: ${affiliate.shareUrl}")};context.startActivity(Intent.createChooser(intent,"Share PredIQ")) },modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Share,null);Spacer(Modifier.width(8.dp));Text("Share referral link")}; Text("Minimum payout ${ugx(affiliate.minimumPayoutUgx)}",color=PrediqMuted,style=MaterialTheme.typography.bodySmall) } }
            }
            item {
                PrediqCard(Modifier.fillMaxWidth()) {
                    AccountAction(Icons.Outlined.Notifications, "Notification Settings", onNotifications)
                    HorizontalDivider(color = Color(0xFFEEEEF0))
                    AccountAction(Icons.Outlined.HealthAndSafety, "Responsible Use", onResponsible)
                    HorizontalDivider(color = Color(0xFFEEEEF0))
                    AccountAction(Icons.Outlined.Logout, "Log Out", vm::logout, danger = true)
                }
            }
        }
    }
}

@Composable
fun MatchIntelligenceScreen(data: MatchIntelligenceResponse?, onBack: () -> Unit) {
    val context = LocalContext.current
    if (data == null) {
        Column(Modifier.fillMaxSize().background(PrediqBackground).padding(20.dp)) {
            BackHeader("Match Intelligence", onBack)
            Spacer(Modifier.height(20.dp))
            StateCard("Building match intelligence…", "PredIQ is resolving the latest assessment, evidence, risk and supporting context.")
        }
        return
    }
    val event = data.event
    val assessment = data.assessment
    val riskLabel = assessment.riskLevel?.replace('_', ' ')?.replaceFirstChar(Char::uppercase) ?: "Not stated"
    val riskColor = when (assessment.riskLevel?.lowercase()) { "low" -> PrediqGreen; "high", "very_high" -> PrediqRed; else -> PrediqAmber }
    LazyColumn(
        Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 50.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Match Intelligence", onBack, onShare = data.share.text?.let { text -> { val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "$text\n${data.share.url.orEmpty()}") }; context.startActivity(Intent.createChooser(intent, "Share PredIQ assessment")) } }) }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompetitionMark(event.competition, event.sportCode, 28.dp)
                    if (!event.competition.isNullOrBlank()) Spacer(Modifier.width(8.dp))
                    Text("${event.competition ?: prettySport(event.sportCode)} • ${kickoff(event.startsAt)}", color = PrediqMuted, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        TeamCrest(event.homeParticipant, event.sportCode, 68.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(compactTeamName(event.homeParticipant, event.sportCode), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                    Text("VS", color = PrediqMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        TeamCrest(event.awayParticipant, event.sportCode, 68.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(compactTeamName(event.awayParticipant, event.sportCode), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PrediqLiveInk), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("CURRENT PREDIQ CALL", color = PrediqLiveLime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(assessment.selectionLabel ?: "Current assessment", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(marketName(assessment.marketKey), color = PrediqLiveMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill(confidenceBand(assessment.confidence), PrediqLiveInk, PrediqLiveLime)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        MatchMetric("PROBABILITY", probability(assessment.probability), Color.White, Modifier.weight(1f))
                        MatchMetric("CONFIDENCE", probability(assessment.confidence), PrediqLiveLime, Modifier.weight(1f))
                        MatchMetric("RISK", riskLabel, if (riskColor == PrediqRed) Color(0xFFFFA8A2) else Color(0xFFFFD08A), Modifier.weight(1f))
                    }
                    Text("Probability is the model's estimated chance. Confidence reflects how strongly the available evidence supports publishing this call.", color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (assessment.why.isNotEmpty()) {
            item {
                PrediqCard(Modifier.fillMaxWidth()) {
                    Text("Why PredIQ sees it", style = MaterialTheme.typography.titleMedium)
                    IndicatorList(assessment.why.take(5))
                }
            }
        }
        if (assessment.watchOuts.isNotEmpty()) {
            item { InsightCard(Icons.Outlined.WarningAmber, "What can break the call", assessment.watchOuts.joinToString(" • "), PrediqAmber) }
        }
        assessment.changeReason?.let { reason -> item { InsightCard(Icons.Outlined.Update, "What changed", reason, PrediqBlue) } }
        if (data.assessmentHistory.size > 1) {
            item { InsightCard(Icons.Outlined.History, "Re-analysis history", "PredIQ has recorded ${data.assessmentHistory.size} assessment snapshots for this event. The current call reflects the latest available evidence.", PrediqBlue) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormCard(event.homeParticipant, formString(data.teamForm, "home"), Modifier.weight(1f))
                FormCard(event.awayParticipant, formString(data.teamForm, "away"), Modifier.weight(1f))
            }
        }
        data.marketSignal?.let { signal -> item { InsightCard(Icons.Outlined.TrendingUp, "Market signal", marketSignalText(signal), PrediqBlue) } }
        data.leagueContext?.takeIf { it.isNotEmpty() }?.let { contextData ->
            item { InsightCard(Icons.Outlined.Leaderboard, "Competition context", "This assessment includes ${contextData.keys.size} competition-level context dimension${if (contextData.keys.size == 1) "" else "s"} alongside match and team evidence.", PrediqBlue) }
        }
        item { InsightCard(Icons.Outlined.Analytics, "PredIQ history in ${event.competition ?: "this competition"}", if (data.prediqHistory.accuracy == null) "Not enough graded history yet to claim a reliable competition-specific accuracy rate." else "${probability(data.prediqHistory.accuracy)} accuracy across ${data.prediqHistory.graded} graded predictions.", PrediqBlue) }
        item { InsightCard(Icons.Outlined.Groups, "Lineups", if (data.lineups.confirmed) "Confirmed lineups are included in this assessment." else "Lineups are not yet confirmed. PredIQ will re-analyse when they arrive.", if (data.lineups.confirmed) PrediqGreen else PrediqMuted) }
        item { Text("Last analysed ${relativeTime(assessment.lastAnalysedAt)}", color = PrediqMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun MatchMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = PrediqLiveCardAlt, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

private fun confidenceBand(value: Double?): String = when {
    value == null -> "UNRATED"
    value >= .80 -> "TOP"
    value >= .70 -> "HIGH"
    value >= .58 -> "MODERATE"
    else -> "CAUTIOUS"
}

@Composable
fun LeagueWinnersScreen(leagues: List<LeagueForecast>, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(PrediqBackground), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 50.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { BackHeader("League Winner Predictions", onBack) }
        if (leagues.isEmpty()) item { StateCard("League forecasts are being prepared", "PredIQ needs enough current competition history before it publishes a title probability.") }
        items(leagues, key = { "${it.competition}-${it.season}" }) { league ->
            PrediqCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color(0xFFEAF0FF), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EmojiEvents, null, tint = PrediqBlue) }; Spacer(Modifier.width(12.dp)); Column { Text(league.competition, style = MaterialTheme.typography.titleLarge); Text(league.season, color = PrediqMuted) } }
                HorizontalDivider(color = Color(0xFFEEEEF0))
                league.predictions.take(6).forEachIndexed { index, team ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", color = PrediqMuted, modifier = Modifier.width(24.dp)); TeamBadge(team.team, Modifier.size(34.dp)); Spacer(Modifier.width(10.dp)); Text(team.team, Modifier.weight(1f), fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal); Text(probability(team.probability), color = if (index == 0) PrediqBlue else PrediqMuted, fontWeight = FontWeight.Bold) }
                }
                league.insight?.let { Surface(color = Color(0xFFEAF0FF), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(13.dp)) { Icon(Icons.Outlined.Lightbulb, null, tint = PrediqBlue); Spacer(Modifier.width(8.dp)); Text(it, color = Color(0xFF294574), modifier = Modifier.weight(1f)) } } }
                Text("PredIQ probabilities only. Sportsbook title odds are shown only when an authorised pricing source is available.", color = PrediqMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable private fun ModeTab(label: String, active: Boolean, onClick: () -> Unit) { Column(Modifier.clickable(onClick = onClick).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = if (active) PrediqBlue else PrediqMuted, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); Box(Modifier.width(50.dp).height(2.dp).background(if (active) PrediqBlue else Color.Transparent)) } }
@Composable private fun ToggleSegment(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) { Surface(modifier.clickable(onClick = onClick), color = if (active) PrediqLiveInk else Color.Transparent, shape = RoundedCornerShape(14.dp), shadowElevation = 0.dp) { Box(Modifier.heightIn(min = 46.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(label, color = if (active) PrediqLiveLime else PrediqMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) } } }
@Composable private fun SummaryMetric(label: String, value: String, sub: String, modifier: Modifier, highlight: Boolean = false) { Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (highlight) PrediqBlue else Color.White)) { Column(Modifier.padding(16.dp).heightIn(min = 108.dp), verticalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (highlight) Color(0xFFDAE1FF) else PrediqMuted); Text(value, style = MaterialTheme.typography.headlineMedium, color = if (highlight) Color.White else PrediqBlue); Text(sub, fontSize = 12.sp, color = if (highlight) Color(0xFFDAE1FF) else PrediqMuted) } } }
@Composable private fun PlanCard(plan: PlanDto, active: Boolean, onClick: () -> Unit) { PrediqCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(plan.name, style = MaterialTheme.typography.titleLarge); Text("Full access for ${plan.durationDays} day${if (plan.durationDays == 1) "" else "s"}", color = PrediqMuted) }; Text(ugx(plan.priceUgx), color = PrediqBlue, fontWeight = FontWeight.Bold) }; Button(onClick = onClick, enabled = true, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) { Text(if (active) "Extend ${plan.name}" else "Subscribe ${plan.name}") } } }
@Composable private fun AccountAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit, danger: Boolean = false) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (danger) PrediqRed else PrediqMuted); Spacer(Modifier.width(14.dp)); Text(title, Modifier.weight(1f), color = if (danger) PrediqRed else MaterialTheme.colorScheme.onSurface); if (!danger) Icon(Icons.Outlined.ChevronRight, null, tint = PrediqMuted) } }
@Composable private fun BackHeader(title: String, onBack: () -> Unit, onShare: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = PrediqBlue) }; Text(title, style = MaterialTheme.typography.titleLarge, color = PrediqBlue, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); if (onShare != null) IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, "Share", tint = PrediqBlue) } else Spacer(Modifier.width(48.dp)) } }
@Composable private fun FormCard(team: String, form: String, modifier: Modifier) { PrediqCard(modifier) { Text(team, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("Last 5", fontSize = 11.sp, color = PrediqMuted); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { val values = form.split(' ').filter { it.isNotBlank() }.take(5); if (values.isEmpty()) Text("Limited history", color = PrediqMuted, fontSize = 12.sp) else values.forEach { value -> val color = when (value) { "W" -> PrediqGreen; "L" -> PrediqRed; else -> PrediqMuted }; Box(Modifier.size(25.dp).background(color.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) { Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp) } } } } }
@Composable private fun InsightCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, accent: Color) { PrediqCard(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.Top) { Icon(icon, null, tint = accent); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.labelLarge, color = PrediqMuted); Text(body, style = MaterialTheme.typography.bodyMedium) } } } }
private fun primarySports(sports: List<String>): List<String> = (listOf("football", "basketball", "cricket", "baseball", "rugby", "tennis") + sports).distinct().take(10)
private fun greeting(name: String?): String { val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY); val g = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }; return if (name.isNullOrBlank()) "$g." else "$g, ${name.substringBefore(' ')}." }
private fun subscriptionLabel(account: AccountResponse): String = if (account.access.subscriptionBypass) "Full Access" else account.subscriptionProgress?.let { "${account.subscription?.name ?: "Premium"}: ${it.daysRemaining} days left" } ?: account.subscriptionState.replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun formString(teamForm: kotlinx.serialization.json.JsonObject?, side: String): String = runCatching { teamForm?.get(side)?.jsonObject?.get("overall")?.jsonObject?.get("form")?.jsonPrimitive?.contentOrNull.orEmpty() }.getOrDefault("")
private fun marketSignalText(signal: MarketSignal): String = when (signal.direction) { "supports_pick" -> "Market movement supports the current PredIQ assessment${signal.currentOdds?.let { " (current ${String.format(Locale.US, "%.2f", it)})" } ?: ""}."; "moves_against_pick" -> "Market movement is moving against this assessment, so confidence should be treated cautiously."; else -> "Market pricing is broadly stable across ${signal.snapshots} captured snapshots." }
