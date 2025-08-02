package com.tukandado.tukandadov2.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    navController: NavHostController,
    // si quieres inyectar el % batería desde la pantalla actual:
    batteryPercent: Int? = null,
    usePrimaryColor: Boolean = false // ponlo en true si quieres el azul tipo TTLock
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val canGoBack = navController.previousBackStackEntry != null

    val title = titleForDestination(destination)

    val colors = if (usePrimaryColor)
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    else TopAppBarDefaults.topAppBarColors()

    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            if (canGoBack) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                }
            } else {
                // ocupa el hueco para que el título quede centrado aunque no haya back
                Spacer(Modifier.size(48.dp))
            }
        },
        colors = colors
    )
}

/** Mapea tus rutas a títulos legibles */
private fun titleForDestination(dest: NavDestination?): String {
    val route = dest?.route ?: return ""
    return when {
        route.startsWith("activeReservation") -> "Reserva activa"
        route.startsWith("adminLock")         -> "Tukandado"
        route.startsWith("home")              -> "Inicio"
        route.startsWith("locks")             -> "Candados"
        route.startsWith("clubs")             -> "Clubs"
        route.startsWith("profile")           -> "Perfil"
        route.startsWith("login")             -> ""
        else                                  -> ""
    }
}