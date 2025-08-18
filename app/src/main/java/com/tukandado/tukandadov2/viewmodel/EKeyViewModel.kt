package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.api.ClubResponse
import com.tukandado.tukandadov2.api.EkeyResponse
import com.tukandado.tukandadov2.api.RetrofitInstance
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EkeyViewModel : ViewModel() {

    private val _ekeys = MutableStateFlow<List<EkeyResponse>>(emptyList())
    val ekeys: StateFlow<List<EkeyResponse>> = _ekeys

    private val _selectedClub = MutableStateFlow<ClubResponse?>(null)
    val selectedClub: StateFlow<ClubResponse?> = _selectedClub

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /*fun getAllEkeys(context: Context) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getClubApi(context)
                val response = api.getAllClubs()

                if (response.isSuccessful) {
                    _ekeys.value = response.body() ?: emptyList()
                    Log.d("ClubViewModel", "Clubs cargados: ${_clubs.value.size}")
                } else {
                    _error.value = "Error al obtener clubs: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Excepción al obtener clubs: ${e.message}"
            }
        }
    }*/

    // En EkeyViewModel
    fun getLockEkeys(context: Context, lockId: String) {
        viewModelScope.launch {
            try {
                Log.d("EkeyVM", "🔌 Creando API y llamando getLockEkeys(lockId=$lockId)")
                val api = RetrofitInstance.getEkeyApi(context)
                Log.d("EkeyVM", "➡️ GET /locks/$lockId/ekeys (simbolico)")

                val response = api.getLockEkeys(lockId)

                Log.d("EkeyVM", "⬅️ Response code=${response.code()} success=${response.isSuccessful}")

                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    Log.d("EkeyVM", "✅ eKeys recibidos: ${body.size}")
                    Log.d("EkeyVM", "✅ eKeys recibidos: ${response.body().toString()}")
                    _ekeys.value = body
                    _error.value = null
                } else {
                    val msg = "Error al obtener eKeys: HTTP ${response.code()}"
                    Log.e("EkeyVM", "❌ $msg")
                    _error.value = msg
                }
            } catch (e: Exception) {
                val msg = "Excepción al obtener eKeys: ${e.message}"
                Log.e("EkeyVM", "💥 $msg", e)
                _error.value = msg
            }
        }
    }

}