package com.example.data.plano

import com.example.data.supabase.WorkoutDto
import com.example.data.supabase.toDomain
import com.example.model.ReferenciaDoPlano
import com.example.model.TrainingLevel
import com.example.model.Workout

/** O que a pessoa escolheu ao criar o plano, mais os treinos que ela trocou depois. */
data class ConfigDoPlano(
    val semanas: Int,
    val treinosPorSemana: Int,
    // Vazio = todas as capacidades.
    val focos: List<String>,
    val semanasRelaxadas: Boolean,
    val encerramentoRelaxado: Boolean,
    val nivel: TrainingLevel,
    // "semana-treino" -> "foco:Velocidade" ou "meu:<id de Meus treinos>".
    val trocas: Map<String, String> = emptyMap()
)

/** De onde veio o treino daquela vaga do plano. */
enum class OrigemDoTreino { METODO_NC, TROCADO, MEU_TREINO }

data class TreinoDoPlano(
    val semana: Int,
    val numero: Int,
    val workout: Workout,
    val origem: OrigemDoTreino = OrigemDoTreino.METODO_NC
) {
    val foco: String get() = workout.focus ?: if (origem == OrigemDoTreino.MEU_TREINO) "Seu treino" else ""
    val metros: Int get() = workout.totalDistanceMeters
}

data class SemanaDoPlano(val numero: Int, val bloco: String, val treinos: List<TreinoDoPlano>) {
    val relaxada: Boolean get() = bloco == PlanoDeTreino.BLOCO_RELAXADO
    val metros: Int get() = treinos.sumOf { it.metros }
}

/** Resumo para o cartão da tela inicial. */
data class ResumoDoPlano(
    val semanas: Int,
    val treinosPorSemana: Int,
    val feitos: Int,
    val total: Int,
    val proximo: TreinoDoPlano?
)

/**
 * Plano de treino de várias semanas montado com os treinos do Método NC.
 *
 * Periodização do método: as semanas seguem Base → Construção → Pico e, com
 * "semanas relaxadas", a 4ª de cada bloco é de Regeneração (treinos mais leves).
 * "Semana de encerramento relaxada" faz a última ser de Regeneração.
 *
 * Em cada semana, os treinos alternam o que a pessoa quer melhorar com as
 * outras capacidades, para o plano não virar um treino só repetido. O nadador
 * pode trocar qualquer vaga por outra capacidade ou por um treino dele.
 *
 * Nada do plano é gravado além da configuração e das trocas: ele é remontado
 * igual a partir delas, e o que já foi feito vem de treinos_realizados.
 */
object PlanoDeTreino {

    const val MAX_SEMANAS = 12
    const val MAX_TREINOS_POR_SEMANA = 6
    const val BLOCO_RELAXADO = "Regeneração"
    private const val TROCA_POR_FOCO = "foco:"
    private const val TROCA_POR_MEU_TREINO = "meu:"

    // As capacidades do Método NC, na ordem do programa. "Recuperação" não entra:
    // é o dia leve do ciclo diário, não algo que se escolhe melhorar.
    val FOCOS = listOf("Técnica", "Resistência", "Velocidade", "Estilos", "Ritmo", "Força específica")

    private val BLOCOS_COM_RELAXADA = listOf("Base", "Construção", "Pico", BLOCO_RELAXADO)
    private val BLOCOS_SEM_RELAXADA = listOf("Base", "Construção", "Pico")

    fun chave(semana: Int, numero: Int) = "$semana-$numero"
    fun trocaPorFoco(foco: String) = TROCA_POR_FOCO + foco
    fun trocaPorMeuTreino(id: String) = TROCA_POR_MEU_TREINO + id

    fun blocoDaSemana(semana: Int, config: ConfigDoPlano): String {
        if (config.encerramentoRelaxado && config.semanas > 1 && semana == config.semanas) return BLOCO_RELAXADO
        val blocos = if (config.semanasRelaxadas) BLOCOS_COM_RELAXADA else BLOCOS_SEM_RELAXADA
        return blocos[(semana - 1) % blocos.size]
    }

    fun focosEscolhidos(focos: Collection<String>): List<String> =
        FOCOS.filter { it in focos }.ifEmpty { FOCOS }

