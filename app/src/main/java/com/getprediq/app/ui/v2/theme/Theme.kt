package com.getprediq.app.ui.v2.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val V2ColorScheme = darkColorScheme(
    primary = V2DecisionLime,
    onPrimary = V2Black,
    primaryContainer = V2DecisionSoft,
    onPrimaryContainer = V2TextPrimary,
    secondary = V2SecondaryBlue,
    onSecondary = V2Black,
    secondaryContainer = V2SurfaceHigh,
    onSecondaryContainer = V2TextPrimary,
    tertiary = V2BrandViolet,
    onTertiary = V2White,
    tertiaryContainer = V2SurfaceElevated,
    onTertiaryContainer = V2TextPrimary,
    background = V2Background,
    onBackground = V2TextPrimary,
    surface = V2SurfacePrimary,
    onSurface = V2TextPrimary,
    surfaceVariant = V2SurfaceElevated,
    onSurfaceVariant = V2TextSecondary,
    outline = V2Divider,
    outlineVariant = V2SurfaceHigh,
    error = V2Negative,
    onError = V2Black,
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
