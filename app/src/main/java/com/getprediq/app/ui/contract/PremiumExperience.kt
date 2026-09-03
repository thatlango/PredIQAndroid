package com.getprediq.app.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.data.v2.V2DecisionCard
import com.getprediq.app.data.v2.V2LiveCard
import com.getprediq.app.data.v3.V3SavedTicket
import com.getprediq.app.data.v3.V3TicketLeg
import kotlinx.coroutines.delay

private val PremiumInk = Color(0xFFF8FAFF)
private val PremiumMuted = Color(0xFFA8B0C8)
private val PremiumBg = Color(0xFF080D1D)
private val PremiumPanel = Color(0xFF10172C)
private val PremiumPanel2 = Color(0xFF151E38)
private val PremiumLine = Color(0xFF26304D)
private val PremiumPurple = Color(0xFF6041F5)
private val PremiumBlue = Color(0xFF2457E8)
private val PremiumLime = Color(0xFFB8F23A)
private val PremiumGreen = Color(0xFF32D583)
private val PremiumAmber = Color(0xFFF5A524)
private val PremiumRed = Color(0xFFF97066)
private val PremiumHero = Brush.linearGradient(listOf(Color(0xFF4E2FDB), Color(0xFF1D56E7)))

@Composable
private fun PremiumScreen(
    title: String,
    subtitle: String? = null,
    onNotifications: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PremiumBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = PremiumInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    subtitle?.let { Text(it, color = PremiumMuted, style = MaterialTheme.typography.bodySmall) }
                }
                onNotifications?.let {
                    IconButton(onClick = it, modifier = Modifier.size(46.dp).background(PremiumPanel2, CircleShape)) {
                        Icon(Icons.Outlined.Notifications, "Notifications", tint = PremiumInk)
                    }
                }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
    }
}

@Composable
private fun PremiumCard(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .then(if (brush == null) Modifier.background(PremiumPanel) else Modifier.background(brush))
        .border(1.dp, if (brush == null) PremiumLine else Color.White.copy(alpha = .08f), RoundedCornerShape(24.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(18.dp)
    Column(base, verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun PremiumPill(text: String, tone: String = "neutral") {
    val (bg, fg) = when (tone) {
        "good" -> PremiumGreen.copy(alpha = .16f) to PremiumGreen
        "lime" -> PremiumLime.copy(alpha = .18f) to PremiumLime
        "warn" -> PremiumAmber.copy(alpha = .16f) to PremiumAmber
        "bad" -> PremiumRed.copy(alpha = .16f) to PremiumRed
        "purple" -> PremiumPurple.copy(alpha = .20f) to Color(0xFFC8BCFF)
        else -> PremiumPanel2 to PremiumMuted
    }
    Text(text, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp))
}

@Composable
private fun PremiumSectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = PremiumInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action, color = PremiumLime) }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White.copy(alpha = .08f), RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(value, color = PremiumInk, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(alpha = .66f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PremiumTodayScreen(
    state: PrediqContractState,
    onRefresh: () -> Unit,
    onDecision: (String) -> Unit,
    onBuild: () -> Unit,
    onLive: () -> Unit,
    onFilters: () -> Unit,
    onResearch: () -> Unit,
    onNotifications: () -> Unit,
) {
    val data = state.today
    val firstName = data?.viewer?.displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    PremiumScreen(
        title = if (firstName == null) "Today" else "Good afternoon, $firstName",
        subtitle = "Your decision desk for today",
        onNotifications = onNotifications,
    ) {
        PremiumCard(brush = PremiumHero) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = PremiumLime)
                Spacer(Modifier.width(8.dp))
                Text("DAILY INTELLIGENCE", color = PremiumLime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
            }
            Text(data?.briefing?.headline ?: "PredIQ is checking today's slate", color = Color.White,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                if (data == null) "Loading the latest decisions without blocking the rest of the app."
                else if (data.briefing.picks == 0) "No pick clears the standard yet. Waiting is a decision — PredIQ will surface stronger edges when they appear."
                else "Only decisions that clear PredIQ's evidence and confidence checks are promoted.",
                color = Color.White.copy(alpha = .78f),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell("Top picks", (data?.briefing?.topPicks ?: 0).toString(), Modifier.weight(1f))
                MetricCell("Games checked", (data?.briefing?.gamesChecked ?: 0).toString(), Modifier.weight(1f))
                MetricCell("Changed", (data?.briefing?.changedSince ?: 0).toString(), Modifier.weight(1f))
            }
            Button(onClick = onBuild, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumLime, contentColor = Color(0xFF10141F))) {
                Icon(Icons.Outlined.AutoGraph, null); Spacer(Modifier.width(8.dp)); Text("Build ticket", fontWeight = FontWeight.ExtraBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLive, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Outlined.Sensors, null); Spacer(Modifier.width(6.dp)); Text("Live now")
                }
                OutlinedButton(onClick = onFilters, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Outlined.Tune, null); Spacer(Modifier.width(6.dp)); Text("Filters")
                }
                IconButton(onClick = onRefresh, modifier = Modifier.background(Color.White.copy(alpha = .10f), CircleShape)) {
                    Icon(Icons.Outlined.Refresh, "Refresh", tint = Color.White)
                }
            }
        }

        PremiumSectionTitle("Best picks today", "Research", onResearch)
        when {
            data == null && (state.busy || state.refreshing) -> PremiumCard { Text("Checking today's games…", color = PremiumMuted) }
            data?.topPicks.orEmpty().isEmpty() -> PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(PremiumPurple.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.HourglassTop, null, tint = Color(0xFFC8BCFF))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Wait for stronger edges", color = PremiumInk, fontWeight = FontWeight.Bold)
                        Text("PredIQ has checked the slate. None of the current calls deserve a strong recommendation yet.", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            else -> data!!.topPicks.take(6).forEach { card -> PremiumDecisionRow(card) { onDecision(card.id) } }
        }

        if (!data?.changes.isNullOrEmpty()) {
            PremiumSectionTitle("Changed since you checked")
            data!!.changes.take(4).forEach { change ->
                PremiumCard {
                    Text(change.title, color = PremiumInk, fontWeight = FontWeight.Bold)
                    Text(change.summary ?: "PredIQ updated this decision.", color = PremiumMuted)
                    PremiumPill(change.type.replaceFirstChar { it.uppercase() }, if (change.type == "strengthened") "good" else if (change.type == "weakened") "warn" else "purple")
                }
            }
        }
    }
}

