package com.getprediq.app.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.data.v2.V2DecisionCard
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.theme.*
import com.getprediq.app.ui.v2.media.*

@Composable
fun TodayV2Screen(
    vm: PrediqContractViewModel,
    onDecision: (String) -> Unit
) {
    val state = vm.state
    val today = state.today

    LaunchedEffect(state.ready, today) {
        if (state.ready && today == null && !state.busy && !state.refreshing) {
            vm.loadToday()
        }
    }

    if (state.busy && today == null) {
        PrediqLoadingState()
        return
    }

    if (today == null && !state.busy && state.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(V2Background),
            contentAlignment = Alignment.Center
        ) {
            PrediqErrorState(
                message = state.error ?: "PredIQ could not load today's analysis.",
                actionLabel = "Retry",
                onAction = vm::loadToday
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
                PrediqSectionHeader(title = "Top Picks")
            }
            items(today.topPicks, key = { it.id }) { card ->
                DecisionCard(card, onClick = { onDecision(card.predictionId ?: card.id) })
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
        }

        if (today != null && today.topPicks.isEmpty() && !state.busy && !state.refreshing) {
            item {
                PrediqEmptyState(
                    title = "No strong call right now",
                    message = "PredIQ checked the current slate, but no call clears the required evidence threshold yet."
                )
            }
        }

        item {
            Spacer(Modifier.height(LocalV2Spacing.current.xxl))
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
    PrediqHeroSurface {
        Text(
            text = "DAILY INTELLIGENCE",
            style = V2Typography.labelSmall,
            color = V2White.copy(alpha = 0.7f)
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
                label = "Top Picks",
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
        Text(text = label, style = V2Typography.labelSmall, color = V2White.copy(alpha = 0.6f))
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
