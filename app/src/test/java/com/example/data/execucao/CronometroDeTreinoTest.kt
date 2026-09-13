package com.example.data.execucao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CronometroDeTreinoTest {

    @Test
    fun `conta pelo instante de inicio e respeita pausas`() {
        var c = CronometroDeTreino().iniciar(agora = 1_000)
        assertTrue(c.rodando)
        assertEquals(0L, c.decorridoMs(1_000))
        assertEquals(61_500L, c.decorridoMs(62_500))

        c = c.pausar(62_500)
        assertFalse(c.rodando)
        assertEquals(61_500L, c.decorridoMs(999_999)) // parado não anda

        c = c.iniciar(100_000)
        assertEquals(71_500L, c.decorridoMs(110_000))
    }

    @Test
    fun `iniciar rodando e pausar parado nao mexem no tempo`() {
        val rodando = CronometroDeTreino().iniciar(10)
        assertEquals(rodando, rodando.iniciar(500))
        val parado = CronometroDeTreino(acumuladoMs = 5_000)
        assertEquals(parado, parado.pausar(9_000))
    }

    @Test
    fun `relogio que volta nao gera tempo negativo`() {
        val c = CronometroDeTreino(acumuladoMs = 2_000).iniciar(10_000)
        assertEquals(2_000L, c.decorridoMs(9_000))
    }
}
