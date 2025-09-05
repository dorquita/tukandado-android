package com.tukandado.tukandadov2.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        listOf(
            Screen.Home,     // Dashboard admin
            Screen.Locks,    // Candados
            Screen.Clubs,    // Clubs (solo admin)
            Screen.Profile   // Perfil
        )
    } else {
        listOf(
            Screen.Home,     // Dashboard cliente
            Screen.Locks,    // Candados / Reservar
            Screen.Bookings, // Mis reservas (cliente)
            Screen.Profile   // Perfil
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 👉 Ruta con params tal y como la declaraste en el NavGraph
    val activeReservationRoute =
        "activeReservation/{lockId}/{lockName}/{lockAlias}/{lockData}/{lockMac}"

    val isOnActiveReservation =
        currentDestination?.hierarchy?.any { it.route == activeReservationRoute } == true

    // Mostrar la bottom bar si estás en cualquiera de las tabs o en activeReservation
    val showBottomBar = items.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    } || isOnActiveReservation

    if (showBottomBar) {
        NavigationBar {
            items.forEach { screen ->
                // Marcar como seleccionada la tab normal o, si estás en activeReservation,
                // marcar “Candados” para mantener contexto.
                val selectedInHierarchy =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val selectedWhenActiveReservation =
                    isOnActiveReservation && screen.route == Screen.Locks.route

                val selected = selectedInHierarchy || selectedWhenActiveReservation

                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                    label = { Text(screen.title) },
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
}