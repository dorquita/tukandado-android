package com.tukandado.tukandadov2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.navigation.NavGraph
import com.tukandado.tukandadov2.ui.MainScreen
import com.tukandado.tukandadov2.ui.theme.TukandadoV2Theme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TukandadoV2Theme {
                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    val token = SessionManager(applicationContext).getAccessToken().first()
                    isLoggedIn = token != null
                }

                when (isLoggedIn) {
                    true -> MainScreen() // con BottomBar y navegación de pantallas internas
                    false -> {
                        val navController = rememberNavController()
                        NavGraph(navController = navController, startDestination = "login")
                    }
                    null -> Text("Cargando...")
                }
            }
        }
    }
}