@Composable
private fun PremiumDecisionRow(card: V2DecisionCard, onOpen: () -> Unit) {
    PremiumCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${card.event.participants.home.name} vs ${card.event.participants.away.name}", color = PremiumInk, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.event.competition.name ?: card.event.sport.orEmpty(), color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
            }
            PremiumPill(card.decision.label, when (card.decision.code) { "top_pick" -> "lime"; "pass" -> "warn"; else -> "purple" })
        }
        Text(card.pick.label ?: card.pick.selection ?: "Open decision", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumPill(card.chance.percent?.let { "$it%" } ?: "Chance pending", if ((card.chance.percent ?: 0) >= 75) "good" else "neutral")
            if (card.value.currentOdds != null) PremiumPill("Odds ${formatPremiumOdds(card.value.currentOdds)}")
            PremiumPill(card.risk.label, if (card.risk.level == "high") "warn" else "neutral")
        }
    }
}

private data class GoalFlash(val eventId: String, val label: String)

@Composable
fun PremiumLiveScreen(
    state: PrediqContractState,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onFilters: () -> Unit,
    onNotifications: () -> Unit,
) {
    val data = state.live
    val previousScores = remember { mutableStateMapOf<String, Pair<Int?, Int?>>() }
    var goalFlash by remember { mutableStateOf<GoalFlash?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            onRefresh()
        }
    }

    LaunchedEffect(data?.updatedAt, data?.generatedAt) {
        allLiveCards(state).forEach { card ->
            val current = scorePair(card)
            val old = previousScores[card.event.id]
            if (old != null && current.first != null && current.second != null) {
                val homeScored = (current.first ?: 0) > (old.first ?: 0)
                val awayScored = (current.second ?: 0) > (old.second ?: 0)
                if (homeScored || awayScored) {
                    val scorer = if (homeScored) card.event.participants.home.name else card.event.participants.away.name
                    goalFlash = GoalFlash(card.event.id, "GOAL · $scorer ${current.first}–${current.second}")
                }
            }
            previousScores[card.event.id] = current
        }
    }
    LaunchedEffect(goalFlash) {
        if (goalFlash != null) {
            delay(9_000)
            goalFlash = null
        }
    }

    PremiumScreen(title = "Live", subtitle = "Material events refresh every ~15 seconds while this screen is open", onNotifications = onNotifications) {
        goalFlash?.let { flash ->
            PremiumCard(brush = Brush.linearGradient(listOf(Color(0xFF6B37F5), Color(0xFF4023C8))), onClick = { onOpen(flash.eventId) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SportsSoccer, null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(flash.label, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text("PredIQ recalculating the live view…", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall)
                    }
                    PremiumPill("LIVE", "lime")
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumPill(if (state.refreshing) "Updating…" else "Live feed", if (state.refreshing) "purple" else "good")
            PremiumPill(data?.updatedAt?.let { "Fresh" } ?: "Awaiting feed")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFilters, modifier = Modifier.background(PremiumPanel2, CircleShape)) { Icon(Icons.Outlined.Tune, "Filters", tint = PremiumInk) }
            IconButton(onClick = onRefresh, modifier = Modifier.background(PremiumPanel2, CircleShape)) { Icon(Icons.Outlined.Refresh, "Refresh", tint = PremiumInk) }
        }

        if (data == null) {
            PremiumCard { Text(if (state.refreshing || state.busy) "Checking live games…" else state.error ?: "Live intelligence is temporarily unavailable.", color = PremiumMuted) }
        } else {
            if (data.liveState == "cached") PremiumCard { PremiumPill("Cached", "warn"); Text("Showing the last-known state while a fresh live update arrives.", color = PremiumMuted) }
            val cards = allLiveCards(state)
            if (cards.isEmpty()) {
                PremiumCard { Text("Nothing live right now", color = PremiumInk, fontWeight = FontWeight.Bold); Text("Upcoming events will appear automatically when play starts.", color = PremiumMuted) }
            } else {
                cards.forEach { card -> PremiumLiveCard(card, onOpen) }
            }
        }
    }
}

