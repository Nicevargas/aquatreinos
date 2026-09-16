package com.example.data.compartilhar

import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.data.supabase.toDomain
import com.example.data.treinos.paraDto
import com.example.data.treinos.paraGravacao
import com.example.data.treinos.paraWorkout
import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TreinoCompartilhadoTest {

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/programa_nc.json").readText())
    private val treino = ciclo.sugestao(DataCivil.deIso("2026-10-01"), TrainingLevel.INTERMEDIARIO)!!

    @Test
    fun `treino vira JSON e volta igual, com zonas, intervalos e corretivos`() {
        val volta = treino.paraDto().toDomain()
        assertEquals(treino.title, volta.title)
        assertEquals(treino.level, volta.level)
        assertEquals(treino.totalDistanceMeters, volta.totalDistanceMeters)
        assertEquals(treino.focus, volta.focus)
        assertEquals(treino.phases.map { it.title }, volta.phases.map { it.title })
        val series = treino.phases.flatMap { it.sets }
        val seriesVolta = volta.phases.flatMap { it.sets }
        assertEquals(series.map { it.header }, seriesVolta.map { it.header })
        assertEquals(series.map { it.distanceMeters }, seriesVolta.map { it.distanceMeters })
        assertEquals(series.map { it.zona }, seriesVolta.map { it.zona })
        assertEquals(series.map { it.intervalTarget }, seriesVolta.map { it.intervalTarget })
        assertEquals(series.map { it.corretivo }, seriesVolta.map { it.corretivo })
        assertTrue("o dia 17 tem corretivo", series.any { it.corretivo != null })
    }

    @Test
    fun `gravar em Meus treinos e usar o treino montado no editor`() {
        val gravacao = treino.paraGravacao("2026-10-01")
        assertEquals("Meu treino", gravacao.tag)
        assertEquals("2026-10-01", gravacao.workoutDate)
        assertEquals(treino.totalDistanceMeters, gravacao.totalDistanceMeters)
        assertEquals("INTERMEDIARIO", gravacao.level)

        val usado = gravacao.paraWorkout("ajustado_1")
        assertEquals("ajustado_1", usado.id)
        assertFalse(usado.isSuggestion)
        assertEquals(treino.phases.sumOf { it.sets.size }, usado.phases.sumOf { it.sets.size })
    }

    @Test
    fun `codigo sai do link, da mensagem inteira ou sozinho`() {
        assertEquals("a1b2c3d4e5", CodigoDoTreino.extrair("a1b2c3d4e5"))
        assertEquals("a1b2c3d4e5", CodigoDoTreino.extrair("  A1B2C3D4E5 "))
        assertEquals("a1b2c3d4e5", CodigoDoTreino.extrair(CodigoDoTreino.link("a1b2c3d4e5")))
        assertEquals("a1b2c3d4e5", CodigoDoTreino.extrair("natacaocriativa://treino/a1b2c3d4e5"))
        assertEquals("a1b2c3d4e5", CodigoDoTreino.extrair("Olha esse treino!\nCódigo do treino: a1b2c3d4e5"))
        assertNull(CodigoDoTreino.extrair("sem código aqui"))
        assertNull("11 caracteres não é código", CodigoDoTreino.extrair("a1b2c3d4e5f"))
        assertNull(CodigoDoTreino.extrair(null))
    }

    @Test
    fun `mensagem traz o treino para ler, o link e o codigo`() {
        val texto = MensagemDoTreino.paraCompartilhar(treino, "a1b2c3d4e5")
        assertTrue(texto.startsWith("🏊 *${treino.title}*"))
        treino.phases.forEach { assertTrue(it.title, texto.contains("*${it.title}*")) }
        assertTrue(texto.contains(CodigoDoTreino.link("a1b2c3d4e5")))
        assertTrue(texto.contains("Código do treino: a1b2c3d4e5"))
        assertEquals("a1b2c3d4e5", CodigoDoTreino.extrair(texto))
    }
}
