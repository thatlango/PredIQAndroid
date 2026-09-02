package com.getprediq.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getprediq.app.data.*
import com.getprediq.app.ui.*
import com.getprediq.app.ui.theme.PrediqBackground
import com.getprediq.app.ui.theme.PrediqBlue
import com.getprediq.app.ui.theme.PrediqMuted
import com.getprediq.app.ui.theme.PrediqSurfaceLow
import kotlinx.coroutines.launch

private enum class MainTabV2(val label: String) { Today("Today"), Live("Live"), Results("Results"), Explore("Explore"), Account("Account") }

@Composable
fun PrediqAppV2(authCallback: Uri? = null, onAuthCallbackConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: PrediqViewModel = viewModel(factory = PrediqViewModel.factory(context.applicationContext))
    val state by vm.state
    val nav = rememberNavController()

    LaunchedEffect(authCallback) {
        authCallback?.let { uri ->
            val code = uri.getQueryParameter("code")
            val callbackState = uri.getQueryParameter("state")
            if (!code.isNullOrBlank() && !callbackState.isNullOrBlank()) vm.finishTuku(code, callbackState)
            onAuthCallbackConsumed()
        }
    }
    LaunchedEffect(state.account?.user?.id) {
        if (state.account != null) PushRegistrationCoordinator.sync(context.applicationContext)
    }

    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainTabsV2(
                state = state,
                vm = vm,
                onMatch = { eventId -> if (vm.fullAccess) nav.navigate("match/$eventId") },
                onLeagueWinners = { if (vm.fullAccess) { vm.loadLeagueForecasts(); nav.navigate("leagues") } },
            )
        }
        composable("match/{eventId}", arguments = listOf(navArgument("eventId") { type = NavType.StringType })) { entry ->
            val eventId = entry.arguments?.getString("eventId").orEmpty()
            LaunchedEffect(eventId) { if (eventId.isNotBlank()) vm.loadMatch(eventId) }
            MatchIntelligenceScreen(state.matchIntelligence, onBack = { nav.popBackStack() })
        }
        composable("leagues") {
            LaunchedEffect(Unit) { vm.loadLeagueForecasts() }
            LeagueWinnersScreen(state.leagueForecasts, onBack = { nav.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabsV2(state: PrediqUiState, vm: PrediqViewModel, onMatch: (String) -> Unit, onLeagueWinners: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTabV2.Today) }
    var authOpen by rememberSaveable { mutableStateOf(false) }
    var paymentPlan by remember { mutableStateOf<PlanDto?>(null) }
    var notificationsOpen by rememberSaveable { mutableStateOf(false) }
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var responsibleOpen by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    fun requestAccess() { if (state.account == null) authOpen = true else tab = MainTabV2.Account }

    LaunchedEffect(vm.fullAccess) { if (!vm.fullAccess) tab = MainTabV2.Account }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                (if (vm.fullAccess) MainTabV2.entries else listOf(MainTabV2.Account)).forEach { item ->
                    val icon = when (item) {
                        MainTabV2.Today -> Icons.Outlined.Today
                        MainTabV2.Live -> Icons.Outlined.Sensors
                        MainTabV2.Results -> Icons.Outlined.Analytics
                        MainTabV2.Explore -> Icons.Outlined.Explore
                        MainTabV2.Account -> Icons.Outlined.Person
                    }
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, null) }, label = { Text(item.label) })
                }
            }
        },
        floatingActionButton = {
            if (vm.fullAccess && (tab == MainTabV2.Today || tab == MainTabV2.Live || tab == MainTabV2.Results)) {
                SmallFloatingActionButton(onClick = { filtersOpen = true }, containerColor = MaterialTheme.colorScheme.surface) {
                    Icon(Icons.Outlined.Tune, "Filters", tint = PrediqBlue)
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                MainTabV2.Today -> TodayScreen(state, vm, ::requestAccess, onMatch, onLeagueWinners)
                MainTabV2.Live -> LiveScreen(state, vm, ::requestAccess, onMatch)
                MainTabV2.Results -> ResultsScreen(state, vm)
                MainTabV2.Explore -> ExploreScreen(state, vm)
                MainTabV2.Account -> AccountHubScreen(
                    state = state,
                    vm = vm,
                    onAuth = { authOpen = true },
                    onPlan = { paymentPlan = it },
                    onProfile = { profileOpen = true },
                    onHistory = { historyOpen = true },
                    onNotifications = { notificationsOpen = true },
                    onResponsible = { responsibleOpen = true },
                )
            }
        }
    }

    if (authOpen) AuthSheetV2(state, vm) { authOpen = false }
    paymentPlan?.let { plan -> PaymentSheetV2(plan, state, vm) { paymentPlan = null } }
    if (profileOpen) ProfileSheetV2(state, vm) { profileOpen = false }
    if (historyOpen) PaymentHistorySheetV2 { historyOpen = false }
    if (notificationsOpen) NotificationSheetV2(state, vm) { notificationsOpen = false }
    if (responsibleOpen) ResponsibleUseSheetV2 { responsibleOpen = false }
    if (filtersOpen) FilterSheetV2(state, vm) { filtersOpen = false }
}