@Composable
private fun PremiumLiveCard(card: V2LiveCard, onOpen: (String) -> Unit) {
    val score = scorePair(card)
    PremiumCard(onClick = { onOpen(card.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.event.competition.name ?: card.event.sport.orEmpty(), color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                Text("${card.event.participants.home.name} vs ${card.event.participants.away.name}", color = PremiumInk, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PremiumPill(card.event.score?.statusText ?: "LIVE", "lime")
        }
        if (score.first != null || score.second != null) {
            Text("${score.first ?: 0}  –  ${score.second ?: 0}", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.pick.label ?: card.pick.selection ?: "Live assessment", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                Text(card.currentChance.percent?.let { "$it%" } ?: "Recalculating", color = PremiumLime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            val change = card.change
            if (change.points != null) PremiumPill("${if (change.direction == "up") "↑" else if (change.direction == "down") "↓" else "•"} ${change.points} pts", if (change.direction == "up") "good" else if (change.direction == "down") "warn" else "neutral")
        }
        Text(if (card.analysisQuality == "score_only") "Score state only — PredIQ is not promoting this as a full live call." else card.decision.reason ?: "Updated live intelligence", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PremiumBuilderScreen(
    state: PrediqContractState,
    onTarget: (Double) -> Unit,
    onRisk: (String) -> Unit,
    onSource: (String) -> Unit,
    onBuild: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onRemoveLeg: (V3TicketLeg) -> Unit,
    onSave: () -> Unit,
    onSaved: () -> Unit,
) {
    val ticket = state.v3Ticket
    val source = state.v3Bookmakers?.bookmakers?.firstOrNull { it.code == state.v3Bookmaker }
    PremiumScreen(title = "Ticket Builder", subtitle = "Target → source odds → ticket") {
        BuilderSteps(if (ticket?.legs?.isNotEmpty() == true) 3 else if (source != null) 2 else 1)
        PremiumCard {
            Text("Target odds", color = PremiumInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(3.0, 5.0, 10.0, 20.0, 35.0, 50.0, 75.0)) { target ->
                    FilterChip(
                        selected = state.v3TargetOdds == target,
                        onClick = { onTarget(target) },
                        label = { Text(formatPremiumOdds(target)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PremiumPanel2, labelColor = PremiumMuted,
                            selectedContainerColor = PremiumLime, selectedLabelColor = Color(0xFF10141F),
                        ),
                    )
                }
            }
            Text("Risk profile", color = PremiumMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("safer", "balanced", "aggressive").forEach { risk ->
                    FilterChip(
                        selected = state.v3Risk == risk,
                        onClick = { onRisk(risk) },
                        label = { Text(risk.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = if (state.v3Risk == risk) {{ Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }} else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PremiumPanel2, labelColor = PremiumMuted,
                            selectedContainerColor = PremiumLime.copy(alpha = .94f), selectedLabelColor = Color(0xFF10141F),
                        ),
                    )
                }
            }
        }

        PremiumCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(PremiumPurple.copy(alpha = .18f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.StackedLineChart, null, tint = Color(0xFFC8BCFF))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(source?.label ?: "PredIQ Reference Odds", color = PremiumInk, fontWeight = FontWeight.ExtraBold)
                    Text(buildString {
                        append(if (source?.referenceOnly != false) "Reference benchmark" else "Named source")
                        source?.snapshots?.takeIf { it > 0 }?.let { append(" · $it snapshots") }
                        source?.providers?.size?.takeIf { it > 0 }?.let { append(" · $it provider${if (it == 1) "" else "s"}") }
                    }, color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                }
                PremiumPill("Fresh", "good")
            }
            if (state.v3Bookmakers?.bookmakers.orEmpty().size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.v3Bookmakers!!.bookmakers) { book ->
                        AssistChip(onClick = { onSource(book.code) }, label = { Text(book.label) })
                    }
                }
            }
            Text("Reference odds are neutral comparison prices, not executable bookmaker offers.", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = onBuild,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumLime, contentColor = Color(0xFF10141F)),
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(21.dp), color = Color(0xFF10141F), strokeWidth = 2.dp)
            else { Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Build ticket to ${formatPremiumOdds(state.v3TargetOdds)}", fontWeight = FontWeight.ExtraBold) }
        }

        ticket?.let { PremiumBuiltTicket(it, onTarget, onBuild, onOpenEvent, onRemoveLeg, onSave, onSaved) }
        state.error?.let { PremiumCard { PremiumPill("Needs attention", "warn"); Text(it, color = PremiumMuted) } }
    }
}

@Composable
private fun BuilderSteps(active: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        listOf("Target", "Odds", "Ticket").forEachIndexed { index, label ->
            val step = index + 1
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).background(if (step <= active) PremiumLime else PremiumPanel2, CircleShape), contentAlignment = Alignment.Center) {
                    Text(step.toString(), color = if (step <= active) Color(0xFF10141F) else PremiumMuted, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(6.dp))
                Text(label, color = if (step <= active) PremiumInk else PremiumMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
            if (index < 2) HorizontalDivider(Modifier.weight(1f).padding(horizontal = 8.dp), color = PremiumLine)
        }
    }
}

@Composable
private fun PremiumBuiltTicket(
    ticket: com.getprediq.app.data.v3.V3TicketResponse,
    onTarget: (Double) -> Unit,
    onBuild: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onRemoveLeg: (V3TicketLeg) -> Unit,
    onSave: () -> Unit,
    onSaved: () -> Unit,
) {
    PremiumSectionTitle("PredIQ build")
    if (ticket.legs.isEmpty()) {
        PremiumCard {
            PremiumPill("No qualified build", "warn")
            Text(ticket.message ?: "PredIQ could not build this target from trustworthy priced selections.", color = PremiumMuted)
            Text("Try a lower target or a more permissive risk profile. PredIQ will not invent missing prices.", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val weak = ticket.weakestLegs.map { legKey(it) }.toSet()
    PremiumCard(brush = Brush.linearGradient(listOf(Color(0xFF202A45), Color(0xFF11182C)))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PredIQ build · ${formatPremiumOdds(ticket.combinedReferenceOdds ?: ticket.combinedOdds)}", color = PremiumInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${ticket.legCount} legs · reference odds", color = PremiumMuted)
            }
            PremiumPill(if (weak.isEmpty()) "Strong profile" else "Mixed confidence", if (weak.isEmpty()) "good" else "warn")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCell("Target", formatPremiumOdds(ticket.targetOdds), Modifier.weight(1f))
            MetricCell("Built", formatPremiumOdds(ticket.combinedReferenceOdds ?: ticket.combinedOdds), Modifier.weight(1f))
            MetricCell("Joint est.", ticket.jointProbability?.let { "${(it * 100).toInt()}%" } ?: "—", Modifier.weight(1f))
        }
    }

    ticket.legs.forEach { leg ->
        val isWeak = legKey(leg) in weak
        PremiumCard(onClick = { onOpenEvent(leg.eventId) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${leg.home} vs ${leg.away}", color = PremiumInk, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(leg.competition, color = PremiumMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                PremiumPill(confidenceLabel(leg, isWeak), confidenceTone(leg, isWeak))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(leg.selectionLabel.ifBlank { leg.selectionKey.replace('_', ' ') }, color = PremiumInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(leg.marketLabel.ifBlank { leg.marketKey.replace('_', ' ') }, color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text(formatPremiumOdds(leg.referenceOdds ?: leg.odds), color = PremiumLime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumPill("${(leg.probability * 100).toInt()}%", if (leg.probability >= .76) "good" else "purple")
                if (isWeak) PremiumPill("PredIQ is less sure", "warn")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onRemoveLeg(leg) }) { Icon(Icons.Outlined.RemoveCircleOutline, null, tint = PremiumRed); Spacer(Modifier.width(4.dp)); Text("Remove", color = PremiumRed) }
                TextButton(onClick = { onOpenEvent(leg.eventId) }) { Text("Full intelligence", color = Color(0xFFC8BCFF)); Icon(Icons.Outlined.ChevronRight, null, tint = Color(0xFFC8BCFF)) }
            }
        }
    }

    if (weak.isNotEmpty() && ticket.targetOdds > 5.0) {
        PremiumCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, null, tint = PremiumAmber)
                Spacer(Modifier.width(8.dp))
                Text("A stronger route is available at a lower target.", color = PremiumInk, fontWeight = FontWeight.Bold)
            }
            Text("The weakest legs are shown explicitly. PredIQ would rather lower the target than pretend every leg is equally strong.", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onTarget(5.0); onBuild() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumLime)) {
                Text("Use stronger 5.0 build")
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PremiumLime, contentColor = Color(0xFF10141F))) {
            Icon(Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text("Save")
        }
        OutlinedButton(onClick = onSaved, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumInk)) {
            Icon(Icons.Outlined.Bookmarks, null); Spacer(Modifier.width(6.dp)); Text("Saved")
        }
    }
    Text("Reference odds are non-executable benchmark prices. Combined probability is an approximation and every selection can lose.", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun PremiumTicketsScreen(
    state: PrediqContractState,
    onLoad: () -> Unit,
    onOpen: (V3SavedTicket) -> Unit,
    onDelete: (String) -> Unit,
    onBuild: () -> Unit,
) {
    LaunchedEffect(Unit) { onLoad() }
    PremiumScreen(title = "Tickets", subtitle = "Saved builds and audited outcomes") {
        val summary = state.resultsSummary
        PremiumCard(brush = Brush.linearGradient(listOf(Color(0xFF1D2948), Color(0xFF12182B)))) {
            Text("Track record", color = PremiumInk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell("Won", (summary?.record?.won ?: 0).toString(), Modifier.weight(1f))
                MetricCell("Lost", (summary?.record?.lost ?: 0).toString(), Modifier.weight(1f))
                MetricCell("Settled", (summary?.record?.settled ?: 0).toString(), Modifier.weight(1f))
            }
        }
        PremiumSectionTitle("Saved builds")
        val saved = state.v3SavedTickets?.tickets.orEmpty()
        if (saved.isEmpty()) {
            PremiumCard {
                Text("No saved tickets yet", color = PremiumInk, fontWeight = FontWeight.Bold)
                Text("Build a ticket, inspect the confidence labels, then save the version you want to track.", color = PremiumMuted)
                Button(onClick = onBuild, colors = ButtonDefaults.buttonColors(containerColor = PremiumLime, contentColor = Color(0xFF10141F))) { Text("Build a ticket") }
            }
        } else saved.forEach { item ->
            PremiumCard(onClick = { onOpen(item) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title ?: "PredIQ ${formatPremiumOdds(item.combinedOdds)} build", color = PremiumInk, fontWeight = FontWeight.Bold)
                        Text("Target ${formatPremiumOdds(item.targetOdds)} · ${item.riskProfile?.replaceFirstChar { it.uppercase() } ?: "Balanced"}", color = PremiumMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(item.id) }) { Icon(Icons.Outlined.DeleteOutline, "Delete", tint = PremiumRed) }
                }
            }
        }
    }
}

