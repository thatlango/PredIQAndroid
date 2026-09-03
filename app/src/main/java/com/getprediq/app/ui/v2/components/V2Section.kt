package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.getprediq.app.ui.v2.theme.*

@Composable
fun PrediqSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LocalV2Spacing.current.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = V2Typography.labelSmall,
            color = V2TextSecondary,
            fontWeight = FontWeight.Bold
        )
        action?.invoke()
    }
}
