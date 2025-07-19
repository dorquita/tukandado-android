package com.tukandado.tukandadov2.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val ROLE_KEY = stringPreferencesKey("role_key")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    suspend fun saveRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[ROLE_KEY] = role
        }
    }

    suspend fun saveUserEmail(userEmail: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_EMAIL_KEY] = userEmail
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    fun getUserEmail(): Flow<String?> {
        return context.dataStore.data.map { it[USER_EMAIL_KEY] }
    }

    fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    }

    fun getRefreshToken(): Flow<String?> {
        return context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}