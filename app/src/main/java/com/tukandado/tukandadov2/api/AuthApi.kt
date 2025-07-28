package com.tukandado.tukandadov2.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class LockEmbedded(
    val _id: String,
    val lockName: String,
    val lockAlias: String,
    val lockId: Long,
    val lockData: String,
    val clubId: String,
    val lastUsed: String?,
    val updatedAt: String,
    val isInUse: Boolean,
    val lockMac: String
)

data class UserEmbedded(
    val _id: String,
    val name: String,
    val surname: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String,
    val clubId: List<String>,
    val sendNotifications: Boolean,
    val isVerified: Boolean,
    val activeBookingId: String,
    val updatedAt: String
)

data class ActiveBooking(
    val _id: String,
    val userId: UserEmbedded,
    val lockId: LockEmbedded,
    val startTime: String,
    val endTime: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val __v: Int
)

data class LoginResponse(
    val name: String,
    val email: String,
    val role: String,
    val activeBooking: ActiveBooking?
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