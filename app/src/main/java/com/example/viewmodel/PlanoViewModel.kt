package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.ciclo.CicloDeTreinos
import com.example.data.execucao.TreinosRealizadosRepository
import com.example.data.plano.ConfigDoPlano
import com.example.data.plano.PlanoDeTreino
import com.example.data.plano.PlanoDto
import com.example.data.plano.PlanosRepository
import com.example.data.plano.ResumoDoPlano
import com.example.data.plano.SemanaDoPlano
import com.example.data.plano.TreinoDoPlano
import com.example.model.TrainingLevel
import com.example.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EtapaDoPlano { MODO, SEMANAS, VEZES, FOCOS }

/** O passo a passo de "Criar plano de treino". */
data class AssistenteDoPlano(
    val etapa: EtapaDoPlano = EtapaDoPlano.MODO,
    // true = o nadador escolhe cada treino; o app só dá o ponto de partida.
    val manual: Boolean = false,
    val semanas: Int = 8,
    val treinosPorSemana: Int = 3,
    // Vazio = todas.
    val focos: Set<String> = emptySet(),
    val semanasRelaxadas: Boolean = true,
    val encerramentoRelaxado: Boolean = true,
    val nivel: TrainingLevel = TrainingLevel.INTERMEDIARIO,
    val salvando: Boolean = false,
    val erro: String? = null
) {
    fun config() = ConfigDoPlano(
        semanas = semanas,
        treinosPorSemana = treinosPorSemana,
        focos = PlanoDeTreino.FOCOS.filter { it in focos },
        semanasRelaxadas = semanasRelaxadas,
        encerramentoRelaxado = encerramentoRelaxado,
        nivel = nivel
    )
}

data class PlanoUiState(
    val plano: PlanoDto? = null,
    val semanas: List<SemanaDoPlano> = emptyList(),
    // (semana, treino) já concluídos.
    val feitos: Set<Pair<Int, Int>> = emptySet(),
    val carregando: Boolean = false,
    val erro: String? = null,
    val aberto: Boolean = false,
    val assistente: AssistenteDoPlano? = null,
    val confirmarExclusao: Boolean = false,
    val excluindo: Boolean = false,
    val mensagem: String? = null,
    // Trocar um treino: a vaga escolhida e as opções para ela.
    val trocando: TreinoDoPlano? = null,
    val opcoesDaTroca: List<Workout> = emptyList(),
    val meusTreinos: List<Workout> = emptyList(),
    val salvandoTroca: Boolean = false
) {
    val resumo: ResumoDoPlano?
        get() = plano?.let { PlanoDeTreino.resumo(it.config(), semanas, feitos) }
}

/** Plano de treino: criar, acompanhar, trocar treinos e excluir. */
class PlanoViewModel(application: Application) : AndroidViewModel(application) {

    private val treinosNC by lazy {
        CicloDeTreinos.treinosDoJson(application.assets.open("programa_nc.json").bufferedReader().use { it.readText() })
    }

    private val _ui = MutableStateFlow(PlanoUiState())
    val ui: StateFlow<PlanoUiState> = _ui.asStateFlow()

    private var usuario: String? = null

    /** Troca de conta zera tudo: o plano de uma pessoa não pode aparecer para outra. */
    fun definirUsuario(id: String?) {
        if (id == usuario) return
        usuario = id
        _ui.value = PlanoUiState()
        if (id != null) carregar()
    }

    private fun montar(plano: PlanoDto?, meusTreinos: List<Workout>): List<SemanaDoPlano> =
        plano?.let { PlanoDeTreino.montar(it.config(), treinosNC, it.id, meusTreinos) }.orEmpty()

    /** Meus treinos chegaram ou mudaram: as vagas trocadas por eles se atualizam. */
    fun definirMeusTreinos(treinos: List<Workout>) {
        _ui.update { it.copy(meusTreinos = treinos, semanas = montar(it.plano, treinos)) }
    }

