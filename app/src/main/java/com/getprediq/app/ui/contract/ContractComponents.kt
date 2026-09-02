package com.getprediq.app.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getprediq.app.data.v2.*

val Ivory = Color(0xFFF7F5F0)
val IvoryDeep = Color(0xFFF0EEE8)
val Ink = Color(0xFF111827)
val Muted = Color(0xFF6B7280)
val Purple = Color(0xFF5B3DF5)
val PurpleDeep = Color(0xFF3520C9)
val Indigo = Color(0xFF1D4ED8)
val Blue = Color(0xFF2563EB)
val Green = Color(0xFF16A34A)
val GreenDeep = Color(0xFF08783A)
val Lime = Color(0xFFB7F32D)
val Amber = Color(0xFFF59E0B)
val Red = Color(0xFFDC2626)
val Hairline = Color(0xFFE4E1DA)

val HeroBrush = Brush.linearGradient(listOf(Purple, Indigo))
val LiveBrush = Brush.linearGradient(listOf(Color(0xFF08783A), Color(0xFF064E3B)))
val ResearchBrush = Brush.linearGradient(listOf(Color(0xFF3B2BC5), Color(0xFF2563EB)))

@Composable
fun ContractSurface(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Ivory), content = content)
}

@Composable
fun PrediqHeader(
    title: String? = null,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            FilledTonalIconButton(onClick = onBack, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Ink)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (title == null) {
                Text("Pred", fontSize = 31.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF183A7A))
                Text("IQ", fontSize = 31.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8EDB16), modifier = Modifier.offset(x = 61.dp, y = (-37).dp))
            } else {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (actionIcon != null) {
            FilledTonalIconButton(onClick = onAction, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)) {
                Icon(actionIcon, null, tint = Ink)
            }
        }
    }
}

@Composable
fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    brush: Brush = HeroBrush,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().background(brush).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Lime, modifier = Modifier.size(18.dp))
                Text(eyebrow.uppercase(), color = Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            subtitle?.let { Text(it, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium) }
            content()
        }
    }
}

