package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.api.LockResponse
import com.tukandado.tukandadov2.api.RetrofitInstance
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LockViewModel : ViewModel() {

    private val _locks = MutableStateFlow<List<LockResponse>>(emptyList())
    val locks: StateFlow<List<LockResponse>> = _locks

    private val _selectedLock = MutableStateFlow<LockResponse?>(null)
    val selectedLock: StateFlow<LockResponse?> = _selectedLock

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun getAllLocks(context: Context) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getLockApi(context)
                val response = api.getAllLocks()

                if (response.isSuccessful) {
                    _locks.value = response.body() ?: emptyList()
                    Log.d("LockViewModel", "Locks recibidos: ${_locks.value.size}")
                } else {
                    _error.value = "Error al obtener candados: ${response.code()}"
                    Log.e("LockViewModel", _error.value!!)
                }
            } catch (e: Exception) {
                _error.value = "Excepción al obtener candados: ${e.message}"
                Log.e("LockViewModel", _error.value!!, e)
            }
        }
    }

    fun getLocksByClub(context: Context, clubId: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getLockApi(context)
                val response = api.getAllLocksByClub(clubId)

                if (response.isSuccessful) {
                    _locks.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error al obtener candados del club: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error en la llamada: ${e.message}"
            }
        }
    }

    fun getAvailableLocksByClub(context: Context, clubId: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getLockApi(context)
                val response = api.getAllLocksByClubAvailable(clubId)

                if (response.isSuccessful) {
                    _locks.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error al obtener candados disponibles: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error en la llamada: ${e.message}"
            }
        }
    }

    fun getLockById(context: Context, lockId: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getLockApi(context)
                val response = api.getLockById(lockId)

                if (response.isSuccessful) {
                    _selectedLock.value = response.body()
                } else {
                    _error.value = "Error al obtener candado por ID: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error en la llamada: ${e.message}"
            }
        }
    }
}