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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.data.v2.*
import com.getprediq.app.ui.CompetitionMark
import com.getprediq.app.ui.PlayerHeadshot
import com.getprediq.app.ui.TeamCrest

@Composable
fun TodayContractScreen(
    state: PrediqContractState,
    onRefresh: () -> Unit,
    onOpenDecision: (String) -> Unit,
    onFollow: (V2DecisionCard) -> Unit,
    onFilters: () -> Unit,
    onUpcoming: () -> Unit,
    onBuilder: () -> Unit,
    onPlans: () -> Unit,
) {
    val data = state.today
    val filteredTopPicks = data?.topPicks.orEmpty().filter { matchesDecisionFilters(it, state) }
    val filteredWaiting = data?.waiting.orEmpty().filter { matchesDecisionFilters(it, state) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PrediqHeader() }
        if (data == null) {
            item {
                if (state.busy || state.refreshing) LoadingState("Checking today’s games…")
                else if (accessRequired(state.error)) EmptyState("Unlock PredIQ Intelligence", "Your trial or subscription is needed for ranked analysis, live intelligence and the Odds Builder.", Icons.Outlined.WorkspacePremium, "View plans", onPlans)
                else EmptyState("Today is not ready", state.error ?: "PredIQ could not load today's analysis.", action = "Try again", onAction = onRefresh)
            }
            return@LazyColumn
        }
        item {
            val name = data.viewer.displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            Text("Good ${if (name == null) "day" else "morning, $name"}", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(data.briefing.headline, style = MaterialTheme.typography.headlineMedium, color = Ink, fontWeight = FontWeight.ExtraBold)
        }
        item {
            HeroCard("Daily briefing", "Quality over quantity.", "PredIQ checks the slate and only surfaces decisions that clear its standard.") {
                MetricStrip(
                    listOf(
                        Triple("Top picks", data.briefing.picks.toString(), Icons.Outlined.AutoAwesome),
                        Triple("Games checked", data.briefing.gamesChecked.toString(), Icons.Outlined.FactCheck),
                        Triple("Changed", data.briefing.changedSince.toString(), Icons.Outlined.Update),
                    )
                )
                Button(
                    onClick = onBuilder,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PurpleDeep),
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Outlined.AutoGraph, null); Spacer(Modifier.width(7.dp)); Text("Build target odds", fontWeight = FontWeight.Bold) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onFilters) { Icon(Icons.Outlined.Tune, null, tint = Color.White); Spacer(Modifier.width(5.dp)); Text("Filters", color = Color.White) }
                    TextButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, null, tint = Color.White); Spacer(Modifier.width(5.dp)); Text("Refresh", color = Color.White) }
                }
            }
        }
        if (data.changes.isNotEmpty()) {
            item { SectionHeading("Since you last checked", "See all") }
            item {
                WhiteCard {
                    data.changes.take(4).forEachIndexed { index, change ->
                        ChangeRow(change)
                        if (index < data.changes.take(4).lastIndex) HorizontalDivider(color = Hairline)
                    }
                }
            }
        }
        item { SectionHeading("Best picks today", if (filteredTopPicks.size > 3) "View all" else null) }
        if (filteredTopPicks.isEmpty()) {
            item { EmptyState("No picks clear the standard yet", "PredIQ is still checking today's games. Waiting or passing is a valid decision.", Icons.Outlined.HourglassTop) }
        } else {
            items(filteredTopPicks.take(8), key = { it.id }) { card ->
                DecisionCard(card, onOpen = { onOpenDecision(card.id) }, onFollow = { onFollow(card) }, featured = card.decision.code == "top_pick")
            }
        }
        if (filteredWaiting.isNotEmpty()) {
            item { SectionHeading("Worth waiting for") }
            items(filteredWaiting.take(6), key = { "wait-${it.id}" }) { card ->
                WhiteCard(onClick = { onOpenDecision(card.id) }) {
                    TeamLine(card.event)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(card.decision.label, decisionTone(card.decision.code))
                        Spacer(Modifier.width(10.dp))
                        Text(card.decision.reason ?: "PredIQ is waiting for stronger evidence.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                    }
                }
            }
        }
        if (data.upcoming.isNotEmpty()) {
            item { SectionHeading("Later today", "Calendar", onUpcoming) }
            item {
                WhiteCard {
                    data.upcoming.take(6).forEachIndexed { index, item ->
                        UpcomingCompactRow(item)
                        if (index < data.upcoming.take(6).lastIndex) HorizontalDivider(color = Hairline)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: V2Change) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val good = change.type == "strengthened"
        val bad = change.type == "weakened"
        val tone = if (good) Green else if (bad) Red else Purple
        Box(Modifier.size(40.dp).background(tone.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (good) Icons.Outlined.TrendingUp else if (bad) Icons.Outlined.TrendingDown else Icons.Outlined.Update, null, tint = tone)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(change.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(change.summary ?: humanize(change.type), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        if (change.oldChance?.percent != null || change.newChance?.percent != null) {
            Text("${change.oldChance?.percent ?: "–"} → ${change.newChance?.percent ?: "–"}%", color = tone, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UpcomingCompactRow(item: V2Upcoming) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(54.dp), contentAlignment = Alignment.CenterStart) { Text(compactTime(item.event.startsAt).takeLast(5), color = Muted, style = MaterialTheme.typography.bodySmall) }
        Column(Modifier.weight(1f)) {
            Text("${item.event.participants.home.name} vs ${item.event.participants.away.name}", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.event.competition.name ?: humanize(item.event.sport), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        StatusPill(item.decision.label, decisionTone(item.decision.code))
    }
}

@Composable
fun LiveContractScreen(
    state: PrediqContractState,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onFilters: () -> Unit,
    onPlans: () -> Unit,
) {
    val data = state.live
    val filteredFollowing = data?.following.orEmpty().filter { matchesLiveFilters(it, state) }
    val filteredOpportunities = data?.opportunities.orEmpty().filter { matchesLiveFilters(it, state) }
    val filteredGames = data?.games.orEmpty().filter { matchesLiveFilters(it, state) }
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PrediqHeader() }
        if (data == null) {
            item {
                if (state.busy || state.refreshing) LoadingState("Checking live games…")
                else if (accessRequired(state.error)) EmptyState("Live intelligence is part of full access", "Activate or extend PredIQ access to see live changes and market intelligence.", Icons.Outlined.WorkspacePremium, "View plans", onPlans)
                else EmptyState("Live is unavailable", state.error ?: "PredIQ could not refresh live analysis.", Icons.Outlined.SensorsOff, "Try again", onRefresh)
            }
            return@LazyColumn
        }
        item {
            HeroCard("Live now", if (data.summary.liveGames == 0) "No live games right now" else "${data.summary.opportunities} live ${if (data.summary.opportunities == 1) "opportunity" else "opportunities"}", data.message ?: "What changed, what matters, and whether PredIQ still stands behind the call.", LiveBrush) {
                MetricStrip(
                    listOf(
                        Triple("Live games", data.summary.liveGames.toString(), Icons.Outlined.SportsSoccer),
                        Triple("Worth watching", data.summary.opportunities.toString(), Icons.Outlined.Visibility),
                        Triple("Following", data.summary.following.toString(), Icons.Outlined.Bookmark),
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onFilters) { Icon(Icons.Outlined.Tune, null, tint = Color.White); Spacer(Modifier.width(4.dp)); Text("Filters", color = Color.White) }
                    TextButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, null, tint = Color.White); Spacer(Modifier.width(4.dp)); Text("Refresh", color = Color.White) }
                }
            }
        }
        if (data.liveState == "cached") item { EmptyState("Showing the latest available analysis", "Fresh live data is still arriving. PredIQ will replace this view when the next update lands.", Icons.Outlined.CloudSync) }
        if (data.liveState == "error") item { EmptyState("Live refresh failed", data.message ?: "The latest saved state is still available.", Icons.Outlined.CloudOff, "Retry", onRefresh) }
        if (filteredFollowing.isNotEmpty()) {
            item { SectionHeading("Following") }
            items(filteredFollowing, key = { "following-${it.id}" }) { card -> LiveDecisionCard(card) { onOpen(card.id) } }
        }
        if (filteredOpportunities.isNotEmpty()) {
            item { SectionHeading("Live opportunities") }
            items(filteredOpportunities, key = { "opp-${it.id}" }) { card -> LiveDecisionCard(card) { onOpen(card.id) } }
        }
        if (data.changes.isNotEmpty()) {
            item { SectionHeading("Changed recently") }
            item { WhiteCard { data.changes.take(5).forEachIndexed { index, change -> ChangeRow(change); if (index < data.changes.take(5).lastIndex) HorizontalDivider(color = Hairline) } } }
        }
        item { SectionHeading("All live games") }
        if (filteredGames.isEmpty() && filteredOpportunities.isEmpty() && filteredFollowing.isEmpty()) {
            item { EmptyState("Nothing live right now", "Upcoming events will appear here once play starts.", Icons.Outlined.Schedule) }
        } else {
            items(filteredGames, key = { "game-${it.id}" }) { card ->
                WhiteCard(onClick = { onOpen(card.id) }) {
                    TeamLine(card.event)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(if (card.analysisQuality == "score_only") "Game state only" else card.decision.label, if (card.analysisQuality == "score_only") "neutral" else decisionTone(card.decision.code))
                        Spacer(Modifier.weight(1f))
                        if (card.analysisQuality != "score_only" && card.currentChance.percent != null) Text("${card.currentChance.percent}%", color = PurpleDeep, fontWeight = FontWeight.Bold)
                        Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsContractScreen(
    state: PrediqContractState,
    onPeriod: (Int) -> Unit,
    onOutcome: (String) -> Unit,
    onSport: (String) -> Unit,
    onCompetition: (String) -> Unit,
    onMarket: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    onPerformance: (String) -> Unit,
    onPlans: () -> Unit,
) {
    val summary = state.resultsSummary
    val feed = state.results
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PrediqHeader() }
        if (summary == null) {
            item {
                if (state.busy || state.refreshing) LoadingState("Loading PredIQ's record…")
                else if (accessRequired(state.error)) EmptyState("Full track record requires access", "Activate PredIQ to audit the same decision system you use for picks and Odds Builder combinations.", Icons.Outlined.WorkspacePremium, "View plans", onPlans)
                else EmptyState("Results are unavailable", state.error ?: "PredIQ could not load the track record.", Icons.Outlined.Analytics)
            }
            return@LazyColumn
        }
        item {
            HeroCard("Track record", "Accuracy you can audit.", "Every published pick stays visible after settlement.") {
                MetricStrip(
                    listOf(
                        Triple("Won", summary.record.won.toString(), Icons.Outlined.CheckCircle),
                        Triple("Lost", summary.record.lost.toString(), Icons.Outlined.Cancel),
                        Triple("Settled", summary.record.settled.toString(), Icons.Outlined.FactCheck),
                    )
                )
                Text("Hit rate ${percent(summary.record.hitRate)} · ${summary.record.void} void", color = Color.White.copy(alpha = .85f), fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(7, 30, 90, 365)) { days -> FilterChip(selected = state.resultPeriodDays == days, onClick = { onPeriod(days) }, label = { Text(if (days == 365) "1Y" else "${days}D") }) }
            }
        }
        if (summary.bySport.isNotEmpty()) {
            item {
                Text("Sport", color = Muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedSport.isBlank(), onClick = { onSport("") }, label = { Text("All") }) }
                    items(summary.bySport.mapNotNull { it.sport }.distinct()) { sport -> FilterChip(selected = state.selectedSport == sport, onClick = { onSport(sport) }, label = { Text(humanize(sport)) }) }
                }
            }
        }
        if (summary.byCompetition.isNotEmpty()) {
            item {
                Text("Competition", color = Muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedCompetition.isBlank(), onClick = { onCompetition("") }, label = { Text("All leagues") }) }
                    items(summary.byCompetition.mapNotNull { it.competition }.distinct().take(30)) { competition -> FilterChip(selected = state.selectedCompetition == competition, onClick = { onCompetition(competition) }, label = { Text(competition, maxLines = 1) }) }
                }
            }
        }
        if (summary.byMarket.isNotEmpty()) {
            item {
                Text("Market", color = Muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.resultMarket.isBlank(), onClick = { onMarket("") }, label = { Text("All markets") }) }
                    items(summary.byMarket.mapNotNull { it.market }.distinct()) { market -> FilterChip(selected = state.resultMarket == market, onClick = { onMarket(market) }, label = { Text(humanize(market)) }) }
                }
            }
        }
        if (summary.calibration.isNotEmpty()) {
            item { SectionHeading("Accuracy by chance") }
            item {
                WhiteCard {
                    summary.calibration.takeLast(5).reversed().forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(item.range, color = Ink, fontWeight = FontWeight.SemiBold); Text("${item.settled} settled", color = Muted, style = MaterialTheme.typography.bodySmall) }
                            Text(item.observedRate?.let { "${it.toInt()}% landed" } ?: "Not enough data", color = if (item.status == "below") Amber else GreenDeep, fontWeight = FontWeight.Bold)
                        }
                        if (index < summary.calibration.takeLast(5).lastIndex) HorizontalDivider(color = Hairline)
                    }
                }
            }
        }
        if (summary.byMarket.isNotEmpty()) {
            item { SectionHeading("Best-performing markets") }
            item {
                WhiteCard {
                    summary.byMarket.take(5).forEachIndexed { index, slice ->
                        InfoRow(Icons.Outlined.Insights, humanize(slice.market), "${slice.settled} settled", percent(slice.hitRate), onClick = { onPerformance(slice.market ?: "") })
                        if (index < summary.byMarket.take(5).lastIndex) HorizontalDivider(color = Hairline)
                    }
                }
            }
        }
        if (summary.pricePerformance.tracked > 0) {
            item {
                BrightCard(brush = LiveBrush) {
                    Text("Price performance", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Beat the closing price ${percent(summary.pricePerformance.beatClosingRate)} of tracked picks", color = Color.White.copy(alpha = .85f))
                    summary.pricePerformance.meanClv?.let { Text("Average closing value ${if (it >= 0) "+" else ""}${(it * 100).toInt()}%", color = Lime, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            SectionHeading("Recent results")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("", "won", "lost", "void", "pending")) { outcome -> FilterChip(selected = state.resultOutcome == outcome, onClick = { onOutcome(outcome) }, label = { Text(if (outcome.isBlank()) "All" else humanize(outcome)) }) }
            }
        }
        if (feed == null || feed.results.isEmpty()) {
            item { EmptyState("No results in this view", "Try a different period or result filter.", Icons.Outlined.FilterAltOff) }
        } else {
            items(feed.results, key = { it.publishedForecastId ?: it.id }) { card ->
                WhiteCard(onClick = { card.publishedForecastId?.let(onOpenResult) }) {
                    TeamLine(card.event)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(humanize(card.result?.outcome), when (card.result?.outcome) { "won" -> "good"; "lost" -> "bad"; "void" -> "neutral"; else -> "warn" })
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(card.pick.label ?: "PredIQ pick", color = Ink, fontWeight = FontWeight.Bold)
                            Text("Original chance ${card.chance.percent ?: "–"}%", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        card.closingMarket?.clvPrice?.let { Text("${if (it > 0) "+" else ""}${(it * 100).toInt()}% CLV", color = if (it >= 0) GreenDeep else Red, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun ResearchContractScreen(
    state: PrediqContractState,
    onSearch: () -> Unit,
    onTeam: (String) -> Unit,
    onPlayer: (String) -> Unit,
    onLeague: (String) -> Unit,
    onPlans: () -> Unit,
) {
    val data = state.research
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PrediqHeader() }
        item {
            HeroCard("Research", "Understand the evidence.", "Teams, players and competitions connected to today's decisions.", ResearchBrush) {
                OutlinedButton(onClick = onSearch, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = .5f)))) {
                    Icon(Icons.Outlined.Search, null); Spacer(Modifier.width(8.dp)); Text("Search teams, players or leagues")
                }
            }
        }
        if (data == null) {
            item {
                if (state.busy) LoadingState("Loading research…")
                else if (accessRequired(state.error)) EmptyState("Research requires PredIQ access", "Team, player, league and multi-market intelligence are part of full access.", Icons.Outlined.WorkspacePremium, "View plans", onPlans)
                else EmptyState("Research is unavailable", state.error ?: "Try again in a moment.")
            }
            return@LazyColumn
        }
        if (data.teamsInTodayPicks.isNotEmpty()) {
            item { SectionHeading("Teams in today's picks") }
            items(data.teamsInTodayPicks.take(8), key = { it.id }) { team ->
                WhiteCard(onClick = { onTeam(team.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamCrest(team.name, team.sport.ifBlank { "football" }, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(team.name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(team.country, "${team.matchesCount} matches observed").joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                    }
                    profileHighlights(team.profile).take(2).forEach { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (data.leagues.isNotEmpty()) {
            item { SectionHeading("League intelligence") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(data.leagues.take(10), key = { it.id }) { league ->
                        Card(Modifier.width(240.dp).clickable { onLeague(league.id) }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CompetitionMark(league.name, league.sport.ifBlank { "football" }, size = 42.dp)
                                Text(league.name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(listOfNotNull(league.country, league.season).joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall)
                                profileHighlights(league.profile).take(2).forEach { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                                Text("${league.matchesCount} matches", color = PurpleDeep, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        if (data.playersToWatch.isNotEmpty()) {
            item { SectionHeading("Players to watch") }
            items(data.playersToWatch.take(8), key = { it.id }) { player ->
                WhiteCard(onClick = { onPlayer(player.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerHeadshot(player.name, player.sportCode.ifBlank { "football" }, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(player.name, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(listOfNotNull(player.position ?: player.positionGroup, player.nationality).joinToString(" · "), color = Muted)
                            player.headline?.let { Text(it, color = PurpleDeep, style = MaterialTheme.typography.bodySmall) }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountContractScreen(
    state: PrediqContractState,
    onProfile: () -> Unit,
    onFollowing: () -> Unit,
    onNotifications: () -> Unit,
    onPlan: () -> Unit,
    onPayments: () -> Unit,
    onHelp: () -> Unit,
    onLogout: () -> Unit,
) {
    val account = state.account
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PrediqHeader() }
        item { Text("Account & access", style = MaterialTheme.typography.headlineMedium, color = Ink, fontWeight = FontWeight.ExtraBold); Text("Manage your profile, access and preferences.", color = Muted) }
        if (account == null) {
            item { LoadingState("Loading your account…") }
            return@LazyColumn
        }
        item {
            WhiteCard(onClick = onProfile) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EntityAvatar(account.profile.name ?: account.profile.email)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.profile.name ?: "PredIQ member", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                        Text(account.profile.email, color = Muted)
                        StatusPill(if (account.membership.fullAccess) "Full access" else humanize(account.membership.state), if (account.membership.fullAccess) "good" else "warn")
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                }
            }
        }
        item {
            BrightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = Lime, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.membership.planName ?: "PredIQ access", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(account.membership.daysRemaining?.let { "$it days remaining" } ?: humanize(account.membership.state), color = Color.White.copy(alpha = .8f))
                    }
                    TextButton(onClick = onPlan) { Text("Manage", color = Color.White) }
                }
            }
        }
        item {
            WhiteCard {
                InfoRow(Icons.Outlined.Notifications, "Notifications", "Manage alerts and preferences", onClick = onNotifications)
                HorizontalDivider(color = Hairline)
                val fs = account.followingSummary
                InfoRow(Icons.Outlined.Bookmarks, "Following", "${fs.matches} matches · ${fs.teams} teams · ${fs.leagues} leagues", onClick = onFollowing)
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.CreditCard, "Payment & billing", "Manage payments and receipts", onClick = onPayments)
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.Person, "Profile & region", listOfNotNull(account.profile.country, account.profile.currency).joinToString(" · "), onClick = onProfile)
                HorizontalDivider(color = Hairline)
                InfoRow(Icons.Outlined.HelpOutline, "Help, methodology & safer play", "How PredIQ works and support", onClick = onHelp)
            }
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                Icon(Icons.Outlined.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign out")
            }
        }
    }
}

private fun profileHighlights(profile: kotlinx.serialization.json.JsonObject): List<String> = profile.entries.mapNotNull { (key, value) ->
    val text = value.toString().trim('"')
    if (text == "null" || text == "{}" || text == "[]") null else "${humanize(key)} · $text"
}


private fun matchesDecisionFilters(card: V2DecisionCard, state: PrediqContractState): Boolean {
    if (state.selectedMarket.isNotBlank() && card.pick.market != state.selectedMarket) return false
    if (state.selectedStatusFilter.isNotBlank() && card.decision.code != state.selectedStatusFilter) return false
    if (state.selectedValueFilter.isNotBlank() && card.value.status != state.selectedValueFilter) return false
    val p = card.chance.percent
    if (state.selectedChanceBand.isNotBlank()) {
        if (p == null) return false
        val ok = when (state.selectedChanceBand) {
            "80+" -> p >= 80
            "70-79" -> p in 70..79
            "60-69" -> p in 60..69
            "<60" -> p < 60
            else -> true
        }
        if (!ok) return false
    }
    return true
}

private fun matchesLiveFilters(card: V2LiveCard, state: PrediqContractState): Boolean {
    if (state.selectedMarket.isNotBlank() && card.pick.market != state.selectedMarket) return false
    if (state.selectedStatusFilter.isNotBlank() && card.decision.code != state.selectedStatusFilter) return false
    if (state.selectedValueFilter.isNotBlank() && card.value.status != state.selectedValueFilter) return false
    val p = card.currentChance.percent ?: card.chance.percent
    if (state.selectedChanceBand.isNotBlank()) {
        if (p == null) return false
        val ok = when (state.selectedChanceBand) {
            "80+" -> p >= 80
            "70-79" -> p in 70..79
            "60-69" -> p in 60..69
            "<60" -> p < 60
            else -> true
        }
        if (!ok) return false
    }
    return true
}

private fun accessRequired(message: String?): Boolean = message.orEmpty().lowercase().let { "subscription" in it || "trial" in it || "full access" in it }
