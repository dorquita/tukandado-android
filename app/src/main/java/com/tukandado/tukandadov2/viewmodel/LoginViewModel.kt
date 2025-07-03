// viewmodel/LoginViewModel.kt
package com.tukandado.tukandadov2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.api.LoginRequest
import com.tukandado.tukandadov2.api.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                // Guarda token si quieres
                _loginSuccess.value = true
            } catch (e: Exception) {
                _loginSuccess.value = false
                println("Login error: ${e.message}")
            }
        }
    }
}