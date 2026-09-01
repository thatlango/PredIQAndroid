package com.getprediq.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getprediq.app.data.*
import com.getprediq.app.ui.theme.*
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun probability(value: Double?): String = value?.let { "${(it * 100).toInt()}%" } ?: "—"
fun ugx(value: Int): String = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(value)}"
fun marketName(value: String?): String = when (value) {
    "match_result" -> "Match Result"
    "match_winner" -> "Match Winner"
    "double_chance" -> "Double Chance"
    "total_goals" -> "Goals"
    "both_teams_to_score" -> "BTTS"
    "home_team_goals" -> "Home Team Goals"
    "away_team_goals" -> "Away Team Goals"
    "first_half_goals" -> "First Half"
    "second_half_goals" -> "Second Half"
    "asian_handicap" -> "Handicap"
    else -> value?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Prediction"
}
fun prettySport(value: String): String = value.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
fun kickoff(value: String?): String {
    if (value.isNullOrBlank()) return "Time TBC"
    return runCatching {
        val instant = Instant.parse(value)
        DateTimeFormatter.ofPattern("EEE • HH:mm", Locale.ENGLISH).withZone(ZoneId.of("Africa/Kampala")).format(instant)
    }.getOrElse { value.take(16).replace('T', ' ') }
}
fun relativeTime(value: String?): String {
    if (value.isNullOrBlank()) return "recently"
    return runCatching {
        val seconds = (Instant.now().epochSecond - Instant.parse(value).epochSecond).coerceAtLeast(0)
        when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86400}d ago"
        }
    }.getOrDefault("recently")
}
fun teamInitials(name: String): String = name.split(' ').filter { it.isNotBlank() }.take(3).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(3)

@Composable
fun PrediqSectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(action) }
    }
}

@Composable
fun PrediqCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
}

@Composable
fun TeamBadge(name: String, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).clip(CircleShape).background(PrediqSurfaceLow), contentAlignment = Alignment.Center) {
        Text(teamInitials(name), fontWeight = FontWeight.Bold, color = PrediqMuted, fontSize = 13.sp)
    }
}

@Composable
fun ConfidenceBar(value: Double?, label: String = "Confidence") {
    val progress = (value ?: 0.0).coerceIn(0.0, 1.0).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PrediqMuted)
            Text(probability(value), style = MaterialTheme.typography.labelLarge, color = PrediqBlue)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0xFFE2E2E5))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(PrediqBlue))
        }
    }
}

@Composable
fun SportChips(sports: List<String>, selected: String, onSelect: (String) -> Unit) {
    val values = listOf("") + sports.filter { it.isNotBlank() }.distinct()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(values, key = { it }) { sport ->
            FilterChip(
                selected = selected == sport,
                onClick = { onSelect(sport) },
                label = { Text(if (sport.isBlank()) "All Sports" else prettySport(sport)) },
                modifier = Modifier.heightIn(min = 44.dp),
                shape = CircleShape,
            )
        }
    }
}

@Composable
fun StateCard(title: String, message: String, error: Boolean = false, cached: Boolean = false, action: String? = null, onAction: (() -> Unit)? = null) {
    val container = when { error -> Color(0xFFFFEDEC); cached -> Color(0xFFFFF6DF); else -> PrediqSurface }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = PrediqMuted, style = MaterialTheme.typography.bodyMedium)
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun PickFeatureCard(pick: PickDto, onOpen: () -> Unit) {
    PrediqCard(Modifier.fillMaxWidth(), onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${pick.competition ?: prettySport(pick.sportCode)} • ${kickoff(pick.startsAt)}", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            if (pick.status == "live") StatusPill("LIVE", PrediqGreen, Color(0xFFE9F8EF))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { TeamBadge(pick.homeParticipant); Spacer(Modifier.height(6.dp)); Text(pick.homeParticipant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text("VS", color = PrediqMuted, style = MaterialTheme.typography.labelLarge)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { TeamBadge(pick.awayParticipant); Spacer(Modifier.height(6.dp)); Text(pick.awayParticipant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Surface(shape = RoundedCornerShape(16.dp), color = PrediqBackground) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("PREDIQ", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); Text(pick.selectionLabel ?: "Current assessment", style = MaterialTheme.typography.titleLarge, color = PrediqBlue) }
                ConfidenceBar(pick.confidence ?: pick.probability)
            }
        }
        if (pick.explanation.isNotEmpty()) IndicatorList(pick.explanation.take(3))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), shape = RoundedCornerShape(12.dp)) { Text("View Full Analysis") }
    }
}

