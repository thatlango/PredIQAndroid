package com.getprediq.app.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.theme.*
import com.getprediq.app.ui.v2.media.*

@Composable
fun PredictionDetailV2Screen(
    vm: PrediqContractViewModel,
    onBack: () -> Unit
) {
    val state = vm.state
    val detail = state.prediction

    if (state.busy && detail == null) {
        PrediqLoadingState(message = "Building match intelligence...")
        return
    }

    if (detail == null) {
        PrediqErrorState(message = "Match intelligence could not be loaded.", onAction = onBack, actionLabel = "Back")
        return
    }

    val event = detail.decision.event

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
            DetailHeader(
                title = "Match Intelligence",
                onBack = onBack,
                onShare = {}
            )
        }

        item {
            MatchHero(event)
            Spacer(Modifier.height(LocalV2Spacing.current.xl))
        }

        item {
            PrediqDecisionHero(detail.decision)
            Spacer(Modifier.height(LocalV2Spacing.current.xl))
        }

        if (detail.reasons.isNotEmpty()) {
            item {
                PrediqSectionHeader(title = "Why PredIQ sees it")
                PrediqElevatedSurface {
                    detail.reasons.forEach { reason ->
                        Text(text = "• ${reason.label}", style = V2Typography.bodyMedium)
                        Spacer(Modifier.height(LocalV2Spacing.current.xs))
                    }
                }
                Spacer(Modifier.height(LocalV2Spacing.current.xl))
            }
        }
        
        item {
            Spacer(Modifier.height(LocalV2Spacing.current.xxl))
        }
    }
}

@Composable
fun DetailHeader(title: String, onBack: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LocalV2Spacing.current.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, null, tint = V2White)
        }
        Text(text = title, style = V2Typography.titleMedium)
        androidx.compose.material3.IconButton(onClick = onShare) {
            Icon(Icons.Outlined.Share, null, tint = V2White)
        }
    }
}

@Composable
fun MatchHero(event: com.getprediq.app.data.v2.V2Event) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${event.competition.name ?: "Competition"} · ${event.startsAt?.take(16) ?: ""}",
            style = V2Typography.labelMedium,
            color = V2TextSecondary
        )
        Spacer(Modifier.height(LocalV2Spacing.current.l))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                TeamLogo(name = event.participants.home.name, sport = event.sport ?: "football", size = 64.dp)
                Spacer(Modifier.height(LocalV2Spacing.current.s))
                Text(text = event.participants.home.name, style = V2Typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(text = "VS", style = V2Typography.labelLarge, color = V2TextMuted)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                TeamLogo(name = event.participants.away.name, sport = event.sport ?: "football", size = 64.dp)
                Spacer(Modifier.height(LocalV2Spacing.current.s))
                Text(text = event.participants.away.name, style = V2Typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PrediqDecisionHero(card: com.getprediq.app.data.v2.V2DecisionCard) {
    PrediqHeroSurface(
        color = V2SurfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = "CURRENT PREDIQ CALL", style = V2Typography.labelSmall, color = V2DecisionLime)
                Text(text = card.pick.label ?: "Assessment", style = V2Typography.headlineMedium, color = V2White)
                Text(text = card.pick.market ?: "", style = V2Typography.bodyMedium, color = V2TextSecondary)
            }
            Box(
                modifier = Modifier
                    .background(V2DecisionLime, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = card.strength ?: "Strong", style = V2Typography.labelSmall, color = V2Black)
            }
        }
        
        Spacer(Modifier.height(LocalV2Spacing.current.xl))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalV2Spacing.current.s)
        ) {
            DetailMetric(label = "PROBABILITY", value = "${card.chance.percent ?: 0}%", modifier = Modifier.weight(1f))
            DetailMetric(label = "RISK", value = card.risk.label, modifier = Modifier.weight(1f))
            DetailMetric(label = "VALUE", value = card.value.label.take(8), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DetailMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text = label, style = V2Typography.labelSmall, color = V2TextMuted)
        Text(text = value, style = V2Typography.titleMedium, color = V2White)
    }
}
