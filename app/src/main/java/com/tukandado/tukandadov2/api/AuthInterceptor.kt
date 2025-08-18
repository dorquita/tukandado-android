package com.tukandado.tukandadov2.api

import android.content.Context
import android.util.Log
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        Log.d("AuthInterceptor", "➡️ Interceptando request a: ${req.toString()}")

        Log.d("BYPASS", BuildConfig.BYPASS_LOGIN.toString())
        Log.d("BuildConfig.TEST_JWT", BuildConfig.TEST_JWT.toString())
        // BYPASS en debug: token de test
        if (BuildConfig.BYPASS_LOGIN && BuildConfig.TEST_JWT.isNotBlank()) {
            Log.d("AuthInterceptor", "🔓 BYPASS_LOGIN activo, usando TEST_JWT")
            Log.d("AuthInterceptor", "TEST_JWT = ${BuildConfig.TEST_JWT}")
            val bypassReq = req.newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.TEST_JWT}")
                .build()
            return chain.proceed(bypassReq)
        }

        // Flujo normal: toma tokens de SessionManager
        val sessionManager = SessionManager(context)
        val (accessToken, refreshToken) = runBlocking {
            val at = sessionManager.getAccessToken().firstOrNull()
            val rt = sessionManager.getRefreshToken().firstOrNull()
            at to rt
        }

        Log.d("AuthInterceptor", "AccessToken = ${accessToken ?: "null"}")
        Log.d("AuthInterceptor", "RefreshToken = ${refreshToken ?: "null"}")

        val newReqBuilder = req.newBuilder()

        // Header Authorization si hay access token
        accessToken?.let {
            Log.d("AuthInterceptor", "🛡 Añadiendo Authorization header")
            newReqBuilder.header("Authorization", "Bearer $it")
        }

        // Cookies httpOnly si tu backend las usa
        val cookieHeader = buildString {
            if (!accessToken.isNullOrBlank()) append("accessToken=$accessToken; ")
            if (!refreshToken.isNullOrBlank()) append("refreshToken=$refreshToken")
        }.trim()

        if (cookieHeader.isNotEmpty()) {
            Log.d("AuthInterceptor", "🍪 Añadiendo Cookie header: $cookieHeader")
            newReqBuilder.header("Cookie", cookieHeader)
        } else {
            Log.d("AuthInterceptor", "⚠️ No se añadieron cookies")
        }

        val newReq = newReqBuilder.build()
        return chain.proceed(newReq)
    }
}