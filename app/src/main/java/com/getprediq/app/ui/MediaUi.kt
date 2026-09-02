package com.getprediq.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.getprediq.app.ui.theme.PrediqMuted
import com.getprediq.app.ui.theme.PrediqSurfaceLow

@Composable
private fun HydrateMediaCatalog() {
    LaunchedEffect(Unit) { MediaCatalogStore.ensureLoaded() }
}

fun compactTeamName(name: String, sport: String = "football"): String {
    val media = MediaCatalogStore.participant(name, sport)
    return media?.shortCode?.takeIf { media.isCanonicalCode && it.isNotBlank() } ?: name
}

fun sportIcon(sport: String): ImageVector = when (sport) {
    "football" -> Icons.Outlined.SportsSoccer
    "basketball" -> Icons.Outlined.SportsBasketball
    "tennis" -> Icons.Outlined.SportsTennis
    "cricket" -> Icons.Outlined.SportsCricket
    "american_football" -> Icons.Outlined.SportsFootball
    "baseball" -> Icons.Outlined.SportsBaseball
    "ice_hockey" -> Icons.Outlined.SportsHockey
    "boxing", "mma" -> Icons.Outlined.SportsMma
    "golf" -> Icons.Outlined.SportsGolf
    else -> Icons.Outlined.EmojiEvents
}

@Composable
fun SportGlyph(sport: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Icon(sportIcon(sport), contentDescription = null, modifier = modifier, tint = tint)
}

@Composable
fun TeamCrest(name: String, sport: String = "football", size: Dp = 48.dp, modifier: Modifier = Modifier, dark: Boolean = false) {
    HydrateMediaCatalog()
    val individual = sport in setOf("tennis", "boxing", "mma")
    val media = MediaCatalogStore.participant(name, sport)
    val url = media?.optimizedImageUrl ?: media?.imageUrl
    val shape = if (individual) CircleShape else RoundedCornerShape(size * .28f)
    val frameBackground = if (dark) Color.White.copy(alpha = .10f) else Color.White
    val frameBorder = if (dark) Color.White.copy(alpha = .16f) else Color(0xFFE3E9F0)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(frameBackground)
            .border(1.dp, frameBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = if (individual) "$name player photo" else "$name crest",
                modifier = if (individual) Modifier.size(size) else Modifier.size(size * .78f),
                contentScale = if (individual) ContentScale.Crop else ContentScale.Fit,
            )
        } else if (individual) {
            SportGlyph(sport, Modifier.size(size * .48f), tint = if (dark) Color.White else PrediqMuted)
        } else {
            Text(teamInitials(name), fontWeight = FontWeight.Bold, color = if (dark) Color.White else PrediqMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun CompetitionMark(name: String?, sport: String = "football", size: Dp = 28.dp, modifier: Modifier = Modifier) {
    if (name.isNullOrBlank()) return
    HydrateMediaCatalog()
    val media = MediaCatalogStore.competition(name, sport)
    val url = media?.optimizedImageUrl ?: media?.imageUrl
    val shape = RoundedCornerShape(size * .28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE3E9F0), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "$name logo",
                modifier = Modifier.size(size * .72f),
                contentScale = ContentScale.Fit,
            )
        } else {
            SportGlyph(sport, Modifier.size(size * .56f), tint = PrediqMuted)
        }
    }
}

@Composable
fun PlayerHeadshot(name: String, sport: String = "football", size: Dp = 46.dp, modifier: Modifier = Modifier) {
    HydrateMediaCatalog()
    val media = MediaCatalogStore.player(name, sport)
    val url = media?.optimizedImageUrl ?: media?.imageUrl
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PrediqSurfaceLow)
            .border(1.dp, Color(0xFFE3E9F0), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "$name player photo",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
            )
        } else {
            SportGlyph(sport, Modifier.size(size * .48f), tint = PrediqMuted)
        }
    }
}
