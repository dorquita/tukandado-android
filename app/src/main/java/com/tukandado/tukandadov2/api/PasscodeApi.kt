package com.tukandado.tukandadov2.api

import retrofit2.Response
import retrofit2.http.*

data class PasscodeListResponse(
    val items: List<PasscodeDto>,
    val total: Int,
    val page: Int,
    val pages: Int
)

data class PasscodeDto(
    val _id: String,
    val lockId: String,
    val name: String? = null,
    val type: String,
    val status: String,
    val validFrom: String? = null,
    val validTo: String? = null,
    val receiverUserId: String? = null,
    val clubId: String? = null,
    val usage: UsageDto? = null,
    val external: ExternalDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class UsageDto(
    val count: Int = 0,
    val lastUsedAt: String? = null
)

data class ExternalDto(
    val lockId: Long? = null,
    val keyboardPwdId: Long? = null,
    val version: Int? = null
)

data class CreatePasscodeRequest(
    val lockId: String,
    val code: String,
    val type: String,
    val validFrom: String? = null,
    val validTo: String? = null,
    val name: String? = null,
    val receiverUserId: String? = null,
    val isCustom: Boolean? = null,
    val lockData: String,
    val lockMac: String
)

data class UpdatePasscodeRequest(
    val name: String? = null,
    val validFrom: String? = null,
    val validTo: String? = null,
    val receiverUserId: String? = null
)

// DTO opcional de respuesta (puedes ignorarlo si devuelves 204)
data class ResetByLockResponse(
    val affected: Int? = null
)

interface PasscodeApi {

    @GET("passcodes/list")
    suspend fun listPasscodes(
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20,
        @Query("lockId") lockId: String? = null,
        @Query("status") status: String? = null,
        @Query("q") q: String? = null,
        @Query("activeOnly") activeOnly: Boolean? = null,
        @Query("clubId") clubId: String? = null
    ): Response<PasscodeListResponse>

    @GET("passcodes/{id}")
    suspend fun getPasscodeById(
        @Path("id") id: String
    ): Response<PasscodeDto>

    /** POST /passcodes/createPasscode */
    @POST("passcodes/createPasscode")
    suspend fun createPasscode(
        @Body body: CreatePasscodeRequest
    ): Response<PasscodeDto>

    /** POST /passcodes/{id}/revoke */
    @POST("passcodes/{id}/revoke")
    suspend fun revokePasscode(
        @Path("id") id: String
    ): Response<PasscodeDto>

    /** PATCH /passcodes/{id} */
    @PATCH("passcodes/{id}")
    suspend fun updatePasscode(
        @Path("id") id: String,
        @Body body: UpdatePasscodeRequest
    ): Response<PasscodeDto>

    /** DELETE /passcodes/{id} */
    @DELETE("passcodes/{id}")
    suspend fun softDeletePasscode(
        @Path("id") id: String
    ): Response<Unit>

    // POST /locks/{lockId}/passcodes/reset
    @POST("locks/{lockId}/passcodes/reset")
    suspend fun resetPasscodesByLock(
        @Path("lockId") lockId: String
    ): Response<ResetByLockResponse>
}