package com.getprediq.app.ui.v2.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val V2ColorScheme = darkColorScheme(
    primary = V2DecisionLime,
    onPrimary = V2Black,
    primaryContainer = V2SurfaceElevated,
    onPrimaryContainer = V2TextPrimary,
    secondary = V2SecondaryBlue,
    onSecondary = V2White,
    background = V2Background,
    onBackground = V2TextPrimary,
    surface = V2SurfacePrimary,
    onSurface = V2TextPrimary,
    surfaceVariant = V2SurfaceElevated,
    onSurfaceVariant = V2TextSecondary,
    outline = V2Divider,
    error = V2Negative
)

@Composable
fun PrediqV2Theme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalV2Spacing provides V2Spacing()
    ) {
        MaterialTheme(
            colorScheme = V2ColorScheme,
            typography = V2Typography,
            shapes = V2Shapes,
            content = content
        )
    }
}
