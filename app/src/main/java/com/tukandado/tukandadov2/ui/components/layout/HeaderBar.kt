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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    navController: NavHostController,
    batteryPercent: Int? = null,
    usePrimaryColor: Boolean = false,
    actionLabelOverride: String? = null,            // 👈 label opcional
    onAction: (() -> Unit)? = null,                 // 👈 callback opcional
    actionEnabled: Boolean = true                   // 👈 enabled
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val canGoBack = navController.previousBackStackEntry != null

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
            if (canGoBack) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                }
            } else {
                Spacer(Modifier.size(48.dp))
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

/** Mapea tus rutas a títulos legibles */
private fun titleForDestination(dest: NavDestination?): String {
    val route = dest?.route ?: return ""
    return when {
        route.startsWith("activeReservation") -> "Reserva activa"
        route.startsWith("adminLock")         -> "Tukandado"

        // Passcodes (orden importa: create -> detail -> lista)
        route.startsWith("passcodes/create")                -> "Crear passcode"
        route.startsWith("passcodes/") && route.endsWith("/detail") -> "Detalle del passcode"
        route.startsWith("passcodes")                       -> "Contraseñas"

        // Ekeys
        route.startsWith("ekeys") || route.startsWith("ekey")-> "Llaves digitales"

        // Resto de secciones
        route.startsWith("configuration")    -> "Configuración"
        route.startsWith("registers")        -> "Registros"
        route.startsWith("rfid")             -> "Tarjetas RFID"
        route.startsWith("home")             -> "Inicio"
        route.startsWith("locks")            -> "Candados"
        route.startsWith("clubs")            -> "Clubs"
        route.startsWith("profile")          -> "Perfil"
        route.startsWith("passwords")        -> "Contraseñas"
        route.startsWith("login")            -> ""
        else                                 -> ""
    }
}


private fun actionLabelForDestination(dest: NavDestination?): String {
    val route = dest?.route ?: return ""
    return when {
        // Acciones de secciones
        route.startsWith("ekeys") || route.startsWith("ekey") -> "Reiniciar"
        route.startsWith("rfid")                              -> "Reiniciar"
        route.startsWith("registers")                         -> "Reiniciar"

        route.startsWith("passcodes/create/")                  -> "Guardar"     // 👈 importante
        route.startsWith("passcodes/") && route.endsWith("/detail") -> ""
        route.startsWith("passcodes")                  -> ""     // 👈 importante

        else                                                  -> ""
    }
}