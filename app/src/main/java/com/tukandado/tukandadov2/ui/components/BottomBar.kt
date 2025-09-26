package com.tukandado.tukandadov2.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tukandado.tukandadov2.navigation.Screen
import com.tukandado.tukandadov2.data.SessionManager

@Composable
fun BottomBar(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val role by sessionManager.getRole().collectAsState(initial = "client")
    val isAdmin = role == "admin" || role == "superadmin"

    val items = if (isAdmin) {
        listOf(Screen.Home, Screen.Locks, Screen.Clubs, Screen.Profile)
    } else {
        listOf(Screen.Home, Screen.Locks, Screen.Bookings, Screen.Profile)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val activeReservationRoute =
        "activeReservation/{lockId}/{lockName}/{lockAlias}/{lockData}/{lockMac}"
    val isOnActiveReservation =
        currentDestination?.hierarchy?.any { it.route == activeReservationRoute } == true

    val showBottomBar = items.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    } || isOnActiveReservation

    if (!showBottomBar) return

    // ⬇️ Oculta labels si el usuario tiene el texto muy grande
    val fontScale = LocalConfiguration.current.fontScale
    val showLabels = fontScale < 1.3f   // umbral; ajusta a 1.2–1.3 según te guste

    NavigationBar {
        items.forEach { screen ->
            val selectedInHierarchy =
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val selectedWhenActiveReservation =
                isOnActiveReservation && screen.route == Screen.Locks.route
            val selected = selectedInHierarchy || selectedWhenActiveReservation

            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                // ⬇️ Solo mostramos label si cabe; si no, solo icono
                label = if (showLabels) {
                    { Text(text = screen.title, maxLines = 1, softWrap = false) }
                } else null,
                alwaysShowLabel = showLabels,
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}