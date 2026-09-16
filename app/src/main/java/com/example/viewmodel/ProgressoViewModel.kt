package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.ciclo.DataCivil
import com.example.data.execucao.TreinosRealizadosRepository
import com.example.data.progresso.PainelDoProgresso
import com.example.data.progresso.Progresso
import com.example.data.progresso.paraAtividade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressoUiState(
    val painel: PainelDoProgresso? = null,
    val carregando: Boolean = false,
    val erro: String? = null
)

/** Histórico, série de semanas, prêmios e esforço de quem está logado. */
class ProgressoViewModel : ViewModel() {

    private val _ui = MutableStateFlow(ProgressoUiState())
    val ui: StateFlow<ProgressoUiState> = _ui.asStateFlow()

    private var usuario: String? = null

    /** Troca de conta zera tudo: o histórico de uma pessoa não pode aparecer para outra. */
    fun definirUsuario(id: String?) {
        if (id == usuario) return
        usuario = id
        _ui.value = ProgressoUiState()
        if (id != null) carregar()
    }

    fun carregar() {
        val id = usuario ?: return
        if (_ui.value.carregando) return
        _ui.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            val resultado = TreinosRealizadosRepository.listar()
            if (id != usuario) return@launch
            _ui.update { s ->
                when (resultado) {
                    is Resultado.Ok -> s.copy(
                        carregando = false,
                        painel = Progresso.painel(resultado.valor.mapNotNull { it.paraAtividade() }, DataCivil.hoje())
                    )
                    is Resultado.Falha -> s.copy(carregando = false, erro = resultado.mensagem)
                }
            }
        }
    }
}
