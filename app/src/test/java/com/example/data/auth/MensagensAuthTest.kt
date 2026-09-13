package com.example.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MensagensAuthTest {

    @Test
    fun `traduz os erros do Supabase Auth`() {
        assertEquals("E-mail ou senha incorretos.",
            MensagensAuth.deErroDeAuth(400, """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""))
        // Formato antigo do GoTrue, sem error_code
        assertEquals("E-mail ou senha incorretos.",
            MensagensAuth.deErroDeAuth(400, """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""))
        assertEquals("Confirme seu e-mail antes de entrar: o link está na sua caixa de entrada.",
            MensagensAuth.deErroDeAuth(400, """{"error_code":"email_not_confirmed","msg":"Email not confirmed"}"""))
        assertEquals("Já existe uma conta com este e-mail. Entre com ela.",
            MensagensAuth.deErroDeAuth(422, """{"error_code":"user_already_exists","msg":"User already registered"}"""))
        assertEquals("Muitas tentativas. Espere alguns minutos e tente de novo.",
            MensagensAuth.deErroDeAuth(429, """{"error_code":"over_email_send_rate_limit"}"""))
        assertEquals("O servidor não respondeu. Tente de novo em instantes.",
            MensagensAuth.deErroDeAuth(502, "<html>Bad gateway</html>"))
        assertEquals("Não foi possível concluir (erro 418).", MensagensAuth.deErroDeAuth(418, null))
    }

    @Test
    fun `traduz os erros da API`() {
        assertEquals("Sem permissão para isso.",
            MensagensAuth.deErroDaApi(403, """{"code":"42501","message":"new row violates row-level security policy"}"""))
        assertEquals("Sua sessão expirou. Entre de novo.", MensagensAuth.deErroDaApi(401, """{"message":"JWT expired"}"""))
        assertEquals("Algum dado ficou inválido. Confira e tente de novo.",
            MensagensAuth.deErroDaApi(400, """{"code":"23514","message":"violates check constraint"}"""))
    }

    @Test
    fun `traduz os erros da recuperacao de senha`() {
        assertEquals("Código inválido ou vencido. Confira os números ou peça um novo.",
            MensagensAuth.deErroDeAuth(403, """{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}"""))
        // A frase contém "password should be": não pode cair em "senha fraca".
        assertEquals("A nova senha precisa ser diferente da anterior.",
            MensagensAuth.deErroDeAuth(422, """{"error_code":"same_password","msg":"New password should be different from the old password."}"""))
        assertEquals("Muitas tentativas. Espere alguns minutos e tente de novo.",
            MensagensAuth.deErroDeAuth(429, """{"error_code":"over_email_send_rate_limit","msg":"email rate limit exceeded"}"""))
    }

    @Test
    fun `valida codigo e confirmacao`() {
        assertNull(MensagensAuth.validarCodigo("123456"))
        assertNull(MensagensAuth.validarCodigo(" 12345678 "))
        assertNotNull(MensagensAuth.validarCodigo("12345"))
        assertNotNull(MensagensAuth.validarCodigo("12a456"))
        assertNull(MensagensAuth.validarConfirmacao("nova123", "nova123"))
        assertNotNull(MensagensAuth.validarConfirmacao("nova123", "nova124"))
    }

    @Test
    fun `valida o formulario`() {
        assertNull(MensagensAuth.validarEmail(" ana@exemplo.com "))
        assertNotNull(MensagensAuth.validarEmail("ana@exemplo"))
        assertNotNull(MensagensAuth.validarEmail("ana exemplo.com"))
        assertNull(MensagensAuth.validarSenha("123456"))
        assertNotNull(MensagensAuth.validarSenha("12345"))
        assertNull(MensagensAuth.validarNome("Ana"))
        assertNotNull(MensagensAuth.validarNome(" A "))
    }

    @Test
    fun `sessao sai da resposta de login e nao sai do cadastro sem confirmacao`() {
        val login = SessionDto(
            accessToken = "a", refreshToken = "r", expiresIn = 3600,
            user = AuthUserDto(id = "u1", email = "ana@exemplo.com")
        )
        assertEquals(Sessao("a", "r", 1_000 + 3600, "u1", "ana@exemplo.com"), login.paraSessao(1_000))
        assertEquals(5_000L, login.copy(expiresAt = 5_000).paraSessao(1_000)!!.expiraEm)

        val cadastroPendente = SessionDto(id = "u2", email = "novo@exemplo.com", identities = listOf(mapOf("id" to "x")))
        assertNull(cadastroPendente.paraSessao(1_000))
    }
}
