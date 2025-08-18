package com.tukandado.tukandadov2.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun MainScreen() {
    val navController = rememberNavController()

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
                }
            )
        }
    }
}
