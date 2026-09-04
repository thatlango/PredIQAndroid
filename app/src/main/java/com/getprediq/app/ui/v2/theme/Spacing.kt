package com.getprediq.app.ui.v2.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class V2Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val pageHorizontal: Dp = 18.dp,
    val sectionGap: Dp = 24.dp
)

val LocalV2Spacing = staticCompositionLocalOf { V2Spacing() }
