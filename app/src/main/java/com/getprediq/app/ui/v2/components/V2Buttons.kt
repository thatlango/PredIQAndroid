package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getprediq.app.ui.v2.theme.*

@Composable
fun PrediqPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = V2ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = V2DecisionLime,
            contentColor = V2Black,
            disabledContainerColor = V2SurfaceElevated,
            disabledContentColor = V2TextMuted
        ),
        content = content
    )
}

@Composable
fun PrediqSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = V2ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = V2TextPrimary,
            disabledContentColor = V2TextMuted
        ),
        content = content
    )
}
