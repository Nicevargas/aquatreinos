package com.example.data.auth

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

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

    // ---- Esqueci minha senha ----

    /** Manda o e-mail de recuperação. Responde sucesso mesmo se o e-mail não tiver conta. */
    @POST("auth/v1/recover")
    suspend fun recover(@Body body: RecoverBody): Response<Unit>

    /** Troca o código do e-mail por uma sessão de recuperação. */
    @POST("auth/v1/verify")
    suspend fun verify(@Body body: VerifyOtpBody): Response<SessionDto>

    /** Grava a senha nova; o Authorization é o da sessão de recuperação, não o salvo no aparelho. */
    @PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("Authorization") bearer: String,
        @Body body: UpdatePasswordBody
    ): Response<AuthUserDto>
}
