package com.tukandado.tukandadov2.ttlock

import android.util.Log
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Callback
import java.io.IOException

class TTLockCloud {
    private val BASE_URL = "https://euapi.ttlock.com/v3"
    private val CLIENT_ID = "TU_CLIENT_ID"
    private val CLIENT_SECRET = "TU_CLIENT_SECRET"
    private val REDIRECT_URI = "https://tu-callback.com"

    private val client = OkHttpClient()

    fun registerUser(email: String, password: String) {
        val body = FormBody.Builder()
            .add("clientId", CLIENT_ID)
            .add("username", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/user/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }

            override fun onResponse(call: Call, response: Response) {
                TODO("Not yet implemented")
            }

        })

    }

    fun getAccessToken(code: String) {
        val body = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("redirect_uri", REDIRECT_URI)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url("https://euopen.ttlock.com/oauth2/token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }

            override fun onResponse(call: Call, response: Response) {
                TODO("Not yet implemented")
            }

        })
    }

    fun sendEKey(
        accessToken: String,
        lockId: Long,
        receiverEmail: String,
        keyName: String,
        startDate: Long,
        endDate: Long
    ) {
        val body = FormBody.Builder()
            .add("clientId", CLIENT_ID)
            .add("accessToken", accessToken)
            .add("lockId", lockId.toString())
            .add("receiverUsername", receiverEmail)
            .add("keyName", keyName)
            .add("startDate", startDate.toString())
            .add("endDate", endDate.toString())
            .add("remoteEnable", "1")
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/key/send")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }

            override fun onResponse(call: Call, response: Response) {
                TODO("Not yet implemented")
            }

        })
    }

    fun getEKeys(accessToken: String, lockId: Long) {
        val body = FormBody.Builder()
            .add("clientId", CLIENT_ID)
            .add("accessToken", accessToken)
            .add("lockId", lockId.toString())
            .add("pageNo", "1")
            .add("pageSize", "20")
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/key/list")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }

            override fun onResponse(call: Call, response: Response) {
                TODO("Not yet implemented")
            }

        })
    }
}



