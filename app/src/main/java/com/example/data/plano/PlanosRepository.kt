package com.example.data.plano

import com.example.data.Resultado
import com.example.data.supabase.chamarApi
import com.example.model.TrainingLevel
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Linha de public.planos_treino. id, user_id e criado_em vêm do banco. */
@JsonClass(generateAdapter = true)
data class PlanoDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "semanas") val semanas: Int,
    @Json(name = "treinos_por_semana") val treinosPorSemana: Int,
    @Json(name = "focos") val focos: List<String> = emptyList(),
    @Json(name = "semanas_relaxadas") val semanasRelaxadas: Boolean,
    @Json(name = "encerramento_relaxado") val encerramentoRelaxado: Boolean,
    @Json(name = "nivel") val nivel: String,
    // Nulos ficam fora do JSON: o banco usa 'automatico' e {}.
    @Json(name = "modo") val modo: String? = null,
    @Json(name = "trocas") val trocas: Map<String, String>? = null,
    @Json(name = "criado_em") val criadoEm: String? = null
) {
    val manual: Boolean get() = modo == MODO_MANUAL

    fun config() = ConfigDoPlano(
        semanas = semanas,
        treinosPorSemana = treinosPorSemana,
        focos = focos,
        semanasRelaxadas = semanasRelaxadas,
        encerramentoRelaxado = encerramentoRelaxado,
        nivel = TrainingLevel.entries.firstOrNull { it.name == nivel } ?: TrainingLevel.INTERMEDIARIO,
        trocas = trocas.orEmpty()
    )

    companion object {
        const val MODO_MANUAL = "manual"

        fun de(config: ConfigDoPlano, manual: Boolean = false) = PlanoDto(
            semanas = config.semanas,
            treinosPorSemana = config.treinosPorSemana,
            focos = PlanoDeTreino.FOCOS.filter { it in config.focos },
            semanasRelaxadas = config.semanasRelaxadas,
            encerramentoRelaxado = config.encerramentoRelaxado,
            nivel = config.nivel.name,
            modo = if (manual) MODO_MANUAL else null
        )
    }
}

/** Corpo do PATCH: só as trocas mudam depois de o plano criado. */
@JsonClass(generateAdapter = true)
data class TrocasDoPlanoDto(@Json(name = "trocas") val trocas: Map<String, String>)

/** Plano de treino do usuário logado. Quem garante que é só dele é o RLS. */
object PlanosRepository {

    /** O plano mais recente; nulo se a pessoa não tem plano. */
    suspend fun atual(): Resultado<PlanoDto?> =
        chamarApi({ it.planoAtual() }) { lista -> Resultado.Ok(lista?.firstOrNull()) }

    suspend fun criar(config: ConfigDoPlano, manual: Boolean = false): Resultado<PlanoDto> =
        chamarApi({ it.criarPlano(PlanoDto.de(config, manual)) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it) }
                ?: Resultado.Falha("O plano não foi salvo. Tente de novo.")
        }

    suspend fun trocar(id: String, trocas: Map<String, String>): Resultado<PlanoDto> =
        chamarApi({ it.trocarTreinosDoPlano("eq.$id", TrocasDoPlanoDto(trocas)) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it) }
                ?: Resultado.Falha("A troca não foi salva. Tente de novo.")
        }

    suspend fun excluir(id: String): Resultado<Unit> =
        chamarApi({ it.excluirPlano("eq.$id") }) { lista ->
            if (lista.isNullOrEmpty()) Resultado.Falha("Plano não encontrado: ele pode já ter sido excluído.")
            else Resultado.Ok(Unit)
        }
}
