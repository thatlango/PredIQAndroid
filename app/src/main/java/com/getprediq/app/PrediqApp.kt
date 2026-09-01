package com.getprediq.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getprediq.app.data.NotificationSettings
import com.getprediq.app.data.PlanDto
import com.getprediq.app.ui.*
import com.getprediq.app.ui.theme.PrediqBlue
import com.getprediq.app.ui.theme.PrediqMuted

private enum class MainTab(val label: String) { Today("Today"), Live("Live"), Results("Results"), Explore("Explore"), Account("Account") }

@Composable
fun PrediqApp(authCallback: Uri? = null, onAuthCallbackConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: PrediqViewModel = viewModel(factory = PrediqViewModel.factory(context.applicationContext))
    val state by vm.state
    val nav = rememberNavController()
    LaunchedEffect(authCallback) { authCallback?.let { uri -> val code=uri.getQueryParameter("code"); val callbackState=uri.getQueryParameter("state"); if(!code.isNullOrBlank()&&!callbackState.isNullOrBlank())vm.finishTuku(code,callbackState); onAuthCallbackConsumed() } }
    NavHost(navController = nav, startDestination = "main") {
        composable("main") { MainTabs(state, vm, { eventId -> if (vm.fullAccess) nav.navigate("match/$eventId") }, { if (vm.fullAccess) { vm.loadLeagueForecasts(); nav.navigate("leagues") } }) }
        composable("match/{eventId}", arguments = listOf(navArgument("eventId") { type = NavType.StringType })) { entry ->
            val eventId = entry.arguments?.getString("eventId").orEmpty(); LaunchedEffect(eventId) { if (eventId.isNotBlank()) vm.loadMatch(eventId) }; MatchIntelligenceScreen(state.matchIntelligence, onBack = { nav.popBackStack() })
        }
        composable("leagues") { LaunchedEffect(Unit) { vm.loadLeagueForecasts() }; LeagueWinnersScreen(state.leagueForecasts, onBack = { nav.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(state: PrediqUiState, vm: PrediqViewModel, onMatch: (String) -> Unit, onLeagueWinners: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Today) }; var authOpen by rememberSaveable { mutableStateOf(false) }; var paymentPlan by remember { mutableStateOf<PlanDto?>(null) }; var notificationsOpen by rememberSaveable { mutableStateOf(false) }; var responsibleOpen by rememberSaveable { mutableStateOf(false) }; var filtersOpen by rememberSaveable { mutableStateOf(false) }
    fun requestAccess() { if (state.account == null) authOpen = true else tab = MainTab.Account }
    LaunchedEffect(vm.fullAccess) { if (!vm.fullAccess) tab = MainTab.Account }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) { (if (vm.fullAccess) MainTab.entries else listOf(MainTab.Account)).forEach { item -> val icon = when (item) { MainTab.Today -> Icons.Outlined.Today; MainTab.Live -> Icons.Outlined.Sensors; MainTab.Results -> Icons.Outlined.Analytics; MainTab.Explore -> Icons.Outlined.Explore; MainTab.Account -> Icons.Outlined.Person }; NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, null) }, label = { Text(item.label) }) } } },
        floatingActionButton = { if (vm.fullAccess && (tab == MainTab.Today || tab == MainTab.Live || tab == MainTab.Results)) SmallFloatingActionButton(onClick = { filtersOpen = true }, containerColor = MaterialTheme.colorScheme.surface) { Icon(Icons.Outlined.Tune, "Filters", tint = PrediqBlue) } }
    ) { padding ->
        Box(Modifier.padding(bottom = padding.calculateBottomPadding())) { when (tab) { MainTab.Today -> TodayScreen(state, vm, ::requestAccess, onMatch, onLeagueWinners); MainTab.Live -> LiveScreen(state, vm, ::requestAccess, onMatch); MainTab.Results -> ResultsScreen(state, vm); MainTab.Explore -> ExploreScreen(state, vm); MainTab.Account -> AccountScreen(state, vm, { authOpen = true }, { paymentPlan = it }, { notificationsOpen = true }, { responsibleOpen = true }) } }
    }
    if (authOpen) AuthSheet(state, vm) { authOpen = false }; paymentPlan?.let { plan -> PaymentSheet(plan, state, vm) { paymentPlan = null } }; if (notificationsOpen) NotificationSheet(state, vm) { notificationsOpen = false }; if (responsibleOpen) ResponsibleUseSheet { responsibleOpen = false }; if (filtersOpen) FilterSheet(state, vm) { filtersOpen = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSheet(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    val context=LocalContext.current; var register by rememberSaveable { mutableStateOf(false) }; var name by rememberSaveable { mutableStateOf("") }; var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }; var country by rememberSaveable { mutableStateOf("") }; var referral by rememberSaveable { mutableStateOf("") }; var consent by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onClose) { Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (register) "Create PredIQ account" else "Sign in to PredIQ", style = MaterialTheme.typography.headlineMedium); Text("One secure Tuku account across web and Android. New accounts receive seven days of full access.", color = PrediqMuted)
        Button(onClick={ vm.startTuku(referral.takeIf(String::isNotBlank)){ url->context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } },enabled=!state.authBusy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){ Text("Continue with Tuku") }
        HorizontalDivider(); Text("Or use password",color=PrediqMuted,style=MaterialTheme.typography.labelMedium,modifier=Modifier.align(Alignment.CenterHorizontally))
        if (register) { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(country, { country = it.uppercase().take(2) }, label = { Text("Country code") }, placeholder = { Text("UG") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(referral,{referral=it.uppercase().take(32)},label={Text("Referral code (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (register) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { Checkbox(checked = consent, onCheckedChange = { consent = it }); Spacer(Modifier.width(8.dp)); Text("I agree to PredIQ’s terms and responsible-use notice.", color = PrediqMuted, modifier = Modifier.padding(top = 12.dp)) }
        state.authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button(onClick = { if (register) vm.register(name, email, password, country, consent, referral.takeIf(String::isNotBlank), onClose) else vm.login(email, password, onClose) }, enabled = !state.authBusy && email.isNotBlank() && password.length >= 8 && (!register || (name.isNotBlank() && country.length == 2 && consent)), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { if (state.authBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (register) "Create account" else "Sign in") }
        TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Already have an account? Sign in" else "Create a PredIQ account") }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSheet(plan: PlanDto, state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    var phone by rememberSaveable { mutableStateOf(state.account?.user?.phone.orEmpty()) }; LaunchedEffect(plan.code) { vm.clearPaymentMessage() }
    ModalBottomSheet(onDismissRequest = onClose) { Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("${plan.name} access", style = MaterialTheme.typography.headlineMedium); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total to pay", color = PrediqMuted); Text(ugx(plan.priceUgx), style = MaterialTheme.typography.titleLarge, color = PrediqBlue) }
        OutlinedTextField(phone, { phone = it }, label = { Text("Pay with Mobile Money") }, leadingIcon = { Icon(Icons.Outlined.PhoneAndroid, null) }, placeholder = { Text("0772 123 456") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("PredIQ creates a pending transaction first. Access activates only after Daraza confirms settlement.", color = PrediqMuted, style = MaterialTheme.typography.bodyMedium); state.paymentMessage?.let { Text(it, color = if (it.contains("failed", true) || it.contains("not configured", true)) MaterialTheme.colorScheme.error else PrediqBlue) }
        Button(onClick = { vm.checkout(plan.code, phone) }, enabled = !state.paymentBusy && state.paymentCapabilities.mobileMoney && phone.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { if (state.paymentBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Request payment") }; if (!state.paymentCapabilities.mobileMoney) Text(state.paymentCapabilities.message, color = MaterialTheme.colorScheme.error)
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSheet(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadNotifications() }; var local by remember(state.notifications) { mutableStateOf(state.notifications ?: NotificationSettings()) }
    ModalBottomSheet(onDismissRequest = onClose) { Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Notification Settings", style = MaterialTheme.typography.headlineMedium); Text("Choose the intelligence changes worth interrupting you for.", color = PrediqMuted)
        SettingSwitch("Push notifications", local.pushEnabled) { local = local.copy(pushEnabled = it) }; SettingSwitch("WhatsApp", local.whatsappEnabled) { local = local.copy(whatsappEnabled = it) }; SettingSwitch("Email", local.emailEnabled) { local = local.copy(emailEnabled = it) }; HorizontalDivider(); Text("ALERT ME ABOUT", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
        SettingSwitch("Daily picks", local.alerts.dailyPicks) { local = local.copy(alerts = local.alerts.copy(dailyPicks = it)) }; SettingSwitch("Live prediction changes", local.alerts.liveChanges) { local = local.copy(alerts = local.alerts.copy(liveChanges = it)) }; SettingSwitch("Confirmed lineup changes", local.alerts.lineupChanges) { local = local.copy(alerts = local.alerts.copy(lineupChanges = it)) }; SettingSwitch("Results", local.alerts.results) { local = local.copy(alerts = local.alerts.copy(results = it)) }; SettingSwitch("Subscription status", local.alerts.subscription) { local = local.copy(alerts = local.alerts.copy(subscription = it)) }
        Button(onClick = { vm.saveNotifications(local); onClose() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save preferences") }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResponsibleUseSheet(onClose: () -> Unit) { ModalBottomSheet(onDismissRequest = onClose) { Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.HealthAndSafety, null, tint = PrediqBlue); Spacer(Modifier.width(10.dp)); Text("Responsible Use", style = MaterialTheme.typography.headlineMedium) }; Text("PredIQ estimates probabilities. It does not guarantee results and it will sometimes be wrong.", style = MaterialTheme.typography.bodyLarge); Text("• Never stake money you cannot afford to lose.\n• Treat confidence as uncertainty-aware evidence, not certainty.\n• Losses remain visible in Results.\n• Follow local age and gambling laws.\n• Stop when betting stops being recreational.", color = PrediqMuted); Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Understood") } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    var sport by rememberSaveable { mutableStateOf(state.selectedSport) }; var country by rememberSaveable { mutableStateOf(state.selectedCountry) }; var competition by rememberSaveable { mutableStateOf(state.selectedCompetition) }; var confidence by rememberSaveable { mutableStateOf(state.selectedConfidence) }; var market by rememberSaveable { mutableStateOf(state.selectedMarket) }
    ModalBottomSheet(onDismissRequest = onClose) { Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Filters", style = MaterialTheme.typography.headlineMedium); Text("Sport", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); SportChips((listOf("football", "basketball", "cricket", "baseball", "rugby", "tennis") + state.filterOptions.sports).distinct(), sport) { sport = it; competition = "" }
        Text("Country", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); ChoiceRow(listOf("") + state.filterOptions.countries, country, { if (it.isBlank()) "All" else it }) { country = it }
        Text("League / Competition", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); ChoiceRow(listOf("") + state.filterOptions.competitions.take(40), competition, { if (it.isBlank()) "All" else it }) { competition = it }
        Text("Confidence", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); ChoiceRow(listOf("", "top", "high", "moderate", "try"), confidence, { if (it.isBlank()) "All" else it.replaceFirstChar(Char::uppercase) }) { confidence = it }
        Text("Market", style = MaterialTheme.typography.labelLarge, color = PrediqMuted); ChoiceRow(listOf("") + state.filterOptions.markets.take(20), market, { if (it.isBlank()) "All" else marketName(it) }) { market = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { sport = ""; country = ""; competition = ""; confidence = ""; market = ""; vm.clearAdvancedFilters(); onClose() }, modifier = Modifier.weight(1f)) { Text("Clear") }; Button(onClick = { vm.applyFilters(sport, country, competition, confidence, market); onClose() }, modifier = Modifier.weight(1f)) { Text("Apply") } }
    } }
}

@Composable
private fun ChoiceRow(values: List<String>, selected: String, label: (String) -> String, onSelect: (String) -> Unit) { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(values.distinct(), key = { it }) { value -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value), maxLines = 1) }, shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.heightIn(min = 44.dp)) } } }
@Composable private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onChange) } }
