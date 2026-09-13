package com.example.data.supabase

import org.junit.Assert.assertEquals
import org.junit.Test

class EscolherAuthorizationTest {

    private val anon = "chave-anon"

    @Test
    fun `dados vao com o token de quem esta logado`() {
        assertEquals("Bearer tok", escolherAuthorization("/rest/v1/workouts", null, "tok", anon))
        assertEquals("Bearer tok", escolherAuthorization("/rest/v1/rpc/excluir_minha_conta", null, "tok", anon))
    }

    @Test
    fun `sem sessao vai a chave publica`() {
        assertEquals("Bearer $anon", escolherAuthorization("/rest/v1/rpc/treinos_sugeridos", null, null, anon))
    }

    @Test
    fun `login, cadastro e recuperacao vao com a chave publica mesmo logado`() {
        listOf("/auth/v1/token", "/auth/v1/signup", "/auth/v1/recover", "/auth/v1/verify").forEach { caminho ->
            assertEquals(caminho, "Bearer $anon", escolherAuthorization(caminho, null, "tok", anon))
        }
    }

    @Test
    fun `logout vai com o token da sessao`() {
        assertEquals("Bearer tok", escolherAuthorization("/auth/v1/logout", null, "tok", anon))
    }

    @Test
    fun `authorization posto por quem chamou e mantido`() {
        // Troca de senha: o token é o da recuperação, não o salvo no aparelho (que nem existe).
        assertEquals("Bearer tok-rec", escolherAuthorization("/auth/v1/user", "Bearer tok-rec", null, anon))
        assertEquals("Bearer tok-rec", escolherAuthorization("/auth/v1/user", "Bearer tok-rec", "outro", anon))
    }
}
