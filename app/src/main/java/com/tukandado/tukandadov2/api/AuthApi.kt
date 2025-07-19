package com.tukandado.tukandadov2.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val name: String,
    val email: String,
    val role: String,
    val activeBooking: String?
)

data class VerifyResponse(
    val ok: Boolean,
    val user: UserData
)

data class UserData(
    val id: String,
    val role: String,
    val iat: Long,
    val exp: Long
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/verify")
    suspend fun verifyToken(): Response<VerifyResponse>
}