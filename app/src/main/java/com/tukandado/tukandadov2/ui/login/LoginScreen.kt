package com.tukandado.tukandadov2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tukandado.tukandadov2.api.LoginRequest
import com.tukandado.tukandadov2.api.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    errorMessage = ""
                    try {
                        val response = RetrofitInstance.api.login(LoginRequest(username, password))

                        println("🟢 Login API status: ${response.code()}")
                        println("🟡 Headers: ${response.headers()}")

                        val user = response.body()
                        val setCookieHeaders = response.headers().values("Set-Cookie")

                        val accessToken = setCookieHeaders
                            .firstOrNull { it.startsWith("accessToken=") }
                            ?.substringAfter("accessToken=")
                            ?.substringBefore(";")

                        val refreshToken = setCookieHeaders
                            .firstOrNull { it.startsWith("refreshToken=") }
                            ?.substringAfter("refreshToken=")
                            ?.substringBefore(";")

                        println("📦 Usuario: $user")
                        println("🔑 AccessToken: $accessToken")
                        println("🔁 RefreshToken: $refreshToken")

                        if (accessToken != null && refreshToken != null && user != null) {
                            println("✅ Login exitoso, navegando a home")
                            onLoginSuccess()
                        } else {
                            errorMessage = "Faltan datos en la respuesta del servidor"
                            println("❌ Datos incompletos: user=$user, accessToken=$accessToken, refreshToken=$refreshToken")
                        }

                    } catch (e: Exception) {
                        errorMessage = "Login fallido: ${e.message ?: "desconocido"}"
                        println("🔥 Error de red: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}