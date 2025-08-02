package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.api.LoginRequest
import com.tukandado.tukandadov2.api.LoginResponse
import com.tukandado.tukandadov2.api.RetrofitInstance
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _userData = MutableStateFlow<LoginResponse?>(null)
    val userData: StateFlow<LoginResponse?> = _userData

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d("LoginViewModel", "Iniciando login con email: $email")

            try {
                val api = RetrofitInstance.getAuthApi(context)
                val response = api.login(LoginRequest(email, password))

                Log.d("LoginViewModel", "Respuesta asecas: $response")
                Log.d("LoginViewModel", "Respuesta HTTP: ${response.code()}")

                val user = response.body()
                Log.d("LoginViewModel", "Usuario recibido: $user")

                val setCookieHeaders = response.headers().values("Set-Cookie")
                Log.d("LoginViewModel", "Headers Set-Cookie: $setCookieHeaders")

                val accessToken = setCookieHeaders
                    .firstOrNull { it.startsWith("accessToken=") }
                    ?.substringAfter("accessToken=")
                    ?.substringBefore(";")

                val refreshToken = setCookieHeaders
                    .firstOrNull { it.startsWith("refreshToken=") }
                    ?.substringAfter("refreshToken=")
                    ?.substringBefore(";")

                Log.d("LoginViewModel", "AccessToken: $accessToken")
                Log.d("LoginViewModel", "RefreshToken: $refreshToken")

                if (accessToken != null && refreshToken != null && user != null) {
                    SessionManager(context).saveUserEmail(user.email)
                    SessionManager(context).saveRole(user.role)
                    SessionManager(context).saveTokens(accessToken, refreshToken)

                    _userData.value = user
                    _loginSuccess.value = true
                    Log.d("LoginViewModel", "Login correcto, sesión guardada.")
                } else {
                    _errorMessage.value = "Faltan datos en la respuesta del servidor"
                    Log.e("LoginViewModel", "Datos incompletos en la respuesta")
                }

            } catch (e: Exception) {
                _errorMessage.value = "Login fallido: ${e.message ?: "desconocido"}"
                Log.e("LoginViewModel", "Excepción durante el login", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NUEVA FUNCIÓN: verificar token actual
    fun verifyToken(context: Context) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getAuthApi(context)
                val response = api.verifyToken()

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("VerifyToken", "✅ Verificación correcta: $body")
                } else {
                    Log.w("VerifyToken", "⚠️ Verificación fallida: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VerifyToken", "❌ Error al verificar token", e)
            }
        }
    }
}