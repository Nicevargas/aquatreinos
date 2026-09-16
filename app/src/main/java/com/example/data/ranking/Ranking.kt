package com.example.data.ranking

import com.example.data.Resultado
import com.example.data.supabase.chamarApi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Uma linha de public.ranking_nadadores(). Só nome escolhido e números: nada sensível. */
@JsonClass(generateAdapter = true)
data class LinhaDoRanking(
    @Json(name = "posicao") val posicao: Int,
    @Json(name = "nome") val nome: String,
    @Json(name = "pontos") val pontos: Int,
    @Json(name = "metros") val metros: Int,
    @Json(name = "treinos") val treinos: Int,
    @Json(name = "semanas") val semanas: Int,
    @Json(name = "sou_eu") val souEu: Boolean
)

/** Parâmetros da função. Nulo fica de fora do JSON e vale o padrão do banco (sem filtro). */
@JsonClass(generateAdapter = true)
data class ParametrosDoRanking(
    @Json(name = "p_periodo") val periodo: String,
    @Json(name = "p_faixa") val faixa: String? = null,
    @Json(name = "p_sexo") val sexo: String? = null,
    @Json(name = "p_horario") val horario: String? = null,
    @Json(name = "p_cidade") val cidade: String? = null,
    @Json(name = "p_local") val local: String? = null,
    @Json(name = "p_limite") val limite: Int = 100
)

/** Os dados do ranking no perfil, lidos e gravados só pela própria pessoa (RLS). */
@JsonClass(generateAdapter = true)
data class ParticipacaoDto(
    @Json(name = "ranking_publico") val publico: Boolean = false,
    @Json(name = "ranking_nome") val nome: String? = null,
    @Json(name = "ano_nascimento") val anoNascimento: Int? = null,
    @Json(name = "sexo") val sexo: String? = null,
    @Json(name = "cidade") val cidade: String? = null,
    @Json(name = "local_treino") val local: String? = null
)

enum class PeriodoDoRanking(val chave: String, val rotulo: String) {
    SEMANA("semana", "Semana"), MES("mes", "Mês"), ANO("ano", "Ano"), TUDO("tudo", "Geral")
}

object OpcoesDoRanking {
    val FAIXAS = listOf("ate-17" to "Até 17", "18-29" to "18–29", "30-39" to "30–39", "40-49" to "40–49", "50-59" to "50–59", "60+" to "60+")
    val SEXOS = listOf("F" to "Feminino", "M" to "Masculino", "OUTRO" to "Outro")
    val HORARIOS = listOf("manha" to "Manhã", "tarde" to "Tarde", "noite" to "Noite")

    /** "Ana Maria dos Santos" -> "Ana S.": o nome que o app sugere para o ranking. */
    fun nomeSugerido(nomeCompleto: String): String {
        val partes = nomeCompleto.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            partes.isEmpty() -> ""
            partes.size == 1 -> partes[0].take(40)
            else -> "${partes.first()} ${partes.last().first().uppercaseChar()}.".take(40)
        }
    }
}

object RankingRepository {

    suspend fun participacao(userId: String): Resultado<ParticipacaoDto> =
        chamarApi({ it.lerParticipacaoNoRanking("eq.$userId") }) { lista ->
            Resultado.Ok(lista?.firstOrNull() ?: ParticipacaoDto())
        }

    suspend fun salvar(userId: String, participacao: ParticipacaoDto): Resultado<ParticipacaoDto> =
        chamarApi({ it.salvarParticipacaoNoRanking("eq.$userId", participacao) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it) }
                ?: Resultado.Falha("Seus dados do ranking não foram salvos. Tente de novo.")
        }

    suspend fun listar(parametros: ParametrosDoRanking): Resultado<List<LinhaDoRanking>> =
        chamarApi({ it.rankingNadadores(parametros) }) { lista -> Resultado.Ok(lista.orEmpty()) }
}
