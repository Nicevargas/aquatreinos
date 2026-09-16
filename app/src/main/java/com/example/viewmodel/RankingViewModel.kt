package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Resultado
import com.example.data.ciclo.DataCivil
import com.example.data.ranking.LinhaDoRanking
import com.example.data.ranking.OpcoesDoRanking
import com.example.data.ranking.ParametrosDoRanking
import com.example.data.ranking.ParticipacaoDto
import com.example.data.ranking.PeriodoDoRanking
import com.example.data.ranking.RankingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FiltrosDoRanking(
    val periodo: PeriodoDoRanking = PeriodoDoRanking.MES,
    val faixa: String? = null,
    val sexo: String? = null,
    val horario: String? = null,
    val soMinhaCidade: Boolean = false,
    val soMeuLocal: Boolean = false
)

/** Formulário "Participar do ranking". O ano fica como texto enquanto se digita. */
data class FormularioDoRanking(
    val nome: String = "",
    val ano: String = "",
    val sexo: String? = null,
    val cidade: String = "",
    val local: String = "",
    val aceite: Boolean = false,
    val erro: String? = null,
    val salvando: Boolean = false
)

data class RankingUiState(
    val aberto: Boolean = false,
    val participacao: ParticipacaoDto? = null,
    val formulario: FormularioDoRanking? = null,
    val filtros: FiltrosDoRanking = FiltrosDoRanking(),
    val linhas: List<LinhaDoRanking> = emptyList(),
    val carregando: Boolean = false,
    val erro: String? = null,
    val mensagem: String? = null
)

class RankingViewModel : ViewModel() {

    private val _ui = MutableStateFlow(RankingUiState())
    val ui: StateFlow<RankingUiState> = _ui.asStateFlow()

    private var usuario: String? = null
    private var busca: Job? = null

    fun definirUsuario(id: String?) {
        if (id == usuario) return
        usuario = id
        busca?.cancel()
        _ui.value = RankingUiState()
    }

    fun abrir() {
        _ui.update { it.copy(aberto = true, erro = null) }
        val id = usuario ?: return
        viewModelScope.launch {
            (RankingRepository.participacao(id) as? Resultado.Ok)?.valor?.let { p ->
                if (id == usuario) _ui.update { it.copy(participacao = p) }
            }
        }
        carregar()
    }

    fun fechar() {
        _ui.update { it.copy(aberto = false, formulario = null) }
    }

    fun filtrar(mudanca: (FiltrosDoRanking) -> FiltrosDoRanking) {
        _ui.update { it.copy(filtros = mudanca(it.filtros)) }
        carregar()
    }

    fun carregar() {
        if (usuario == null) return
        val estado = _ui.value
        val f = estado.filtros
        val p = estado.participacao
        val parametros = ParametrosDoRanking(
            periodo = f.periodo.chave,
            faixa = f.faixa,
            sexo = f.sexo,
            horario = f.horario,
            cidade = p?.cidade?.takeIf { f.soMinhaCidade && it.isNotBlank() },
            local = p?.local?.takeIf { f.soMeuLocal && it.isNotBlank() }
        )
        busca?.cancel()
        _ui.update { it.copy(carregando = true, erro = null) }
        busca = viewModelScope.launch {
            when (val r = RankingRepository.listar(parametros)) {
                is Resultado.Ok -> _ui.update { it.copy(carregando = false, linhas = r.valor) }
                is Resultado.Falha -> _ui.update { it.copy(carregando = false, erro = r.mensagem) }
            }
        }
    }

    /** Abre o formulário já preenchido com o que a pessoa tem, ou com o nome sugerido. */
    fun editarParticipacao(nomeDoPerfil: String) {
        val p = _ui.value.participacao ?: ParticipacaoDto()
        _ui.update {
            it.copy(
                formulario = FormularioDoRanking(
                    nome = p.nome?.takeIf { n -> n.isNotBlank() } ?: OpcoesDoRanking.nomeSugerido(nomeDoPerfil),
                    ano = p.anoNascimento?.toString().orEmpty(),
                    sexo = p.sexo,
                    cidade = p.cidade.orEmpty(),
                    local = p.local.orEmpty(),
                    aceite = p.publico
                )
            )
        }
    }

    fun alterarFormulario(mudanca: (FormularioDoRanking) -> FormularioDoRanking) {
        _ui.update { s -> s.formulario?.let { f -> if (f.salvando) s else s.copy(formulario = mudanca(f).copy(erro = null)) } ?: s }
    }

    fun cancelarFormulario() {
        _ui.update { s -> if (s.formulario?.salvando == true) s else s.copy(formulario = null) }
    }

    fun salvarParticipacao() {
        val id = usuario ?: return
        val f = _ui.value.formulario ?: return
        if (f.salvando) return
        val (anoAtual, _, _) = DataCivil.civil(DataCivil.hoje())
        val nome = f.nome.trim()
        val ano = f.ano.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val erro = when {
            nome.length !in 2..40 -> "O nome no ranking precisa ter de 2 a 40 letras."
            f.ano.isNotBlank() && (ano == null || ano !in (anoAtual - 100)..anoAtual) -> "Confira o ano de nascimento (4 números)."
            f.cidade.length > 80 || f.local.length > 80 -> "Cidade e local podem ter até 80 letras."
            !f.aceite -> "Marque que você aceita aparecer no ranking."
            else -> null
        }
        if (erro != null) {
            _ui.update { it.copy(formulario = f.copy(erro = erro)) }
            return
        }
        salvar(
            id,
            ParticipacaoDto(
                publico = true,
                nome = nome,
                anoNascimento = ano,
                sexo = f.sexo,
                cidade = f.cidade.trim(),
                local = f.local.trim()
            ),
            "Pronto: você está no ranking."
        )
    }

    fun sairDoRanking() {
        val id = usuario ?: return
        val atual = _ui.value.participacao ?: return
        salvar(id, atual.copy(publico = false), "Você saiu do ranking. Seus treinos continuam no seu histórico.")
    }

    private fun salvar(id: String, participacao: ParticipacaoDto, aviso: String) {
        _ui.update { s -> s.copy(formulario = s.formulario?.copy(salvando = true), carregando = s.formulario == null) }
        viewModelScope.launch {
            when (val r = RankingRepository.salvar(id, participacao)) {
                is Resultado.Ok -> {
                    _ui.update { it.copy(participacao = r.valor, formulario = null, mensagem = aviso) }
                    carregar()
                }
                is Resultado.Falha -> _ui.update { s ->
                    s.copy(
                        carregando = false,
                        formulario = s.formulario?.copy(salvando = false, erro = r.mensagem),
                        erro = if (s.formulario == null) r.mensagem else s.erro
                    )
                }
            }
        }
    }

    fun mensagemMostrada() {
        _ui.update { it.copy(mensagem = null) }
    }
}