    /** Treinos pares (1º, 3º...) são do que a pessoa quer melhorar; os outros completam. */
    fun focosDaSemana(semana: Int, treinos: Int, focos: Collection<String>): List<String> {
        val escolhidos = focosEscolhidos(focos)
        val outros = FOCOS - escolhidos.toSet()
        var e = 0
        var o = 0
        return (0 until treinos).map { i ->
            if (outros.isEmpty() || i % 2 == 0) {
                escolhidos[(e++ + semana - 1) % escolhidos.size]
            } else {
                outros[(o++ + semana - 1) % outros.size]
            }
        }
    }

    private fun tag(semana: Int, numero: Int) = "Plano · Semana $semana · Treino $numero"

    private fun referencia(planoId: String?, semana: Int, numero: Int) =
        planoId?.let { ReferenciaDoPlano(it, semana, numero) }

    private fun doMetodo(
        treinosNC: List<WorkoutDto>, foco: String, bloco: String, nivel: TrainingLevel,
        semana: Int, numero: Int, planoId: String?
    ): Workout? = treinosNC
        .firstOrNull { it.foco == foco && it.bloco == bloco && it.level.equals(nivel.name, ignoreCase = true) }
        ?.copy(workoutDate = null, isSuggestion = true, tag = tag(semana, numero))
        ?.toDomain()
        ?.copy(plano = referencia(planoId, semana, numero))

    /** Uma opção de cada capacidade para a vaga: mesma semana (bloco) e nível do plano. */
    fun opcoesDoMetodo(config: ConfigDoPlano, semana: Int, treinosNC: List<WorkoutDto>): List<Workout> {
        val bloco = blocoDaSemana(semana, config)
        return FOCOS.mapNotNull { doMetodo(treinosNC, it, bloco, config.nivel, semana, 0, null) }
    }

    fun montar(
        config: ConfigDoPlano,
        treinosNC: List<WorkoutDto>,
        planoId: String?,
        meusTreinos: List<Workout> = emptyList()
    ): List<SemanaDoPlano> {
        val semanas = config.semanas.coerceIn(1, MAX_SEMANAS)
        val porSemana = config.treinosPorSemana.coerceIn(1, MAX_TREINOS_POR_SEMANA)
        val ajustada = config.copy(semanas = semanas)
        return (1..semanas).map { s ->
            val bloco = blocoDaSemana(s, ajustada)
            val treinos = focosDaSemana(s, porSemana, config.focos).mapIndexedNotNull { i, focoDoApp ->
                val numero = i + 1
                val troca = config.trocas[chave(s, numero)]
                val meu = troca?.takeIf { it.startsWith(TROCA_POR_MEU_TREINO) }
                    ?.removePrefix(TROCA_POR_MEU_TREINO)
                    ?.let { id -> meusTreinos.firstOrNull { it.id == id } }
                val focoTrocado = troca?.takeIf { it.startsWith(TROCA_POR_FOCO) }
                    ?.removePrefix(TROCA_POR_FOCO)
                    ?.takeIf { it in FOCOS }
                when {
                    // Treino de Meus treinos que ainda existe.
                    meu != null -> TreinoDoPlano(
                        s, numero,
                        meu.copy(tag = tag(s, numero), isSuggestion = false, plano = referencia(planoId, s, numero)),
                        OrigemDoTreino.MEU_TREINO
                    )
                    focoTrocado != null -> doMetodo(treinosNC, focoTrocado, bloco, config.nivel, s, numero, planoId)
                        ?.let { TreinoDoPlano(s, numero, it, OrigemDoTreino.TROCADO) }
                    // Sem troca, ou o treino escolhido foi apagado: vale o sugerido pelo app.
                    else -> null
                } ?: doMetodo(treinosNC, focoDoApp, bloco, config.nivel, s, numero, planoId)
                    ?.let { TreinoDoPlano(s, numero, it) }
            }
            SemanaDoPlano(s, bloco, treinos)
        }
    }

    fun resumo(config: ConfigDoPlano, semanas: List<SemanaDoPlano>, feitos: Set<Pair<Int, Int>>): ResumoDoPlano {
        val todos = semanas.flatMap { it.treinos }
        return ResumoDoPlano(
            semanas = semanas.size,
            treinosPorSemana = config.treinosPorSemana,
            feitos = todos.count { (it.semana to it.numero) in feitos },
            total = todos.size,
            proximo = todos.firstOrNull { (it.semana to it.numero) !in feitos }
        )
    }
}
