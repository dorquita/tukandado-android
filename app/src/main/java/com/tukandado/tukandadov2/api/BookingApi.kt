package com.tukandado.tukandadov2.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class StartBookingRequest(
    val userEmail: String,
    val lockId: String
)

interface BookingApi {
    @GET("bookings/")
    suspend fun getAllBookings(): Response<List<ActiveBooking>>

    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") id: String): Response<ActiveBooking>

    @GET("bookings/user/{userId}")
    suspend fun getBookingsByUser(@Path("userId") userId: String): Response<List<ActiveBooking>>

    @GET("bookings/club/{clubId}")
    suspend fun getBookingsByClub(@Path("clubId") id: String): Response<List<ActiveBooking>>

    @POST("bookings")
    suspend fun startBooking(@Body request: StartBookingRequest): Response<ActiveBooking>

    @PUT("bookings/endBooking/{bookingId}")
    suspend fun endBooking(@Path("bookingId") bookingId: String): Response<ActiveBooking>
}