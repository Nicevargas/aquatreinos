package com.example.data.supabase

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class SupabaseStatus {
    CONNECTED,
    CONFIG_NEEDED,
    CONNECTING,
    OFFLINE_LOCAL
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trim()
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean by lazy {
        supabaseUrl.isNotBlank() &&
                !supabaseUrl.contains("placeholder", ignoreCase = true) &&
                supabaseAnonKey.isNotBlank() &&
                !supabaseAnonKey.contains("placeholder", ignoreCase = true)
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $supabaseAnonKey")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: SupabaseApi? by lazy {
        if (!isConfigured) {
            Log.w(TAG, "Supabase credentials are not configured. Falling back to local offline mode.")
            null
        } else {
            try {
                val formattedUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
                Retrofit.Builder()
                    .baseUrl(formattedUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseApi::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Supabase Retrofit client", e)
                null
            }
        }
    }
}
