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
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.AutoGraph, null, tint = PrediqBlue); Spacer(Modifier.width(7.dp)); Text("PredIQ", style = MaterialTheme.typography.titleLarge, color = PrediqBlue, fontWeight = FontWeight.Bold) }
                    SubscriptionPill(state.account?.subscriptionProgress)
                }
                Text(greeting(state.account?.user?.displayName), style = MaterialTheme.typography.headlineMedium)
                Text("Football, basketball, cricket, tennis and more—held to one evidence standard.", color = PrediqMuted)
            }
        }
        item { SportChips(primarySports(state.filterOptions.sports), state.selectedSport, vm::selectSport) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ModeTab("Today", state.todayMode == "today") { vm.setTodayMode("today") }
                ModeTab("Upcoming", state.todayMode == "upcoming") { vm.setTodayMode("upcoming") }
            }
        }
        if (state.todayMode == "today" && state.picks.isNotEmpty()) {
            item { PrediqSectionTitle("PredIQ Picks of the Day") }
            items(state.picks.take(2), key = { "pick-${it.eventId}" }) { pick -> PickFeatureCard(pick) { if (state.account == null) onAuth() else onMatch(pick.eventId) } }
        }
        item {
            PrediqCard(Modifier.fillMaxWidth(), onLeagueWinners) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Color(0xFFEAF0FF), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EmojiEvents, null, tint = PrediqBlue) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text("League Winner Predictions", style = MaterialTheme.typography.titleLarge); Text("Competition-specific title probabilities from PredIQ", color = PrediqMuted) }
                    Icon(Icons.Outlined.ChevronRight, null, tint = PrediqMuted)
                }
            }
        }
        item { PrediqSectionTitle(if (state.todayMode == "upcoming") "Upcoming Intelligence" else "Trending Signals") }
        when {
            state.loadingToday -> item { StateCard("Analysing current games…", "PredIQ is combining the latest match and competition context.") }
            state.todayError != null -> item { StateCard("Could not refresh intelligence", state.todayError, error = true, action = "Retry", onAction = vm::loadToday) }
            !vm.fullAccess -> item { StateCard("Full analysis is locked", "Start a trial or activate a plan to open PredIQ.", action = if (state.account == null) "Start trial" else "View Account", onAction = onAuth) }
            state.assessments.isEmpty() -> item { StateCard("No strong assessment in this view", "PredIQ will not fill the list with weak or unrelated picks. Try another sport or check Upcoming.") }
            else -> items(state.assessments.take(30), key = { it.eventId }) { assessment -> AssessmentCard(assessment) { onMatch(assessment.eventId) } }
        }
    }
}

@Composable
fun LiveScreen(state: PrediqUiState, vm: PrediqViewModel, onAuth: () -> Unit, onMatch: (String) -> Unit) {
    var bestOnly by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) { delay(300_000); vm.loadLive() }
    }
    val live = state.live
    val filtered = live?.games.orEmpty().filter { (state.selectedSport.isBlank() || it.sportCode == state.selectedSport) && (!bestOnly || it.analysisPromotable) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column { Row(verticalAlignment = Alignment.CenterVertically) { Text("LIVE", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.width(8.dp)); Text("(${live?.liveCount ?: 0})", color = PrediqBlue, style = MaterialTheme.typography.titleLarge) }; Text(live?.message ?: "Checking live games…", color = PrediqMuted) }
                IconButton(onClick = vm::loadLive) { Icon(Icons.Outlined.Refresh, "Refresh") }
            }
        }
        item { SportChips(primarySports(state.filterOptions.sports), state.selectedSport, vm::selectSport) }
        item {
            Row(Modifier.fillMaxWidth().background(PrediqSurfaceLow, RoundedCornerShape(14.dp)).padding(4.dp)) {
                ToggleSegment("All Live", !bestOnly, Modifier.weight(1f)) { bestOnly = false }
                ToggleSegment("Best Opportunities", bestOnly, Modifier.weight(1f)) { bestOnly = true }
            }
        }
        if (state.loadingLive && live == null) item { StateCard("Checking live games…", "This request will resolve into live analysis, no opportunity, cached analysis or an error state.") }
        if (state.liveError != null) item { StateCard(if (live != null) "Showing last-known analysis" else "Live could not refresh", state.liveError, error = live == null, cached = live != null, action = "Retry", onAction = vm::loadLive) }
        if (live != null && state.liveError == null) item {
            when (live.state) {
                "cached" -> StateCard("Showing last-known analysis", live.message, cached = true, action = "Refresh", onAction = vm::loadLive)
                "service_error" -> StateCard("Live is temporarily unavailable", live.message, error = true, action = "Retry", onAction = vm::loadLive)
                "live_no_opportunities" -> StateCard("Live checked", live.message)
                else -> StateCard("Live analysis is active", live.message)
            }
        }
        if (state.account == null && (live?.liveCount ?: 0) > 0) item { StateCard("Live games are active", "Sign in to see PredIQ’s current live probabilities and reasons.", action = "Sign in", onAction = onAuth) }
        if (vm.fullAccess) {
            if (filtered.isEmpty() && !state.loadingLive) item { StateCard(if (bestOnly) "No strong live opportunities" else "No tracked live games", if (bestOnly) "PredIQ is tracking the games but none currently clear the promotion checks." else "Check back when tracked events are underway.") }
            items(filtered.take(10), key = { it.eventId }) { game -> LiveMatchCard(game) { onMatch(game.eventId) } }
        }
    }
}

