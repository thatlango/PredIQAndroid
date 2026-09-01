package com.getprediq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PrediqBlue = Color(0xFF0050CB)
val PrediqBlueBright = Color(0xFF0066FF)
val PrediqGreen = Color(0xFF006E2A)
val PrediqGreenSoft = Color(0xFF5CFD80)
val PrediqRed = Color(0xFFBA1A1A)
val PrediqAmber = Color(0xFFA33200)
val PrediqBackground = Color(0xFFF9F9FC)
val PrediqSurfaceLow = Color(0xFFF3F3F6)
val PrediqSurface = Color(0xFFFFFFFF)
val PrediqOutline = Color(0xFFC2C6D8)
val PrediqText = Color(0xFF1A1C1E)
val PrediqMuted = Color(0xFF424656)

private val scheme = lightColorScheme(
    primary = PrediqBlue,
    onPrimary = Color.White,
    primaryContainer = PrediqBlueBright,
    onPrimaryContainer = Color.White,
    secondary = PrediqGreen,
    onSecondary = Color.White,
    secondaryContainer = PrediqGreenSoft,
    onSecondaryContainer = Color(0xFF00732C),
    tertiary = PrediqAmber,
    error = PrediqRed,
    background = PrediqBackground,
    onBackground = PrediqText,
    surface = PrediqSurface,
    onSurface = PrediqText,
    surfaceVariant = PrediqSurfaceLow,
    onSurfaceVariant = PrediqMuted,
    outline = PrediqOutline,
)

private val type = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.7.sp),
)

@Composable
fun PrediqTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
