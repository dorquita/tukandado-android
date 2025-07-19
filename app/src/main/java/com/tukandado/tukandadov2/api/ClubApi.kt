package com.tukandado.tukandadov2.api

import retrofit2.Response
//import retrofit2.http.Body
import retrofit2.http.GET
//import retrofit2.http.POST
import retrofit2.http.Path


data class ClubResponse(
    val clubId: String,
    val name: String,
    val contactEmail: Long,
    val contactPhone: Long,
    val logoUrl: Boolean,
)

interface ClubApi {
    @GET("clubs")
    suspend fun getAllClubs(): Response<List<ClubResponse>>

    @GET("clubs/{id}")
    suspend fun getClubById(@Path("id") id: String): Response<ClubResponse>

    @GET("clubs/getClubsByUser/{userEmail}")
    suspend fun getClubsByUser(@Path("userEmail") userEmail: String): Response<List<ClubResponse>>
}