@Composable
fun MetricStrip(metrics: List<Triple<String, String, ImageVector>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.forEach { (label, value, icon) ->
            Column(
                Modifier.weight(1f).background(Color.White.copy(alpha = .13f), RoundedCornerShape(18.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(icon, null, tint = Color.White.copy(alpha = .86f), modifier = Modifier.size(20.dp))
                Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(label, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun WhiteCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val clickable = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    Card(
        modifier = clickable.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun BrightCard(
    modifier: Modifier = Modifier,
    brush: Brush = HeroBrush,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    Card(clickable.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.fillMaxWidth().background(brush).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun StatusPill(text: String, tone: String = "neutral", onDark: Boolean = false) {
    val (bg, fg) = when (tone) {
        "good" -> if (onDark) Lime.copy(alpha = .18f) to Lime else Color(0xFFE8F8EA) to GreenDeep
        "warn" -> if (onDark) Amber.copy(alpha = .18f) to Color(0xFFFFD166) else Color(0xFFFFF3D6) to Color(0xFF9A5A00)
        "bad" -> if (onDark) Red.copy(alpha = .18f) to Color(0xFFFFB4AB) else Color(0xFFFFE9E7) to Red
        "purple" -> if (onDark) Color.White.copy(alpha = .16f) to Color.White else Color(0xFFEDE9FE) to PurpleDeep
        else -> if (onDark) Color.White.copy(alpha = .14f) to Color.White else IvoryDeep to Muted
    }
    Box(Modifier.background(bg, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
    }
}

fun decisionTone(code: String): String = when (code) {
    "top_pick", "pick", "settled" -> "good"
    "wait", "watching" -> "warn"
    "pass", "withdrawn" -> "bad"
    else -> "neutral"
}

@Composable
fun TeamLine(event: V2Event, dark: Boolean = false) {
    val fg = if (dark) Color.White else Ink
    val muted = if (dark) Color.White.copy(alpha = .7f) else Muted
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(event.competition.name ?: event.sport.orEmpty().replaceFirstChar(Char::uppercase), color = muted, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EntityAvatar(event.participants.home.name, dark)
            Spacer(Modifier.width(8.dp))
            Text(event.participants.home.name, color = fg, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            event.score?.let {
                Text("${it.home ?: "–"}  :  ${it.away ?: "–"}", color = fg, fontWeight = FontWeight.ExtraBold)
            } ?: Text("VS", color = muted, fontWeight = FontWeight.Bold)
            Text(event.participants.away.name, color = fg, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            EntityAvatar(event.participants.away.name, dark)
        }
    }
}

@Composable
fun EntityAvatar(name: String, dark: Boolean = false, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier.size(38.dp).background(if (dark) Color.White.copy(alpha = .16f) else Color(0xFFEDE9FE), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = if (dark) Color.White else PurpleDeep, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun DecisionCard(
    card: V2DecisionCard,
    onOpen: () -> Unit,
    onFollow: (() -> Unit)? = null,
    featured: Boolean = false,
) {
    if (featured) {
        BrightCard(onClick = onOpen) {
            TeamLine(card.event, dark = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    StatusPill(card.decision.label, decisionTone(card.decision.code), onDark = true)
                    Spacer(Modifier.height(8.dp))
                    Text(card.pick.label ?: "PredIQ pick", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(card.chance.label, color = Color.White.copy(alpha = .82f))
                }
                Text("${card.chance.percent ?: "–"}%", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (card.value.available) StatusPill(card.value.label + (card.value.edgePoints?.let { " · +${it.toInt()} edge" } ?: ""), "good", true)
                StatusPill(card.evidence.label, "purple", true)
                StatusPill(card.risk.label, if (card.risk.level == "high") "bad" else "neutral", true)
            }
            card.reasons.firstOrNull()?.let { Text(it.label, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onFollow != null) TextButton(onClick = onFollow) { Icon(if (card.followState.following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, null, tint = Color.White); Spacer(Modifier.width(4.dp)); Text(if (card.followState.following) "Following" else "Follow", color = Color.White) }
                TextButton(onClick = onOpen) { Text("View", color = Color.White); Icon(Icons.Outlined.ChevronRight, null, tint = Color.White) }
            }
        }
    } else {
        WhiteCard(onClick = onOpen) {
            TeamLine(card.event)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    StatusPill(card.decision.label, decisionTone(card.decision.code))
                    Spacer(Modifier.height(7.dp))
                    Text(card.pick.label ?: card.decision.reason ?: "PredIQ view", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${card.chance.percent ?: "–"}%", color = PurpleDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(card.chance.label, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (card.value.available) StatusPill(card.value.label, "good")
                StatusPill(card.evidence.label, "purple")
                StatusPill(card.risk.label, if (card.risk.level == "high") "bad" else "neutral")
            }
            card.reasons.firstOrNull()?.let { Text(it.label, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onFollow != null) IconButton(onClick = onFollow) { Icon(if (card.followState.following) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd, "Follow", tint = Purple) }
                TextButton(onClick = onOpen) { Text("View analysis"); Icon(Icons.Outlined.ChevronRight, null) }
            }
        }
    }
}

@Composable
fun LiveDecisionCard(card: V2LiveCard, onOpen: () -> Unit) {
    BrightCard(brush = LiveBrush, onClick = onOpen) {
        TeamLine(card.event, dark = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StatusPill(card.decision.label, decisionTone(card.decision.code), onDark = true)
                Spacer(Modifier.height(6.dp))
                Text(card.pick.label ?: "Live view", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${card.currentChance.percent ?: card.chance.percent ?: "–"}%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                if (card.change.points != null) Text("${if (card.change.direction == "up") "↑" else if (card.change.direction == "down") "↓" else ""} ${card.change.points.toInt()} pts", color = if (card.change.direction == "down") Color(0xFFFFB4AB) else Lime, fontWeight = FontWeight.Bold)
            }
        }
        card.reasons.firstOrNull()?.let { Text(it.label, color = Color.White.copy(alpha = .82f)) }
        if (card.analysisQuality == "score_only") StatusPill("Game state only", "warn", true)
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, subtitle: String? = null, trailing: String? = null, onClick: (() -> Unit)? = null, tone: Color = Purple) {
    val modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(tone.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tone) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        trailing?.let { Text(it, color = Ink, fontWeight = FontWeight.Bold) }
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
    }
}

@Composable
fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Outlined.Insights, action: String? = null, onAction: () -> Unit = {}) {
    WhiteCard {
        Box(Modifier.size(50.dp).background(Color(0xFFEDE9FE), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Purple) }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
        Text(body, color = Muted)
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun LoadingState(label: String = "Checking PredIQ…") {
    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = Purple)
            Text(label, color = Muted)
        }
    }
}

fun percent(value: Double?): String = value?.let { "${(it * 100).toInt()}%" } ?: "–"
fun compactTime(raw: String?): String = raw?.replace('T', ' ')?.take(16) ?: ""
fun humanize(raw: String?): String = raw.orEmpty().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
