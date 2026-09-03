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
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.PrediqUiState
import com.getprediq.app.PrediqViewModel
import com.getprediq.app.data.PlanDto
import com.getprediq.app.data.v3.*
import com.getprediq.app.ui.TeamCrest

private val BuilderBrush = Brush.linearGradient(listOf(Color(0xFF4F2BEA), Color(0xFF2457E8)))
private val BuilderAccent = Color(0xFFB7F32D)

@Composable
fun OddsBuilderContractScreen(
    state: PrediqContractState,
    onBack: () -> Unit,
    onTarget: (Double) -> Unit,
    onRisk: (String) -> Unit,
    onSource: (String) -> Unit,
    onBuild: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onRemoveLeg: (V3TicketLeg) -> Unit,
    onSave: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var customTarget by rememberSaveable { mutableStateOf("") }
    val ticket = state.v3Ticket
    val sources = state.v3Bookmakers?.bookmakers.orEmpty()
    LazyColumn(
        Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PrediqHeader(title = "Odds Builder", subtitle = "Tell PredIQ the target. Intelligence builds the route.", showBack = true, onBack = onBack) }
        item {
            BrightCard(brush = BuilderBrush) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(Color.White.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AutoGraph, null, tint = BuilderAccent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Build to ${formatOdds(state.v3TargetOdds)}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("PredIQ can mix supported markets instead of stacking obvious favourites.", color = Color.White.copy(alpha = .82f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(humanize(state.v3Risk), if (state.v3Risk == "safer") "good" else if (state.v3Risk == "aggressive") "warn" else "purple", true)
                    StatusPill("Reference odds", "purple", true)
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Target odds")
                Text("Choose the combined reference-odds target you want PredIQ to work towards.", color = Muted, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(3.0, 5.0, 10.0, 20.0, 35.0, 50.0, 75.0)) { target ->
                        FilterChip(selected = state.v3TargetOdds == target, onClick = { onTarget(target) }, label = { Text(formatOdds(target)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customTarget,
                        onValueChange = { customTarget = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) },
                        label = { Text("Custom") },
                        placeholder = { Text("e.g. 25") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Button(onClick = { customTarget.toDoubleOrNull()?.takeIf { it in 1.5..500.0 }?.let(onTarget) }, enabled = customTarget.toDoubleOrNull()?.let { it in 1.5..500.0 } == true) { Text("Set") }
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Risk profile")
                Text("This changes the minimum probability PredIQ accepts into the combination.", color = Muted, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("safer", "balanced", "aggressive")) { risk ->
                        FilterChip(
                            selected = state.v3Risk == risk,
                            onClick = { onRisk(risk) },
                            label = { Text(when (risk) { "safer" -> "Safer"; "aggressive" -> "Aggressive"; else -> "Balanced" }) },
                            leadingIcon = if (state.v3Risk == risk) {{ Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp)) }} else null,
                        )
                    }
                }
            }
        }
        if (sources.isNotEmpty()) {
            item {
                WhiteCard {
                    SectionHeading("Price source")
                    Text(state.v3Bookmakers?.note?.takeIf { it.isNotBlank() } ?: "Reference odds are neutral market benchmarks, not executable bookmaker offers.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sources, key = { it.code }) { source ->
                            val sourceLabel = source.label.takeIf { it.isNotBlank() } ?: humanize(source.code)
                            FilterChip(
                                selected = state.v3Bookmaker == source.code,
                                onClick = { onSource(source.code) },
                                label = { Text(sourceLabel) },
                            )
                        }
                    }
                    sources.firstOrNull { it.code == state.v3Bookmaker }?.let { source ->
                        Text(
                            buildString {
                                append(if (source.referenceOnly) "Reference only" else if (source.executable) "Named source" else "Analysis source")
                                if (source.snapshots > 0) append(" · ${source.snapshots} snapshots")
                                if (source.providers.isNotEmpty()) append(" · ${source.providers.size} provider${if (source.providers.size == 1) "" else "s"}")
                                source.lastSeen?.let { append(" · ${friendlyDateTime(it)}") }
                            },
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = onBuild,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
            ) {
                if (state.busy) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
                else { Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Build ${formatOdds(state.v3TargetOdds)}") }
            }
        }
        state.error?.let { message -> item { EmptyState("Builder needs attention", message, Icons.Outlined.ErrorOutline) } }
        if (ticket != null) {
            item { SectionHeading("PredIQ build") }
            item {
                BrightCard(brush = if (ticket.legs.isEmpty()) Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF1F2937))) else LiveBrush) {
                    Text(if (ticket.legs.isEmpty()) "No combination cleared this profile" else "${ticket.legCount} legs · ${formatOdds(ticket.combinedReferenceOdds ?: ticket.combinedOdds)}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(ticket.message ?: if (ticket.legs.isEmpty()) "PredIQ will not invent missing prices or force weak selections." else "Built against ${humanize(ticket.priceSource)} reference pricing.", color = Color.White.copy(alpha = .82f))
                    if (ticket.legs.isNotEmpty()) {
                        MetricStrip(listOf(
                            Triple("Target", formatOdds(ticket.targetOdds), Icons.Outlined.Flag),
                            Triple("Built", formatOdds(ticket.combinedReferenceOdds ?: ticket.combinedOdds), Icons.Outlined.StackedLineChart),
                            Triple("Joint est.", probabilityLabel(ticket.jointProbability), Icons.Outlined.Percent),
                        ))
                    }
                }
            }
            if (ticket.legs.isNotEmpty()) {
                items(ticket.legs, key = { "${it.eventId}-${it.marketKey}-${it.selectionKey}" }) { leg ->
                    val weak = ticket.weakestLegs.any { it.eventId == leg.eventId && it.marketKey == leg.marketKey && it.selectionKey == leg.selectionKey }
                    WhiteCard(onClick = { onOpenEvent(leg.eventId) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamCrest(leg.home, "football", size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${leg.home} vs ${leg.away}", color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(leg.competition, friendlyDateTime(leg.startsAt)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (weak) StatusPill("Weakest leg", "warn")
                        }
                        HorizontalDivider(color = Hairline)
                        Text(leg.selectionLabel.ifBlank { humanize(leg.selectionKey) }, color = PurpleDeep, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(humanize(leg.marketLabel.ifBlank { leg.marketKey }), color = Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(probabilityLabel(leg.probability), if (leg.probability >= .76) "good" else "purple")
                            StatusPill("Ref ${formatOdds(leg.referenceOdds ?: leg.odds)}", "neutral")
                            StatusPill(humanize(leg.decisionStatus), decisionTone(leg.decisionStatus))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onRemoveLeg(leg) }) { Icon(Icons.Outlined.RemoveCircleOutline, null, tint = Red); Spacer(Modifier.width(4.dp)); Text("Remove", color = Red) }
                            TextButton(onClick = { onOpenEvent(leg.eventId) }) { Text("Full intelligence"); Icon(Icons.Outlined.ChevronRight, null) }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onBuild, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(5.dp)); Text("Regenerate") }
                        Button(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(5.dp)); Text("Save") }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                append("PredIQ ${formatOdds(ticket.combinedReferenceOdds ?: ticket.combinedOdds)} reference-odds build\n")
                                ticket.legs.forEachIndexed { index, leg -> append("${index + 1}. ${leg.home} vs ${leg.away} — ${leg.selectionLabel} @ ${formatOdds(leg.referenceOdds ?: leg.odds)}\n") }
                                append("Reference odds only. Outcomes are not guaranteed.")
                            }
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share PredIQ build"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(7.dp)); Text("Share build") }
                }
            }
        }
        item { TextButton(onClick = onSaved, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Bookmarks, null); Spacer(Modifier.width(7.dp)); Text("Saved odds builds") } }
        item {
            Text("PredIQ Reference Odds are neutral benchmark prices compiled for analysis. They are not an offer to place a bet. Combined probability is an approximation and every selection can lose.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun V3EventContractScreen(state: PrediqContractState, onBack: () -> Unit, onFollowEvent: (String, String) -> Unit) {
    val data = state.v3Event
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Match intelligence", subtitle = "Markets, evidence and alternatives", showBack = true, onBack = onBack) }
        if (data == null) {
            item { if (state.busy) LoadingState("Opening V3 intelligence…") else EmptyState("Intelligence unavailable", state.error ?: "This event could not be loaded.") }
            return@LazyColumn
        }
        item {
            BrightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamCrest(data.event.home, data.event.sportCode, size = 52.dp, dark = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${data.event.home} vs ${data.event.away}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("${data.event.competition} · ${friendlyDateTime(data.event.startsAt)}", color = Color.White.copy(alpha = .78f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(humanize(data.status), decisionTone(data.status), true)
                    StatusPill(data.priceSourceLabel, "purple", true)
                }
                val following = state.follows?.follows.orEmpty().any { it.entityType == "event" && it.entityKey == data.event.id }
                Button(
                    onClick = { if (!following) onFollowEvent(data.event.id, "${data.event.home} vs ${data.event.away}") },
                    enabled = !following,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PurpleDeep, disabledContainerColor = Color.White.copy(alpha = .18f), disabledContentColor = Color.White),
                ) { Icon(if (following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(7.dp)); Text(if (following) "Following match" else "Follow match") }
            }
        }
        data.primary?.let { choice -> item { IntelligenceChoiceCard("Best angle", choice, true) } }
        if (data.saferAlternative != null || data.higherReturnAlternative != null) {
            item { SectionHeading("Alternatives") }
            data.saferAlternative?.let { choice -> item { IntelligenceChoiceCard("Safer route", choice, false) } }
            data.higherReturnAlternative?.let { choice -> item { IntelligenceChoiceCard("Higher return", choice, false) } }
        }
        if (data.why.isNotEmpty()) {
            item { WhiteCard { SectionHeading("Why PredIQ sees it this way"); data.why.take(8).forEach { InfoRow(Icons.Outlined.CheckCircle, it, null) } } }
        }
        if (data.watchOuts.isNotEmpty()) {
            item { WhiteCard { SectionHeading("Watch outs"); data.watchOuts.take(8).forEach { InfoRow(Icons.Outlined.WarningAmber, it, null) } } }
        }
        if (data.marketGroups.isNotEmpty()) {
            item { SectionHeading("Market intelligence") }
            items(data.marketGroups, key = { "${it.marketKey}-${it.line}" }) { group ->
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(group.marketLabel.ifBlank { humanize(group.marketKey) }, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(group.reason.takeIf { it.isNotBlank() } ?: humanize(group.decisionStatus), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill(if (group.qualified) "Qualified" else humanize(group.decisionStatus), if (group.qualified) "good" else decisionTone(group.decisionStatus))
                    }
                    group.recommended?.let { choice ->
                        HorizontalDivider(color = Hairline)
                        Text(choice.selectionLabel, color = PurpleDeep, fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(probabilityLabel(choice.probability), if (choice.probability >= .76) "good" else "purple")
                            StatusPill("Ref ${formatOdds(choice.referenceOdds ?: choice.odds)}")
                            choice.edgePercentagePoints?.let { StatusPill("${if (it >= 0) "+" else ""}${"%.1f".format(it)} edge", if (it > 0) "good" else "neutral") }
                        }
                    }
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Evidence coverage")
                val e = data.evidence
                InfoRow(Icons.Outlined.DataUsage, "Evidence", e?.band?.let(::humanize) ?: "Limited", e?.sampleDepth?.let { "$it samples" })
                InfoRow(Icons.Outlined.Groups, "Lineups", if (e?.lineupConfirmed == true) "Confirmed" else "Not confirmed")
                InfoRow(Icons.Outlined.Public, "Context", data.context?.status?.let(::humanize) ?: "Minimal", data.context?.coverage?.let { probabilityLabel(it) })
                data.lastAnalysedAt?.let { InfoRow(Icons.Outlined.Schedule, "Last analysed", friendlyDateTime(it)) }
            }
        }
    }
}

@Composable
private fun IntelligenceChoiceCard(title: String, choice: V3Choice, featured: Boolean) {
    if (featured) {
        BrightCard(brush = LiveBrush) {
            Text(title.uppercase(), color = BuilderAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(choice.selectionLabel.ifBlank { humanize(choice.selectionKey) }, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(choice.marketLabel.ifBlank { humanize(choice.marketKey) }, color = Color.White.copy(alpha = .78f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(probabilityLabel(choice.probability), "good", true)
                StatusPill("Ref ${formatOdds(choice.referenceOdds ?: choice.odds)}", "purple", true)
            }
            choice.reason?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha = .82f)) }
        }
    } else {
        WhiteCard {
            Text(title, color = PurpleDeep, fontWeight = FontWeight.Bold)
            Text(choice.selectionLabel.ifBlank { humanize(choice.selectionKey) }, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("${probabilityLabel(choice.probability)} · Ref ${formatOdds(choice.referenceOdds ?: choice.odds)} · ${humanize(choice.marketLabel.ifBlank { choice.marketKey })}", color = Muted)
        }
    }
}

@Composable
fun SavedOddsContractScreen(state: PrediqContractState, onBack: () -> Unit, onDelete: (String) -> Unit, onOpen: (V3SavedTicket) -> Unit) {
    val saved = state.v3SavedTickets?.tickets.orEmpty()
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Saved odds builds", subtitle = "Draft combinations you asked PredIQ to keep", showBack = true, onBack = onBack) }
        if (state.v3SavedTickets == null && state.busy) item { LoadingState("Loading saved builds…") }
        else if (saved.isEmpty()) item { EmptyState("Nothing saved yet", "Build a target-odds combination and save it here.", Icons.Outlined.Bookmarks) }
        else items(saved, key = { it.id }) { ticket ->
            WhiteCard(onClick = { onOpen(ticket) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoGraph, null, tint = Purple) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ticket.title ?: "PredIQ odds build", color = Ink, fontWeight = FontWeight.Bold)
                        Text("${humanize(ticket.riskProfile)} · ${friendlyDateTime(ticket.updatedAt ?: ticket.createdAt)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(ticket.id) }) { Icon(Icons.Outlined.DeleteOutline, "Delete", tint = Red) }
                }
                val combined = ticket.combinedOdds ?: ticket.payload.combinedReferenceOdds ?: ticket.payload.combinedOdds
                val legs = ticket.payload.legCount.takeIf { it > 0 } ?: ticket.payload.legs.size
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("Built ${formatOdds(combined)}", "purple")
                    (ticket.targetOdds ?: ticket.payload.targetOdds.takeIf { it > 0 })?.let { StatusPill("Target ${formatOdds(it)}") }
                    StatusPill("$legs legs")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onOpen(ticket) }) { Text("Open build"); Icon(Icons.Outlined.ChevronRight, null) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentContractSheet(plan: PlanDto, state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    var phone by rememberSaveable { mutableStateOf(state.account?.user?.phone.orEmpty()) }
    LaunchedEffect(plan.code) { vm.clearPaymentMessage() }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Ivory) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.WorkspacePremium, null, tint = Purple) }
                Spacer(Modifier.width(12.dp))
                Column { Text(plan.name, style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.ExtraBold); Text("${plan.durationDays} days full access", color = Muted) }
            }
            BrightCard {
                Text("TOTAL", color = BuilderAccent, fontWeight = FontWeight.Bold)
                Text("UGX ${"%,d".format(plan.priceUgx)}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Paid access begins only after the payment provider confirms settlement.", color = Color.White.copy(alpha = .8f))
            }
            OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(18) }, label = { Text("Mobile Money number") }, leadingIcon = { Icon(Icons.Outlined.PhoneAndroid, null) }, placeholder = { Text("0772 123 456") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
            state.paymentMessage?.let { Text(it, color = if (it.contains("fail", true) || it.contains("not configured", true)) Red else GreenDeep) }
            Button(onClick = { vm.checkout(plan.code, phone) }, enabled = !state.paymentBusy && state.paymentCapabilities.mobileMoney && phone.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (state.paymentBusy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Request payment")
            }
            if (!state.paymentCapabilities.mobileMoney) Text(state.paymentCapabilities.message.ifBlank { "Mobile Money checkout is not available right now." }, color = Red)
        }
    }
}

private fun probabilityLabel(value: Double?): String = value?.let { v -> "${((if (v <= 1.0) v else v / 100.0) * 100).toInt()}%" } ?: "–"
private fun formatOdds(value: Double?): String = value?.takeIf { it > 0 }?.let { if (kotlin.math.abs(it - it.toInt()) < .001) it.toInt().toString() else "%.2f".format(it) } ?: "–"
