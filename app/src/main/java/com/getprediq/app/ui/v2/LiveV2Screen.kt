package com.getprediq.app.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.data.v2.V2LiveCard
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.theme.*
import com.getprediq.app.ui.v2.media.*

@Composable
fun LiveV2Screen(vm: PrediqContractViewModel) {
    val state = vm.state
    val live = state.live

    LaunchedEffect(state.ready, live) {
        if (state.ready && live == null && !state.busy && !state.refreshing) {
            vm.loadLive()
        }
    }

    if (state.busy && live == null) {
        PrediqLoadingState(message = "Connecting to live intelligence...")
        return
    }

    if (live == null && !state.busy && state.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(V2Background),
            contentAlignment = Alignment.Center
        ) {
            PrediqErrorState(
                message = state.error ?: "PredIQ could not connect to live intelligence.",
                actionLabel = "Retry",
                onAction = vm::loadLive
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
            LiveHeader(
                liveCount = live?.summary?.liveGames ?: 0,
                connected = live != null
            )
        }

        if (live?.games?.isNotEmpty() == true) {
            item {
                PrediqSectionHeader(title = "Active Fixtures")
            }
            items(live.games, key = { it.id }) { card ->
                LiveMatchCard(card)
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
        }

        if (live != null && live.games.isEmpty() && !state.busy && !state.refreshing) {
            item {
                PrediqEmptyState(
                    title = "No tracked live games",
                    message = "The live feed is connected, but no tracked event is underway right now."
                )
            }
        }
    }
}

@Composable
fun LiveHeader(liveCount: Int, connected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LocalV2Spacing.current.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Live Intelligence", style = V2Typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (connected) V2Positive else V2Warning, CircleShape)
                )
                Spacer(Modifier.width(LocalV2Spacing.current.xs))
                Text(
                    text = if (connected) "Live connected" else "Connecting...",
                    style = V2Typography.labelMedium,
                    color = V2TextSecondary
                )
            }
        }
        PrediqSurface(
            color = V2SurfaceElevated,
            shape = CircleShape,
            modifier = Modifier.padding(LocalV2Spacing.current.xs)
        ) {
            Text(
                text = liveCount.toString(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = V2Typography.labelLarge,
                color = V2DecisionLime,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LiveMatchCard(card: V2LiveCard) {
    PrediqElevatedSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${card.event.competition.name ?: "Competition"} · ${card.event.score?.statusText ?: "LIVE"}",
                style = V2Typography.labelMedium,
                color = V2TextSecondary
            )
            if (card.change.direction != "stable") {
                LiveTrendBadge(card.change)
            }
        }

        Spacer(Modifier.height(LocalV2Spacing.current.m))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamLogo(name = card.event.participants.home.name, sport = card.event.sport ?: "football", size = 20.dp)
                    Spacer(Modifier.width(LocalV2Spacing.current.s))
                    Text(text = card.event.participants.home.name, style = V2Typography.bodyLarge)
                }
                Spacer(Modifier.height(LocalV2Spacing.current.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamLogo(name = card.event.participants.away.name, sport = card.event.sport ?: "football", size = 20.dp)
                    Spacer(Modifier.width(LocalV2Spacing.current.s))
                    Text(text = card.event.participants.away.name, style = V2Typography.bodyLarge)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = card.event.score?.home?.toString() ?: "0",
                    style = V2Typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(LocalV2Spacing.current.xs))
                Text(
                    text = card.event.score?.away?.toString() ?: "0",
                    style = V2Typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(LocalV2Spacing.current.l))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(V2SurfacePrimary.copy(alpha = 0.5f), V2Shapes.small)
                .padding(LocalV2Spacing.current.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = card.pick.label ?: "Selection", style = V2Typography.titleSmall, color = V2DecisionLime)
                Text(text = card.pick.market ?: "", style = V2Typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${card.currentChance.percent ?: 0}%",
                    style = V2Typography.titleLarge,
                    color = V2White
                )
                Text(
                    text = "Updated just now",
                    style = V2Typography.labelSmall,
                    color = V2TextMuted
                )
            }
        }
    }
}

@Composable
fun LiveTrendBadge(change: com.getprediq.app.data.v2.V2LiveChange) {
    val color = when (change.direction) {
        "up" -> V2Positive
        "down" -> V2Negative
        else -> V2TextSecondary
    }
    val icon = when (change.direction) {
        "up" -> Icons.Outlined.TrendingUp
        "down" -> Icons.Outlined.TrendingDown
        else -> Icons.Outlined.TrendingFlat
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = change.label, style = V2Typography.labelSmall, color = color)
    }
}
