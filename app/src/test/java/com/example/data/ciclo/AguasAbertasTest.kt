package com.example.data.ciclo

import com.example.data.treinos.MontadorDeTreino
import com.example.model.ModoDeTreino
import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AguasAbertasTest {

    private fun asset(nome: String) = File("src/main/assets/$nome").readText()

    private val treinos = TreinosSugeridosRepository { modo ->
        when (modo) {
            ModoDeTreino.PISCINA -> listOf(asset("treinos_ciclo.json"), asset("programa_nc.json"))
            ModoDeTreino.AGUAS_ABERTAS -> listOf(asset("programa_aa.json"))
        }
    }
    private val quinta = DataCivil.deIso("2026-09-17")

    @Test
    fun `aguas abertas e piscina no mesmo dia sao treinos diferentes`() {
        val piscina = treinos.embarcado(quinta, TrainingLevel.INTERMEDIARIO)
        val aguas = treinos.embarcado(quinta, TrainingLevel.INTERMEDIARIO, ModoDeTreino.AGUAS_ABERTAS)!!
        assertEquals("Velocidade", piscina.title)
        assertEquals("Mudança de ritmo", aguas.title)
        assertTrue(aguas.tag.startsWith("Águas abertas"))
        assertTrue("dica traz o aviso de segurança", aguas.motivationalTip.contains("nunca nade sozinho"))
        assertEquals("A3", aguas.zona)
    }

    @Test
    fun `pre-condicionamento nao tem aguas abertas`() {
        assertNull(treinos.embarcado(quinta, TrainingLevel.INICIANTE, ModoDeTreino.AGUAS_ABERTAS))
        assertTrue(ModoDeTreino.AGUAS_ABERTAS.temTreinoPara(TrainingLevel.AVANCADO))
        assertTrue(ModoDeTreino.PISCINA.temTreinoPara(TrainingLevel.INICIANTE))
    }

    @Test
    fun `os 56 treinos de aguas abertas abrem com zona em toda serie`() {
        val ciclo = CicloDeTreinos.deJson(asset("programa_aa.json"))
        for (dia in 0L until 28L) {
            for (level in listOf(TrainingLevel.INTERMEDIARIO, TrainingLevel.AVANCADO)) {
                val t = ciclo.sugestao(ciclo.ancoraEpochDay + dia, level)
                assertNotNull("dia ${dia + 1} $level", t)
                t!!
                assertTrue(t.phases.isNotEmpty())
                assertEquals(t.totalDistanceMeters, t.phases.sumOf { it.distanceMeters })
                assertTrue("${t.id} sem zona", t.phases.flatMap { it.sets }.all { it.zona != null })
            }
        }
    }

    @Test
    fun `sete partes da arte, e o editor nao perde nenhuma serie`() {
        val aguas = treinos.embarcado(quinta, TrainingLevel.AVANCADO, ModoDeTreino.AGUAS_ABERTAS)!!
        assertEquals(
            listOf("Respiração", "Corretivos", "Ativação", "Pernas + braço", "Desenvolvimento", "Consolidação", "Recuperação"),
            aguas.phases.map { it.title }
        )
        val digitado = MontadorDeTreino.paraDigitacao(aguas, quinta)
        assertEquals(aguas.phases.sumOf { it.sets.size }, digitado.fases.values.sumOf { it.size })
        assertEquals(2, digitado.fases.getValue("Recuperação").size + digitado.fases.getValue("Consolidação").size)
    }
}
