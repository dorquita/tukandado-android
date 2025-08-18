package com.tukandado.tukandadov2.api

import android.content.Context
import android.util.Log
import com.tukandado.tukandadov2.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    @Volatile
    private var retrofit: Retrofit? = null

    private fun buildClient(context: Context): OkHttpClient {
        Log.d("RetrofitInstance", "🛠️ Construyendo OkHttpClient (AuthInterceptor ON)")
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
    }

    private fun getRetrofit(context: Context): Retrofit {
        val existing = retrofit
        if (existing != null) {
            Log.d("RetrofitInstance", "♻️ Reutilizando instancia Retrofit existente")
            return existing
        }

        synchronized(this) {
            val again = retrofit
            if (again != null) {
                Log.d("RetrofitInstance", "♻️ Reutilizando instancia Retrofit existente (tras sync)")
                return again
            }

            val baseUrl = BuildConfig.BACKEND_URL
            Log.d("RetrofitInstance", "🚀 Creando nueva instancia Retrofit con baseUrl=$baseUrl")

            val newRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildClient(context))
                .build()

            retrofit = newRetrofit
            return newRetrofit
        }
    }

    private fun <T> createApi(context: Context, apiClass: Class<T>): T {
        Log.d("RetrofitInstance", "📦 Creando API: ${apiClass.simpleName}")
        return getRetrofit(context).create(apiClass)
    }

    fun getAuthApi(context: Context): AuthApi = createApi(context, AuthApi::class.java)
    fun getClubApi(context: Context): ClubApi = createApi(context, ClubApi::class.java)
    fun getLockApi(context: Context): LockApi = createApi(context, LockApi::class.java)
    fun getBookingApi(context: Context): BookingApi = createApi(context, BookingApi::class.java)
    fun getEkeyApi(context: Context): EkeyApi = createApi(context, EkeyApi::class.java)
    fun getPasscodeApi(context: Context): PasscodeApi = createApi(context, PasscodeApi::class.java)
}