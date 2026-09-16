package com.example.data.progresso

import com.example.data.ciclo.DataCivil
import com.example.data.execucao.EscalaDeEsforco
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstatisticaDeEsforcoTest {

    private fun dia(iso: String) = DataCivil.deIso(iso)
    private fun treino(iso: String, esforco: Int?) =
        Atividade(null, dia(iso), "Treino", null, 1000, 1800, true, false, esforco)

    @Test
    fun `escala do professor tem 11 notas com nome e cor`() {
        assertEquals(11, EscalaDeEsforco.NOMES.size)
        assertEquals(11, EscalaDeEsforco.CORES.size)
        assertEquals("0 · Nada cansado", EscalaDeEsforco.rotulo(0))
        assertEquals("Moderadamente difícil", EscalaDeEsforco.nome(4))
        assertEquals("10 · Máximo", EscalaDeEsforco.rotulo(10))
        assertEquals("Máximo", EscalaDeEsforco.nome(15))
        assertFalse("amarelo leva texto escuro", EscalaDeEsforco.textoClaro(5))
        assertTrue("vermelho leva texto branco", EscalaDeEsforco.textoClaro(9))
    }

    @Test
    fun `sem nota nenhuma nao inventa media`() {
        val e = Progresso.esforco(listOf(treino("2026-09-01", null)), dia("2026-09-15"))
        assertEquals(0, e.treinosComNota)
        assertNull(e.media)
        assertNull(e.mediaUltimas4Semanas)
        assertNull(e.ultima)
        assertEquals(List(11) { 0 }, e.porNota)
        assertTrue(e.porSemana.isEmpty())
    }

    @Test
    fun `medias, distribuicao, ultima nota e semanas`() {
        val hoje = dia("2026-09-15") // terça
        val e = Progresso.esforco(
            listOf(
                treino("2026-09-15", 8),
                treino("2026-08-10", 2), // antes das últimas 4 semanas
                treino("2026-08-24", 4), // segunda de 3 semanas atrás: conta
                treino("2026-09-14", 6),
                treino("2026-09-10", null)
            ),
            hoje
        )
        assertEquals(4, e.treinosComNota)
        assertEquals(5.0, e.media!!, 0.001)
        assertEquals(6.0, e.mediaUltimas4Semanas!!, 0.001)
        assertEquals(8, e.maior)
        assertEquals(8, e.ultima)
        assertEquals(1, e.porNota[2])
        assertEquals(1, e.porNota[8])
        assertEquals(listOf(dia("2026-08-10"), dia("2026-08-24"), dia("2026-09-14")), e.porSemana.map { it.first })
        assertEquals(7.0, e.porSemana.last().second, 0.001)
    }
}
