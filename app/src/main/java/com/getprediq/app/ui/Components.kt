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
    val accent = when {
        progress >= .75f -> PrediqGreen
        progress >= .60f -> PrediqBlue
        else -> PrediqAmber
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PrediqMuted)
            Text(probability(value), style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0xFFE2E6E2))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(accent))
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
    val callProbability = pick.probability
    val callConfidence = pick.confidence ?: pick.probability
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFDDE5E8)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompetitionMark(pick.competition, pick.sportCode, 28.dp)
                    if (!pick.competition.isNullOrBlank()) Spacer(Modifier.width(8.dp))
                    Column {
                        Text("HIGH-CONVICTION CALL", style = MaterialTheme.typography.labelSmall, color = PrediqBlue, fontWeight = FontWeight.Bold)
                        Text("${pick.competition ?: prettySport(pick.sportCode)} • ${kickoff(pick.startsAt)}", style = MaterialTheme.typography.labelMedium, color = PrediqMuted)
                    }
                }
                if (pick.status == "live") StatusPill("LIVE", PrediqLiveInk, PrediqLiveLime)
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    TeamCrest(pick.homeParticipant, pick.sportCode, 58.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(compactTeamName(pick.homeParticipant, pick.sportCode), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                }
                Text("VS", color = PrediqMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    TeamCrest(pick.awayParticipant, pick.sportCode, 58.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(compactTeamName(pick.awayParticipant, pick.sportCode), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                }
            }

            Surface(shape = RoundedCornerShape(20.dp), color = PrediqLiveInk) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("PREDIQ CALL", color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(pick.selectionLabel ?: "Current assessment", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text(marketName(pick.marketKey), color = PrediqLiveMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(probability(callProbability), style = MaterialTheme.typography.headlineMedium, color = PrediqLiveLime, fontWeight = FontWeight.Bold)
                            Text("model probability", color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    ConfidenceBarDark(callConfidence)
                }
            }

            if (pick.explanation.isNotEmpty()) {
                Text("Why it ranks", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
                IndicatorList(pick.explanation.take(2))
            }
            pick.watchOuts.firstOrNull()?.let { risk ->
                RiskNote(risk)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Open the evidence, risks and context", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Outlined.ArrowForward, "Open analysis", tint = PrediqBlue)
            }
        }
    }
}

@Composable
private fun ConfidenceBarDark(value: Double?) {
    val progress = (value ?: 0.0).coerceIn(0.0, 1.0).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CONFIDENCE", color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall)
            Text(probability(value), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(PrediqLiveCardAlt)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(PrediqLiveLime))
        }
    }
}