@Composable
private fun AccountHubScreen(
    state: PrediqUiState,
    vm: PrediqViewModel,
    onAuth: () -> Unit,
    onPlan: (PlanDto) -> Unit,
    onProfile: () -> Unit,
    onHistory: () -> Unit,
    onNotifications: () -> Unit,
    onResponsible: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrediqBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Your PredIQ", style = MaterialTheme.typography.headlineMedium)
                Text("Subscription, payments, profile and intelligence alerts in one place.", color = PrediqMuted)
            }
        }
        val account = state.account
        if (account == null) {
            item {
                V2Card {
                    Icon(Icons.Outlined.LockOpen, null, tint = PrediqBlue, modifier = Modifier.size(48.dp))
                    Text("Seven days of full access", style = MaterialTheme.typography.titleLarge)
                    Text("Create one Tuku account and open every sport, live view, result and intelligence screen.", color = PrediqMuted)
                    Button(onClick = onAuth, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start 7-day trial") }
                    Text("Plans start at UGX 15,000. Paid time begins after any remaining trial access.", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            item {
                V2Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(58.dp).background(PrediqSurfaceLow, CircleShape), contentAlignment = Alignment.Center) {
                            Text(initialsV2(account.user.displayName ?: account.user.email), fontWeight = FontWeight.Bold, color = PrediqBlue)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account.user.displayName ?: "PredIQ member", style = MaterialTheme.typography.titleLarge)
                            Text(account.user.email, color = PrediqMuted)
                            Text(account.subscription?.name ?: account.subscriptionState.replace('_', ' ').replaceFirstChar(Char::uppercase), color = PrediqBlue, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = onProfile) { Icon(Icons.Outlined.Edit, "Edit profile") }
                    }
                    account.subscriptionProgress?.let { progress ->
                        Box(Modifier.fillMaxWidth().height(5.dp).background(PrediqSurfaceLow, CircleShape)) {
                            Box(Modifier.fillMaxWidth(progress.fractionRemaining.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(PrediqBlue, CircleShape))
                        }
                        Text("${progress.daysRemaining} days remaining", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (!account.access.subscriptionBypass) {
                item { Text(if (account.access.fullSelections) "Extend Subscription" else "Choose a Plan", style = MaterialTheme.typography.titleLarge) }
                items(state.plans, key = { it.code }) { plan -> PlanChoiceCardV2(plan) { onPlan(plan) } }
                if (!state.paymentCapabilities.mobileMoney) item { StateCard("Mobile money is not active yet", state.paymentCapabilities.message) }
            }

            state.affiliate?.let { affiliate ->
                item {
                    V2Card {
                        Text("Recommend PredIQ", style = MaterialTheme.typography.titleLarge)
                        Text("Earn ${(affiliate.commissionRate * 100).toInt()}% for ${affiliate.commissionMonths} months on settled referred payments.", color = PrediqMuted)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniMetric("Referred", affiliate.referrals.toString(), Modifier.weight(1f))
                            MiniMetric("Available", ugxV2(affiliate.availableUgx), Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Try PredIQ with 7 days of full access: ${affiliate.shareUrl}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PredIQ"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(8.dp)); Text("Share referral link") }
                    }
                }
            }

            item {
                V2Card {
                    AccountRowV2(Icons.Outlined.Person, "Profile & regional settings", onProfile)
                    HorizontalDivider(color = Color(0xFFEEEEF0))
                    AccountRowV2(Icons.Outlined.ReceiptLong, "Payment history", onHistory)
                    HorizontalDivider(color = Color(0xFFEEEEF0))
                    AccountRowV2(Icons.Outlined.Notifications, "Notification & league alerts", onNotifications)
                    HorizontalDivider(color = Color(0xFFEEEEF0))
                    AccountRowV2(Icons.Outlined.HealthAndSafety, "Responsible Use", onResponsible)
                    HorizontalDivider(color = Color(0xFFEEEEF0))
                    AccountRowV2(Icons.Outlined.Logout, "Log Out", {
                        scope.launch {
                            PushRegistrationCoordinator.deactivate(context.applicationContext)
                            vm.logout()
                        }
                    }, danger = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSheetV2(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var referral by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (register) "Create PredIQ account" else "Sign in to PredIQ", style = MaterialTheme.typography.headlineMedium)
            Text("One secure Tuku account across web and Android. New accounts receive seven days of full access.", color = PrediqMuted)
            Button(onClick = { vm.startTuku(referral.takeIf(String::isNotBlank)) { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }, enabled = !state.authBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continue with Tuku") }
            HorizontalDivider()
            if (register) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(country, { country = it.uppercase().take(2) }, label = { Text("Country code") }, placeholder = { Text("UG") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(referral, { referral = it.uppercase().take(32) }, label = { Text("Referral code (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (register) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Spacer(Modifier.width(8.dp)); Text("I agree to PredIQ’s terms and responsible-use notice.", color = PrediqMuted, modifier = Modifier.padding(top = 12.dp))
            }
            state.authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { if (register) vm.register(name, email, password, country, consent, referral.takeIf(String::isNotBlank), onClose) else vm.login(email, password, onClose) },
                enabled = !state.authBusy && email.isNotBlank() && password.length >= 8 && (!register || (name.isNotBlank() && country.length == 2 && consent)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { if (state.authBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (register) "Create account" else "Sign in") }
            TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Already have an account? Sign in" else "Create a PredIQ account") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSheetV2(plan: PlanDto, state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    var phone by rememberSaveable { mutableStateOf(state.account?.user?.phone.orEmpty()) }
    LaunchedEffect(plan.code) { vm.clearPaymentMessage() }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${plan.name} access", style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total to pay", color = PrediqMuted); Text(ugxV2(plan.priceUgx), style = MaterialTheme.typography.titleLarge, color = PrediqBlue) }
            OutlinedTextField(phone, { phone = it }, label = { Text("Pay with Mobile Money") }, leadingIcon = { Icon(Icons.Outlined.PhoneAndroid, null) }, placeholder = { Text("0772 123 456") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("Access activates only after the payment provider confirms settlement.", color = PrediqMuted)
            state.paymentMessage?.let { Text(it, color = PrediqBlue) }
            Button(onClick = { vm.checkout(plan.code, phone) }, enabled = !state.paymentBusy && state.paymentCapabilities.mobileMoney && phone.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (state.paymentBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Request payment")
            }
            if (!state.paymentCapabilities.mobileMoney) Text(state.paymentCapabilities.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheetV2(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AccountFeatureRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val user = state.account?.user
    var name by remember(user?.id) { mutableStateOf(user?.displayName.orEmpty()) }
    var country by remember(user?.id) { mutableStateOf(user?.countryCode ?: "UG") }
    var currency by remember(user?.id) { mutableStateOf(user?.currency ?: "UGX") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Profile & regional settings", style = MaterialTheme.typography.headlineMedium)
            Text("These settings change display and regional preferences only. Your Tuku sign-in remains unchanged.", color = PrediqMuted)
            OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(country, { country = it.uppercase().filter(Char::isLetter).take(2) }, label = { Text("Country code") }, supportingText = { Text("Two-letter code, for example UG") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(currency, { currency = it.uppercase().filter(Char::isLetter).take(3) }, label = { Text("Currency") }, supportingText = { Text("Three-letter code, for example UGX") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    scope.launch {
                        if (country.length != 2 || currency.length != 3) { error = "Use a 2-letter country code and 3-letter currency code."; return@launch }
                        busy = true; error = null
                        runCatching { repo.updateProfile(name, country, currency) }
                            .onSuccess { vm.refreshAccount(); busy = false; onClose() }
                            .onFailure { busy = false; error = it.message ?: "Profile could not be saved" }
                    }
                },
                enabled = !busy && country.length == 2 && currency.length == 3,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save profile") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentHistorySheetV2(onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AccountFeatureRepository(context.applicationContext) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var payments by remember { mutableStateOf<List<PaymentHistoryItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching { repo.paymentHistory() }
            .onSuccess { payments = it.payments; loading = false }
            .onFailure { error = it.message ?: "Payment history could not load"; loading = false }
    }
    ModalBottomSheet(onDismissRequest = onClose) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Payment history", style = MaterialTheme.typography.headlineMedium) }
            item { Text("Only confirmed settlement activates or extends PredIQ access.", color = PrediqMuted) }
            if (loading) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Loading transactions…", color = PrediqMuted) } }
            error?.let { message -> item { StateCard("Payment history unavailable", message, error = true) } }
            if (!loading && error == null && payments.isEmpty()) item { StateCard("No payments yet", "Your completed and pending PredIQ transactions will appear here.") }
            items(payments, key = { it.id }) { payment -> PaymentHistoryCardV2(payment) }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSheetV2(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AccountFeatureRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val pushConfigured = remember { context.resources.getIdentifier("google_app_id", "string", context.packageName) != 0 }
    var local by remember(state.notifications) { mutableStateOf(state.notifications ?: NotificationSettings()) }
    var availableLeagues by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedLeagues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loadingLeagues by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) local = local.copy(pushEnabled = false)
    }
    LaunchedEffect(Unit) {
        if (!pushConfigured) local = local.copy(pushEnabled = false)
        vm.loadNotifications()
        runCatching { repo.leagueAlerts() }
            .onSuccess { availableLeagues = it.available; selectedLeagues = it.leagues.toSet(); loadingLeagues = false }
            .onFailure { error = it.message ?: "League alert preferences could not load"; loadingLeagues = false }
    }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Notification Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Choose the intelligence changes worth interrupting you for, including specific football competitions.", color = PrediqMuted)
            SettingSwitchV2("Push notifications", local.pushEnabled && pushConfigured) { enabled ->
                if (pushConfigured) {
                    local = local.copy(pushEnabled = enabled)
                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    local = local.copy(pushEnabled = false)
                }
            }
            if (!pushConfigured) Text("Push alerts are unavailable in this build until PredIQ is linked to a Firebase app. Email and WhatsApp preferences still work.", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
            SettingSwitchV2("WhatsApp", local.whatsappEnabled) { local = local.copy(whatsappEnabled = it) }
            SettingSwitchV2("Email", local.emailEnabled) { local = local.copy(emailEnabled = it) }
            HorizontalDivider()
            Text("ALERT ME ABOUT", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            SettingSwitchV2("Daily picks", local.alerts.dailyPicks) { local = local.copy(alerts = local.alerts.copy(dailyPicks = it)) }
            SettingSwitchV2("Live prediction changes", local.alerts.liveChanges) { local = local.copy(alerts = local.alerts.copy(liveChanges = it)) }
            SettingSwitchV2("Confirmed lineup changes", local.alerts.lineupChanges) { local = local.copy(alerts = local.alerts.copy(lineupChanges = it)) }
            SettingSwitchV2("Results", local.alerts.results) { local = local.copy(alerts = local.alerts.copy(results = it)) }
            SettingSwitchV2("Subscription status", local.alerts.subscription) { local = local.copy(alerts = local.alerts.copy(subscription = it)) }
            HorizontalDivider()
            Text("FOLLOW LEAGUES", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            Text("Select competitions whose title-race and intelligence changes you want PredIQ to track for your alert profile.", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
            if (loadingLeagues) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (availableLeagues.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableLeagues, key = { it }) { league ->
                        FilterChip(
                            selected = selectedLeagues.contains(league),
                            onClick = { selectedLeagues = if (selectedLeagues.contains(league)) selectedLeagues - league else selectedLeagues + league },
                            label = { Text(league, maxLines = 1) },
                            shape = CircleShape,
                        )
                    }
                }
                Text(if (selectedLeagues.isEmpty()) "No league-specific alerts selected" else "Following ${selectedLeagues.size} league${if (selectedLeagues.size == 1) "" else "s"}", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    scope.launch {
                        busy = true; error = null
                        runCatching {
                            repo.updateLeagueAlerts(selectedLeagues.sorted())
                            vm.saveNotifications(local)
                            if (local.pushEnabled) PushRegistrationCoordinator.sync(context.applicationContext)
                        }.onSuccess { busy = false; onClose() }
                            .onFailure { busy = false; error = it.message ?: "Notification preferences could not be saved" }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save preferences") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResponsibleUseSheetV2(onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.HealthAndSafety, null, tint = PrediqBlue); Spacer(Modifier.width(10.dp)); Text("Responsible Use", style = MaterialTheme.typography.headlineMedium) }
            Text("PredIQ estimates probabilities. It does not guarantee results and it will sometimes be wrong.", style = MaterialTheme.typography.bodyLarge)
            Text("• Never stake money you cannot afford to lose.\n• Treat confidence as uncertainty-aware evidence, not certainty.\n• Losses remain visible in Results.\n• Follow local age and gambling laws.\n• Stop when betting stops being recreational.", color = PrediqMuted)
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Understood") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetV2(state: PrediqUiState, vm: PrediqViewModel, onClose: () -> Unit) {
    var sport by rememberSaveable { mutableStateOf(state.selectedSport) }
    var country by rememberSaveable { mutableStateOf(state.selectedCountry) }
    var competition by rememberSaveable { mutableStateOf(state.selectedCompetition) }
    var confidence by rememberSaveable { mutableStateOf(state.selectedConfidence) }
    var market by rememberSaveable { mutableStateOf(state.selectedMarket) }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Filters", style = MaterialTheme.typography.headlineMedium)
            Text("Sport", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            ChoiceRowV2((listOf("") + state.filterOptions.sports).distinct(), sport, { if (it.isBlank()) "All" else it.replaceFirstChar(Char::uppercase) }) { sport = it; competition = "" }
            Text("Country", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            ChoiceRowV2(listOf("") + state.filterOptions.countries, country, { if (it.isBlank()) "All" else it }) { country = it }
            Text("League / Competition", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            ChoiceRowV2(listOf("") + state.filterOptions.competitions.take(40), competition, { if (it.isBlank()) "All" else it }) { competition = it }
            Text("Confidence", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            ChoiceRowV2(listOf("", "top", "high", "moderate", "try"), confidence, { if (it.isBlank()) "All" else it.replaceFirstChar(Char::uppercase) }) { confidence = it }
            Text("Market", style = MaterialTheme.typography.labelLarge, color = PrediqMuted)
            ChoiceRowV2(listOf("") + state.filterOptions.markets.take(20), market, { if (it.isBlank()) "All" else it.replace('_', ' ').replaceFirstChar(Char::uppercase) }) { market = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.clearAdvancedFilters(); onClose() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(onClick = { vm.applyFilters(sport, country, competition, confidence, market); onClose() }, modifier = Modifier.weight(1f)) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun ChoiceRowV2(values: List<String>, selected: String, label: (String) -> String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values.distinct(), key = { it }) { value -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value), maxLines = 1) }, shape = CircleShape) }
    }
}

@Composable
private fun V2Card(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun AccountRowV2(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, danger: Boolean = false) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else PrediqBlue)
            Spacer(Modifier.width(12.dp)); Text(label, Modifier.weight(1f), color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Outlined.ChevronRight, null, tint = PrediqMuted)
        }
    }
}

@Composable
private fun PlanChoiceCardV2(plan: PlanDto, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(plan.name, fontWeight = FontWeight.SemiBold); Text("${plan.durationDays} days", color = PrediqMuted) }
            Text(ugxV2(plan.priceUgx), color = PrediqBlue, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.ChevronRight, null, tint = PrediqMuted)
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = PrediqSurfaceLow, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) { Text(label.uppercase(), color = PrediqMuted, style = MaterialTheme.typography.labelSmall); Text(value, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PaymentHistoryCardV2(payment: PaymentHistoryItem) {
    val statusColor = when (payment.status.lowercase()) {
        "successful" -> Color(0xFF1B7F4B)
        "failed" -> MaterialTheme.colorScheme.error
        else -> Color(0xFF9A6700)
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(payment.planName.ifBlank { payment.planCode }, fontWeight = FontWeight.SemiBold); Text(payment.createdAt?.take(10) ?: "", color = PrediqMuted, style = MaterialTheme.typography.bodySmall) }
                Text(ugxV2(payment.amountUgx), fontWeight = FontWeight.Bold)
            }
            Text(payment.status.replace('_', ' ').replaceFirstChar(Char::uppercase), color = statusColor, style = MaterialTheme.typography.labelMedium)
            if (!payment.failureReason.isNullOrBlank()) Text("Reason: ${payment.failureReason}", color = PrediqMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingSwitchV2(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun initialsV2(value: String): String = value.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "P" }
private fun ugxV2(value: Int): String = "UGX %,d".format(value)
