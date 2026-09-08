package com.getprediq.app.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.data.v2.V2DecisionCard
import com.getprediq.app.data.v3.V3SlateCard
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.media.*
import com.getprediq.app.ui.v2.theme.*

@Composable
fun TodayV2Screen(
    vm: PrediqContractViewModel,
    onDecision: (String) -> Unit
) {
    val state = vm.state
    val today = state.today
    val modelViews = state.v3Slate?.cards?.filter { it.primary != null }.orEmpty()
    val waitingViews = today?.waiting.orEmpty()

    LaunchedEffect(state.ready, today) {
        if (state.ready && today == null && !state.busy && !state.refreshing) {
            vm.loadToday()
        }
    }

    if (state.busy && today == null && state.v3Slate == null) {
        PrediqLoadingState()
        return
    }

    if (today == null && state.v3Slate == null && !state.busy && state.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(V2Background),
            contentAlignment = Alignment.Center
        ) {
            PrediqErrorState(
                message = state.error ?: "PredIQ could not load today's analysis.",
                actionLabel = "Retry",
                onAction = { vm.bootstrap(force = true) }
            )
        }
        return
    }

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
            TodayHeader(
                displayName = today?.viewer?.displayName ?: "there",
                onNotifications = {}
            )
        }

        item {
            Spacer(Modifier.height(LocalV2Spacing.current.l))
            TodayHero(today?.briefing)
        }

        if (today?.topPicks?.isNotEmpty() == true) {
            item {
                PrediqSectionHeader(title = "Official picks")
            }
            items(today.topPicks, key = { it.id }) { card ->
                DecisionCard(card, onClick = { onDecision(card.predictionId ?: card.id) })
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
        }

        if (modelViews.isNotEmpty()) {
            item {
                Spacer(Modifier.height(LocalV2Spacing.current.s))
                PrediqSectionHeader(title = "Model view")
                ModelViewNotice()
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
            items(modelViews.take(16), key = { "model-${it.event.id}" }) { card ->
                ModelViewCard(card)
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
        } else if (waitingViews.isNotEmpty()) {
            item {
                Spacer(Modifier.height(LocalV2Spacing.current.s))
                PrediqSectionHeader(title = "Model view")
                ModelViewNotice()
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
            items(waitingViews.take(16), key = { "waiting-${it.id}" }) { card ->
                WatchingCard(card, onClick = { onDecision(card.predictionId ?: card.id) })
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
        }

        if (
            today != null &&
            today.topPicks.isEmpty() &&
            modelViews.isEmpty() &&
            waitingViews.isEmpty() &&
            !state.busy &&
            !state.refreshing
        ) {
            item {
                PrediqEmptyState(
                    title = "No fixtures to analyse right now",
                    message = "PredIQ is connected. When a supported fixture enters the analysis window, its model outcome will appear here even if it does not clear the official-pick gate."
                )
            }
        }

        item {
            Spacer(Modifier.height(LocalV2Spacing.current.xxl))
        }
    }
}

@Composable
private fun ModelViewNotice() {
    PrediqSurface(
        color = V2DecisionSoft,
        shape = V2Shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Current model outcomes are shown here even when PredIQ's stricter publication gate does not promote them to official picks.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = V2Typography.bodyMedium,
            color = V2TextSecondary,
        )
    }
}

@Composable
private fun ModelViewCard(card: V3SlateCard) {
    val primary = card.primary ?: return
    val percent = (primary.probability * 100).toInt().coerceIn(0, 100)
    val statusLabel = when (primary.decisionStatus) {
        "routable" -> "Clears gate"
        "lean" -> "Lean"
        "analysis_only" -> "Model view"
        else -> "Model view"
    }

    PrediqElevatedSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.event.competition.ifBlank { "Football" },
                    style = V2Typography.labelMedium,
                    color = V2TextMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${card.event.home} vs ${card.event.away}",
                    style = V2Typography.titleMedium,
                    color = V2TextPrimary,
                )
            }
            Spacer(Modifier.width(LocalV2Spacing.current.s))
            PrediqBadge(
                text = statusLabel,
                containerColor = V2DecisionSoft,
                contentColor = V2DecisionLime,
            )
        }

        Spacer(Modifier.height(LocalV2Spacing.current.l))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primary.marketLabel.ifBlank { "Model outcome" },
                    style = V2Typography.labelMedium,
                    color = V2TextMuted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = primary.selectionLabel.ifBlank { primary.selectionKey },
                    style = V2Typography.titleLarge,
                    color = V2DecisionLime,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$percent%",
                    style = V2Typography.headlineMedium,
                    color = V2TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "model probability",
                    style = V2Typography.labelSmall,
                    color = V2TextMuted,
                )
            }
        }

        primary.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Spacer(Modifier.height(LocalV2Spacing.current.m))
            Text(
                text = reason,
                style = V2Typography.bodyMedium,
                color = V2TextSecondary,
            )
        }
    }
}

