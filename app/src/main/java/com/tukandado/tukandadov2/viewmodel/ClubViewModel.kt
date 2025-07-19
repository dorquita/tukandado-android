package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.api.ClubResponse
import com.tukandado.tukandadov2.api.RetrofitInstance
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClubViewModel : ViewModel() {

    private val _clubs = MutableStateFlow<List<ClubResponse>>(emptyList())
    val clubs: StateFlow<List<ClubResponse>> = _clubs

    private val _selectedClub = MutableStateFlow<ClubResponse?>(null)
    val selectedClub: StateFlow<ClubResponse?> = _selectedClub

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun getAllClubs(context: Context) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getClubApi(context)
                val response = api.getAllClubs()

                if (response.isSuccessful) {
                    _clubs.value = response.body() ?: emptyList()
                    Log.d("ClubViewModel", "Clubs cargados: ${_clubs.value.size}")
                } else {
                    _error.value = "Error al obtener clubs: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Excepción al obtener clubs: ${e.message}"
            }
        }
    }

    fun getClubById(context: Context, clubId: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getClubApi(context)
                val response = api.getClubById(clubId)

                if (response.isSuccessful) {
                    _selectedClub.value = response.body()
                } else {
                    _error.value = "Error al obtener club: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Excepción al obtener club: ${e.message}"
            }
        }
    }

    fun getClubsByUser(context: Context) {
        viewModelScope.launch {
            try {
                val userEmail = SessionManager(context).getUserEmail().first()
                if (userEmail == null) {
                    _error.value = "No se encontró userId en sesión"
                    return@launch
                }

                val api = RetrofitInstance.getClubApi(context)
                val response = api.getClubsByUser(userEmail)

                if (response.isSuccessful) {
                    _clubs.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error al obtener clubs del usuario: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Excepción en getClubsByUser: ${e.message}"
            }
        }
    }
}