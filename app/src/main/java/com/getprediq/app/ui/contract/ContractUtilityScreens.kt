package com.getprediq.app.ui.contract

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractState
import com.getprediq.app.data.v2.V2Change
import com.getprediq.app.data.v2.V2ResearchTeam
import com.getprediq.app.data.v2.V2NotificationSettings
import com.getprediq.app.ui.TeamCrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

@Composable
fun NotificationInboxContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prediq_notification_inbox", android.content.Context.MODE_PRIVATE) }
    var readKeys by remember { mutableStateOf(prefs.getStringSet("read", emptySet())?.toSet().orEmpty()) }
    var tab by rememberSaveable { mutableStateOf("all") }
    val changes = buildList {
        addAll(state.today?.changes.orEmpty())
        state.live?.changes.orEmpty().forEach { live -> if (none { it.eventId == live.eventId && it.occurredAt == live.occurredAt && it.type == live.type }) add(live) }
    }.sortedByDescending { it.occurredAt.orEmpty() }
    fun key(change: V2Change) = "${change.eventId}|${change.occurredAt}|${change.type}"
    fun bucket(change: V2Change): String {
        val text = "${change.type} ${change.title} ${change.summary}".lowercase()
        return when {
            "result" in text || "settled" in text || "won" in text || "lost" in text -> "results"
            "news" in text || "injur" in text || "lineup" in text || "unavailable" in text -> "news"
            else -> "picks"
        }
    }
    val visible = changes.filter { change ->
        when (tab) {
            "unread" -> key(change) !in readKeys
            "picks", "results", "news" -> bucket(change) == tab
            else -> true
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Notifications", subtitle = "Prediction, lineup, result and account changes", showBack = true, onBack = onBack) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all", "unread", "picks", "results", "news")) { item ->
                    val count = when (item) {
                        "all" -> changes.size
                        "unread" -> changes.count { key(it) !in readKeys }
                        else -> changes.count { bucket(it) == item }
                    }
                    FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text("${humanize(item)}${if (count > 0) "  $count" else ""}") })
                }
            }
        }
        if (visible.isEmpty()) item { EmptyState(if (tab == "unread") "You're caught up" else "Nothing here yet", "Meaningful PredIQ changes will appear here.", Icons.Outlined.NotificationsNone) }
        else items(visible, key = { key(it) }) { change ->
            val isRead = key(change) in readKeys
            Box(Modifier.fillMaxWidth().clickable {
                if (!isRead) {
                    val next = readKeys + key(change); readKeys = next; prefs.edit().putStringSet("read", next).apply()
                }
            }) { NotificationChangeCard(change, isRead) }
        }
        if (changes.isNotEmpty() && changes.any { key(it) !in readKeys }) {
            item {
                TextButton(onClick = {
                    val all = changes.map(::key).toSet(); readKeys = all; prefs.edit().putStringSet("read", all).apply()
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.DoneAll, null); Spacer(Modifier.width(7.dp)); Text("Mark all as read") }
            }
        }
    }
}

