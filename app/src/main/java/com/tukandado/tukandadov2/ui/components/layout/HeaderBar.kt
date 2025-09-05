package com.tukandado.tukandadov2.ui.components.layout

import android.util.Log
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
import com.tukandado.tukandadov2.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    navController: NavHostController,
    batteryPercent: Int? = null,
    usePrimaryColor: Boolean = false,
    actionLabelOverride: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val route = destination?.route?.trimEnd('/')

    // Top-level tabs (no flecha)
    val isTopLevel = route in listOf(
        Screen.Home.route,
        Screen.Locks.route,
        Screen.Clubs.route,
        Screen.Profile.route,
        "login"
    )

    val canGoBackStack = navController.previousBackStackEntry != null
    // Mostrar flecha si: hay back stack, o NO es una ruta de pestaña principal
    val showBack = canGoBackStack || !isTopLevel

    val title = titleForDestination(destination)
    val computedLabel = actionLabelOverride?.takeIf { it.isNotBlank() }
        ?: actionLabelForDestination(destination).takeIf { it.isNotBlank() }

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
            val isActiveReservation = route?.startsWith("activeReservation/") == true

            if (showBack) {
                IconButton(onClick = {
                    if (isActiveReservation) {
                        // Evita el rebote a activeReservation cuando hay reserva activa
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else if (canGoBackStack) {
                        navController.popBackStack()
                    } else {
                        // Ruta no top-level sin backstack: lleva a Home
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                }
            } else {
                Spacer(Modifier.size(48.dp)) // reserva espacio para alinear el título
            }
        },
        colors = colors,
        actions = {
            val actionsColor =
                if (usePrimaryColor) MaterialTheme.colorScheme.onPrimary
                else LocalContentColor.current

            if (!computedLabel.isNullOrBlank()) {
                if (onAction != null) {
                    TextButton(
                        onClick = onAction,
                        enabled = actionEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = actionsColor,
                            disabledContentColor = actionsColor.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(computedLabel)
                    }
                } else {
                    Text(
                        computedLabel,
                        color = actionsColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    )
}

/** Título por patrón EXACTO de la route (se ignora barra final) */
private fun titleForDestination(dest: NavDestination?): String {
    val r = dest?.route?.trimEnd('/') ?: return ""
    return when (r) {
        "login" -> ""
        Screen.Home.route    -> "Inicio"
        Screen.Locks.route   -> "Candados"
        Screen.Clubs.route   -> "Clubs"
        Screen.Profile.route -> "Perfil"
        "clubs" -> "Centros"
        "club/{clubId}" -> "Centro"
        "configuration" -> "Configuración"
        "registers"     -> "Registros"
        "rfid"          -> "Tarjetas RFID"
        "bookings"          -> "Mis reservas"
        "ekeys/{lockId}" -> "Llaves digitales"
        "activeReservation/{lockId}/{lockName}/{lockAlias}/{lockData}/{lockMac}" -> "Reserva activa"
        "adminLock/{lockName}/{lockAlias}/{lockData}/{lockMac}/{lockId}" -> "Tukandado"
        "passcodes/create/{lockId}/{lockData}/{lockMac}" -> "Crear passcode"
        "passcodes/{passcodeId}/detail/{lockId}/{lockData}/{lockMac}"   -> "Detalle del passcode"
        "passcodes/{lockId}/{lockData}/{lockMac}"                        -> "Contraseñas"
        else -> ""
    }
}

/** Label del botón de acción del AppBar por ruta exacta (sin barra final) */
private fun actionLabelForDestination(dest: NavDestination?): String {
    val r = dest?.route?.trimEnd('/') ?: return ""
    return when (r) {
        "ekeys/{lockId}" -> "Reiniciar"
        "rfid"           -> "Reiniciar"
        "registers"      -> "Reiniciar"
        "passcodes/{lockId}/{lockData}/{lockMac}" -> "Reiniciar"
        "passcodes/create/{lockId}/{lockData}/{lockMac}" -> "Guardar"
        "passcodes/{passcodeId}/detail/{lockId}/{lockData}/{lockMac}" -> ""
        "adminLock/{lockName}/{lockAlias}/{lockData}/{lockMac}/{lockId}" -> ""
        else -> ""
    }
}