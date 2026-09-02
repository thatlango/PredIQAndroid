package com.getprediq.app.ui

import androidx.compose.foundation.BorderStroke
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E9E5)),
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
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0xFFE2E6E2))) {
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrediqLiveLime,
                    selectedLabelColor = PrediqLiveInk,
                    containerColor = Color.White,
                    labelColor = PrediqMuted,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == sport,
                    borderColor = PrediqOutline,
                    selectedBorderColor = PrediqLiveLime,
                ),
            )
        }
    }
}

@Composable
fun StateCard(title: String, message: String, error: Boolean = false, cached: Boolean = false, action: String? = null, onAction: (() -> Unit)? = null) {
    val container = when { error -> Color(0xFFFFEDEC); cached -> Color(0xFFFFF6DF); else -> PrediqSurface }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, if (error) Color(0xFFF4C6C2) else Color(0xFFE5E9E5))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
fun LiveOverviewHero(live: LiveResponse?, loading: Boolean, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = PrediqLiveInk),
        border = BorderStroke(1.dp, PrediqLiveOutline),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(PrediqLiveLime, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("LIVE INTELLIGENCE", color = PrediqLiveLime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("What matters now", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
                Surface(onClick = onRefresh, shape = CircleShape, color = PrediqLiveCardAlt) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Refresh, "Refresh live intelligence", tint = Color.White)
                    }
                }
            }
            Text(
                if (loading && live == null) "Scanning live fixtures and refreshing PredIQ signals…" else live?.message ?: "Scanning current fixtures for evidence-backed opportunities.",
                color = PrediqLiveMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveMetric("LIVE", (live?.liveCount ?: 0).toString(), Modifier.weight(1f))
                LiveMetric("WITH SIGNAL", (live?.predictedCount ?: 0).toString(), Modifier.weight(1f))
                LiveMetric("TOP", (live?.strongCount ?: 0).toString(), Modifier.weight(1f), highlight = true)
            }
        }
    }
}

@Composable
private fun LiveMetric(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Surface(
        modifier = modifier,
        color = if (highlight) PrediqLiveLime else PrediqLiveCard,
        shape = RoundedCornerShape(18.dp),
        border = if (highlight) null else BorderStroke(1.dp, PrediqLiveOutline),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = if (highlight) PrediqLiveInk else PrediqLiveMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = if (highlight) PrediqLiveInk else Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LiveTeamBadge(name: String) {
    Surface(shape = CircleShape, color = PrediqLiveCardAlt, border = BorderStroke(1.dp, PrediqLiveOutline)) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Text(teamInitials(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun LiveMatchCard(game: LiveGameDto, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PrediqLiveCard),
        border = BorderStroke(1.dp, PrediqLiveOutline),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((game.competition ?: prettySport(game.sportCode)).uppercase(), color = PrediqLiveMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(game.statusText ?: "LIVE", color = PrediqLiveLime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                StatusPill(if (game.analysisPromotable) "TOP SIGNAL" else "LIVE", if (game.analysisPromotable) PrediqLiveInk else Color.White, if (game.analysisPromotable) PrediqLiveLime else PrediqLiveCardAlt)
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    LiveTeamBadge(game.homeParticipant)
                    Spacer(Modifier.height(8.dp))
                    Text(game.homeParticipant, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text("${game.homeScore ?: "–"}  ${game.awayScore ?: "–"}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("SCORE", color = PrediqLiveMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    LiveTeamBadge(game.awayParticipant)
                    Spacer(Modifier.height(8.dp))
                    Text(game.awayParticipant, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (game.predictionAvailable) {
                Surface(color = PrediqLiveCardAlt, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, PrediqLiveOutline)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("PREDIQ LIVE CALL", color = PrediqLiveMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(game.selectionLabel ?: "Current assessment", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            }
                            Surface(shape = CircleShape, color = PrediqLiveLime) {
                                Text(probability(game.confidence ?: game.probability), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = PrediqLiveInk, fontWeight = FontWeight.Bold)
                            }
                        }
                        game.why.firstOrNull()?.let { reason ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.AutoGraph, null, tint = PrediqLiveLime, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(reason, color = PrediqLiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                        game.watchOuts.firstOrNull()?.let { risk ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.WarningAmber, null, tint = Color(0xFFFFC66B), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(risk, color = Color(0xFFE7D2AF), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Surface(color = PrediqLiveCardAlt, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Visibility, null, tint = PrediqLiveMuted)
                        Spacer(Modifier.width(10.dp))
                        Text("Tracking this match. PredIQ is withholding a call until the evidence clears its confidence gate.", color = PrediqLiveMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Analysed ${relativeTime(game.lastAnalysedAt ?: game.updatedAt)}", color = PrediqLiveMuted, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open analysis", color = PrediqLiveLime, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Icon(Icons.Outlined.ChevronRight, null, tint = PrediqLiveLime, modifier = Modifier.size(18.dp))
                }
            }
        }
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
