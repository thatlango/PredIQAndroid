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

val PrediqBlue = Color(0xFF134B70)
val PrediqBlueBright = Color(0xFF1C6C96)
val PrediqGreen = Color(0xFF087443)
val PrediqGreenSoft = Color(0xFFB8F7CE)
val PrediqRed = Color(0xFFC53B3B)
val PrediqAmber = Color(0xFFB86418)
val PrediqBackground = Color(0xFFF5F7F5)
val PrediqSurfaceLow = Color(0xFFEEF1EE)
val PrediqSurface = Color(0xFFFFFFFF)
val PrediqOutline = Color(0xFFCAD0CA)
val PrediqText = Color(0xFF141814)
val PrediqMuted = Color(0xFF5A625B)

// Live surfaces deliberately carry more visual weight than the rest of the app.
// They echo the premium sports-reference direction without sacrificing Material 3
// accessibility or the existing PredIQ information architecture.
val PrediqLiveInk = Color(0xFF0D130B)
val PrediqLiveCard = Color(0xFF171E15)
val PrediqLiveCardAlt = Color(0xFF222A20)
val PrediqLiveLime = Color(0xFFB7FF18)
val PrediqLiveMuted = Color(0xFFB8C1B4)
val PrediqLiveOutline = Color(0xFF354032)

private val scheme = lightColorScheme(
    primary = PrediqBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EAF3),
    onPrimaryContainer = Color(0xFF12364B),
    secondary = PrediqGreen,
    onSecondary = Color.White,
    secondaryContainer = PrediqGreenSoft,
    onSecondaryContainer = Color(0xFF0B4A2D),
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
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.9).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 31.sp, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.55.sp),
)

@Composable
fun PrediqTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
