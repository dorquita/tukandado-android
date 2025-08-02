package com.tukandado.tukandadov2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tukandado.tukandadov2.navigation.NavGraph
import com.tukandado.tukandadov2.ui.components.BottomBar
import com.tukandado.tukandadov2.ui.components.layout.HeaderBar

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        topBar = { HeaderBar(navController, 80, usePrimaryColor = true) },
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController)
        }
    }
}