@Composable
private fun WatchingCard(card: V2DecisionCard, onClick: () -> Unit) {
    PrediqElevatedSurface(
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                TeamLogo(name = card.event.participants.home.name, sport = card.event.sport ?: "football", size = 24.dp)
                Spacer(Modifier.width(LocalV2Spacing.current.s))
                Text(
                    text = "${card.event.participants.home.name} vs ${card.event.participants.away.name}",
                    style = V2Typography.titleMedium,
                    color = V2TextPrimary,
                )
            }
            Spacer(Modifier.width(LocalV2Spacing.current.s))
            PrediqBadge(
                text = "Model view",
                containerColor = V2DecisionSoft,
                contentColor = V2DecisionLime,
            )
        }
        Spacer(Modifier.height(LocalV2Spacing.current.l))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.pick.market ?: "Model outcome", style = V2Typography.labelMedium, color = V2TextMuted)
                Text(text = card.pick.label ?: card.pick.selection ?: "Selection", style = V2Typography.titleLarge, color = V2DecisionLime)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${card.chance.percent ?: 0}%", style = V2Typography.headlineMedium, color = V2TextPrimary)
                Text(text = "model probability", style = V2Typography.labelSmall, color = V2TextMuted)
            }
        }
    }
}

@Composable
fun TodayHeader(displayName: String, onNotifications: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good ${greeting()}, ${displayName.substringBefore(" ")}",
                style = V2Typography.titleLarge
            )
            Text(
                text = "Your decision desk for today",
                style = V2Typography.bodyMedium
            )
        }
        androidx.compose.material3.IconButton(onClick = onNotifications) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = V2TextPrimary
            )
        }
    }
}

@Composable
fun TodayHero(briefing: com.getprediq.app.data.v2.V2Briefing?) {
    PrediqHeroSurface(
        color = V2BrandViolet,
    ) {
        Text(
            text = "DAILY INTELLIGENCE",
            style = V2Typography.labelSmall,
            color = V2White.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(LocalV2Spacing.current.xs))
        Text(
            text = briefing?.headline ?: "PredIQ is scanning the slate",
            style = V2Typography.headlineMedium,
            color = V2White
        )
        Spacer(Modifier.height(LocalV2Spacing.current.l))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalV2Spacing.current.s)
        ) {
            HeroMetric(
                label = "Official",
                value = briefing?.topPicks?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
            HeroMetric(
                label = "Checked",
                value = briefing?.gamesChecked?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
            HeroMetric(
                label = "Changes",
                value = briefing?.changedSince?.toString() ?: "0",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text = label, style = V2Typography.labelSmall, color = V2White.copy(alpha = 0.64f))
        Text(text = value, style = V2Typography.titleLarge, color = V2White)
    }
}

@Composable
fun DecisionCard(card: V2DecisionCard, onClick: () -> Unit) {
    PrediqElevatedSurface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(name = card.event.participants.home.name, sport = card.event.sport ?: "football", size = 24.dp)
                Spacer(Modifier.width(LocalV2Spacing.current.s))
                Text(text = card.event.participants.home.name, style = V2Typography.bodyLarge)
            }
            Text(text = "vs", style = V2Typography.labelMedium, color = V2TextMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = card.event.participants.away.name, style = V2Typography.bodyLarge)
                Spacer(Modifier.width(LocalV2Spacing.current.s))
                TeamLogo(name = card.event.participants.away.name, sport = card.event.sport ?: "football", size = 24.dp)
            }
        }

        Spacer(Modifier.height(LocalV2Spacing.current.l))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(text = card.pick.label ?: "Selection", style = V2Typography.titleMedium, color = V2DecisionLime)
                Text(text = card.pick.market ?: "", style = V2Typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${card.chance.percent ?: 0}%",
                    style = V2Typography.headlineMedium,
                    color = V2White
                )
                Text(
                    text = card.strength ?: "Strong",
                    style = V2Typography.labelSmall,
                    color = V2DecisionLime
                )
            }
        }
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
}
