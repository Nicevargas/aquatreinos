package com.example.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sessão salva no aparelho, para não pedir login toda vez que o app abre.
 *
 * Fica em SharedPreferences privado do app; o arquivo está fora do backup na
 * nuvem e da transferência entre aparelhos (res/xml/backup_rules.xml e
 * data_extraction_rules.xml), para o token não sair do celular.
 */
class SessaoStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
    private val _sessao = MutableStateFlow(ler())
    val sessao: StateFlow<Sessao?> = _sessao.asStateFlow()

    fun atual(): Sessao? = _sessao.value

    @Synchronized
    fun salvar(sessao: Sessao) {
        prefs.edit()
            .putString(ACESSO, sessao.accessToken)
            .putString(RENOVACAO, sessao.refreshToken)
            .putLong(EXPIRA_EM, sessao.expiraEm)
            .putString(USUARIO, sessao.userId)
            .putString(EMAIL, sessao.email)
            .apply()
        _sessao.value = sessao
    }

    @Synchronized
    fun limpar() {
        prefs.edit().clear().apply()
        _sessao.value = null
    }

    private fun ler(): Sessao? {
        val acesso = prefs.getString(ACESSO, null) ?: return null
        val renovacao = prefs.getString(RENOVACAO, null) ?: return null
        val usuario = prefs.getString(USUARIO, null) ?: return null
        return Sessao(
            accessToken = acesso,
            refreshToken = renovacao,
            expiraEm = prefs.getLong(EXPIRA_EM, 0L),
            userId = usuario,
            email = prefs.getString(EMAIL, null).orEmpty()
        )
    }

    companion object {
        const val ARQUIVO = "aquagenda_sessao"
        private const val ACESSO = "access_token"
        private const val RENOVACAO = "refresh_token"
        private const val EXPIRA_EM = "expira_em"
        private const val USUARIO = "user_id"
        private const val EMAIL = "email"
    }
}
