package com.getprediq.app.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.data.v3.V3TicketLeg
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.theme.*
import com.getprediq.app.ui.v2.media.*

@Composable
fun BuildV2Screen(vm: PrediqContractViewModel) {
    val state = vm.state
    val ticket = state.v3Ticket

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(V2Background),
        contentPadding = PaddingValues(
            horizontal = LocalV2Spacing.current.pageHorizontal,
            vertical = LocalV2Spacing.current.m
        )
    ) {
        item {
            Text(text = "Odds Builder", style = V2Typography.headlineMedium)
            Text(text = "Algorithmic ticket generation based on your target", style = V2Typography.bodyMedium)
            Spacer(Modifier.height(LocalV2Spacing.current.l))
        }

        item {
            TargetSettingsCard(
                targetOdds = state.v3TargetOdds,
                riskProfile = state.v3Risk,
                onTargetChange = vm::setV3Target,
                onRiskChange = vm::setV3Risk,
                onBuild = vm::buildV3Ticket,
                busy = state.busy
            )
            Spacer(Modifier.height(LocalV2Spacing.current.xl))
        }

        if (ticket != null && ticket.legs.isNotEmpty()) {
            item {
                TicketSummaryHeader(ticket)
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
            items(ticket.legs) { leg ->
                LegCard(leg, onRemove = { vm.removeV3Leg(leg) })
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
            item {
                Spacer(Modifier.height(LocalV2Spacing.current.l))
                PrediqPrimaryButton(
                    onClick = vm::saveV3Ticket,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Save, null)
                    Spacer(Modifier.width(LocalV2Spacing.current.xs))
                    Text("Save this ticket")
                }
            }
        } else if (!state.busy && ticket == null) {
            item {
                PrediqEmptyState(
                    title = "Ready to build",
                    message = "Set your target odds and risk profile above to generate a smart ticket."
                )
            }
        }
        
        if (state.busy) {
            item {
                Box(Modifier.fillMaxWidth().padding(LocalV2Spacing.current.xl), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = V2DecisionLime)
                }
            }
        }
    }
}

@Composable
fun TargetSettingsCard(
    targetOdds: Double,
    riskProfile: String,
    onTargetChange: (Double) -> Unit,
    onRiskChange: (String) -> Unit,
    onBuild: () -> Unit,
    busy: Boolean
) {
    PrediqElevatedSurface {
        Text(text = "TARGET ODDS", style = V2Typography.labelSmall)
        Spacer(Modifier.height(LocalV2Spacing.current.s))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalV2Spacing.current.xs)
        ) {
            listOf(5.0, 10.0, 20.0, 35.0, 50.0).forEach { value ->
                TargetChip(
                    label = value.toInt().toString(),
                    selected = targetOdds == value,
                    onClick = { onTargetChange(value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(LocalV2Spacing.current.l))
        
        Text(text = "RISK PROFILE", style = V2Typography.labelSmall)
        Spacer(Modifier.height(LocalV2Spacing.current.s))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalV2Spacing.current.xs)
        ) {
            listOf("safer", "balanced", "aggressive").forEach { risk ->
                TargetChip(
                    label = risk.replaceFirstChar { it.uppercase() },
                    selected = riskProfile == risk,
                    onClick = { onRiskChange(risk) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(LocalV2Spacing.current.xl))
        
        PrediqPrimaryButton(
            onClick = onBuild,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("Build Ticket")
        }
    }
}

@Composable
fun TargetChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = V2Shapes.small,
        color = if (selected) V2DecisionLime else V2SurfacePrimary,
        border = if (selected) null else BorderStroke(1.dp, V2Divider)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = V2Typography.labelLarge,
                color = if (selected) V2Black else V2TextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun TicketSummaryHeader(ticket: com.getprediq.app.data.v3.V3TicketResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "GENERATED TICKET", style = V2Typography.labelSmall, color = V2DecisionLime)
            Text(text = "${ticket.legCount} legs · ${ticket.riskProfile.replaceFirstChar { it.uppercase() }}", style = V2Typography.bodyMedium)
        }
        Text(
            text = "%.2f".format(ticket.combinedOdds ?: 0.0),
            style = V2Typography.headlineMedium,
            color = V2White
        )
    }
}

@Composable
fun LegCard(leg: V3TicketLeg, onRemove: () -> Unit) {
    PrediqElevatedSurface(
        color = V2SurfacePrimary,
        shape = V2Shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = "${leg.home} vs ${leg.away}", style = V2Typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = leg.competition, style = V2Typography.labelMedium)
            }
            androidx.compose.material3.IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Close, null, tint = V2TextMuted, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(Modifier.height(LocalV2Spacing.current.m))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = leg.selectionLabel, style = V2Typography.titleSmall, color = V2DecisionLime)
                Text(text = leg.marketLabel, style = V2Typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "%.2f".format(leg.odds ?: 0.0), style = V2Typography.titleMedium, color = V2White)
                Text(text = "Prob: ${ (leg.probability * 100).toInt() }%", style = V2Typography.labelSmall)
            }
        }
    }
}
