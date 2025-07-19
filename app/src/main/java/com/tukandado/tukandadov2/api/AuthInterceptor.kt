package com.tukandado.tukandadov2.api

import android.content.Context
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionManager = SessionManager(context)

        val (accessToken, refreshToken) = runBlocking {
            val at = sessionManager.getAccessToken().firstOrNull()
            val rt = sessionManager.getRefreshToken().firstOrNull()
            Pair(at, rt)
        }

        val request = chain.request().newBuilder().apply {
            // Authorization header (opcional si solo usas cookies)
            accessToken?.let {
                addHeader("Authorization", "Bearer $it")
            }

            // Cookie header (lo importante para renovación automática)
            val cookieHeader = buildString {
                if (accessToken != null) append("accessToken=$accessToken; ")
                if (refreshToken != null) append("refreshToken=$refreshToken")
            }.trim()

            if (cookieHeader.isNotEmpty()) {
                addHeader("Cookie", cookieHeader)
            }
        }.build()

        return chain.proceed(request)
    }
}