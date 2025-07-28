package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.api.*
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    fun endBooking(context: Context) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.getBookingApi(context)

                val booking = SessionManager(context).getActiveBooking().first()
                Log.d("DiceComo7", booking.toString())

                if (booking == null) {
                    _error.value = "No hay ninguna reserva activa"
                    Log.d("BookingViewModel", "Intento de finalizar sin reserva activa")
                    return@launch
                }

                Log.d("BookingViewModel", "Finalizando reserva con ID: ${booking._id}")
                val response = api.endBooking(booking._id)

                if (response.isSuccessful) {
                    SessionManager(context).clearActiveBooking()
                    Log.d("BookingViewModel", "Reserva finalizada correctamente")
                } else {
                    _error.value = "Error al finalizar reserva: ${response.code()}"
                    Log.e("BookingViewModel", _error.value!!)
                }
            } catch (e: Exception) {
                _error.value = "Error al finalizar reserva: ${e.message}"
                Log.e("BookingViewModel", _error.value!!, e)
            }
        }
    }
}