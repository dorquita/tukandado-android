package com.tukandado.tukandadov2.api

import retrofit2.Response
import retrofit2.http.GET

data class UserData(
    val _id: String,
    val name: String,
    val surname: String,
    val email: String,
    val phone: String,
    val role: String,
    val sendNotifications: Boolean,
    val isVerified: Boolean,
    val clubId: List<String>
)

data class LockData(
    val _id: String,
    val lockName: String,
    val lockAlias: String,
    val lockId: Int,
    val lockData: String,
    val clubId: String,
    val lastUsed: String,
    val updatedAt: String,
    val isInUse: Boolean,
    val lockMac: String?
)

data class ClubData(
    val _id: String,
    val name: String,
    val location: String,
    val contactEmail: String,
    val contactPhone: String,
    val logoUrl: String
)


data class BookingResponse(
    val _id: String,
    val userId: UserData,
    val lockId: LockData,
    val clubId: ClubData,
    val startTime: String,
    val endTime: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

interface BookingApi {
    @GET("bookings")
    suspend fun getAllBookings(): Response<List<BookingResponse>>
}