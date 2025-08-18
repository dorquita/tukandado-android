package com.tukandado.tukandadov2.api

import retrofit2.Response
//import retrofit2.http.Body
import retrofit2.http.GET
//import retrofit2.http.POST
import retrofit2.http.Path


data class EkeyResponse(
    val source: String? = null,
    val lastSyncAt: Long? = null,

    val _id: String? = null,          // Mongo ID
    val date: Long? = null,
    val lockAlias: String? = null,
    val keyStatus: String? = null,
    val endDate: Long? = null,
    val keyId: Long? = null,
    val lockMac: String? = null,
    val deletePwd: String? = null,
    val featureValue: String? = null,
    val hasGateway: Int? = null,
    val wirelessKeypadFeatureValue: String? = null,
    val lockName: String? = null,
    val keyRight: Int? = null,
    val specialValue: Long? = null,
    val keyName: String? = null,
    val noKeyPwd: String? = null,
    val passageMode: Int? = null,
    val timezoneRawOffset: Long? = null,
    val lockId: Long? = null,
    val electricQuantity: Int? = null,
    val lockData: String? = null,
    val keyboardPwdVersion: Int? = null,
    val remoteEnable: Int? = null,
    val lockVersion: LockVersion? = null,
    val userType: String? = null,
    val startDate: Long? = null,
    val remarks: String? = null,
    val eKeyName: String? = null
)

data class LockVersion(
    val showAdminKbpwdFlag: Boolean? = null,
    val groupId: Int? = null,
    val protocolVersion: Int? = null,
    val protocolType: Int? = null,
    val orgId: Int? = null,
    val logoUrl: String? = null,
    val scene: Int? = null
)

interface EkeyApi {
    @GET("ekeys")
    suspend fun getAllEkeys(): Response<List<EkeyResponse>>

    @GET("ekeys/lock/{lockId}")
    suspend fun getLockEkeys(@Path("lockId") lockId: String): Response<List<EkeyResponse>>
}