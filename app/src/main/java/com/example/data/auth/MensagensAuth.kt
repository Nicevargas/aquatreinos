package com.example.data.auth

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Erros do Supabase e validação dos formulários, em português. */
object MensagensAuth {

    const val SEM_CONFIGURACAO = "O app não está conectado ao Supabase (SUPABASE_URL e SUPABASE_ANON_KEY)."
    const val SEM_REDE = "Sem conexão com a internet. Tente de novo."
    const val DEMOROU = "O servidor demorou demais para responder. Tente de novo em instantes."
    const val CADASTRO_DEMOROU = "O servidor demorou para responder. Sua conta pode ter sido criada: confira seu e-mail (e o spam) antes de tentar de novo."

    private val moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    private val adapterAuth by lazy { moshi.adapter(AuthErrorDto::class.java) }
    private val adapterApi by lazy { moshi.adapter(ApiErrorDto::class.java) }

    private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

    fun deErroDeAuth(codigoHttp: Int, corpo: String?): String {
        val erro = corpo?.let { runCatching { adapterAuth.fromJson(it) }.getOrNull() }
        val codigo = erro?.errorCode?.lowercase()
        val texto = listOfNotNull(erro?.msg, erro?.message, erro?.errorDescription, erro?.error)
            .joinToString(" ")
            .lowercase()

        return when {
            codigo == "invalid_credentials" || "invalid login credentials" in texto ->
                "E-mail ou senha incorretos."
            codigo == "email_not_confirmed" || "email not confirmed" in texto ->
                "Confirme seu e-mail antes de entrar: o link está na sua caixa de entrada."
            codigo == "otp_expired" || "token has expired or is invalid" in texto ->
                "Código inválido ou vencido. Confira os números ou peça um novo."
            // Antes de weak_password: a mensagem também contém "password should be".
            codigo == "same_password" || "different from the old password" in texto ->
                "A nova senha precisa ser diferente da anterior."
            codigo == "user_already_exists" || codigo == "email_exists" || "already registered" in texto ->
                "Já existe uma conta com este e-mail. Entre com ela."
            codigo == "weak_password" || "password should be" in texto ->
                "Senha fraca: use pelo menos 6 caracteres, misturando letras e números."
            codigo == "email_address_invalid" || (codigo == "validation_failed" && "email" in texto) ->
                "E-mail inválido."
            codigo == "signup_disabled" ->
                "O cadastro de novas contas está desativado."
            codigo?.startsWith("over_") == true || codigoHttp == 429 ->
                "Muitas tentativas. Espere alguns minutos e tente de novo."
            codigoHttp >= 500 ->
                "O servidor não respondeu. Tente de novo em instantes."
            else ->
                "Não foi possível concluir (erro $codigoHttp)."
        }
    }

    fun deErroDaApi(codigoHttp: Int, corpo: String?): String {
        val erro = corpo?.let { runCatching { adapterApi.fromJson(it) }.getOrNull() }
        return when {
            codigoHttp == 401 -> "Sua sessão expirou. Entre de novo."
            erro?.code == "42501" || codigoHttp == 403 -> "Sem permissão para isso."
            erro?.code == "23514" || erro?.code == "22P02" -> "Algum dado ficou inválido. Confira e tente de novo."
            codigoHttp >= 500 -> "O servidor não respondeu. Tente de novo em instantes."
            else -> "Não foi possível concluir (erro $codigoHttp)."
        }
    }

    fun validarNome(nome: String): String? =
        if (nome.trim().length >= 2) null else "Digite seu nome."

    fun validarEmail(email: String): String? =
        if (EMAIL.matches(email.trim())) null else "Digite um e-mail válido."

    fun validarSenha(senha: String): String? =
        if (senha.length >= 6) null else "A senha precisa de pelo menos 6 caracteres."

    // O Supabase manda 6 dígitos por padrão; o painel permite até 10.
    fun validarCodigo(codigo: String): String? =
        if (codigo.trim().matches(Regex("""\d{6,10}"""))) null else "Digite o código de números que chegou por e-mail."

    fun validarConfirmacao(senha: String, confirmacao: String): String? =
        if (senha == confirmacao) null else "As duas senhas não conferem."
}
