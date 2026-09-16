package com.example.data.progresso

import com.example.data.ciclo.DataCivil

data class NivelDePontos(val nome: String, val minimo: Int)

/**
 * Pontos de gamificação: metros e constância, para não premiar só quem nada muito.
 *   1 ponto a cada 100 m · +10 por treino completo · +20 por semana com treino.
 *
 * As mesmas regras estão em public.ranking_nadadores() (20260915000004_ranking.sql):
 * mudou aqui, muda lá.
 */
object Pontuacao {

    const val METROS_POR_PONTO = 100
    const val BONUS_TREINO_COMPLETO = 10
    const val BONUS_SEMANA_COM_TREINO = 20

    val NIVEIS = listOf(
        NivelDePontos("Bronze", 0),
        NivelDePontos("Prata", 300),
        NivelDePontos("Ouro", 1500),
        NivelDePontos("Platina", 4000),
        NivelDePontos("Diamante", 8000)
    )

    fun doTreino(metros: Int, completo: Boolean): Int =
        metros.coerceAtLeast(0) / METROS_POR_PONTO + if (completo) BONUS_TREINO_COMPLETO else 0

    fun total(atividades: List<Atividade>): Int =
        atividades.sumOf { doTreino(it.metros, it.completo) } +
            BONUS_SEMANA_COM_TREINO * atividades.map { DataCivil.segundaDaSemana(it.dia) }.toSet().size

    fun nivel(pontos: Int): NivelDePontos = NIVEIS.last { pontos >= it.minimo }

    fun proximoNivel(pontos: Int): NivelDePontos? = NIVEIS.firstOrNull { it.minimo > pontos }
}