@Composable
private fun NotificationChangeCard(change: V2Change, isRead: Boolean) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val tone = when (change.type) { "strengthened" -> Green; "weakened" -> Red; else -> Purple }
            Box(Modifier.size(42.dp).background(tone.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(when (change.type) { "strengthened" -> Icons.Outlined.TrendingUp; "weakened" -> Icons.Outlined.TrendingDown; else -> Icons.Outlined.Notifications }, null, tint = tone)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(change.title, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (!isRead) Box(Modifier.size(8.dp).background(Purple, CircleShape))
                }
                Text(change.summary ?: humanize(change.type), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (change.oldChance?.percent != null || change.newChance?.percent != null) Text("${change.oldChance?.percent ?: "–"}% → ${change.newChance?.percent ?: "–"}%", color = tone, fontWeight = FontWeight.SemiBold)
            }
            Text(compactTime(change.occurredAt).takeLast(5), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TeamCompareContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val teams = state.research?.teamsInTodayPicks.orEmpty()
    var left by remember(teams) { mutableStateOf(teams.getOrNull(0)) }
    var right by remember(teams) { mutableStateOf(teams.getOrNull(1)) }
    var tab by rememberSaveable { mutableStateOf("overview") }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Compare teams", subtitle = "Form, scoring and match-profile evidence", showBack = true, onBack = onBack) }
        if (teams.size < 2) { item { EmptyState("More team data needed", "Search or open more teams before comparing them.", Icons.Outlined.CompareArrows) }; return@LazyColumn }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TeamSelector(left, teams.filter { it.id != right?.id }, Modifier.weight(1f)) { left = it }
                TeamSelector(right, teams.filter { it.id != left?.id }, Modifier.weight(1f)) { right = it }
            }
        }
        val l = left; val r = right
        if (l != null && r != null) {
            item {
                BrightCard(brush = HeroBrush) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            TeamCrest(l.name, l.sport, size = 58.dp, dark = true)
                            Spacer(Modifier.height(6.dp)); Text(l.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                        }
                        Text("VS", color = Lime, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            TeamCrest(r.name, r.sport, size = 58.dp, dark = true)
                            Spacer(Modifier.height(6.dp)); Text(r.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("overview", "form", "stats", "head-to-head")) { item ->
                        FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(if (item == "head-to-head") "H2H" else humanize(item)) })
                    }
                }
            }
            when (tab) {
                "overview" -> item {
                    WhiteCard {
                        SectionHeading("At a glance")
                        CompareMetricRow("Profile matches", l.matchesCount.toString(), r.matchesCount.toString())
                        CompareMetricRow("Country", l.country ?: "–", r.country ?: "–")
                        val lo = l.profile["overall"] as? JsonObject
                        val ro = r.profile["overall"] as? JsonObject
                        CompareMetricRow("Points per game", compareMetric(lo, "ppg"), compareMetric(ro, "ppg"))
                        CompareMetricRow("Goals / game", compareMetric(lo, "gf_pg"), compareMetric(ro, "gf_pg"))
                        CompareMetricRow("Goals allowed / game", compareMetric(lo, "ga_pg"), compareMetric(ro, "ga_pg"))
                    }
                }
                "form" -> item {
                    WhiteCard {
                        SectionHeading("Recent form")
                        val lo = l.profile["overall"] as? JsonObject
                        val ro = r.profile["overall"] as? JsonObject
                        CompareMetricRow("Form", compareMetric(lo, "form"), compareMetric(ro, "form"))
                        CompareMetricRow("Wins", compareMetric(lo, "wins"), compareMetric(ro, "wins"))
                        CompareMetricRow("Draws", compareMetric(lo, "draws"), compareMetric(ro, "draws"))
                        CompareMetricRow("Losses", compareMetric(lo, "losses"), compareMetric(ro, "losses"))
                        HorizontalDivider(color = Hairline)
                        Text("Home / away", color = Ink, fontWeight = FontWeight.Bold)
                        CompareMetricRow("Home PPG", compareMetric(l.profile["home"] as? JsonObject, "ppg"), compareMetric(r.profile["home"] as? JsonObject, "ppg"))
                        CompareMetricRow("Away PPG", compareMetric(l.profile["away"] as? JsonObject, "ppg"), compareMetric(r.profile["away"] as? JsonObject, "ppg"))
                    }
                }
                "stats" -> item {
                    WhiteCard {
                        SectionHeading("Match profile")
                        val lo = l.profile["overall"] as? JsonObject
                        val ro = r.profile["overall"] as? JsonObject
                        CompareMetricRow("BTTS rate", compareRate(lo, "btts_rate"), compareRate(ro, "btts_rate"))
                        CompareMetricRow("Over 2.5 rate", compareRate(lo, "over25_rate"), compareRate(ro, "over25_rate"))
                        CompareMetricRow("Goals scored", compareMetric(lo, "gf_pg"), compareMetric(ro, "gf_pg"))
                        CompareMetricRow("Goals conceded", compareMetric(lo, "ga_pg"), compareMetric(ro, "ga_pg"))
                        CompareMetricRow("Rest days", compareMetric(l.profile, "rest_days"), compareMetric(r.profile, "rest_days"))
                    }
                }
                else -> item {
                    EmptyState(
                        "Head-to-head data is not in this contract yet",
                        "PredIQ will show recent meetings here when the comparison contract exposes canonical head-to-head history. Current form and match-profile comparisons above are live.",
                        Icons.Outlined.SwapHoriz,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareMetricRow(label: String, left: String, right: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(left, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.ExtraBold)
        Text(label, Modifier.weight(1.3f), color = Muted, style = MaterialTheme.typography.bodySmall)
        Text(right, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.ExtraBold)
    }
    HorizontalDivider(color = Hairline)
}

private fun compareMetric(obj: JsonObject?, key: String): String = (obj?.get(key) as? JsonPrimitive)?.contentOrNull ?: "–"
private fun compareRate(obj: JsonObject?, key: String): String = (obj?.get(key) as? JsonPrimitive)?.doubleOrNull?.let { "${(it * 100).toInt()}%" } ?: "–"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSelector(selected: V2ResearchTeam?, choices: List<V2ResearchTeam>, modifier: Modifier, onSelect: (V2ResearchTeam) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }, modifier) {
        OutlinedTextField(selected?.name ?: "Choose team", {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) }, shape = RoundedCornerShape(16.dp))
        ExposedDropdownMenu(open, { open = false }) {
            choices.forEach { team -> DropdownMenuItem(text = { Text(team.name) }, onClick = { onSelect(team); open = false }) }
        }
    }
}

