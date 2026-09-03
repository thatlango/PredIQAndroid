package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
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
    contentPadding: Dp = 16.dp,
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
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun PrediqHeroSurface(
    modifier: Modifier = Modifier,
    shape: Shape = V2Shapes.large,
    color: Color = V2BrandViolet,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalV2Spacing.current.xl),
            content = content
        )
    }
}

@Composable
fun PrediqBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = V2SurfaceElevated,
    contentColor: Color = V2TextPrimary
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = V2Typography.labelSmall,
            color = contentColor
        )
    }
}
