package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.ciclo.DataCivil
import com.example.data.treinos.MeusTreinosRepository
import com.example.data.treinos.Montagem
import com.example.data.treinos.MontadorDeTreino
import com.example.data.treinos.SerieDigitada
import com.example.data.treinos.TreinoDigitado
import com.example.model.TrainingLevel
import com.example.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulário aberto. id nulo = treino novo. */
data class EditorDeTreino(
    val id: String? = null,
    val digitado: TreinoDigitado,
    val erros: List<String> = emptyList(),
    val salvando: Boolean = false
)

data class MeusTreinosUiState(
    val treinos: List<Workout> = emptyList(),
    val carregando: Boolean = false,
    val erro: String? = null,
    val editor: EditorDeTreino? = null,
    val paraExcluir: Workout? = null,
    val excluindo: Boolean = false,
    val mensagem: String? = null
)

/** CRUD de "Meus treinos". */
class MeusTreinosViewModel : ViewModel() {

    private val _ui = MutableStateFlow(MeusTreinosUiState())
    val ui: StateFlow<MeusTreinosUiState> = _ui.asStateFlow()

    private var userId: String? = null

    /** Troca de conta zera tudo: a lista de uma pessoa não pode aparecer para outra. */
    fun definirUsuario(id: String?) {
        if (id == userId) return
        userId = id
        _ui.value = MeusTreinosUiState()
        if (id != null) carregar()
    }

    fun carregar() {
        val usuario = userId ?: return
        _ui.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            val resultado = MeusTreinosRepository.listar(usuario)
            if (usuario != userId) return@launch
            _ui.update { s ->
                when (resultado) {
                    is Resultado.Ok -> s.copy(carregando = false, treinos = resultado.valor)
                    is Resultado.Falha -> s.copy(carregando = false, erro = resultado.mensagem)
                }
            }
        }
    }

    fun novo(epochDay: Long, nivel: TrainingLevel) {
        val digitado = TreinoDigitado(
            data = DataCivil.paraBr(epochDay),
            level = nivel,
            fases = MontadorDeTreino.FASES.associateWith { listOf(SerieDigitada()) }
        )
        _ui.update { it.copy(editor = EditorDeTreino(digitado = digitado)) }
    }

    /** Abre o formulário já preenchido com a sugestão do dia, para salvar como treino seu. */
    fun salvarSugestao(sugestao: Workout) {
        _ui.update { it.copy(editor = EditorDeTreino(digitado = MontadorDeTreino.paraDigitacao(sugestao))) }
    }

    fun editar(treino: Workout) {
        _ui.update { it.copy(editor = EditorDeTreino(id = treino.id, digitado = MontadorDeTreino.paraDigitacao(treino))) }
    }

    fun alterarDigitado(digitado: TreinoDigitado) {
        _ui.update { s -> s.copy(editor = s.editor?.copy(digitado = digitado, erros = emptyList())) }
    }

    fun fecharEditor() {
        _ui.update { s -> if (s.editor?.salvando == true) s else s.copy(editor = null) }
    }

    fun salvar() {
        val editor = _ui.value.editor ?: return
        if (editor.salvando) return
        val treino = when (val montagem = MontadorDeTreino.montar(editor.digitado)) {
            is Montagem.ComErros -> {
                _ui.update { it.copy(editor = editor.copy(erros = montagem.erros)) }
                return
            }
            is Montagem.Pronto -> montagem.treino
        }

        _ui.update { it.copy(editor = editor.copy(salvando = true, erros = emptyList())) }
        viewModelScope.launch {
            val resultado = if (editor.id == null) {
                MeusTreinosRepository.criar(treino)
            } else {
                MeusTreinosRepository.atualizar(editor.id, treino)
            }
            _ui.update { s ->
                when (resultado) {
                    is Resultado.Ok -> s.copy(
                        treinos = (s.treinos.filterNot { it.id == resultado.valor.id } + resultado.valor)
                            .sortedByDescending { it.workoutDate },
                        editor = null,
                        mensagem = if (editor.id == null) "Treino salvo em Meus treinos." else "Treino atualizado."
                    )
                    is Resultado.Falha -> s.copy(
                        editor = s.editor?.copy(salvando = false, erros = listOf(resultado.mensagem))
                    )
                }
            }
        }
    }

    fun pedirExclusao(treino: Workout) {
        _ui.update { it.copy(paraExcluir = treino) }
    }

    fun cancelarExclusao() {
        _ui.update { s -> if (s.excluindo) s else s.copy(paraExcluir = null) }
    }

    fun confirmarExclusao() {
        val alvo = _ui.value.paraExcluir ?: return
        if (_ui.value.excluindo) return
        _ui.update { it.copy(excluindo = true) }
        viewModelScope.launch {
            val resultado = MeusTreinosRepository.excluir(alvo.id)
            _ui.update { s ->
                when (resultado) {
                    is Resultado.Ok -> s.copy(
                        treinos = s.treinos.filterNot { it.id == alvo.id },
                        paraExcluir = null,
                        excluindo = false,
                        mensagem = "Treino excluído."
                    )
                    is Resultado.Falha -> s.copy(paraExcluir = null, excluindo = false, erro = resultado.mensagem)
                }
            }
        }
    }

    fun mensagemMostrada() {
        _ui.update { it.copy(mensagem = null) }
    }
}
