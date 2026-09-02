package com.getprediq.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getprediq.app.data.v2.V2DecisionCard
import com.getprediq.app.data.v2.V2LiveCard
import com.getprediq.app.data.v2.V2SearchResult
import com.getprediq.app.ui.contract.*

private enum class ContractTab(val label: String) {
    Today("Today"), Live("Live"), Results("Results"), Research("Research"), Account("Account")
}

@Composable
fun PrediqContractApp(authCallback: Uri? = null, onAuthCallbackConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val authVm: PrediqViewModel = viewModel(factory = PrediqViewModel.factory(context.applicationContext))
    val contractVm: PrediqContractViewModel = viewModel(factory = PrediqContractViewModel.factory(context.applicationContext))
    val authState by authVm.state
    val contractState = contractVm.state
    val nav = rememberNavController()

    LaunchedEffect(authCallback) {
        authCallback?.let { uri ->
            val code = uri.getQueryParameter("code")
            val callbackState = uri.getQueryParameter("state")
            if (!code.isNullOrBlank() && !callbackState.isNullOrBlank()) authVm.finishTuku(code, callbackState)
            onAuthCallbackConsumed()
        }
    }

    LaunchedEffect(authState.account?.user?.id) {
        if (authState.account != null) {
            contractVm.bootstrap(force = true)
            if (nav.currentDestination?.route in setOf("signin", "signup", "forgot")) {
                nav.navigate("main") { popUpTo("signin") { inclusive = true } }
            }
        } else if (!authState.loadingAccount) {
            contractVm.clearForLogout()
        }
    }

    NavHost(navController = nav, startDestination = if (authState.loadingAccount) "launch" else if (authState.account == null) "signin" else "main") {
        composable("launch") {
            LaunchedEffect(authState.loadingAccount, authState.account) {
                if (!authState.loadingAccount) nav.navigate(if (authState.account == null) "signin" else "main") { popUpTo("launch") { inclusive = true } }
            }
            Box(Modifier.fillMaxSize().background(Ivory), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PredIQ", color = PurpleDeep, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    CircularProgressIndicator(color = Purple)
                }
            }
        }
        composable("signin") {
            SignInContractScreen(
                state = authState,
                onSignIn = { email, password -> authVm.login(email, password) {} },
                onTuku = { authVm.startTuku { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                onCreate = { nav.navigate("signup") },
                onForgot = { nav.navigate("forgot") },
            )
        }
        composable("signup") {
            SignUpContractScreen(
                state = authState,
                onBack = { nav.popBackStack() },
                onCreate = { name, email, password, country, consent ->
                    authVm.register(name, email, password, country, consent, null) {
                        nav.navigate("onboarding") { popUpTo("signin") { inclusive = true } }
                    }
                },
                onTuku = { authVm.startTuku { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
            )
        }
        composable("forgot") {
            ForgotPasswordContractScreen(onBack = { nav.popBackStack() }, onTuku = { authVm.startTuku { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } })
        }
        composable("onboarding") {
            OnboardingContractScreen(onDone = { nav.navigate("main") { popUpTo("onboarding") { inclusive = true } } }, onSearchTeams = { nav.navigate("search") })
        }
        composable("main") {
            MainContractTabs(
                state = contractState,
                vm = contractVm,
                onDecision = { ref -> contractVm.loadPrediction(ref); nav.navigate("prediction/${Uri.encode(ref)}") },
                onLive = { id -> nav.navigate("live-detail/${Uri.encode(id)}") },
                onResult = { id -> contractVm.loadResultReview(id); nav.navigate("result/${Uri.encode(id)}") },
                onTeam = { id -> contractVm.loadTeam(id); nav.navigate("team/${Uri.encode(id)}") },
                onPlayer = { id -> contractVm.loadPlayer(id); nav.navigate("player/${Uri.encode(id)}") },
                onLeague = { id -> contractVm.loadCompetition(id); nav.navigate("league/${Uri.encode(id)}") },
                onSearch = { nav.navigate("search") },
                onFollowing = { contractVm.loadAccount(); nav.navigate("following") },
                onNotifications = { nav.navigate("notifications") },
                onInbox = { nav.navigate("inbox") },
                onUpcoming = { nav.navigate("upcoming") },
                onProfile = { nav.navigate("profile") },
                onPlan = { nav.navigate("plan") },
                onPayments = { nav.navigate("payments") },
                onHelp = { nav.navigate("help") },
                onReferral = { nav.navigate("referral") },
                onCompare = { nav.navigate("compare") },
                onPerformance = { market -> nav.navigate("performance/${Uri.encode(market)}") },
                onLogout = {
                    authVm.logout(); contractVm.clearForLogout()
                    nav.navigate("signin") { popUpTo("main") { inclusive = true } }
                },
            )
        }
        composable("prediction/{ref}", arguments = listOf(navArgument("ref") { type = NavType.StringType })) {
            PredictionDetailContractScreen(
                state = contractState,
                onBack = { nav.popBackStack() },
                onFollow = { card -> toggleFollow(contractVm, contractState, card) },
                onTeam = { id -> contractVm.loadTeam(id); nav.navigate("team/${Uri.encode(id)}") },
                onLeague = { id -> contractVm.loadCompetition(id); nav.navigate("league/${Uri.encode(id)}") },
                onSources = { nav.navigate("sources") },
            )
        }
        composable("live-detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = Uri.decode(entry.arguments?.getString("id").orEmpty())
            val card = findLiveCard(contractState, id)
            LiveMatchDetailScreen(card, onBack = { nav.popBackStack() }, onOpenFull = { ref -> contractVm.loadPrediction(ref); nav.navigate("prediction/${Uri.encode(ref)}") })
        }
        composable("result/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { ResultReviewContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("team/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { TeamDetailContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("player/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { PlayerDetailContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("league/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            CompetitionDetailContractScreen(contractState, onBack = { nav.popBackStack() }, onOpenDecision = { ref -> contractVm.loadPrediction(ref); nav.navigate("prediction/${Uri.encode(ref)}") })
        }
        composable("search") { SearchContractScreen(contractState, onBack = { nav.popBackStack() }, onQuery = contractVm::search, onOpen = { item -> openSearchResult(item, contractVm, nav) }) }
        composable("following") { FollowingContractScreen(contractState, onBack = { nav.popBackStack() }, onUnfollow = contractVm::unfollow, onUpdate = contractVm::updateFollow) }
        composable("notifications") { NotificationPreferencesContractScreen(contractState, onBack = { nav.popBackStack() }, onSave = contractVm::saveNotifications) }
        composable("inbox") { NotificationInboxContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("upcoming") { UpcomingContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("sources") { EvidenceSourceContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("compare") { TeamCompareContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("profile") { ProfileContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("help") { HelpContractScreen(onBack = { nav.popBackStack() }) }
        composable("plan") { PlanContractScreen(authState.plans, contractState, onBack = { nav.popBackStack() }) }
        composable("payments") { PaymentsContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("referral") { ReferralContractScreen(contractState, onBack = { nav.popBackStack() }) }
        composable("performance/{market}", arguments = listOf(navArgument("market") { type = NavType.StringType })) { entry ->
            PerformanceBreakdownContractScreen(contractState, Uri.decode(entry.arguments?.getString("market").orEmpty()), onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainContractTabs(
    state: PrediqContractState,
    vm: PrediqContractViewModel,
    onDecision: (String) -> Unit,
    onLive: (String) -> Unit,
    onResult: (String) -> Unit,
    onTeam: (String) -> Unit,
    onPlayer: (String) -> Unit,
    onLeague: (String) -> Unit,
    onSearch: () -> Unit,
    onFollowing: () -> Unit,
    onNotifications: () -> Unit,
    onInbox: () -> Unit,
    onUpcoming: () -> Unit,
    onProfile: () -> Unit,
    onPlan: () -> Unit,
    onPayments: () -> Unit,
    onHelp: () -> Unit,
    onReferral: () -> Unit,
    onCompare: () -> Unit,
    onPerformance: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(ContractTab.Today) }
    var filters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Ivory,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 1.dp) {
                ContractTab.entries.forEach { item ->
                    val icon = when (item) {
                        ContractTab.Today -> Icons.Outlined.Home
                        ContractTab.Live -> Icons.Outlined.Sensors
                        ContractTab.Results -> Icons.Outlined.FactCheck
                        ContractTab.Research -> Icons.Outlined.Search
                        ContractTab.Account -> Icons.Outlined.Person
                    }
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(icon, item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleDeep, selectedTextColor = PurpleDeep,
                            indicatorColor = Color(0xFFEDE9FE), unselectedIconColor = Muted, unselectedTextColor = Muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                ContractTab.Today -> TodayContractScreen(state, vm::loadToday, onDecision, { toggleFollow(vm, state, it) }, { filters = true }, onUpcoming)
                ContractTab.Live -> LiveContractScreen(state, vm::loadLive, onLive, { filters = true })
                ContractTab.Results -> ResultsContractScreen(state, vm::setResultPeriod, vm::setResultOutcome, onResult, onPerformance)
                ContractTab.Research -> ResearchContractScreen(state, onSearch, onTeam, onPlayer, onLeague)
                ContractTab.Account -> AccountContractScreen(state, onProfile, onFollowing, onNotifications, onPlan, onPayments, onHelp, onLogout)
            }
            if (tab == ContractTab.Today || tab == ContractTab.Live) {
                FilledTonalIconButton(
                    onClick = onInbox,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 15.dp, end = 66.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White),
                ) { Icon(Icons.Outlined.Notifications, "Notifications", tint = Ink) }
            }
            if (tab == ContractTab.Research) {
                ExtendedFloatingActionButton(onClick = onCompare, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp), containerColor = Purple, contentColor = Color.White) {
                    Icon(Icons.Outlined.CompareArrows, null); Spacer(Modifier.width(7.dp)); Text("Compare")
                }
            }
            if (tab == ContractTab.Account) {
                SmallFloatingActionButton(onClick = onReferral, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp), containerColor = Color(0xFFE9F7D2), contentColor = GreenDeep) {
                    Icon(Icons.Outlined.GroupAdd, "Refer & earn")
                }
            }
        }
    }
    if (filters) FiltersContractSheet(state, onClose = { filters = false }, onSport = vm::setSport, onCompetition = vm::setCompetition, onFollowing = vm::setFollowingOnly)
}

private fun toggleFollow(vm: PrediqContractViewModel, state: PrediqContractState, card: V2DecisionCard) {
    val type = if (!card.predictionId.isNullOrBlank()) "prediction" else "event"
    val key = card.predictionId ?: card.event.id
    if (card.followState.following) {
        val follow = state.follows?.follows?.firstOrNull { it.entityType == type && it.entityKey == key }
            ?: state.follows?.follows?.firstOrNull { it.entityType == "event" && it.entityKey == card.event.id }
        if (follow != null) vm.unfollow(follow.id)
    } else vm.follow(type, key, card.pick.label ?: "${card.event.participants.home.name} vs ${card.event.participants.away.name}")
}

private fun findLiveCard(state: PrediqContractState, id: String): V2LiveCard? = sequenceOf(
    state.live?.following.orEmpty(), state.live?.opportunities.orEmpty(), state.live?.games.orEmpty(),
).flatten().firstOrNull { it.id == id || it.event.id == id || it.predictionId == id }

private fun openSearchResult(item: V2SearchResult, vm: PrediqContractViewModel, nav: androidx.navigation.NavHostController) {
    when (item.type) {
        "team" -> { vm.loadTeam(item.id); nav.navigate("team/${Uri.encode(item.id)}") }
        "player" -> { vm.loadPlayer(item.id); nav.navigate("player/${Uri.encode(item.id)}") }
        "competition" -> { vm.loadCompetition(item.id); nav.navigate("league/${Uri.encode(item.id)}") }
    }
}

@Composable
private fun SignInContractScreen(
    state: PrediqUiState,
    onSignIn: (String, String) -> Unit,
    onTuku: () -> Unit,
    onCreate: () -> Unit,
    onForgot: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(Modifier.height(28.dp))
            Text("PredIQ", color = PurpleDeep, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Sports intelligence for smarter decisions.", color = Muted)
        }
        item {
            BrightCard {
                Text("Why PredIQ?", color = Lime, fontWeight = FontWeight.Bold)
                InfoDark(Icons.Outlined.AutoAwesome, "Top picks", "Only decisions that clear PredIQ's standard")
                InfoDark(Icons.Outlined.Sensors, "Live changes", "Know when the game changes the view")
                InfoDark(Icons.Outlined.VerifiedUser, "Audited results", "Published calls remain visible")
            }
        }
        item {
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Outlined.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onForgot) { Text("Forgot password?") } }
            state.authError?.let { Text(it, color = Red) }
            Button(onClick = { onSignIn(email, password) }, enabled = !state.authBusy && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                if (state.authBusy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Sign in")
            }
        }
        item {
            OutlinedButton(onClick = onTuku, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AccountCircle, null); Spacer(Modifier.width(8.dp)); Text("Continue with Tuku") }
            TextButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Create an account") }
        }
    }
}

@Composable
private fun SignUpContractScreen(
    state: PrediqUiState,
    onBack: () -> Unit,
    onCreate: (String, String, String, String, Boolean) -> Unit,
    onTuku: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("UG") }
    var consent by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Ivory), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PrediqHeader(title = "Create your account", subtitle = "Join PredIQ and get smarter picks", showBack = true, onBack = onBack) }
        item { OutlinedTextField(name, { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(country, { country = it.uppercase().take(2) }, label = { Text("Country code") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { Row(verticalAlignment = Alignment.Top) { Checkbox(consent, onCheckedChange = { consent = it }); Text("I agree to the Terms, Privacy Policy and responsible-use notice.", color = Muted, modifier = Modifier.padding(top = 12.dp)) } }
        item {
            state.authError?.let { Text(it, color = Red) }
            Button(onClick = { onCreate(name, email, password, country, consent) }, enabled = !state.authBusy && name.isNotBlank() && email.isNotBlank() && password.length >= 8 && country.length == 2 && consent, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Create account") }
        }
        item { OutlinedButton(onClick = onTuku, modifier = Modifier.fillMaxWidth()) { Text("Continue with Tuku") } }
    }
}

@Composable
private fun ForgotPasswordContractScreen(onBack: () -> Unit, onTuku: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ivory).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrediqHeader(title = "Forgot password?", showBack = true, onBack = onBack)
        WhiteCard {
            Icon(Icons.Outlined.LockReset, null, tint = Purple, modifier = Modifier.size(44.dp))
            Text("Recover your Tuku account", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("PredIQ uses your shared Tuku identity. Continue to Tuku to use the available account recovery options.", color = Muted)
            Button(onClick = onTuku, modifier = Modifier.fillMaxWidth()) { Text("Continue to Tuku") }
        }
    }
}

@Composable
private fun InfoDark(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Lime) }
        Spacer(Modifier.width(12.dp))
        Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(body, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall) }
    }
}
