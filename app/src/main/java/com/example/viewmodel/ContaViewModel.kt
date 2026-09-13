package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.auth.AuthRepository
import com.example.data.auth.MensagensAuth
import com.example.data.auth.ResultadoAuth
import com.example.data.auth.Sessao
import com.example.data.conta.Perfil
import com.example.data.conta.PerfilRepository
import com.example.data.supabase.SupabaseClient
import com.example.model.TrainingLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EtapaRecuperacao { EMAIL, CODIGO }

data class RecuperacaoUiState(
    val etapa: EtapaRecuperacao = EtapaRecuperacao.EMAIL,
    val email: String = "",
    val enviando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
    // Código já trocado por sessão: falta só uma senha que o Supabase aceite.
    val codigoValidado: Boolean = false,
    // O Supabase recusa novo envio antes de 60 s.
    val reenviarLiberadoEm: Long = 0L
)

data class ContaUiState(
    val configurado: Boolean = SupabaseClient.isConfigured,
    val sessao: Sessao? = null,
    val enviando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
    val perfil: Perfil? = null,
    val carregandoPerfil: Boolean = false,
    val salvandoPerfil: Boolean = false,
    val excluindoConta: Boolean = false,
    val recuperacao: RecuperacaoUiState? = null
)

/** Login, cadastro, esqueci minha senha, sair, e o CRUD do próprio perfil. */
class ContaViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(ContaUiState())
    val ui: StateFlow<ContaUiState> = _ui.asStateFlow()

    // Fica só na memória: se o app fechar, o código já gasto não volta.
    private var sessaoDeRecuperacao: Sessao? = null

    init {
        AuthRepository.init(application)
        viewModelScope.launch {
            AuthRepository.sessao.collect { sessao ->
                // Renovar o token emite uma sessão nova da MESMA conta: não recarrega o perfil.
                val outraConta = sessao?.userId != _ui.value.sessao?.userId
                _ui.update { it.copy(sessao = sessao, perfil = if (outraConta) null else it.perfil) }
                if (sessao != null && outraConta) carregarPerfil()
            }
        }
    }

    fun entrar(email: String, senha: String) {
        val invalido = MensagensAuth.validarEmail(email) ?: MensagensAuth.validarSenha(senha)
        if (invalido != null) {
            _ui.update { it.copy(erro = invalido, aviso = null) }
            return
        }
        enviar(email) { AuthRepository.entrar(email, senha) }
    }

    fun cadastrar(nome: String, email: String, senha: String, nivel: TrainingLevel) {
        val invalido = MensagensAuth.validarNome(nome)
            ?: MensagensAuth.validarEmail(email)
            ?: MensagensAuth.validarSenha(senha)
        if (invalido != null) {
            _ui.update { it.copy(erro = invalido, aviso = null) }
            return
        }
        enviar(email) { AuthRepository.cadastrar(nome, email, senha, nivel) }
    }

    private fun enviar(email: String, acao: suspend () -> ResultadoAuth) {
        if (_ui.value.enviando) return
        _ui.update { it.copy(enviando = true, erro = null, aviso = null) }
        viewModelScope.launch {
            val resultado = acao()
            _ui.update { s ->
                when (resultado) {
                    ResultadoAuth.Entrou -> s.copy(enviando = false)
                    ResultadoAuth.ConfirmarEmail -> s.copy(
                        enviando = false,
                        aviso = "Conta criada! Enviamos um link para ${email.trim()}. Confirme o e-mail e depois entre."
                    )
                    is ResultadoAuth.Erro -> s.copy(enviando = false, erro = resultado.mensagem)
                }
            }
        }
    }

    fun limparMensagens() {
        _ui.update { it.copy(erro = null, aviso = null) }
    }

    // ---- Esqueci minha senha ----

    fun abrirRecuperacao(emailDigitado: String) {
        sessaoDeRecuperacao = null
        _ui.update {
            it.copy(erro = null, aviso = null, recuperacao = RecuperacaoUiState(email = emailDigitado.trim()))
        }
    }

    fun fecharRecuperacao() {
        sessaoDeRecuperacao = null
        _ui.update { it.copy(recuperacao = null) }
    }

    fun trocarEmailDaRecuperacao() {
        sessaoDeRecuperacao = null
        atualizarRecuperacao { it.copy(etapa = EtapaRecuperacao.EMAIL, erro = null, aviso = null, codigoValidado = false) }
    }

    fun enviarCodigo(email: String, agoraMillis: Long = System.currentTimeMillis()) {
        val atual = _ui.value.recuperacao ?: return
        if (atual.enviando) return
        if (atual.etapa == EtapaRecuperacao.CODIGO && agoraMillis < atual.reenviarLiberadoEm) return
        MensagensAuth.validarEmail(email)?.let { invalido ->
            atualizarRecuperacao { it.copy(erro = invalido, aviso = null) }
            return
        }

        sessaoDeRecuperacao = null
        atualizarRecuperacao { it.copy(enviando = true, erro = null, aviso = null) }
        viewModelScope.launch {
            when (val r = AuthRepository.enviarCodigoDeRecuperacao(email)) {
                is Resultado.Ok -> atualizarRecuperacao {
                    it.copy(
                        etapa = EtapaRecuperacao.CODIGO,
                        email = email.trim(),
                        enviando = false,
                        codigoValidado = false,
                        // O Supabase não conta se o e-mail tem conta; a tela também não.
                        aviso = "Se houver uma conta com ${email.trim()}, o código chega em instantes. Confira também o spam.",
                        reenviarLiberadoEm = System.currentTimeMillis() + 60_000
                    )
                }
                is Resultado.Falha -> atualizarRecuperacao { it.copy(enviando = false, erro = r.mensagem) }
            }
        }
    }

    fun redefinirSenha(codigo: String, novaSenha: String, confirmacao: String) {
        val atual = _ui.value.recuperacao ?: return
        if (atual.enviando) return
        val invalido = (if (sessaoDeRecuperacao == null) MensagensAuth.validarCodigo(codigo) else null)
            ?: MensagensAuth.validarSenha(novaSenha)
            ?: MensagensAuth.validarConfirmacao(novaSenha, confirmacao)
        if (invalido != null) {
            atualizarRecuperacao { it.copy(erro = invalido, aviso = null) }
            return
        }

        atualizarRecuperacao { it.copy(enviando = true, erro = null, aviso = null) }
        viewModelScope.launch {
            val sessao = sessaoDeRecuperacao ?: when (val v = AuthRepository.verificarCodigoDeRecuperacao(atual.email, codigo)) {
                is Resultado.Ok -> v.valor.also {
                    sessaoDeRecuperacao = it
                    atualizarRecuperacao { s -> s.copy(codigoValidado = true) }
                }
                is Resultado.Falha -> {
                    atualizarRecuperacao { it.copy(enviando = false, erro = v.mensagem) }
                    return@launch
                }
            }

            when (val t = AuthRepository.definirNovaSenha(sessao, novaSenha)) {
                is Resultado.Ok -> {
                    sessaoDeRecuperacao = null
                    _ui.update { it.copy(recuperacao = null, aviso = "Senha alterada.") }
                }
                // O código já foi gasto, mas a sessão de recuperação continua guardada.
                is Resultado.Falha -> atualizarRecuperacao { it.copy(enviando = false, erro = t.mensagem) }
            }
        }
    }

    private fun atualizarRecuperacao(transformar: (RecuperacaoUiState) -> RecuperacaoUiState) {
        _ui.update { s -> s.recuperacao?.let { s.copy(recuperacao = transformar(it)) } ?: s }
    }

    // ---- Perfil ----

    fun carregarPerfil() {
        val sessao = _ui.value.sessao ?: return
        _ui.update { it.copy(carregandoPerfil = true, erro = null) }
        viewModelScope.launch {
            val resultado = PerfilRepository.carregar(sessao)
            _ui.update { s ->
                if (s.sessao?.userId != sessao.userId) return@update s
                when (resultado) {
                    is Resultado.Ok -> s.copy(carregandoPerfil = false, perfil = resultado.valor)
                    is Resultado.Falha -> s.copy(carregandoPerfil = false, erro = resultado.mensagem)
                }
            }
        }
    }

    fun salvarPerfil(nome: String, piscinaMetros: Int, nivel: TrainingLevel) {
        val sessao = _ui.value.sessao ?: return
        MensagensAuth.validarNome(nome)?.let { invalido ->
            _ui.update { it.copy(erro = invalido, aviso = null) }
            return
        }
        val base = _ui.value.perfil
            ?: Perfil(sessao.userId, sessao.email, "", 25, TrainingLevel.INTERMEDIARIO)
        _ui.update { it.copy(salvandoPerfil = true, erro = null, aviso = null) }
        viewModelScope.launch {
            val resultado = PerfilRepository.salvar(
                sessao,
                base.copy(nome = nome.trim(), piscinaMetros = piscinaMetros, nivel = nivel)
            )
            _ui.update { s ->
                when (resultado) {
                    is Resultado.Ok -> s.copy(salvandoPerfil = false, perfil = resultado.valor, aviso = "Perfil salvo.")
                    is Resultado.Falha -> s.copy(salvandoPerfil = false, erro = resultado.mensagem)
                }
            }
        }
    }

    fun sair() {
        viewModelScope.launch {
            AuthRepository.sair()
            _ui.update { it.copy(erro = null, aviso = null) }
        }
    }

    fun excluirConta() {
        if (_ui.value.excluindoConta) return
        _ui.update { it.copy(excluindoConta = true, erro = null, aviso = null) }
        viewModelScope.launch {
            when (val resultado = PerfilRepository.excluirConta()) {
                is Resultado.Ok -> {
                    AuthRepository.encerrarNoAparelho()
                    _ui.update { it.copy(excluindoConta = false, aviso = "Sua conta foi excluída.") }
                }
                is Resultado.Falha -> _ui.update { it.copy(excluindoConta = false, erro = resultado.mensagem) }
            }
        }
    }
}
