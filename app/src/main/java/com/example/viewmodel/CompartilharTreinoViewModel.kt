package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.compartilhar.CodigoDoTreino
import com.example.data.compartilhar.MensagemDoTreino
import com.example.data.compartilhar.TreinosCompartilhadosRepository
import com.example.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompartilharTreinoUiState(
    val gerando: Boolean = false,
    val abrindo: Boolean = false,
    // Mensagem pronta para a janela de compartilhar do Android (a tela consome e limpa).
    val textoParaEnviar: String? = null,
    // Treino aberto pelo link ou código (a tela consome e limpa).
    val treinoRecebido: Workout? = null,
    val mensagem: String? = null
)

/** Compartilhar um treino por link e abrir o treino que chegou por link ou código. */
class CompartilharTreinoViewModel : ViewModel() {

    private val _ui = MutableStateFlow(CompartilharTreinoUiState())
    val ui: StateFlow<CompartilharTreinoUiState> = _ui.asStateFlow()

    fun compartilhar(treino: Workout) {
        if (_ui.value.gerando) return
        _ui.update { it.copy(gerando = true) }
        viewModelScope.launch {
            when (val r = TreinosCompartilhadosRepository.compartilhar(treino)) {
                is Resultado.Ok -> _ui.update {
                    it.copy(gerando = false, textoParaEnviar = MensagemDoTreino.paraCompartilhar(treino, r.valor))
                }
                is Resultado.Falha -> _ui.update { it.copy(gerando = false, mensagem = r.mensagem) }
            }
        }
    }

    fun textoEnviado() {
        _ui.update { it.copy(textoParaEnviar = null) }
    }

    /** [linkOuCodigo]: o link, a mensagem colada inteira ou só o código. */
    fun abrir(linkOuCodigo: String) {
        val codigo = CodigoDoTreino.extrair(linkOuCodigo)
        if (codigo == null) {
            _ui.update { it.copy(mensagem = "Não achei o código do treino. Cole o link ou os 10 caracteres do código.") }
            return
        }
        if (_ui.value.abrindo) return
        _ui.update { it.copy(abrindo = true) }
        viewModelScope.launch {
            when (val r = TreinosCompartilhadosRepository.abrir(codigo)) {
                is Resultado.Ok -> _ui.update {
                    it.copy(
                        abrindo = false,
                        treinoRecebido = r.valor,
                        mensagem = "Treino recebido. Ele vai para Meus treinos quando você concluir."
                    )
                }
                is Resultado.Falha -> _ui.update { it.copy(abrindo = false, mensagem = r.mensagem) }
            }
        }
    }

    fun treinoRecebidoUsado() {
        _ui.update { it.copy(treinoRecebido = null) }
    }

    fun mensagemMostrada() {
        _ui.update { it.copy(mensagem = null) }
    }
}
