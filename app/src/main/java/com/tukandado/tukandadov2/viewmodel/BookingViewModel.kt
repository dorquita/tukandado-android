package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttlock.bl.sdk.callback.DeletePasscodeCallback
import com.tukandado.tukandadov2.api.*
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.ttlock.TTLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine


class BookingViewModel : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun getBookingById(context: Context, bookingId: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getBookingApi(context)
                val response = api.getBookingById(bookingId)
                if (response.isSuccessful) {
                    SessionManager(context).saveActiveBooking(response.body()!!)
                } else {
                    _error.value = "Error al obtener reserva: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e("BookingViewModel", _error.value!!, e)
            }
        }
    }

    fun getBookingsByUser(context: Context, userId: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getBookingApi(context)
                val response = api.getBookingsByUser(userId)
                if (response.isSuccessful) {
                   // _bookings.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error al obtener reservas del usuario: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun startBooking(context: Context, lockId: String) {
        viewModelScope.launch {
            try {
                val email = SessionManager(context).getUserEmail().first()
                Log.d("el email que envio es", email.toString())
                val api = RetrofitInstance.getBookingApi(context)
                val request = StartBookingRequest(email ?: "", lockId)

                val response = api.startBooking(request)
                Log.d("startBooking", "Respuesta del servidor: ${response.body()}")

                if (response.isSuccessful) {
                    SessionManager(context).saveActiveBooking(response.body()!!)
                    Log.d("BookingViewModel", "Reserva iniciada correctamente")
                    Log.d("BookingViewModel2", SessionManager(context).getActiveBooking().toString())
                } else {
                    _error.value = "Error al iniciar reserva: ${response.code()}"
                    Log.e("BookingViewModel", _error.value!!)
                }
            } catch (e: Exception) {
                _error.value = "Error al iniciar reserva: ${e.message}"
                Log.e("BookingViewModel", _error.value!!, e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun endBooking(
        context: Context,
        lockData: String? = null,
        lockMac: String? = null
    ): Boolean {
        return try {
            _error.value = null

            val booking = SessionManager(context.applicationContext).getActiveBooking().first()
            if (booking == null) {
                _error.value = "No hay ninguna reserva activa"
                return false
            }

            // 1) (Opcional) Borrar el passcode en el candado por BLE ANTES de cerrar
            if (!lockData.isNullOrBlank() && !lockMac.isNullOrBlank()) {
                try {
                    val passApi = RetrofitInstance.getPasscodeApi(context)
                    val resp = passApi.getPasscodesForBooking(
                        bookingId = booking._id,
                        includeDeleted = false,
                        plain = true   // necesitamos 'code' en claro
                    )
                    if (resp.isSuccessful) {
                        val pin = resp.body().orEmpty().firstOrNull()?.code
                        if (!pin.isNullOrBlank()) {
                            // 👇 LLAMADA DIRECTA A LA SUSPEND (sin lambdas intermedias)
                            val tt = TTLockManager(context)
                            val ble = tt.deletePasscodeSuspend(pin, lockData, lockMac)
                            if (!ble.isSuccess) {
                                Log.w("BookingViewModel", "BLE: fallo borrando PIN ${ble.exceptionOrNull()?.message}")
                            }
                        }
                    } else {
                        Log.w("BookingViewModel", "No se pudo obtener el passcode: HTTP ${resp.code()}")
                    }
                } catch (e: Exception) {
                    Log.w("BookingViewModel", "BLE error: ${e.message}")
                }
            }

            // 2) Cerrar la reserva en backend
            val api = RetrofitInstance.getBookingApi(context)
            val response = api.endBooking(booking._id)
            if (response.isSuccessful) {
                SessionManager(context.applicationContext).clearActiveBooking()
                true
            } else {
                _error.value = "Error al finalizar reserva: HTTP ${response.code()}"
                false
            }
        } catch (e: Exception) {
            _error.value = "Error al finalizar reserva: ${e.message}"
            false
        }
    }

}