@Composable
fun ReferralContractScreen(state: PrediqContractState, onBack: () -> Unit) {
    val context = LocalContext.current
    val affiliate = state.account?.affiliateSummary ?: JsonObject(emptyMap())
    val shareUrl = (affiliate["share_url"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    val available = (affiliate["available_ugx"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L
    val pending = (affiliate["pending_ugx"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L
    val paid = (affiliate["paid_ugx"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L
    val referrals = (affiliate["referrals"] as? JsonPrimitive)?.contentOrNull ?: "0"
    val conversions = (affiliate["conversions"] as? JsonPrimitive)?.contentOrNull ?: "0"
    val recent = (affiliate["recent"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PrediqHeader(title = "Refer & earn", subtitle = "20% on eligible settled referred payments", showBack = true, onBack = onBack) }
        item {
            BrightCard(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF65A30D), Color(0xFF16A34A)))) {
                Icon(Icons.Outlined.GroupAdd, null, tint = Color.White, modifier = Modifier.size(38.dp))
                Text("UGX ${"%,d".format(available)} available", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("UGX ${"%,d".format(pending)} pending · UGX ${"%,d".format(paid)} paid", color = Color.White.copy(alpha = .8f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("$referrals referred", "good", true)
                    StatusPill("$conversions converted", "purple", true)
                }
            }
        }
        item {
            WhiteCard {
                SectionHeading("Your referral link")
                val code = (affiliate["code"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                Text(code.ifBlank { "Referral code unavailable" }, color = PurpleDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                if (shareUrl.isNotBlank()) Text(shareUrl, color = Muted, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        if (shareUrl.isBlank()) return@Button
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Try PredIQ: $shareUrl")
                        }, "Share PredIQ"))
                    },
                    enabled = shareUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(7.dp)); Text("Share referral link") }
            }
        }
        item {
            WhiteCard {
                SectionHeading("How earnings work")
                val rate = (affiliate["commission_rate"] as? JsonPrimitive)?.doubleOrNull ?: .20
                InfoRow(Icons.Outlined.Percent, "Commission", "${(rate * 100).toInt()}%")
                InfoRow(Icons.Outlined.CalendarMonth, "Commission window", referralValue("commission_months", affiliate["commission_months"] as? JsonPrimitive))
                InfoRow(Icons.Outlined.HourglassBottom, "Holding period", referralValue("holding_days", affiliate["holding_days"] as? JsonPrimitive))
                InfoRow(Icons.Outlined.Payments, "Minimum payout", referralValue("minimum_payout_ugx", affiliate["minimum_payout_ugx"] as? JsonPrimitive))
                Text("Payout remains an audited PredIQ admin action; the app only shows backend-confirmed earnings.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionHeading("Recent referrals") }
            items(recent.take(30)) { row ->
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(Green.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonAdd, null, tint = GreenDeep) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(humanize(row.string("status")), color = Ink, fontWeight = FontWeight.Bold)
                            Text(friendlyDateTime(row.string("converted_at") ?: row.string("attributed_at")), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        val amount = row.string("commission_ugx")?.toLongOrNull() ?: 0L
                        Text("UGX ${"%,d".format(amount)}", color = GreenDeep, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        } else if (affiliate.isNotEmpty()) item { EmptyState("No referrals yet", "Share your link with people who will use PredIQ responsibly.", Icons.Outlined.GroupAdd) }
    }
}


private fun referralValue(key: String, value: JsonPrimitive?): String {
    val raw = value?.contentOrNull?.takeUnless { it.equals("null", ignoreCase = true) }?.trim().orEmpty()
    if (raw.isBlank()) return "–"
    return when (key) {
        "commission_rate" -> "${((value?.doubleOrNull ?: 0.0) * 100).toInt()}%"
        "commission_months" -> "$raw months"
        "holding_days" -> "$raw days"
        "minimum_payout_ugx", "pending_ugx", "available_ugx", "paid_ugx" -> runCatching { "UGX %,d".format(raw.toLong()) }.getOrDefault(raw)
        else -> raw
    }
}

@Composable
fun OnboardingContractScreen(
    state: PrediqContractState,
    onDone: () -> Unit,
    onSearchTeams: () -> Unit,
    onSaveNotifications: (V2NotificationSettings) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prediq_onboarding", android.content.Context.MODE_PRIVATE) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var football by rememberSaveable { mutableStateOf(prefs.getBoolean("sport_football", true)) }
    var basketball by rememberSaveable { mutableStateOf(prefs.getBoolean("sport_basketball", false)) }
    var tennis by rememberSaveable { mutableStateOf(prefs.getBoolean("sport_tennis", false)) }
    var cricket by rememberSaveable { mutableStateOf(prefs.getBoolean("sport_cricket", false)) }
    var baseball by rememberSaveable { mutableStateOf(prefs.getBoolean("sport_baseball", false)) }
    var rugby by rememberSaveable { mutableStateOf(prefs.getBoolean("sport_rugby", false)) }
    var dailyPicks by rememberSaveable { mutableStateOf(true) }
    var predictionChanges by rememberSaveable { mutableStateOf(true) }
    var lineups by rememberSaveable { mutableStateOf(true) }
    var live by rememberSaveable { mutableStateOf(true) }
    var results by rememberSaveable { mutableStateOf(true) }
    val titles = listOf("What sports do you follow?", "Pick teams or leagues", "Stay updated your way", "You're ready")
    val selectedSports = buildList {
        if (football) add("Football"); if (tennis) add("Tennis"); if (basketball) add("Basketball")
        if (cricket) add("Cricket"); if (baseball) add("Baseball"); if (rugby) add("Rugby")
    }
    Column(Modifier.fillMaxSize().background(Ivory).padding(22.dp).systemBarsPadding(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Step ${step + 1} of 4", color = PurpleDeep, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth(), color = Purple)
        Text(titles[step], color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        when (step) {
            0 -> {
                Text("Choose what interests you. PredIQ will keep this on this device and use your follows for personalised alerts.", color = Muted)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    item { InterestToggle("Football", Icons.Outlined.SportsSoccer, football) { football = it } }
                    item { InterestToggle("Tennis", Icons.Outlined.SportsTennis, tennis) { tennis = it } }
                    item { InterestToggle("Basketball", Icons.Outlined.SportsBasketball, basketball) { basketball = it } }
                    item { InterestToggle("Cricket", Icons.Outlined.SportsCricket, cricket) { cricket = it } }
                    item { InterestToggle("Baseball", Icons.Outlined.SportsBaseball, baseball) { baseball = it } }
                    item { InterestToggle("Rugby", Icons.Outlined.SportsRugby, rugby) { rugby = it } }
                }
            }
            1 -> {
                Text("Follow the teams, players and competitions that matter. Those follows are saved to your PredIQ account.", color = Muted)
                WhiteCard {
                    InfoRow(Icons.Outlined.Bookmarks, "Following now", "${state.follows?.follows?.size ?: 0} saved")
                    HorizontalDivider(color = Hairline)
                    InfoRow(Icons.Outlined.Shield, "Teams", "${state.account?.followingSummary?.teams ?: 0}")
                    InfoRow(Icons.Outlined.Person, "Players", "${state.account?.followingSummary?.players ?: 0}")
                    InfoRow(Icons.Outlined.EmojiEvents, "Leagues", "${state.account?.followingSummary?.leagues ?: 0}")
                }
                Button(onClick = onSearchTeams, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Search, null); Spacer(Modifier.width(7.dp)); Text("Search & follow") }
            }
            2 -> {
                Text("Choose what is worth interrupting you for. You can refine this later in Account.", color = Muted)
                WhiteCard {
                    ToggleRowOnboarding("Daily top picks", dailyPicks) { dailyPicks = it }
                    ToggleRowOnboarding("Prediction changes", predictionChanges) { predictionChanges = it }
                    ToggleRowOnboarding("Lineups confirmed", lineups) { lineups = it }
                    ToggleRowOnboarding("Live opportunities", live) { live = it }
                    ToggleRowOnboarding("Results settled", results) { results = it }
                }
            }
            else -> {
                BrightCard {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Lime, modifier = Modifier.size(42.dp))
                    Text("All set.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(selectedSports.joinToString().ifBlank { "No sports selected yet" }, color = Color.White.copy(alpha = .82f))
                    Text("${state.follows?.follows?.size ?: 0} follows · ${state.notifications?.timezone ?: "Africa/Kampala"} · ${state.account?.profile?.currency ?: "UGX"}", color = Color.White.copy(alpha = .72f))
                }
                WhiteCard {
                    InfoRow(Icons.Outlined.AutoGraph, "Odds Builder", "Build target odds from PredIQ Intelligence")
                    InfoRow(Icons.Outlined.NotificationsActive, "Alerts", "Your selected changes are ready")
                    InfoRow(Icons.Outlined.VerifiedUser, "Results", "Every published call remains auditable")
                }
            }
        }
        if (step != 0) Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (step < 3) step++ else {
                    prefs.edit()
                        .putBoolean("completed", true)
                        .putBoolean("sport_football", football).putBoolean("sport_tennis", tennis)
                        .putBoolean("sport_basketball", basketball).putBoolean("sport_cricket", cricket)
                        .putBoolean("sport_baseball", baseball).putBoolean("sport_rugby", rugby)
                        .apply()
                    val base = state.notifications ?: V2NotificationSettings()
                    onSaveNotifications(base.copy(alerts = base.alerts.copy(
                        dailyPicks = dailyPicks,
                        liveChanges = predictionChanges || live,
                        lineupChanges = lineups,
                        results = results,
                    )))
                    onDone()
                }
            },
            enabled = step != 0 || selectedSports.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(if (step < 3) "Continue" else "Start exploring") }
        if (step > 0) TextButton(onClick = { step-- }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}

@Composable
private fun InterestToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Purple.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Purple) }; Spacer(Modifier.width(12.dp)); Text(label, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Checkbox(checked, onCheckedChange = onChange) }
    }
}

@Composable
private fun ToggleRowOnboarding(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = Ink); Switch(checked, onCheckedChange = onChange) }
}