@Composable
fun AssessmentCard(item: AssessmentDto, onOpen: () -> Unit) {
    PrediqCard(Modifier.fillMaxWidth(), onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${item.competition ?: prettySport(item.sportCode)} • ${kickoff(item.startsAt)}", style = MaterialTheme.typography.labelLarge, color = PrediqMuted, modifier = Modifier.weight(1f))
            StatusPill(if (item.promotable) (item.confidenceBand ?: "PICK").uppercase() else "WATCH", if (item.promotable) PrediqGreen else PrediqAmber, if (item.promotable) Color(0xFFE9F8EF) else Color(0xFFFFF3E7))
        }
        Text("${item.homeParticipant} vs ${item.awayParticipant}", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) { Text("PredIQ", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); Text(item.selectionLabel ?: "Assessment", style = MaterialTheme.typography.titleLarge, color = PrediqBlue); Text(marketName(item.marketKey), color = PrediqMuted, style = MaterialTheme.typography.bodyMedium) }
            Text(probability(item.probability), style = MaterialTheme.typography.headlineMedium, color = PrediqGreen)
        }
        ConfidenceBar(item.confidence)
        if (item.why.isNotEmpty()) IndicatorList(item.why.take(3))
        item.changeReason?.let { Surface(color = Color(0xFFEEF3FF), shape = RoundedCornerShape(12.dp)) { Text(it, Modifier.padding(12.dp), color = PrediqBlue, style = MaterialTheme.typography.bodyMedium) } }
        Text("Analysed ${relativeTime(item.lastAnalysedAt)}", color = PrediqMuted, fontSize = 12.sp)
    }
}

@Composable
fun LiveMatchCard(game: LiveGameDto, onOpen: () -> Unit) {
    PrediqCard(Modifier.fillMaxWidth(), onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Sensors, null, tint = PrediqRed, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text(game.statusText ?: "LIVE", color = PrediqRed, fontWeight = FontWeight.SemiBold) }
            if (game.changeReason != null) StatusPill("PREDIQ UPDATED", PrediqGreen, Color(0xFFE9F8EF))
        }
        Text(game.competition ?: prettySport(game.sportCode), style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(game.homeParticipant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${game.homeScore ?: "–"}  -  ${game.awayScore ?: "–"}", style = MaterialTheme.typography.titleLarge)
            Text(game.awayParticipant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        if (game.predictionAvailable) {
            Surface(color = PrediqBackground, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Live Prediction", color = PrediqMuted); Text("${probability(game.confidence)} Confidence", color = PrediqBlue, fontWeight = FontWeight.SemiBold) }
                    Text(game.selectionLabel ?: "PredIQ assessment", style = MaterialTheme.typography.titleLarge)
                    game.why.firstOrNull()?.let { Text(it, color = PrediqMuted) }
                }
            }
        } else Text("PredIQ is tracking this match but does not yet have enough reliable evidence for a live call.", color = PrediqMuted)
        Text("Last analysed ${relativeTime(game.lastAnalysedAt ?: game.updatedAt)}", color = PrediqMuted, fontSize = 12.sp)
    }
}

@Composable
fun ResultCard(item: ResultDto) {
    val homeScore = item.actual?.get("home_score")?.jsonPrimitive?.intOrNull
    val awayScore = item.actual?.get("away_score")?.jsonPrimitive?.intOrNull
    PrediqCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${item.competition ?: prettySport(item.sportCode)} • FINAL", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            val (fg, bg) = when (item.outcome) { "won" -> PrediqGreen to Color(0xFFE9F8EF); "lost" -> PrediqRed to Color(0xFFFFEDEC); else -> PrediqMuted to PrediqSurfaceLow }
            StatusPill(item.outcome.uppercase(), fg, bg)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.homeParticipant, style = MaterialTheme.typography.titleLarge); if (homeScore != null) Text(homeScore.toString(), style = MaterialTheme.typography.titleLarge) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.awayParticipant, color = PrediqMuted); if (awayScore != null) Text(awayScore.toString(), color = PrediqMuted) }
        Surface(color = PrediqSurfaceLow, shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("PREDIQ PICK", fontSize = 11.sp, color = PrediqMuted); Text(selectionForResult(item)) }
                Column(horizontalAlignment = Alignment.End) { Text("CONFIDENCE", fontSize = 11.sp, color = PrediqMuted); Text(probability(item.confidence), color = PrediqBlue, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

fun selectionForResult(item: ResultDto): String = when (item.selectionKey) { "home" -> item.homeParticipant; "away" -> item.awayParticipant; "draw" -> "Draw"; else -> item.selectionKey ?: "Selection" }

@Composable
fun IndicatorList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { line -> Row(verticalAlignment = Alignment.Top) { Icon(Icons.Outlined.CheckCircle, null, tint = PrediqGreen, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(line, style = MaterialTheme.typography.bodyMedium, color = PrediqMuted, modifier = Modifier.weight(1f)) } }
    }
}

@Composable
fun StatusPill(text: String, foreground: Color, background: Color) {
    Surface(shape = CircleShape, color = background) { Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SubscriptionPill(progress: SubscriptionProgress?) {
    if (progress == null) return
    Surface(shape = CircleShape, color = Color(0xFFEAF0FF)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = PrediqBlue, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("${progress.daysRemaining}d left", color = PrediqBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
