package com.getprediq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * PredIQ keeps its blue sports-intelligence identity while following the estate-wide
 * Tuku UI contract: neutral surfaces dominate, structural blue is secondary and
 * semantic/accent colour is reserved for meaning.
 */
val PrediqBlue = Color(0xFF0B57D0)
val PrediqBlueStrong = Color(0xFF083E9E)
val PrediqBlueBright = Color(0xFF2F6FDB)
val PrediqBlueSoft = Color(0xFFEAF1FF)
val PrediqGreen = Color(0xFF16794A)
val PrediqGreenSoft = Color(0xFFE9F7EF)
val PrediqRed = Color(0xFFB3261E)
val PrediqRedSoft = Color(0xFFFFEDEA)
val PrediqAmber = Color(0xFF9B6400)
val PrediqAmberSoft = Color(0xFFFFF4D8)
val PrediqBackground = Color(0xFFF7F8FA)
val PrediqSurfaceLow = Color(0xFFF1F3F5)
val PrediqSurface = Color(0xFFFFFFFF)
val PrediqOutline = Color(0xFFDCE2E6)
val PrediqText = Color(0xFF172026)
val PrediqMuted = Color(0xFF66727C)

private val scheme = lightColorScheme(
    primary = PrediqBlue,
    onPrimary = Color.White,
    primaryContainer = PrediqBlueSoft,
    onPrimaryContainer = PrediqBlueStrong,
    secondary = PrediqGreen,
    onSecondary = Color.White,
    secondaryContainer = PrediqGreenSoft,
    onSecondaryContainer = Color(0xFF0A5232),
    tertiary = PrediqAmber,
    onTertiary = Color.White,
    tertiaryContainer = PrediqAmberSoft,
    onTertiaryContainer = Color(0xFF5E3D00),
    error = PrediqRed,
    onError = Color.White,
    errorContainer = PrediqRedSoft,
    onErrorContainer = Color(0xFF76110D),
    background = PrediqBackground,
    onBackground = PrediqText,
    surface = PrediqSurface,
    onSurface = PrediqText,
    surfaceVariant = PrediqSurfaceLow,
    onSurfaceVariant = PrediqMuted,
    outline = PrediqOutline,
)

private val type = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun PrediqTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = type,
        shapes = shapes,
        content = content,
    )
}
