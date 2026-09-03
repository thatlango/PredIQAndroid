package com.getprediq.app.ui.v2.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.getprediq.app.data.MediaCatalogStore
import com.getprediq.app.ui.v2.theme.*

@Composable
fun TeamLogo(
    name: String,
    sport: String = "football",
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val media = MediaCatalogStore.team(name, sport)
    val url = media?.optimizedImageUrl ?: media?.imageUrl
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(V2White)
            .border(1.dp, V2Divider, RoundedCornerShape(size * 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "$name logo",
                modifier = Modifier.size(size * 0.75f),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = teamInitials(name),
                style = V2Typography.labelSmall.copy(fontSize = (size.value * 0.4f).sp),
                color = V2TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayerPhoto(
    name: String,
    sport: String = "football",
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val media = MediaCatalogStore.player(name, sport)
    val url = media?.optimizedImageUrl ?: media?.imageUrl
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(V2SurfaceElevated)
            .border(1.dp, V2Divider, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "$name photo",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = V2TextMuted,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
fun CompetitionLogo(
    name: String,
    sport: String = "football",
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val media = MediaCatalogStore.competition(name, sport)
    val url = media?.optimizedImageUrl ?: media?.imageUrl

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(V2White)
            .border(1.dp, V2Divider, RoundedCornerShape(size * 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "$name logo",
                modifier = Modifier.size(size * 0.75f),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = sportIcon(sport),
                contentDescription = null,
                tint = V2TextMuted,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
fun SportIcon(
    sport: String,
    modifier: Modifier = Modifier,
    tint: Color = V2TextSecondary
) {
    Icon(
        imageVector = sportIcon(sport),
        contentDescription = sport,
        modifier = modifier,
        tint = tint
    )
}

fun sportIcon(sport: String): ImageVector = when (sport.lowercase()) {
    "football", "soccer" -> Icons.Outlined.SportsSoccer
    "basketball" -> Icons.Outlined.SportsBasketball
    "tennis" -> Icons.Outlined.SportsTennis
    "cricket" -> Icons.Outlined.SportsCricket
    "baseball" -> Icons.Outlined.SportsBaseball
    "rugby" -> Icons.Outlined.SportsRugby
    "american_football" -> Icons.Outlined.SportsFootball
    "hockey", "ice_hockey" -> Icons.Outlined.SportsHockey
    else -> Icons.Outlined.EmojiEvents
}

private fun teamInitials(name: String): String = name.split(" ")
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "T" }
