package com.example.data.execucao

import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.data.WorkoutRepository
import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class RegistroDoTreinoTest {

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/treinos_ciclo.json").readText())
    private val sugestao = ciclo.sugestao(DataCivil.deIso("2026-09-13"), TrainingLevel.INTERMEDIARIO)!!

    @Test
    fun `sugestao do ciclo aponta para treinos_ciclo`() {
        val roteiro = RoteiroDeTreino(sugestao)
        val r = RegistroDoTreino.montar(roteiro, ProgressoExecucao(5, 0), 2_520, 6, 4, "  foi bom  ", "2026-09-14")
        assertEquals("ciclo_d14_intermediario", r.treinoCicloId)
        assertNull(r.workoutId)
        assertEquals("Regenerativo", r.foco)
        assertEquals("INTERMEDIARIO", r.nivel)
        assertEquals("2026-09-14", r.dataTreino)
        assertEquals(1800, r.metrosFeitos)
        assertEquals(5, r.seriesFeitas)
        assertEquals("foi bom", r.observacao)

        val resumo = RegistroDoTreino.resumo(r, roteiro)
        assertEquals(14, resumo.cicloDia)
        assertEquals(true, resumo.doCarrossel)
        assertEquals(true, resumo.completo)
    }

    @Test
    fun `treino de Meus treinos aponta para workouts e parcial grava o que foi feito`() {
        val meu = sugestao.copy(id = "3f2b1c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d", isSuggestion = false)
        val roteiro = RoteiroDeTreino(meu)
        val r = RegistroDoTreino.montar(roteiro, ProgressoExecucao(2, 3), 900, null, null, "", "2026-09-13")
        assertEquals(meu.id, r.workoutId)
        assertNull(r.treinoCicloId)
        assertEquals(1025, r.metrosFeitos) // 300 + 500 + 3x75
        assertEquals(2, r.seriesFeitas)
        assertNull(r.intensidade)
        assertNull(r.observacao)
    }

    @Test
    fun `treino de exemplo embarcado nao aponta para nada e valores sao limitados`() {
        val exemplo = WorkoutRepository.getWorkoutForLevel(TrainingLevel.INTERMEDIARIO)
        val roteiro = RoteiroDeTreino(exemplo)
        val r = RegistroDoTreino.montar(roteiro, ProgressoExecucao(99, 0), 999_999, 15, -3, "x".repeat(900), "2026-09-13")
        assertNull(r.workoutId)
        assertNull(r.treinoCicloId)
        assertEquals(86_400, r.duracaoSegundos)
        assertEquals(10, r.intensidade)
        assertEquals(0, r.complexidade)
        assertEquals(500, r.observacao!!.length)
        assertEquals(roteiro.metrosTotais, r.metrosFeitos)
    }
}
