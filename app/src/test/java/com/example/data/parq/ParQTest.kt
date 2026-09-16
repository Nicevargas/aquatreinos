package com.example.data.parq

import com.example.data.ciclo.DataCivil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParQTest {

    private val todasNao: List<Boolean?> = List(7) { false }
    private fun dia(iso: String) = DataCivil.deIso(iso)

    @Test
    fun `sete perguntas e versao do texto`() {
        assertEquals(7, ParQ.PERGUNTAS.size)
        assertTrue(ParQ.VERSAO.isNotBlank())
        assertEquals(7, ParQ.faltam(List(7) { null }))
        assertEquals(0, ParQ.faltam(todasNao))
    }

    @Test
    fun `so envia com tudo respondido, declaracao e termo quando ha SIM`() {
        assertTrue(ParQ.podeEnviar(todasNao, declaracao = true, termo = false))
        assertFalse("sem declaração", ParQ.podeEnviar(todasNao, declaracao = false, termo = false))
        assertFalse("pergunta sem resposta", ParQ.podeEnviar(todasNao.toMutableList().also { it[3] = null }, true, false))
        assertFalse("lista incompleta", ParQ.podeEnviar(List(6) { false }, true, false))

        val comSim = todasNao.toMutableList().also { it[5] = true }
        assertTrue(ParQ.algumSim(comSim))
        assertFalse("SIM sem termo", ParQ.podeEnviar(comSim, declaracao = true, termo = false))
        assertTrue(ParQ.podeEnviar(comSim, declaracao = true, termo = true))
    }

    @Test
    fun `vale doze meses e vence no mesmo dia do ano seguinte`() {
        assertEquals(dia("2027-09-15"), ParQ.venceEm(dia("2026-09-15")))
        assertFalse(ParQ.precisaResponder(dia("2026-09-15"), dia("2026-09-15")))
        assertFalse(ParQ.precisaResponder(dia("2026-09-15"), dia("2027-09-14")))
        assertTrue(ParQ.precisaResponder(dia("2026-09-15"), dia("2027-09-15")))
        assertTrue("nunca respondeu", ParQ.precisaResponder(null, dia("2026-09-15")))
    }

    @Test
    fun `dia que nao existe no mes final vira o ultimo dia do mes`() {
        assertEquals(dia("2026-02-28"), ParQ.venceEm(dia("2026-01-31"), meses = 1))
        assertEquals(dia("2029-02-28"), ParQ.venceEm(dia("2028-02-29")))
        assertEquals(dia("2027-01-10"), ParQ.venceEm(dia("2026-12-10"), meses = 1))
    }
}
