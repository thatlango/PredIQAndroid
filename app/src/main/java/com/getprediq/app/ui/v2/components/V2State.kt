package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getprediq.app.ui.v2.theme.*

@Composable
fun PrediqLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Analysing sports intelligence..."
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = V2DecisionLime)
        Spacer(Modifier.height( LocalV2Spacing.current.m ))
        Text(
            text = message,
            style = V2Typography.bodyMedium,
            color = V2TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PrediqErrorState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(LocalV2Spacing.current.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = V2Negative,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(LocalV2Spacing.current.m))
        Text(
            text = message,
            style = V2Typography.bodyMedium,
            color = V2TextPrimary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(LocalV2Spacing.current.l))
            PrediqSecondaryButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun PrediqEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(LocalV2Spacing.current.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = V2TextMuted,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(LocalV2Spacing.current.m))
        Text(
            text = title,
            style = V2Typography.titleMedium,
            color = V2TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(LocalV2Spacing.current.xs))
        Text(
            text = message,
            style = V2Typography.bodyMedium,
            color = V2TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