    fun carregar() {
        val id = usuario ?: return
        _ui.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            val resultado = PlanosRepository.atual()
            if (id != usuario) return@launch
            when (resultado) {
                is Resultado.Ok -> {
                    _ui.update {
                        it.copy(
                            carregando = false,
                            plano = resultado.valor,
                            semanas = montar(resultado.valor, it.meusTreinos),
                            feitos = emptySet()
                        )
                    }
                    atualizarFeitos()
                }
                is Resultado.Falha -> _ui.update { it.copy(carregando = false, erro = resultado.mensagem) }
            }
        }
    }

    /** Depois de um treino salvo: marca no plano o que foi feito. */
    fun atualizarFeitos() {
        val planoId = _ui.value.plano?.id ?: return
        viewModelScope.launch {
            val lista = (TreinosRealizadosRepository.listar() as? Resultado.Ok)?.valor ?: return@launch
            val feitos = lista
                .filter { it.planoId == planoId && it.planoSemana != null && it.planoTreino != null }
                .map { it.planoSemana!! to it.planoTreino!! }
                .toSet()
            _ui.update { if (it.plano?.id == planoId) it.copy(feitos = feitos) else it }
        }
    }

    /** Cartão da tela inicial ou aba Plano: com plano, abre o plano; sem, começa a criar. */
    fun abrir(nivel: TrainingLevel) {
        if (_ui.value.plano == null) iniciarAssistente(nivel) else _ui.update { it.copy(aberto = true) }
    }

    fun fechar() {
        _ui.update { it.copy(aberto = false, confirmarExclusao = false, trocando = null) }
    }

    fun iniciarAssistente(nivel: TrainingLevel) {
        _ui.update { it.copy(assistente = AssistenteDoPlano(nivel = nivel)) }
    }

    fun alterarAssistente(mudanca: (AssistenteDoPlano) -> AssistenteDoPlano) {
        _ui.update { s ->
            val a = s.assistente ?: return@update s
            if (a.salvando) return@update s
            val novo = mudanca(a)
            s.copy(
                assistente = novo.copy(
                    semanas = novo.semanas.coerceIn(1, PlanoDeTreino.MAX_SEMANAS),
                    treinosPorSemana = novo.treinosPorSemana.coerceIn(1, PlanoDeTreino.MAX_TREINOS_POR_SEMANA),
                    focos = novo.focos.filter { it in PlanoDeTreino.FOCOS }.toSet(),
                    erro = null
                )
            )
        }
    }

    fun avancar() {
        val a = _ui.value.assistente ?: return
        when (a.etapa) {
            EtapaDoPlano.MODO -> alterarAssistente { it.copy(etapa = EtapaDoPlano.SEMANAS) }
            EtapaDoPlano.SEMANAS -> alterarAssistente { it.copy(etapa = EtapaDoPlano.VEZES) }
            EtapaDoPlano.VEZES -> alterarAssistente { it.copy(etapa = EtapaDoPlano.FOCOS) }
            EtapaDoPlano.FOCOS -> criar()
        }
    }

    fun voltarEtapa() {
        val a = _ui.value.assistente ?: return
        when (a.etapa) {
            EtapaDoPlano.MODO -> fecharAssistente()
            EtapaDoPlano.SEMANAS -> alterarAssistente { it.copy(etapa = EtapaDoPlano.MODO) }
            EtapaDoPlano.VEZES -> alterarAssistente { it.copy(etapa = EtapaDoPlano.SEMANAS) }
            EtapaDoPlano.FOCOS -> alterarAssistente { it.copy(etapa = EtapaDoPlano.VEZES) }
        }
    }

    fun fecharAssistente() {
        _ui.update { s -> if (s.assistente?.salvando == true) s else s.copy(assistente = null) }
    }

    private fun criar() {
        val a = _ui.value.assistente ?: return
        if (a.salvando || usuario == null) return
        _ui.update { it.copy(assistente = a.copy(salvando = true, erro = null)) }
        viewModelScope.launch {
            when (val r = PlanosRepository.criar(a.config(), manual = a.manual)) {
                is Resultado.Ok -> _ui.update {
                    it.copy(
                        plano = r.valor,
                        semanas = montar(r.valor, it.meusTreinos),
                        feitos = emptySet(),
                        assistente = null,
                        aberto = true,
                        erro = null,
                        mensagem = if (a.manual) "Plano criado. Toque em Trocar para escolher cada treino." else "Plano criado. Bons treinos!"
                    )
                }
                is Resultado.Falha -> _ui.update { s ->
                    s.copy(assistente = s.assistente?.copy(salvando = false, erro = r.mensagem))
                }
            }
        }
    }

    fun abrirTroca(treino: TreinoDoPlano) {
        val plano = _ui.value.plano ?: return
        _ui.update {
            it.copy(
                trocando = treino,
                opcoesDaTroca = PlanoDeTreino.opcoesDoMetodo(plano.config(), treino.semana, treinosNC)
            )
        }
    }

    fun fecharTroca() {
        _ui.update { s -> if (s.salvandoTroca) s else s.copy(trocando = null) }
    }

    /** [escolha]: PlanoDeTreino.trocaPorFoco / trocaPorMeuTreino, ou nulo para voltar ao sugerido. */
    fun trocar(treino: TreinoDoPlano, escolha: String?) {
        val plano = _ui.value.plano ?: return
        val id = plano.id ?: return
        if (_ui.value.salvandoTroca) return
        val chave = PlanoDeTreino.chave(treino.semana, treino.numero)
        val trocas = plano.trocas.orEmpty().toMutableMap().apply {
            if (escolha == null) remove(chave) else put(chave, escolha)
        }
        _ui.update { it.copy(salvandoTroca = true, erro = null) }
        viewModelScope.launch {
            when (val r = PlanosRepository.trocar(id, trocas)) {
                is Resultado.Ok -> _ui.update {
                    it.copy(
                        plano = r.valor,
                        semanas = montar(r.valor, it.meusTreinos),
                        trocando = null,
                        salvandoTroca = false,
                        mensagem = if (escolha == null) "Voltou o treino sugerido pelo app." else "Treino trocado."
                    )
                }
                is Resultado.Falha -> _ui.update { it.copy(salvandoTroca = false, erro = r.mensagem, trocando = null) }
            }
        }
    }

    fun pedirExclusao() {
        if (_ui.value.plano != null) _ui.update { it.copy(confirmarExclusao = true) }
    }

    fun cancelarExclusao() {
        _ui.update { s -> if (s.excluindo) s else s.copy(confirmarExclusao = false) }
    }

    fun confirmarExclusao() {
        val id = _ui.value.plano?.id ?: return
        if (_ui.value.excluindo) return
        _ui.update { it.copy(excluindo = true) }
        viewModelScope.launch {
            when (val r = PlanosRepository.excluir(id)) {
                is Resultado.Ok -> _ui.update {
                    PlanoUiState(
                        meusTreinos = it.meusTreinos,
                        mensagem = "Plano excluído. Os treinos que você fez continuam no histórico."
                    )
                }
                is Resultado.Falha -> _ui.update {
                    it.copy(excluindo = false, confirmarExclusao = false, erro = r.mensagem)
                }
            }
        }
    }

    fun mensagemMostrada() {
        _ui.update { it.copy(mensagem = null) }
    }
}
