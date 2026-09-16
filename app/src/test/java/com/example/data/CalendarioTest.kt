package com.example.data

import com.example.data.ciclo.DataCivil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarioTest {

    private fun dia(iso: String) = DataCivil.deIso(iso)
    private val hoje = dia("2026-09-15") // terça

    @Test
    fun `calendario rola de 4 semanas antes a 8 depois, de segunda a domingo`() {
        val dias = WorkoutRepository.diasDoCalendario(hoje, hoje)
        assertEquals(dia("2026-08-17"), dias.first().epochDay)
        assertEquals(dia("2026-11-15"), dias.last().epochDay)
        assertEquals(13 * 7, dias.size)
        assertEquals("SEG", dias.first().dayOfWeek)
        assertEquals("DOM", dias.last().dayOfWeek)
        assertEquals(1, dias.count { it.isToday })
        assertTrue(dias.single { it.isSelected }.isToday)
    }

    @Test
    fun `dia escolhido fora da janela estende o calendario ate a semana dele`() {
        val longe = dia("2027-01-13")
        val dias = WorkoutRepository.diasDoCalendario(longe, hoje)
        assertEquals(dia("2026-08-17"), dias.first().epochDay)
        assertEquals(dia("2027-01-17"), dias.last().epochDay)
        assertEquals(longe, dias.single { it.isSelected }.epochDay)

        val antes = WorkoutRepository.diasDoCalendario(dia("2026-06-03"), hoje)
        assertEquals(dia("2026-06-01"), antes.first().epochDay)
    }
}
