package com.tukandado.tukandadov2.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel()
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        //viewModel.getAvailableLocks(context) // Llama a tu API al iniciar
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Vista del perfil",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    sessionManager.clearSession()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true } // Elimina home del stack
                    }
                }
            }
        ) {
            Text("Cerrar sesión")
        }

        Button(
            onClick = { viewModel.verifyToken(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Comprobar tokens")
        }
    }
}