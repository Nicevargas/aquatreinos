package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.Resultado
import com.example.data.supabase.SupabaseClient
import com.example.model.TrainingLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

object AuthRepository {
    private const val TAG = "AuthRepository"

    /** Chamar uma vez, antes de qualquer chamada ao Supabase (MainActivity.onCreate). */
    fun init(context: Context) {
        if (SupabaseClient.sessaoStore == null) {
            synchronized(this) {
                if (SupabaseClient.sessaoStore == null) {
                    SupabaseClient.sessaoStore = SessaoStore(context)
                }
            }
        }
    }

    private fun store(): SessaoStore =
        checkNotNull(SupabaseClient.sessaoStore) { "AuthRepository.init(context) não foi chamado" }

    val sessao: StateFlow<Sessao?>
        get() = store().sessao

    private fun agora(): Long = System.currentTimeMillis() / 1000

    suspend fun entrar(email: String, senha: String): ResultadoAuth = withContext(Dispatchers.IO) {
        val api = SupabaseClient.authApi ?: return@withContext ResultadoAuth.Erro(MensagensAuth.SEM_CONFIGURACAO)
        try {
            val r = api.signIn(PasswordGrantBody(email.trim().lowercase(), senha))
            val nova = r.body()?.paraSessao(agora())
            if (r.isSuccessful && nova != null) {
                store().salvar(nova)
                ResultadoAuth.Entrou
            } else {
                ResultadoAuth.Erro(MensagensAuth.deErroDeAuth(r.code(), r.errorBody()?.string()))
            }
        } catch (e: IOException) {
            ResultadoAuth.Erro(MensagensAuth.SEM_REDE)
        } catch (e: Exception) {
            Log.e(TAG, "Falha inesperada no login", e)
            ResultadoAuth.Erro("Não foi possível entrar. Tente de novo.")
        }
    }

    suspend fun cadastrar(nome: String, email: String, senha: String, nivel: TrainingLevel): ResultadoAuth =
        withContext(Dispatchers.IO) {
            val api = SupabaseClient.authApi ?: return@withContext ResultadoAuth.Erro(MensagensAuth.SEM_CONFIGURACAO)
            try {
                val corpo = SignUpBody(
                    email = email.trim().lowercase(),
                    password = senha,
                    // Vira raw_user_meta_data; o gatilho handle_new_user cria o perfil com isso.
                    data = mapOf(
                        "full_name" to nome.trim(),
                        "training_level" to nivel.name
                    )
                )
                val r = api.signUp(corpo)
                if (!r.isSuccessful) {
                    return@withContext ResultadoAuth.Erro(MensagensAuth.deErroDeAuth(r.code(), r.errorBody()?.string()))
                }
                val resposta = r.body()
                val nova = resposta?.paraSessao(agora())
                if (nova != null) {
                    store().salvar(nova)
                    return@withContext ResultadoAuth.Entrou
                }
                // Sem sessão: o projeto exige confirmar o e-mail. Com a confirmação
                // ligada, e-mail já cadastrado responde "sucesso" sem identities.
                val identidades = resposta?.identities ?: resposta?.user?.identities
                if (identidades != null && identidades.isEmpty()) {
                    ResultadoAuth.Erro("Já existe uma conta com este e-mail. Entre com ela.")
                } else {
                    ResultadoAuth.ConfirmarEmail
                }
            } catch (e: IOException) {
                ResultadoAuth.Erro(MensagensAuth.SEM_REDE)
            } catch (e: Exception) {
                Log.e(TAG, "Falha inesperada no cadastro", e)
                ResultadoAuth.Erro("Não foi possível criar a conta. Tente de novo.")
            }
        }

    // ---- Esqueci minha senha ----

    private fun recuperacao(): RecuperacaoDeSenha? = SupabaseClient.authApi?.let { RecuperacaoDeSenha(it) }

    suspend fun enviarCodigoDeRecuperacao(email: String): Resultado<Unit> =
        recuperacao()?.enviarCodigo(email) ?: Resultado.Falha(MensagensAuth.SEM_CONFIGURACAO)

    /** A sessão devolvida NÃO é salva: só vale depois que a senha nova for gravada. */
    suspend fun verificarCodigoDeRecuperacao(email: String, codigo: String): Resultado<Sessao> =
        recuperacao()?.verificarCodigo(email, codigo) ?: Resultado.Falha(MensagensAuth.SEM_CONFIGURACAO)

    /** Grava a senha nova e, dando certo, entra na conta com a sessão de recuperação. */
    suspend fun definirNovaSenha(sessao: Sessao, novaSenha: String): Resultado<Unit> {
        val resultado = recuperacao()?.trocarSenha(sessao, novaSenha)
            ?: Resultado.Falha(MensagensAuth.SEM_CONFIGURACAO)
        if (resultado is Resultado.Ok) store().salvar(sessao)
        return resultado
    }

    /** Encerra no servidor quando der; no aparelho, sempre. */
    suspend fun sair() {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.authApi?.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "Logout no servidor falhou; encerrando só no aparelho", e)
            }
        }
        store().limpar()
    }

    /** Depois de excluir a conta, o token já não vale nada: só limpa o aparelho. */
    fun encerrarNoAparelho() {
        store().limpar()
    }
}