@Composable
fun ResultsScreen(state: PrediqUiState, vm: PrediqViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Results & Accuracy", style = MaterialTheme.typography.headlineMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric("TODAY", "${state.resultsDashboard.today.wins}W / ${state.resultsDashboard.today.losses}L", "Graded ${state.resultsDashboard.today.graded}", Modifier.weight(1f))
                SummaryMetric("30D TOP PICKS", probability(state.resultsDashboard.topPicks30d.accuracy), "${state.resultsDashboard.topPicks30d.graded} graded", Modifier.weight(1f), highlight = true)
            }
        }
        item { SportChips(primarySports(state.filterOptions.sports), state.selectedSport, vm::selectSport) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("" to "All", "won" to "Won", "lost" to "Lost", "void" to "Void", "pending" to "Pending")) { pair -> FilterChip(selected = state.resultOutcome == pair.first, onClick = { vm.setResultOutcome(pair.first) }, label = { Text(pair.second) }, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(7, 30, 90).forEach { days -> FilterChip(selected = state.resultDays == days, onClick = { vm.setResultDays(days) }, label = { Text("${days}D") }, shape = CircleShape) } }
        }
        item { PrediqSectionTitle("Recent Matches") }
        when {
            state.loadingResults -> item { StateCard("Loading graded predictions…", "Wins and losses are calculated from finished tracked events.") }
            state.resultError != null -> item { StateCard("Results could not refresh", state.resultError, error = true, action = "Retry", onAction = vm::loadResults) }
            state.results.isEmpty() -> item { StateCard("No results in this view", "Try another time window, sport or outcome filter.") }
            else -> items(state.results, key = { "result-${it.predictionId}-${it.startsAt}" }) { ResultCard(it) }
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
        Column(Modifier.fillMaxSize().background(PrediqBackground).padding(20.dp)) { BackHeader("Match Intelligence", onBack); Spacer(Modifier.height(20.dp)); StateCard("Building match intelligence…", "PredIQ is resolving the latest assessment and supporting context.") }
        return
    }
    val event = data.event; val assessment = data.assessment
    LazyColumn(Modifier.fillMaxSize().background(PrediqBackground), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 50.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { BackHeader("Match Intelligence", onBack, onShare = data.share.text?.let { text -> { val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "$text\n${data.share.url.orEmpty()}") }; context.startActivity(Intent.createChooser(intent, "Share PredIQ assessment")) } }) }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("${event.competition ?: prettySport(event.sportCode)} • ${kickoff(event.startsAt)}", color = PrediqMuted, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { TeamBadge(event.homeParticipant, Modifier.size(64.dp)); Text(event.homeParticipant, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) }; Text("VS", color = PrediqMuted); Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { TeamBadge(event.awayParticipant, Modifier.size(64.dp)); Text(event.awayParticipant, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) } }
            }
        }
        item {
            PrediqCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text("PredIQ Prediction", color = PrediqBlue, style = MaterialTheme.typography.labelLarge); Text(assessment.selectionLabel ?: "Current assessment", style = MaterialTheme.typography.headlineMedium) }; StatusPill("${probability(assessment.confidence)} HIGH", PrediqGreen, Color(0xFFE9F8EF)) }
                ConfidenceBar(assessment.probability, "Probability")
                if (assessment.why.isNotEmpty()) { Text("THE WHY", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); IndicatorList(assessment.why) }
                assessment.changeReason?.let { Surface(color = Color(0xFFEEF3FF), shape = RoundedCornerShape(12.dp)) { Text(it, Modifier.padding(12.dp), color = PrediqBlue) } }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormCard(event.homeParticipant, formString(data.teamForm, "home"), Modifier.weight(1f))
                FormCard(event.awayParticipant, formString(data.teamForm, "away"), Modifier.weight(1f))
            }
        }
        data.marketSignal?.let { signal -> item { InsightCard(Icons.Outlined.TrendingUp, "Market Signal", marketSignalText(signal), PrediqBlue) } }
        if (assessment.watchOuts.isNotEmpty()) item { InsightCard(Icons.Outlined.WarningAmber, "Watch Out For", assessment.watchOuts.joinToString(" • "), PrediqAmber) }
        item { InsightCard(Icons.Outlined.Analytics, "PredIQ History in ${event.competition ?: "this competition"}", if (data.prediqHistory.accuracy == null) "Not enough graded history yet" else "${probability(data.prediqHistory.accuracy)} accuracy across ${data.prediqHistory.graded} graded predictions", PrediqBlue) }
        item { InsightCard(Icons.Outlined.Groups, "Lineups", if (data.lineups.confirmed) "Confirmed lineups are included in this assessment." else "Lineups are not yet confirmed; PredIQ will re-analyse when they arrive.", if (data.lineups.confirmed) PrediqGreen else PrediqMuted) }
        item { Text("Last analysed ${relativeTime(assessment.lastAnalysedAt)}", color = PrediqMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    }
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
@Composable private fun ToggleSegment(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) { Surface(modifier.clickable(onClick = onClick), color = if (active) Color.White else Color.Transparent, shape = RoundedCornerShape(10.dp), shadowElevation = if (active) 1.dp else 0.dp) { Box(Modifier.heightIn(min = 44.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(label, color = if (active) PrediqBlue else PrediqMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) } } }
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
