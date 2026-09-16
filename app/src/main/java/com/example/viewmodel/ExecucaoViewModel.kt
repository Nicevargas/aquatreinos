package com.example.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.ciclo.DataCivil
import com.example.data.compartilhar.ResumoDoTreino
import com.example.data.execucao.CronometroDeTreino
import com.example.data.execucao.ProgressoExecucao
import com.example.data.execucao.RegistroDoTreino
import com.example.data.execucao.RoteiroDeTreino
import com.example.data.execucao.TreinoRealizadoDto
import com.example.data.treinos.MeusTreinosRepository
import com.example.data.treinos.paraGravacao
import com.example.data.execucao.TreinosRealizadosRepository
import com.example.data.progresso.Conquista
import com.example.data.progresso.Pontuacao
import com.example.data.progresso.Progresso
import com.example.data.progresso.SerieDeSemanas
import com.example.data.progresso.paraAtividade
import com.example.model.Workout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EtapaExecucao { EXECUTANDO, RESUMO, PUBLICAR }

data class ExecucaoUiState(
    val ativo: Boolean = false,
    val roteiro: RoteiroDeTreino? = null,
    val progresso: ProgressoExecucao = ProgressoExecucao(),
    val decorridoSegundos: Long = 0L,
    val rodando: Boolean = false,
    val etapa: EtapaExecucao = EtapaExecucao.EXECUTANDO,
    // Nulo = a pessoa não deu nota; não gravar um 5 inventado.
    val intensidade: Int? = null,
    val complexidade: Int? = null,
    val observacao: String = "",
    val salvando: Boolean = false,
    val erro: String? = null,
    val resumo: ResumoDoTreino? = null,
    // Depois de salvar: prêmios que este treino liberou e a série de semanas.
    val conquistasNovas: List<Conquista> = emptyList(),
    val serie: SerieDeSemanas? = null,
    val estendeuSerie: Boolean = false,
    // Gamificação: pontos que este treino somou e o nível com eles.
    val pontosGanhos: Int? = null,
    val nivelDePontos: String? = null,
    // Treino ajustado ou recebido: se entrou (ou não) em Meus treinos ao salvar.
    val avisoAoSalvar: String? = null,
    val avisoEhErro: Boolean = false
)

/** O que a tela de treino salvo comemora. */
private data class Celebracao(
    val conquistas: List<Conquista>,
    val serie: SerieDeSemanas,
    val estendeuSerie: Boolean,
    val pontos: Int,
    val nivel: String
)