@Composable
fun AssessmentCard(item: AssessmentDto, onOpen: () -> Unit) {
    val risk = item.riskLevel?.replace('_', ' ')?.replaceFirstChar(Char::uppercase) ?: "Unspecified"
    val riskColor = when (item.riskLevel?.lowercase()) {
        "low" -> PrediqGreen
        "high", "very_high" -> PrediqRed
        else -> PrediqAmber
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (item.promotable) Color(0xFFD7E7DD) else Color(0xFFE3E7E8)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CompetitionMark(item.competition, item.sportCode, 26.dp)
                    if (!item.competition.isNullOrBlank()) Spacer(Modifier.width(8.dp))
                    Text("${item.competition ?: prettySport(item.sportCode)} • ${kickoff(item.startsAt)}", style = MaterialTheme.typography.labelMedium, color = PrediqMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(if (item.promotable) (item.confidenceBand ?: "PICK").uppercase() else "WATCH", if (item.promotable) PrediqGreen else PrediqAmber, if (item.promotable) Color(0xFFE9F8EF) else Color(0xFFFFF3E7))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TeamCrest(item.homeParticipant, item.sportCode, 42.dp)
                Spacer(Modifier.width(9.dp))
                Text(compactTeamName(item.homeParticipant, item.sportCode), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("vs", color = PrediqMuted, modifier = Modifier.padding(horizontal = 8.dp))
                Text(compactTeamName(item.awayParticipant, item.sportCode), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(9.dp))
                TeamCrest(item.awayParticipant, item.sportCode, 42.dp)
            }

            Surface(color = PrediqBackground, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("PREDIQ CALL", style = MaterialTheme.typography.labelSmall, color = PrediqMuted, fontWeight = FontWeight.Bold)
                        Text(item.selectionLabel ?: "Assessment", style = MaterialTheme.typography.titleMedium, color = PrediqBlue, fontWeight = FontWeight.Bold)
                        Text(marketName(item.marketKey), color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(probability(item.probability), style = MaterialTheme.typography.headlineSmall, color = PrediqBlue, fontWeight = FontWeight.Bold)
                        Text("probability", color = PrediqMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactSignal("CONFIDENCE", probability(item.confidence), PrediqBlue, Modifier.weight(1f))
                CompactSignal("RISK", risk, riskColor, Modifier.weight(1f))
                CompactSignal("FRESHNESS", relativeTime(item.lastAnalysedAt), PrediqMuted, Modifier.weight(1f))
            }

            if (item.why.isNotEmpty()) {
                Text("Why PredIQ sees it", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
                IndicatorList(item.why.take(2))
            }
            item.watchOuts.firstOrNull()?.let(::RiskNote)
            item.changeReason?.let {
                Surface(color = Color(0xFFEEF3FF), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Update, null, tint = PrediqBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = PrediqBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Full analysis", color = PrediqBlue, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Outlined.ChevronRight, null, tint = PrediqBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CompactSignal(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = PrediqSurfaceLow, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(label, color = PrediqMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RiskNote(text: String) {
    Surface(color = Color(0xFFFFF4E5), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.WarningAmber, null, tint = PrediqAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color(0xFF70521A), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
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
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(PrediqLiveLime, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("LIVE INTELLIGENCE", color = PrediqLiveLime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("What changed. What matters.", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
                Surface(onClick = onRefresh, shape = CircleShape, color = PrediqLiveCardAlt) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Refresh, "Refresh live intelligence", tint = Color.White)
                    }
                }
            }
            Text(
                if (loading && live == null) "Scanning live fixtures, score changes and current PredIQ signals…" else live?.message ?: "Scanning current fixtures for evidence-backed opportunities.",
                color = PrediqLiveMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveMetric("LIVE", (live?.liveCount ?: 0).toString(), Modifier.weight(1f))
                LiveMetric("ANALYSED", (live?.predictedCount ?: 0).toString(), Modifier.weight(1f))
                LiveMetric("STRONG", (live?.strongCount ?: 0).toString(), Modifier.weight(1f), highlight = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Source ${relativeTime(live?.lastSourceUpdate)}", color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall)
                Text("Auto-refresh ~${live?.refreshSeconds ?: 300}s", color = PrediqLiveMuted, style = MaterialTheme.typography.labelSmall)
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
private fun LiveTeamBadge(name: String, sport: String) {
    TeamCrest(name, sport, 54.dp, dark = true)
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
                    LiveTeamBadge(game.homeParticipant, game.sportCode)
                    Spacer(Modifier.height(8.dp))
                    Text(game.homeParticipant, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text("${game.homeScore ?: "–"}  ${game.awayScore ?: "–"}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("SCORE", color = PrediqLiveMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    LiveTeamBadge(game.awayParticipant, game.sportCode)
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

            game.changeReason?.let { reason ->
                Surface(color = PrediqLiveCardAlt, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, PrediqLiveOutline)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Update, null, tint = PrediqLiveLime, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(reason, color = PrediqLiveMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
    val (fg, bg) = when (item.outcome) {
        "won" -> PrediqGreen to Color(0xFFE9F8EF)
        "lost" -> PrediqRed to Color(0xFFFFEDEC)
        "void" -> PrediqMuted to PrediqSurfaceLow
        else -> PrediqAmber to Color(0xFFFFF3E7)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE3E7E8)),
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CompetitionMark(item.competition, item.sportCode, 26.dp)
                    if (!item.competition.isNullOrBlank()) Spacer(Modifier.width(8.dp))
                    Text("${item.competition ?: prettySport(item.sportCode)} • FINAL", style = MaterialTheme.typography.labelMedium, color = PrediqMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(item.outcome.uppercase(), fg, bg)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TeamCrest(item.homeParticipant, item.sportCode, 40.dp)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(compactTeamName(item.homeParticipant, item.sportCode), fontWeight = FontWeight.SemiBold)
                        Text(homeScore?.toString() ?: "—", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(compactTeamName(item.awayParticipant, item.sportCode), color = PrediqMuted)
                        Text(awayScore?.toString() ?: "—", color = PrediqMuted, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(9.dp))
                TeamCrest(item.awayParticipant, item.sportCode, 40.dp)
            }
            Surface(color = PrediqSurfaceLow, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1.35f)) {
                        Text("PREDIQ PICK", fontSize = 10.sp, color = PrediqMuted, fontWeight = FontWeight.Bold)
                        Text(selectionForResult(item), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(marketName(item.marketKey), color = PrediqMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(.65f)) {
                        Text("MODEL / CONF.", fontSize = 10.sp, color = PrediqMuted, fontWeight = FontWeight.Bold)
                        Text("${probability(item.probability)} / ${probability(item.confidence)}", color = PrediqBlue, fontWeight = FontWeight.Bold)
                    }
                }
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
