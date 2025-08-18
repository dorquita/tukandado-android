package com.tukandado.tukandadov2.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.api.ActiveBooking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val ROLE_KEY = stringPreferencesKey("role_key")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val ACTIVE_BOOKING_KEY = stringPreferencesKey("active_booking")
    }

    // ---------- Setters básicos ----------
    suspend fun saveRole(role: String) {
        context.dataStore.edit { it[ROLE_KEY] = role }
    }

    suspend fun saveUserEmail(userEmail: String) {
        context.dataStore.edit { it[USER_EMAIL_KEY] = userEmail }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN_KEY] = accessToken
            it[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    // ---------- Getters como Flow ----------
    fun getUserEmail(): Flow<String?> =
        context.dataStore.data.map { it[USER_EMAIL_KEY] }

    fun getAccessToken(): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    fun getRefreshToken(): Flow<String?> =
        context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }

    fun getRole(): Flow<String?> =
        context.dataStore.data.map { it[ROLE_KEY] }

    // ---------- Sesión: helpers ----------
    /** Flow que emite true si hay tokens no vacíos */
    val hasValidSessionFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            val at = prefs[ACCESS_TOKEN_KEY]
            val rt = prefs[REFRESH_TOKEN_KEY]
            !at.isNullOrBlank() && !rt.isNullOrBlank()
        }

    /** Lectura puntual (no Flow), útil en arranques/splash */
    suspend fun hasValidSessionOnce(): Boolean {
        val at = getAccessToken().first()
        val rt = getRefreshToken().first()
        return !at.isNullOrBlank() && !rt.isNullOrBlank()
    }

    /**
     * Solo para entorno de desarrollo:
     * Si no hay tokens, crea unos "fake" y un usuario por defecto.
     */
    suspend fun ensureDevToken() {
        if (!BuildConfig.BYPASS_LOGIN) return // o if (!BuildConfig.DEBUG) return
        val at = getAccessToken().first()
        val rt = getRefreshToken().first()
        if (at.isNullOrBlank() || rt.isNullOrBlank()) {
            saveTokens("dev-access-token", "dev-refresh-token")
        }
        val email = getUserEmail().first()
        if (email.isNullOrBlank()) saveUserEmail("dev@tukandado.com")
        saveRole("superadmin")
    }

    suspend fun ensureDevSessionForDebug() {
        if (!BuildConfig.BYPASS_LOGIN) return
    }

    // ---------- Booking persistence ----------
    suspend fun saveActiveBooking(booking: ActiveBooking) {
        val json = Gson().toJson(booking)
        context.dataStore.edit { it[ACTIVE_BOOKING_KEY] = json }
    }

    suspend fun clearActiveBooking() {
        context.dataStore.edit { it.remove(ACTIVE_BOOKING_KEY) }
    }

    fun getActiveBooking(): Flow<ActiveBooking?> =
        context.dataStore.data.map { prefs ->
            prefs[ACTIVE_BOOKING_KEY]?.let { Gson().fromJson(it, ActiveBooking::class.java) }
        }
}