@Composable
fun PremiumAccountScreen(
    state: PrediqContractState,
    onProfile: () -> Unit,
    onFollowing: () -> Unit,
    onNotifications: () -> Unit,
    onPlans: () -> Unit,
    onResearch: () -> Unit,
    onLogout: () -> Unit,
) {
    val account = state.account
    PremiumScreen(title = "Account", subtitle = account?.profile?.email ?: "PredIQ access and preferences") {
        PremiumCard(brush = Brush.linearGradient(listOf(Color(0xFF252F52), Color(0xFF171E36)))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).background(PremiumPurple, CircleShape), contentAlignment = Alignment.Center) {
                    Text(account?.profile?.name?.take(1)?.uppercase() ?: "P", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account?.profile?.name ?: "PredIQ member", color = PremiumInk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text(account?.membership?.planName ?: account?.membership?.state?.replaceFirstChar { it.uppercase() } ?: "Free", color = PremiumMuted)
                }
                PremiumPill(if (account?.membership?.fullAccess == true) "Full access" else "Limited", if (account?.membership?.fullAccess == true) "good" else "warn")
            }
        }
        AccountAction(Icons.Outlined.Person, "Profile", "Identity, country and account details", onProfile)
        AccountAction(Icons.Outlined.Bookmarks, "Following", "Teams, competitions and matches you track", onFollowing)
        AccountAction(Icons.Outlined.Notifications, "Alerts", "Live changes, lineups and results", onNotifications)
        AccountAction(Icons.Outlined.Search, "Research", "Teams, leagues and player intelligence", onResearch)
        AccountAction(Icons.Outlined.WorkspacePremium, "Plan", "Access, subscription and billing", onPlans)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumRed)) { Text("Sign out") }
    }
}

