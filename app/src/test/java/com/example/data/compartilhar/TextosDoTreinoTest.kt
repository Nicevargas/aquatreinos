package com.example.data.compartilhar

import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextosDoTreinoTest {

    private val doCarrossel = ResumoDoTreino(
        titulo = "Regenerativo", foco = "Regenerativo", nivel = TrainingLevel.INTERMEDIARIO,
        dataIso = "2026-09-13", metrosFeitos = 1800, metrosPlanejados = 1800, seriesFeitas = 5, seriesPlanejadas = 5,
        duracaoSegundos = 2_520, intensidade = 6, complexidade = 4, cicloDia = 14, doCarrossel = true
    )

    @Test
    fun `numeros no jeito brasileiro`() {
        assertEquals("1.800m", TextosDoTreino.metros(1800))
        assertEquals("950m", TextosDoTreino.metros(950))
        assertEquals("menos de 1 min", TextosDoTreino.duracao(59))
        assertEquals("42 min", TextosDoTreino.duracao(2_520))
        assertEquals("1h05", TextosDoTreino.duracao(3_900))
        assertEquals("Intermediário", TextosDoTreino.nivel(TrainingLevel.INTERMEDIARIO))
        assertEquals("Iniciante · Menor volume", TextosDoTreino.nivel(TrainingLevel.INICIANTE))
    }

    @Test
    fun `legenda do treino do carrossel concluido`() {
        assertEquals(
            "🏊 Treino concluído: 1.800m em 42 min!\n\n" +
                "Dia 14 do Cada Dia 1 Treino — foco em Regenerativo, nível Intermediário.\n\n" +
                "Intensidade: 6/10 · Complexidade: 4/10\n\n" +
                "Treino de @natacaocriativa\n\n" +
                TextosDoTreino.HASHTAGS,
            TextosDoTreino.legenda(doCarrossel)
        )
    }

    @Test
    fun `legenda de treino parcial e de treino proprio sem notas`() {
        val parcial = doCarrossel.copy(metrosFeitos = 1100, duracaoSegundos = 1_800)
        assertFalse(parcial.completo)
        assertTrue(TextosDoTreino.legenda(parcial).startsWith("🏊 Nadei 1.100m de 1.800m em 30 min."))

        val proprio = doCarrossel.copy(titulo = "Tiros de sábado", foco = null, doCarrossel = false, cicloDia = null, intensidade = null, complexidade = null)
        val legenda = TextosDoTreino.legenda(proprio)
        assertTrue(legenda.contains("Treino \"Tiros de sábado\" — nível Intermediário."))
        assertFalse(legenda.contains("Intensidade"))
        assertFalse(legenda.contains("@natacaocriativa"))
    }
}
