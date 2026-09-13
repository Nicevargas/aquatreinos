package com.example.data.auth

import com.example.data.Resultado
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Esqueci minha senha contra um Supabase Auth de mentira: confere o que vai e o que volta. */
class RecuperacaoDeSenhaTest {

    private lateinit var servidor: MockWebServer
    private lateinit var recuperacao: RecuperacaoDeSenha

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build()
            .create(AuthApi::class.java)
        recuperacao = RecuperacaoDeSenha(api, agoraSegundos = { 1_000 })
    }

    @After
    fun encerrar() {
        runCatching { servidor.shutdown() }
    }

    @Test
    fun `envia o pedido de codigo com o e-mail normalizado`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val r = recuperacao.enviarCodigo("  Ana@Exemplo.com ")

        assertEquals(Resultado.Ok(Unit), r)
        val pedido = servidor.takeRequest()
        assertEquals("POST", pedido.method)
        assertEquals("/auth/v1/recover", pedido.path)
        assertEquals("""{"email":"ana@exemplo.com"}""", pedido.body.readUtf8())
    }

    @Test
    fun `limite de envio vira mensagem`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(429).setBody("""{"error_code":"over_email_send_rate_limit"}"""))

        val r = recuperacao.enviarCodigo("ana@exemplo.com")

        assertEquals(Resultado.Falha("Muitas tentativas. Espere alguns minutos e tente de novo."), r)
    }

    @Test
    fun `codigo valido vira sessao de recuperacao`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"access_token":"tok-rec","refresh_token":"ref-rec","expires_in":3600,
                   "user":{"id":"u1","email":"ana@exemplo.com"}}"""
            )
        )

        val r = recuperacao.verificarCodigo("ana@exemplo.com", " 123456 ")

        assertEquals(Resultado.Ok(Sessao("tok-rec", "ref-rec", 4_600, "u1", "ana@exemplo.com")), r)
        val pedido = servidor.takeRequest()
        assertEquals("/auth/v1/verify", pedido.path)
        assertEquals("""{"type":"recovery","email":"ana@exemplo.com","token":"123456"}""", pedido.body.readUtf8())
    }

    @Test
    fun `codigo vencido vira mensagem`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(403)
                .setBody("""{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}""")
        )

        val r = recuperacao.verificarCodigo("ana@exemplo.com", "000000")

        assertEquals(Resultado.Falha("Código inválido ou vencido. Confira os números ou peça um novo."), r)
    }

    @Test
    fun `troca a senha com o token da recuperacao`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"u1","email":"ana@exemplo.com"}"""))
        val sessao = Sessao("tok-rec", "ref-rec", 4_600, "u1", "ana@exemplo.com")

        val r = recuperacao.trocarSenha(sessao, "senhaNova1")

        assertEquals(Resultado.Ok(Unit), r)
        val pedido = servidor.takeRequest()
        assertEquals("PUT", pedido.method)
        assertEquals("/auth/v1/user", pedido.path)
        assertEquals("Bearer tok-rec", pedido.getHeader("Authorization"))
        assertEquals("""{"password":"senhaNova1"}""", pedido.body.readUtf8())
    }

    @Test
    fun `senha igual a antiga e recusada com mensagem propria`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"error_code":"same_password","msg":"New password should be different from the old password."}""")
        )

        val r = recuperacao.trocarSenha(Sessao("t", "r", 0, "u1", "a@b.com"), "mesmaSenha")

        assertEquals(Resultado.Falha("A nova senha precisa ser diferente da anterior."), r)
    }

    @Test
    fun `servidor que nao responde a tempo vira demora, nao falta de internet`() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        val apiLenta = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(okhttp3.OkHttpClient.Builder().readTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build()
            .create(AuthApi::class.java)

        val r = RecuperacaoDeSenha(apiLenta).enviarCodigo("ana@exemplo.com")

        assertEquals(Resultado.Falha(MensagensAuth.DEMOROU), r)
    }

    @Test
    fun `sem rede vira mensagem e nao estoura`() = runTest {
        servidor.shutdown()

        val r = recuperacao.enviarCodigo("ana@exemplo.com")

        assertTrue(r is Resultado.Falha)
        assertEquals(MensagensAuth.SEM_REDE, (r as Resultado.Falha).mensagem)
    }
}
