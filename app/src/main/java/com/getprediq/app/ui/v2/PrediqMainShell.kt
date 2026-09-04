package com.getprediq.app.ui.v2

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.PrediqViewModel
import com.getprediq.app.data.MediaCatalogStore
import com.getprediq.app.ui.v2.components.PrediqV2Scaffold
import com.getprediq.app.ui.v2.components.V2NavigationItem
import com.getprediq.app.ui.v2.theme.PrediqV2Theme

@Composable
fun PrediqMainShell(authCallback: Uri? = null, onAuthCallbackConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val authVm: PrediqViewModel = viewModel(factory = PrediqViewModel.factory(context.applicationContext))
    val contractVm: PrediqContractViewModel = viewModel(factory = PrediqContractViewModel.factory(context.applicationContext))
    val authState by authVm.state

    val nav = rememberNavController()
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        V2NavigationItem("today", "Today", Icons.Outlined.Home),
        V2NavigationItem("live", "Live", Icons.Outlined.Sensors),
        V2NavigationItem("build", "Build", Icons.Outlined.AutoGraph),
        V2NavigationItem("tickets", "Tickets", Icons.Outlined.Bookmarks),
        V2NavigationItem("me", "Me", Icons.Outlined.Person)
    )

    LaunchedEffect(Unit) {
        MediaCatalogStore.ensureLoaded()
        // V2 Today/Live/Builder expose public intelligence. Load them even before sign-in.
        contractVm.bootstrap()
    }

    LaunchedEffect(authCallback) {
        authCallback?.let { uri ->
            val code = uri.getQueryParameter("code")
            val callbackState = uri.getQueryParameter("state")
            if (!code.isNullOrBlank() && !callbackState.isNullOrBlank()) {
                authVm.finishTuku(code, callbackState)
            }
            onAuthCallbackConsumed()
        }
    }

    LaunchedEffect(authState.account?.user?.id) {
        if (authState.account != null) {
            // Refresh protected V2 account/ticket data after Tuku auth completes.
            contractVm.bootstrap(force = true)
        }
    }

    PrediqV2Theme {
        PrediqV2Scaffold(
            items = navItems,
            currentRoute = currentRoute,
            onNavigate = { route ->
                nav.navigate(route) {
                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(navController = nav, startDestination = "today") {
                    composable("today") {
                        TodayV2Screen(contractVm, onDecision = { ref ->
                            contractVm.loadPrediction(ref)
                            nav.navigate("prediction/${Uri.encode(ref)}")
                        })
                    }
                    composable("live") { LiveV2Screen(contractVm) }
                    composable("build") { BuildV2Screen(contractVm) }
                    composable("tickets") { TicketsV2Screen(contractVm) }
                    composable("me") { AccountV2Screen(contractVm, authVm) }

                    composable(
                        "prediction/{ref}",
                        arguments = listOf(androidx.navigation.navArgument("ref") { type = androidx.navigation.NavType.StringType })
                    ) {
                        PredictionDetailV2Screen(contractVm, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text(
        text = "$name Screen (UI V2)",
        style = com.getprediq.app.ui.v2.theme.V2Typography.headlineMedium,
        modifier = Modifier.padding(16.dp)
    )
}
