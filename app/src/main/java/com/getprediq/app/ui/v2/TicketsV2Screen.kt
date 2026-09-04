package com.getprediq.app.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.data.v3.V3SavedTicket
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.theme.*

@Composable
fun TicketsV2Screen(vm: PrediqContractViewModel) {
    val state = vm.state
    val tickets = state.v3SavedTickets?.tickets ?: emptyList()

    LaunchedEffect(Unit) {
        vm.loadV3SavedTickets()
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
            Text(text = "Saved Tickets", style = V2Typography.headlineMedium)
            Text(text = "Your library of algorithmic odds builds", style = V2Typography.bodyMedium)
            Spacer(Modifier.height(LocalV2Spacing.current.l))
        }

        if (tickets.isNotEmpty()) {
            items(tickets, key = { it.id }) { saved ->
                SavedTicketCard(
                    saved = saved,
                    onOpen = { vm.openV3SavedTicket(saved) },
                    onDelete = { vm.deleteV3Ticket(saved.id) }
                )
                Spacer(Modifier.height(LocalV2Spacing.current.m))
            }
        } else if (!state.busy) {
            item {
                PrediqEmptyState(
                    title = "No saved tickets",
                    message = "Build and save a ticket in the Builder tab to see it here."
                )
            }
        }
    }
}

@Composable
fun SavedTicketCard(
    saved: V3SavedTicket,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    PrediqElevatedSurface(
        modifier = Modifier.clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = saved.title ?: "Untitled Build",
                    style = V2Typography.titleMedium,
                    color = V2White
                )
                Text(
                    text = "${saved.payload.legCount} legs · ${saved.riskProfile?.replaceFirstChar { it.uppercase() } ?: "Balanced"}",
                    style = V2Typography.labelMedium
                )
            }
            Text(
                text = "%.2f".format(saved.combinedOdds ?: 0.0),
                style = V2Typography.headlineMedium,
                color = V2DecisionLime
            )
        }
        
        Spacer(Modifier.height(LocalV2Spacing.current.m))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Built ${saved.createdAt?.take(10) ?: ""}",
                style = V2Typography.labelSmall,
                color = V2TextMuted
            )
            Row {
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = V2Negative)
                }
                androidx.compose.material3.IconButton(onClick = onOpen) {
                    Icon(Icons.Outlined.ChevronRight, null, tint = V2White)
                }
            }
        }
    }
}
