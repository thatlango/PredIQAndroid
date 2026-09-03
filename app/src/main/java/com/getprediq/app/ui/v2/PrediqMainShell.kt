package com.getprediq.app.ui.v2

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.PrediqViewModel
import com.getprediq.app.ui.v2.components.PrediqV2Scaffold
import com.getprediq.app.ui.v2.components.V2NavigationItem
import com.getprediq.app.ui.v2.theme.PrediqV2Theme

@Composable
fun PrediqMainShell(authCallback: Uri? = null, onAuthCallbackConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val authVm: PrediqViewModel = viewModel(factory = PrediqViewModel.factory(context.applicationContext))
    val contractVm: PrediqContractViewModel = viewModel(factory = PrediqContractViewModel.factory(context.applicationContext))
    
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
                    composable("today") { PlaceholderScreen("Today") }
                    composable("live") { PlaceholderScreen("Live") }
                    composable("build") { PlaceholderScreen("Build") }
                    composable("tickets") { PlaceholderScreen("Tickets") }
                    composable("me") { PlaceholderScreen("Me") }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewPrediqMainShell() {
    PrediqMainShell()
}