@Composable
private fun AccountAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    PremiumCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(PremiumPanel2, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = PremiumLime) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = PremiumInk, fontWeight = FontWeight.Bold); Text(subtitle, color = PremiumMuted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Outlined.ChevronRight, null, tint = PremiumMuted)
        }
    }
}

private fun allLiveCards(state: PrediqContractState): List<V2LiveCard> {
    val all = state.live?.following.orEmpty() + state.live?.opportunities.orEmpty() + state.live?.games.orEmpty()
    return all.distinctBy { it.event.id.ifBlank { it.id } }
}

private fun scorePair(card: V2LiveCard): Pair<Int?, Int?> = scoreInt(card.event.score?.home) to scoreInt(card.event.score?.away)

private fun scoreInt(value: kotlinx.serialization.json.JsonElement?): Int? {
    val raw = value?.toString()?.trim('"') ?: return null
    return raw.toIntOrNull() ?: raw.toDoubleOrNull()?.toInt()
}

private fun formatPremiumOdds(value: Double?): String = value?.let {
    if (kotlin.math.abs(it - it.toInt()) < .001) it.toInt().toString() else String.format(java.util.Locale.US, "%.2f", it)
} ?: "—"

private fun legKey(leg: V3TicketLeg) = "${leg.eventId}|${leg.marketKey}|${leg.selectionKey}|${leg.line}"
private fun confidenceLabel(leg: V3TicketLeg, weak: Boolean): String = when {
    weak || leg.probability < .64 -> "Caution"
    leg.probability < .74 -> "Lean"
    else -> "Strong"
}
private fun confidenceTone(leg: V3TicketLeg, weak: Boolean): String = when (confidenceLabel(leg, weak)) { "Strong" -> "good"; "Lean" -> "purple"; else -> "warn" }
