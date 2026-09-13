package com.example.data.ciclo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DataCivilTest {

    @Test
    fun `bate com java time de 1900 a 2100`() {
        var d = LocalDate.of(1900, 1, 1)
        while (d.year <= 2100) {
            val e = DataCivil.epochDay(d.year, d.monthValue, d.dayOfMonth)
            assertEquals(d.toString(), d.toEpochDay(), e)
            assertEquals(d.toString(), DataCivil.paraIso(e))
            assertEquals(d.toString(), d.dayOfWeek.value - 1, DataCivil.diaDaSemana(e))
            d = d.plusDays(1)
        }
    }

    @Test
    fun `ancora do carrossel cai numa segunda`() {
        val ancora = DataCivil.deIso("2026-08-31")
        assertEquals(0, DataCivil.diaDaSemana(ancora))
        assertEquals("SEG", DataCivil.sigla(ancora))
        assertEquals(ancora, DataCivil.segundaDaSemana(DataCivil.deIso("2026-09-06")))
    }

    @Test
    fun `hoje e o dia de Brasilia, nao o UTC`() {
        // 01:30 UTC de 13/09 ainda é 22:30 de 12/09 em Brasília.
        val madrugadaUtc = ZonedDateTime.of(2026, 9, 13, 1, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2026-09-12", DataCivil.paraIso(DataCivil.hoje(madrugadaUtc)))

        val manhaUtc = ZonedDateTime.of(2026, 9, 13, 9, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2026-09-13", DataCivil.paraIso(DataCivil.hoje(manhaUtc)))
    }
}
