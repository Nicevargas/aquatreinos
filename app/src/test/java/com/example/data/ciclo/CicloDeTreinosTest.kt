package com.example.data.ciclo

import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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

    private val metodoNc = CicloDeTreinos.deJson(File("src/main/assets/programa_nc.json").readText())

    @Test
    fun `o metodo NC vale a partir de 15-09 e o ciclo antigo ate 14-09`() {
        val ciclos = listOf(ciclo, metodoNc)
        assertSame(ciclo, CicloDeTreinos.escolher(ciclos, DataCivil.deIso("2026-09-14")))
        assertSame(metodoNc, CicloDeTreinos.escolher(ciclos, DataCivil.deIso("2026-09-15")))
        assertSame(metodoNc, CicloDeTreinos.escolher(ciclos, DataCivil.deIso("2027-01-01")))
        assertSame(ciclo, CicloDeTreinos.escolher(ciclos, DataCivil.deIso("2025-12-31"))) // antes de todos

        // Mesmos dias que natacao-treinos/scripts/programa_nc.py publica (âncora numa terça).
        assertEquals(1, metodoNc.diaDoCiclo(DataCivil.deIso("2026-09-15")))
        assertEquals(17, metodoNc.diaDoCiclo(DataCivil.deIso("2026-10-01")))
        assertEquals(25, metodoNc.diaDoCiclo(DataCivil.deIso("2027-01-01")))

        val dia1 = metodoNc.sugestao(DataCivil.deIso("2026-09-15"), TrainingLevel.INTERMEDIARIO)!!
        assertEquals("Técnica", dia1.focus)
        assertEquals("A1", dia1.zona)
        assertTrue(dia1.objetivo!!.startsWith("Técnica do crawl"))
        assertNotNull(dia1.ajuste)
        assertEquals(listOf("Ativação", "Preparação", "Desenvolvimento", "Recuperação"), dia1.phases.map { it.title })
        val corretivo = dia1.phases[1].sets[0]
        assertEquals("A0", corretivo.zona)
        assertEquals("Rolamento com mãos na coxa", corretivo.corretivo?.nome)
        assertTrue(corretivo.details.any { "nado completo" in it })

        assertEquals("AN", metodoNc.sugestao(DataCivil.deIso("2026-10-01"), TrainingLevel.AVANCADO)!!.zona)
    }

    @Test
    fun `todo treino do metodo NC tem blocos NC, zona em cada serie e metragem que fecha`() {
        val blocos = setOf("Ativação", "Preparação", "Desenvolvimento", "Consolidação", "Recuperação")
        val zonas = setOf("A0", "A1", "A2", "A3", "AN", "AA")
        for (i in 0 until metodoNc.dias) {
            TrainingLevel.entries.forEach { level ->
                val t = metodoNc.sugestao(metodoNc.ancoraEpochDay + i, level)!!
                val rotulo = "dia ${i + 1} $level"
                assertTrue(rotulo, t.phases.all { it.title in blocos })
                assertTrue(rotulo, t.zona in zonas && t.objetivo != null)
                assertTrue(rotulo, t.phases.flatMap { it.sets }.all { it.zona in zonas })
                assertEquals(rotulo, t.totalDistanceMeters, t.phases.sumOf { it.distanceMeters })
                assertEquals(rotulo, 100, t.phases.sumOf { it.percentage })
            }
        }
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
