package com.getprediq.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * PredIQ Android visual foundation.
 *
 * The product keeps a serious sports-intelligence identity rather than inheriting a
 * generic Tuku skin. Neutral surfaces dominate; blue carries product structure;
 * green/amber/red are semantic; lime is reserved for high-attention live surfaces.
 */
val PrediqBlue = Color(0xFF124E78)
val PrediqBlueBright = Color(0xFF1D6E9E)
val PrediqBlueStrong = Color(0xFF0A3657)
val PrediqBlueSoft = Color(0xFFE5F0F7)
val PrediqGreen = Color(0xFF0E7447)
val PrediqGreenSoft = Color(0xFFE6F5EC)
val PrediqRed = Color(0xFFB93535)
val PrediqRedSoft = Color(0xFFFFEDEC)
val PrediqAmber = Color(0xFFA85A16)
val PrediqAmberSoft = Color(0xFFFFF1E2)
val PrediqBackground = Color(0xFFF7F8F6)
val PrediqSurfaceLow = Color(0xFFF0F2EF)
val PrediqSurface = Color(0xFFFFFFFF)
val PrediqOutline = Color(0xFFD6DBD5)
val PrediqText = Color(0xFF161A17)
val PrediqMuted = Color(0xFF606861)

// Live surfaces deliberately carry more visual weight than the rest of the app.
val PrediqLiveInk = Color(0xFF0D130B)
val PrediqLiveCard = Color(0xFF171E15)
val PrediqLiveCardAlt = Color(0xFF222A20)
val PrediqLiveLime = Color(0xFFB7FF18)
val PrediqLiveMuted = Color(0xFFB8C1B4)
val PrediqLiveOutline = Color(0xFF354032)

private val scheme = lightColorScheme(
    primary = PrediqBlue,
    onPrimary = Color.White,
    primaryContainer = PrediqBlueSoft,
    onPrimaryContainer = PrediqBlueStrong,
    secondary = PrediqGreen,
    onSecondary = Color.White,
    secondaryContainer = PrediqGreenSoft,
    onSecondaryContainer = Color(0xFF0B4A2D),
    tertiary = PrediqAmber,
    onTertiary = Color.White,
    tertiaryContainer = PrediqAmberSoft,
    onTertiaryContainer = Color(0xFF61350E),
    error = PrediqRed,
    onError = Color.White,
    errorContainer = PrediqRedSoft,
    onErrorContainer = Color(0xFF751D1D),
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
        letterSpacing = (-0.9).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.25).sp,
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
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
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
