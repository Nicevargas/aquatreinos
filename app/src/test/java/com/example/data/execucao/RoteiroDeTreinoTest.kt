package com.example.data.execucao

import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RoteiroDeTreinoTest {

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/treinos_ciclo.json").readText())
    private val treino = ciclo.sugestao(DataCivil.deIso("2026-09-13"), TrainingLevel.INTERMEDIARIO)!!

    @Test
    fun `monta os passos na ordem das fases`() {
        val roteiro = RoteiroDeTreino(treino)
        assertEquals(
            listOf("300m Costas leve", "500m Crawl contínuo", "8x75m", "200m Pernada suave c/ nadadeira", "200m Sculling longo"),
            roteiro.passos.map { it.cabecalho }
        )
        assertEquals(listOf(1, 1, 8, 1, 1), roteiro.passos.map { it.repeticoes })
        assertEquals(listOf("Aquecimento", "Principal", "Principal", "Final", "Final"), roteiro.passos.map { it.fase })
        assertEquals(1800, roteiro.metrosTotais)
        assertEquals(5, roteiro.seriesTotais)
    }

    @Test
    fun `cada toque conta uma repeticao e os metros de verdade`() {
        val roteiro = RoteiroDeTreino(treino)
        var p = ProgressoExecucao()

        p = roteiro.avancar(p)
        assertEquals(ProgressoExecucao(1, 0), p)
        assertEquals(300, roteiro.metrosFeitos(p))

        p = roteiro.avancar(p) // 500m contínuo
        p = roteiro.avancar(p) // 1ª de 8x75
        assertEquals(ProgressoExecucao(2, 1), p)
        assertEquals(875, roteiro.metrosFeitos(p))
        assertEquals(SituacaoDaFase.CONCLUIDA, roteiro.situacaoDaFase(0, p))
        assertEquals(SituacaoDaFase.ATUAL, roteiro.situacaoDaFase(1, p))
        assertEquals(SituacaoDaFase.PENDENTE, roteiro.situacaoDaFase(2, p))

        repeat(7) { p = roteiro.avancar(p) }
        assertEquals(ProgressoExecucao(3, 0), p)
        assertEquals(1400, roteiro.metrosFeitos(p))
        assertEquals(3, roteiro.seriesFeitas(p))

        p = roteiro.avancar(roteiro.avancar(p))
        assertTrue(roteiro.terminou(p))
        assertEquals(1800, roteiro.metrosFeitos(p))
        assertEquals(5, roteiro.seriesFeitas(p))
        assertEquals(SituacaoDaFase.CONCLUIDA, roteiro.situacaoDaFase(2, p))
        assertEquals(p, roteiro.avancar(p)) // depois do fim, nada muda
    }

    @Test
    fun `voltar desfaz o ultimo toque inclusive entre series`() {
        val roteiro = RoteiroDeTreino(treino)
        assertEquals(ProgressoExecucao(), roteiro.voltar(ProgressoExecucao()))
        assertEquals(ProgressoExecucao(2, 3), roteiro.voltar(ProgressoExecucao(2, 4)))
        assertEquals(ProgressoExecucao(2, 7), roteiro.voltar(ProgressoExecucao(3, 0)))
        assertEquals(ProgressoExecucao(4, 0), roteiro.voltar(ProgressoExecucao(5, 0)))
    }

    @Test
    fun `os 84 treinos do ciclo terminam com a metragem do carrossel`() {
        for (i in 0 until ciclo.dias) {
            TrainingLevel.entries.forEach { level ->
                val sugestao = ciclo.sugestao(ciclo.ancoraEpochDay + i, level)!!
                val roteiro = RoteiroDeTreino(sugestao)
                var p = ProgressoExecucao()
                var toques = 0
                while (!roteiro.terminou(p)) {
                    p = roteiro.avancar(p)
                    toques++
                    check(toques < 1000) { "não terminou: ${sugestao.id}" }
                }
                assertEquals(sugestao.id, sugestao.totalDistanceMeters, roteiro.metrosTotais)
                assertEquals(sugestao.id, sugestao.totalDistanceMeters, roteiro.metrosFeitos(p))
                assertEquals(sugestao.id, roteiro.passos.sumOf { it.repeticoes }, toques)
            }
        }
    }
}
