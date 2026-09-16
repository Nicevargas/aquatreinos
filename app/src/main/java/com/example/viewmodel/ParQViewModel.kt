package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.ciclo.DataCivil
import com.example.data.parq.ParQ
import com.example.data.parq.ParQRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParQUiState(
    val aberto: Boolean = false,
    // O formulário foi aberto por "Iniciar treino": depois de enviar, o treino começa.
    val vaiTreinar: Boolean = false,
    val respostas: List<Boolean?> = List(ParQ.PERGUNTAS.size) { null },
    val declaracao: Boolean = false,
    val termo: Boolean = false,
    val verificando: Boolean = false,
    val enviando: Boolean = false,
    val erro: String? = null,
    // Dia (epoch) do PAR-Q mais recente conhecido; nulo = nunca respondeu.
    val ultimoDia: Long? = null
)

/**
 * PAR-Q antes de treinar. [antesDeTreinar] deixa o treino começar só com um
 * PAR-Q dentro da validade; se não houver, abre o questionário e começa o
 * treino depois do envio.
 *
 * O dia do último PAR-Q fica guardado no aparelho por usuário, para "Iniciar
 * treino" não esperar a rede; o Supabase é conferido quando o local não basta.
 */
class ParQViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    private val _ui = MutableStateFlow(ParQUiState())
    val ui: StateFlow<ParQUiState> = _ui.asStateFlow()

    private var usuario: String? = null
    private var depoisDeResponder: (() -> Unit)? = null

    fun definirUsuario(id: String?) {
        if (id == usuario) return
        usuario = id
        depoisDeResponder = null
        _ui.value = ParQUiState(ultimoDia = id?.let { lerDoAparelho(it) })
        if (id != null) {
            viewModelScope.launch {
                (ParQRepository.ultima() as? Resultado.Ok)?.valor?.dia()?.let { guardar(id, it) }
            }
        }
    }

    fun antesDeTreinar(treinar: () -> Unit) {
        val id = usuario
        if (id == null) {
            // O app só abre logado; sem usuário não há onde registrar.
            treinar()
            return
        }
        if (!ParQ.precisaResponder(_ui.value.ultimoDia, DataCivil.hoje())) {
            treinar()
            return
        }
        if (_ui.value.verificando) return

        _ui.update { it.copy(verificando = true) }
        viewModelScope.launch {
            // Pode ter respondido em outro aparelho.
            val remoto = ParQRepository.ultima()
            (remoto as? Resultado.Ok)?.valor?.dia()?.let { guardar(id, it) }
            _ui.update { it.copy(verificando = false) }

            if (!ParQ.precisaResponder(_ui.value.ultimoDia, DataCivil.hoje())) {
                treinar()
            } else {
                depoisDeResponder = treinar
                abrir(erro = (remoto as? Resultado.Falha)?.mensagem)
            }
        }
    }

    /** "Minha saúde mudou": responder de novo sem começar treino. */
    fun responderDeNovo() {
        depoisDeResponder = null
        abrir(erro = null)
    }

    private fun abrir(erro: String?) {
        _ui.update { ParQUiState(aberto = true, vaiTreinar = depoisDeResponder != null, erro = erro, ultimoDia = it.ultimoDia) }
    }

    fun responder(pergunta: Int, sim: Boolean) {
        _ui.update { s ->
            if (pergunta !in s.respostas.indices) return@update s
            s.copy(respostas = s.respostas.toMutableList().also { it[pergunta] = sim }, erro = null)
        }
    }

    fun marcarDeclaracao(marcado: Boolean) {
        _ui.update { it.copy(declaracao = marcado, erro = null) }
    }

    fun marcarTermo(marcado: Boolean) {
        _ui.update { it.copy(termo = marcado, erro = null) }
    }

    fun fechar() {
        if (_ui.value.enviando) return
        depoisDeResponder = null
        _ui.update { ParQUiState(ultimoDia = it.ultimoDia) }
    }

    fun enviar() {
        val estado = _ui.value
        val id = usuario ?: return
        if (estado.enviando || !ParQ.podeEnviar(estado.respostas, estado.declaracao, estado.termo)) return

        _ui.update { it.copy(enviando = true, erro = null) }
        viewModelScope.launch {
            when (val r = ParQRepository.registrar(estado.respostas.map { it == true }, estado.declaracao, estado.termo)) {
                is Resultado.Ok -> {
                    guardar(id, r.valor.dia() ?: DataCivil.hoje())
                    val treinar = depoisDeResponder
                    depoisDeResponder = null
                    _ui.update { ParQUiState(ultimoDia = it.ultimoDia) }
                    treinar?.invoke()
                }
                is Resultado.Falha -> _ui.update { it.copy(enviando = false, erro = r.mensagem) }
            }
        }
    }

    private fun lerDoAparelho(id: String): Long? =
        prefs.getString(CHAVE + id, null)?.let { runCatching { DataCivil.deIso(it) }.getOrNull() }

    private fun guardar(id: String, dia: Long) {
        prefs.edit().putString(CHAVE + id, DataCivil.paraIso(dia)).apply()
        if (id == usuario) _ui.update { it.copy(ultimoDia = dia) }
    }

    companion object {
        // Fora do backup do Android (res/xml/backup_rules.xml): é dado de saúde.
        const val ARQUIVO = "parq_prontidao"
        private const val CHAVE = "ultimo_dia_"
    }
}
