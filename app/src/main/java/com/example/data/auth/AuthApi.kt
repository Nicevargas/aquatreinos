package com.example.data.auth

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Supabase Auth (GoTrue) pela mesma URL do projeto. */
interface AuthApi {

    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SignUpBody): Response<SessionDto>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(@Body body: PasswordGrantBody): Response<SessionDto>

    // Síncrona: roda dentro do Authenticator do OkHttp, que não é suspend.
    @POST("auth/v1/token?grant_type=refresh_token")
    fun refresh(@Body body: RefreshGrantBody): Call<SessionDto>

    @POST("auth/v1/logout")
    suspend fun signOut(): Response<Unit>
}
