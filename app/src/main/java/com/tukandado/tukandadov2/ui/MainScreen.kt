package com.tukandado.tukandadov2.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tukandado.tukandadov2.navigation.NavGraph
import com.tukandado.tukandadov2.ui.components.BottomBar
import com.tukandado.tukandadov2.ui.components.layout.HeaderBar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(initialRoute: String? = null) {
    val navController = rememberNavController()

    // Navega una sola vez a la ruta inicial solicitada tras login
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            navController.navigate(initialRoute) {
                popUpTo(0) { inclusive = true } // limpia backstack previo
                launchSingleTop = true
            }
        }
    }

    // 🔝 estado del botón del header
    var headerLabel by remember { mutableStateOf<String?>(null) }
    var headerEnabled by remember { mutableStateOf(true) }
    var headerAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        topBar = {
            HeaderBar(
                navController = navController,
                batteryPercent = 80,
                usePrimaryColor = true,
                actionLabelOverride = headerLabel,
                onAction = headerAction,
                actionEnabled = headerEnabled
            )
        },
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            NavGraph(
                navController = navController,
                setHeaderAction = { label, enabled, onClick ->
                    headerLabel = label
                    headerEnabled = enabled
                    headerAction = onClick
                },
                onLoggedIn = { route ->   // ⬅️ AÑADIDO: manejar login también aquí
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
