package com.example.data.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---- Corpos das chamadas ao Supabase Auth (GoTrue) ----

@JsonClass(generateAdapter = true)
data class SignUpBody(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    // Vira raw_user_meta_data: full_name e training_level do perfil.
    @Json(name = "data") val data: Map<String, String>
)

@JsonClass(generateAdapter = true)
data class PasswordGrantBody(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RefreshGrantBody(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RecoverBody(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class VerifyOtpBody(
    @Json(name = "type") val type: String,
    @Json(name = "email") val email: String,
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class UpdatePasswordBody(
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AuthUserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "identities") val identities: List<Any>? = null
)

/**
 * Resposta de login, renovação e cadastro. No cadastro que exige confirmação
 * por e-mail não vem sessão: o usuário vem solto no topo (id, email, identities).
 */
@JsonClass(generateAdapter = true)
data class SessionDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null,
    @Json(name = "user") val user: AuthUserDto? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "identities") val identities: List<Any>? = null
)

@JsonClass(generateAdapter = true)
data class AuthErrorDto(
    @Json(name = "error_code") val errorCode: String? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

/** Erro do PostgREST: {"code":"42501","message":"..."}. */
@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

// ---- Domínio ----

data class Sessao(
    val accessToken: String,
    val refreshToken: String,
    val expiraEm: Long, // epoch em segundos
    val userId: String,
    val email: String
)

sealed interface ResultadoAuth {
    data object Entrou : ResultadoAuth
    data object ConfirmarEmail : ResultadoAuth
    data class Erro(val mensagem: String) : ResultadoAuth
}

fun SessionDto.paraSessao(agoraSegundos: Long): Sessao? {
    val acesso = accessToken ?: return null
    val renovacao = refreshToken ?: return null
    val usuario = user?.id ?: return null
    return Sessao(
        accessToken = acesso,
        refreshToken = renovacao,
        expiraEm = expiresAt ?: (agoraSegundos + (expiresIn ?: 3600)),
        userId = usuario,
        email = user.email.orEmpty()
    )
}