/** Execução ao vivo do treino escolhido, "Concluir treino" e o resumo para publicar. */
class ExecucaoViewModel(
    private val relogio: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {

    private val _ui = MutableStateFlow(ExecucaoUiState())
    val ui: StateFlow<ExecucaoUiState> = _ui.asStateFlow()

    private var cronometro = CronometroDeTreino()
    private var ticker: Job? = null

    fun iniciar(workout: Workout) {
        cronometro = CronometroDeTreino().iniciar(relogio())
        _ui.value = ExecucaoUiState(ativo = true, roteiro = RoteiroDeTreino(workout), rodando = true)
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                atualizarTempo()
                delay(250)
            }
        }
    }

    private fun atualizarTempo() {
        _ui.update { it.copy(decorridoSegundos = cronometro.decorridoMs(relogio()) / 1000, rodando = cronometro.rodando) }
    }

    fun alternarPausa() {
        cronometro = if (cronometro.rodando) cronometro.pausar(relogio()) else cronometro.iniciar(relogio())
        atualizarTempo()
    }

    fun avancar() {
        _ui.update { s -> s.roteiro?.let { s.copy(progresso = it.avancar(s.progresso)) } ?: s }
    }

    fun voltar() {
        _ui.update { s -> s.roteiro?.let { s.copy(progresso = it.voltar(s.progresso)) } ?: s }
    }

    /** Vai para o resumo e para o relógio; dá para voltar ao treino. */
    fun irParaResumo() {
        cronometro = cronometro.pausar(relogio())
        atualizarTempo()
        _ui.update { it.copy(etapa = EtapaExecucao.RESUMO, erro = null) }
    }

    fun voltarAoTreino() {
        if (_ui.value.salvando) return
        cronometro = cronometro.iniciar(relogio())
        _ui.update { it.copy(etapa = EtapaExecucao.EXECUTANDO, erro = null) }
        atualizarTempo()
    }

    fun definirIntensidade(nota: Int) {
        _ui.update { it.copy(intensidade = nota.coerceIn(0, 10)) }
    }

    fun definirComplexidade(nota: Int) {
        _ui.update { it.copy(complexidade = nota.coerceIn(0, 10)) }
    }

    fun definirObservacao(texto: String) {
        _ui.update { it.copy(observacao = texto.take(500)) }
    }

    fun salvar(dataIso: String = DataCivil.paraIso(DataCivil.hoje())) {
        val estado = _ui.value
        val roteiro = estado.roteiro ?: return
        if (estado.salvando) return

        val registro = RegistroDoTreino.montar(
            roteiro = roteiro,
            progresso = estado.progresso,
            duracaoSegundos = cronometro.decorridoMs(relogio()) / 1000,
            intensidade = estado.intensidade,
            complexidade = estado.complexidade,
            observacao = estado.observacao,
            dataIso = dataIso
        )
        if (registro.metrosFeitos == 0) {
            _ui.update { it.copy(erro = "Nenhuma série foi marcada como feita. Volte ao treino ou saia sem salvar.") }
            return
        }
        // A percepção de esforço é o que mostra ao professor o quanto o treino pesou.
        if (registro.intensidade == null) {
            _ui.update { it.copy(erro = "Marque sua percepção de esforço, de 0 a 10, para salvar o treino.") }
            return
        }

        _ui.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            // Treino ajustado na hora ou recebido: entra em Meus treinos agora que foi concluído.
            var registroFinal = registro
            var aviso: Pair<String, Boolean>? = null
            if (roteiro.workout.salvarAoConcluir) {
                when (val salvo = MeusTreinosRepository.criar(roteiro.workout.paraGravacao(dataIso))) {
                    is Resultado.Ok -> {
                        registroFinal = registro.copy(workoutId = salvo.valor.id, treinoCicloId = null)
                        // Se o registro falhar e a pessoa tentar de novo, não salva duas vezes.
                        val salvoNoRoteiro = RoteiroDeTreino(roteiro.workout.copy(id = salvo.valor.id, salvarAoConcluir = false))
                        _ui.update { it.copy(roteiro = salvoNoRoteiro) }
                        aviso = "Este treino foi salvo em Meus treinos." to false
                    }
                    is Resultado.Falha -> aviso = "O treino foi registrado, mas não entrou em Meus treinos: ${salvo.mensagem}" to true
                }
            }
            when (val r = TreinosRealizadosRepository.registrar(registroFinal)) {
                is Resultado.Ok -> {
                    val celebracao = celebracao(r.valor)
                    _ui.update {
                        it.copy(
                            salvando = false,
                            etapa = EtapaExecucao.PUBLICAR,
                            resumo = RegistroDoTreino.resumo(r.valor, roteiro),
                            conquistasNovas = celebracao?.conquistas.orEmpty(),
                            serie = celebracao?.serie,
                            estendeuSerie = celebracao?.estendeuSerie ?: false,
                            pontosGanhos = celebracao?.pontos,
                            nivelDePontos = celebracao?.nivel,
                            avisoAoSalvar = aviso?.first,
                            avisoEhErro = aviso?.second ?: false
                        )
                    }
                }
                is Resultado.Falha -> _ui.update { it.copy(salvando = false, erro = r.mensagem) }
            }
        }
    }

    /**
     * Prêmios novos, série e se ela cresceu, comparando o histórico com e sem o
     * treino salvo. Sem conseguir ler o histórico, a tela de treino salvo aparece
     * igual, só sem essa parte.
     */
    private suspend fun celebracao(salvo: TreinoRealizadoDto): Celebracao? {
        val lista = (TreinosRealizadosRepository.listar() as? Resultado.Ok)?.valor ?: return null
        val antes = lista.filter { it.id != salvo.id }.mapNotNull { it.paraAtividade() }
        val depois = antes + listOfNotNull(salvo.paraAtividade())
        val hoje = DataCivil.hoje()
        val serieAntes = Progresso.serieDeSemanas(antes.map { it.dia }, hoje)
        val serie = Progresso.serieDeSemanas(depois.map { it.dia }, hoje)
        val pontosDepois = Pontuacao.total(depois)
        return Celebracao(
            conquistas = Progresso.novasConquistas(antes, depois),
            serie = serie,
            estendeuSerie = serie.semanas > serieAntes.semanas,
            pontos = pontosDepois - Pontuacao.total(antes),
            nivel = Pontuacao.nivel(pontosDepois).nome
        )
    }

    /** Fecha a execução: depois de publicar, ou saindo sem salvar. */
    fun encerrar() {
        ticker?.cancel()
        ticker = null
        cronometro = CronometroDeTreino()
        _ui.value = ExecucaoUiState()
    }

    override fun onCleared() {
        super.onCleared()
        ticker?.cancel()
    }
}
