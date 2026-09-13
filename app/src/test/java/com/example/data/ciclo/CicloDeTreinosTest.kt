package com.example.data.ciclo

import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Confere a cópia embarcada (assets/treinos_ciclo.json) contra o carrossel.
 * As datas esperadas foram tiradas de natacao-treinos/scripts/treino.py (treino_de),
 * que é quem escolhe o treino publicado no Instagram.
 */
class CicloDeTreinosTest {

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/treinos_ciclo.json").readText())

    @Test
    fun `escolhe o mesmo dia do carrossel`() {
        val casos = listOf(
            "2026-08-31" to (1 to "Técnica"),
            "2026-09-13" to (14 to "Regenerativo"),
            "2026-09-14" to (15 to "Técnica"),
            "2027-01-01" to (12 to "Volume"),
            "2025-12-31" to (10 to "Velocidade"), // antes da âncora
            "2026-08-30" to (28 to "Regenerativo")
        )
        for ((data, esperado) in casos) {
            val dia = DataCivil.deIso(data)
            assertEquals(data, esperado.first, ciclo.diaDoCiclo(dia))
            TrainingLevel.entries.forEach { level ->
                val sugestao = ciclo.sugestao(dia, level)
                assertNotNull("$data $level", sugestao)
                val treino = sugestao!!
                assertEquals(data, esperado.second, treino.focus)
                assertEquals(level, treino.level)
                assertEquals(data, treino.workoutDate)
                assertTrue(treino.isSuggestion)
            }
        }
    }

    @Test
    fun `ciclo completo e a metragem fecha em todos os treinos`() {
        val materiaisConhecidos = setOf("Palmar", "Pull buoy", "Nadadeira", "Prancha")
        for (i in 0 until ciclo.dias) {
            val doDia = TrainingLevel.entries.map { ciclo.sugestao(ciclo.ancoraEpochDay + i, it)!! }

            // Subir de nível é acrescentar exercício: o volume cresce do verde ao vermelho.
            assertTrue("dia ${i + 1}", doDia.zipWithNext().all { (a, b) -> a.totalDistanceMeters < b.totalDistanceMeters })

            doDia.forEach { treino ->
                val rotulo = "dia ${i + 1} ${treino.level}"
                assertEquals(rotulo, i + 1, treino.cycleDay)
                assertEquals(rotulo, listOf("Aquecimento", "Principal", "Final"), treino.phases.map { it.title })
                assertEquals(rotulo, treino.totalDistanceMeters, treino.phases.sumOf { it.distanceMeters })
                assertEquals(rotulo, treino.totalDistanceMeters, treino.phases.flatMap { it.sets }.sumOf { it.distanceMeters })
                assertEquals(rotulo, 100, treino.phases.sumOf { it.percentage })
                assertTrue(rotulo, materiaisConhecidos.containsAll(treino.equipment))
            }
        }
    }

    @Test
    fun `sugestao de 13-09 intermediario igual ao carrossel`() {
        val treino = ciclo.sugestao(DataCivil.deIso("2026-09-13"), TrainingLevel.INTERMEDIARIO)!!
        assertEquals("ciclo_d14_intermediario", treino.id)
        assertEquals(1800, treino.totalDistanceMeters)

        val principal = treino.phases.first { it.title == "Principal" }
        assertEquals(1100, principal.distanceMeters)

        val continuo = principal.sets[0]
        assertEquals("500m Crawl contínuo", continuo.header)
        assertEquals(listOf("Sem olhar o relógio"), continuo.details)
        assertEquals("", continuo.intervalTarget)

        val repetida = principal.sets[1]
        assertEquals("8x75m", repetida.header)
        assertEquals("8x75", repetida.repsDistance)
        assertEquals(listOf("25m Peito", "50m Crawl leve"), repetida.details)
        assertEquals("30\"", repetida.intervalTarget)
        assertEquals(30, repetida.restSeconds)
        assertEquals(600, repetida.distanceMeters)
    }

    @Test
    fun `material de series combinadas vira itens separados`() {
        val todos = (0 until ciclo.dias).flatMap { i ->
            TrainingLevel.entries.map { ciclo.sugestao(ciclo.ancoraEpochDay + i, it)!! }
        }
        val combinado = todos.first { t -> t.phases.flatMap { it.sets }.any { it.equipmentName == "Palmar + Pull buoy" } }
        assertTrue(combinado.equipment.containsAll(listOf("Palmar", "Pull buoy")))
    }
}
