package com.tukandado.tukandadov2.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private var retrofit: Retrofit? = null

    private fun getRetrofit(context: Context): Retrofit {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl("https://tukandado-backend.onrender.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }
        return retrofit!!
    }

    private fun <T> createApi(context: Context, apiClass: Class<T>): T {
        return getRetrofit(context).create(apiClass)
    }

    fun getAuthApi(context: Context): AuthApi = createApi(context, AuthApi::class.java)
    fun getClubApi(context: Context): ClubApi = createApi(context, ClubApi::class.java)
    fun getLockApi(context: Context): LockApi = createApi(context, LockApi::class.java)
}
