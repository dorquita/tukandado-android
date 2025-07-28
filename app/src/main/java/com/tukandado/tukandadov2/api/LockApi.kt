package com.tukandado.tukandadov2.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.Instant


data class LockResponse(
    val _id: String,
    val lockName: String,
    val lockAlias: String,
    val lockId: Int,
    val lockData: String,
    val lockMac: String,
    val clubId: String,
    val lastUsed: String,
    val updatedAt: String,
    val isInUse: Boolean
)

interface LockApi {
    @GET("locks")
    suspend fun getAllLocks(): Response<List<LockResponse>>

    @GET("locks/club/{clubId}")
    suspend fun getAllLocksByClub(@Path("clubId") id: String): Response<List<LockResponse>>

    @GET("locks/user/{userEmail}")
    suspend fun getAllLocksByUser(@Path("userEmail") userEmail: String): Response<List<LockResponse>>

    @GET("locks/club/{clubId}/available")
    suspend fun getAllLocksByClubAvailable(@Path("clubId") id: String): Response<List<LockResponse>>

    @GET("locks/{id}")
    suspend fun getLockById(@Path("id") id: String): Response<LockResponse>
}