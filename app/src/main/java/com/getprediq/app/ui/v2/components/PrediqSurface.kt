package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.getprediq.app.ui.v2.theme.*

@Composable
fun PrediqSurface(
    modifier: Modifier = Modifier,
    shape: Shape = V2Shapes.medium,
    color: Color = V2SurfacePrimary,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        content = content
    )
}

@Composable
fun PrediqElevatedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = V2CardShape,
    color: Color = V2SurfaceElevated,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalV2Spacing.current.m),
            content = content
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewPrediqHeroSurface() {
    PrediqV2Theme {
        PrediqHeroSurface {
            androidx.compose.material3.Text("Hero Surface", color = V2White)
        }
    }
}
