package com.example.data.execucao

import com.example.model.Workout
import com.example.model.WorkoutSet

/** Uma série do treino, na ordem em que é nadada. */
data class PassoDeTreino(
    val indiceFase: Int,
    val fase: String,
    val serie: WorkoutSet,
    val repeticoes: Int,
    val metrosPorRepeticao: Int
) {
    val metros: Int get() = repeticoes * metrosPorRepeticao
    val cabecalho: String get() = serie.header.ifBlank { "${serie.repsDistance}m ${serie.description}".trim() }
}

/** Onde a pessoa está: no passo [passo], com [repeticoesFeitas] repetições dele já nadadas. */
data class ProgressoExecucao(val passo: Int = 0, val repeticoesFeitas: Int = 0)

enum class SituacaoDaFase { CONCLUIDA, ATUAL, PENDENTE }

/**
 * O treino escolhido virado roteiro de piscina: cada série vira um passo, e
 * cada toque em "feito" conta uma repetição ("8x75m" = 8 toques de 75m).
 */
class RoteiroDeTreino(val workout: Workout) {

    val passos: List<PassoDeTreino> = workout.phases.flatMapIndexed { indice, fase ->
        fase.sets.mapNotNull { passoDe(indice, fase.title, it) }
    }

    val metrosTotais: Int = passos.sumOf { it.metros }
    val seriesTotais: Int = passos.size

    fun terminou(p: ProgressoExecucao): Boolean = p.passo >= passos.size

    fun passoAtual(p: ProgressoExecucao): PassoDeTreino? = passos.getOrNull(p.passo)

    fun proximoPasso(p: ProgressoExecucao): PassoDeTreino? = passos.getOrNull(p.passo + 1)

    fun metrosFeitos(p: ProgressoExecucao): Int {
        val concluidos = passos.take(p.passo.coerceIn(0, passos.size)).sumOf { it.metros }
        val emAndamento = passoAtual(p)?.let { p.repeticoesFeitas * it.metrosPorRepeticao } ?: 0
        return concluidos + emAndamento
    }

    fun seriesFeitas(p: ProgressoExecucao): Int = p.passo.coerceIn(0, passos.size)

    /** Uma repetição feita; na última da série, passa para a próxima série. */
    fun avancar(p: ProgressoExecucao): ProgressoExecucao {
        val atual = passoAtual(p) ?: return p
        val feitas = p.repeticoesFeitas + 1
        return if (feitas >= atual.repeticoes) ProgressoExecucao(p.passo + 1, 0) else p.copy(repeticoesFeitas = feitas)
    }

    /** Desfaz o último toque, inclusive voltando para a série anterior. */
    fun voltar(p: ProgressoExecucao): ProgressoExecucao = when {
        p.repeticoesFeitas > 0 -> p.copy(repeticoesFeitas = p.repeticoesFeitas - 1)
        p.passo > 0 -> {
            val anterior = passos[(p.passo - 1).coerceAtMost(passos.size - 1)]
            ProgressoExecucao((p.passo - 1).coerceAtMost(passos.size - 1), anterior.repeticoes - 1)
        }
        else -> p
    }

    fun situacaoDaFase(indiceFase: Int, p: ProgressoExecucao): SituacaoDaFase {
        val faseAtual = passoAtual(p)?.indiceFase ?: return SituacaoDaFase.CONCLUIDA
        return when {
            indiceFase < faseAtual -> SituacaoDaFase.CONCLUIDA
            indiceFase == faseAtual -> SituacaoDaFase.ATUAL
            else -> SituacaoDaFase.PENDENTE
        }
    }

    fun situacaoDoPasso(indice: Int, p: ProgressoExecucao): SituacaoDaFase = when {
        indice < p.passo -> SituacaoDaFase.CONCLUIDA
        indice == p.passo -> SituacaoDaFase.ATUAL
        else -> SituacaoDaFase.PENDENTE
    }

    companion object {
        fun passoDe(indiceFase: Int, fase: String, serie: WorkoutSet): PassoDeTreino? {
            val texto = serie.repsDistance.lowercase()
            // "8x75" -> 8 repetições; "400" (sem x) -> 1
            val repeticoes = texto.substringBefore('x', "1").trim().toIntOrNull()?.takeIf { it > 0 } ?: 1
            val metros = serie.distanceMeters.takeIf { it > 0 }
                ?: texto.substringAfter('x').removeSuffix("m").trim().toIntOrNull()?.times(repeticoes)
                ?: 0
            if (metros <= 0) return null
            return PassoDeTreino(indiceFase, fase, serie, repeticoes, metros / repeticoes)
        }
    }
}
