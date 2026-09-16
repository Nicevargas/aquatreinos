package com.example.data.plano

import com.example.data.ciclo.CicloDeTreinos
import com.example.data.execucao.ProgressoExecucao
import com.example.data.execucao.RegistroDoTreino
import com.example.data.execucao.RoteiroDeTreino
import com.example.data.supabase.toDomain
import com.example.model.TrainingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlanoDeTreinoTest {

    private val treinosNC = CicloDeTreinos.treinosDoJson(File("src/main/assets/programa_nc.json").readText())

    private fun config(
        semanas: Int = 8,
        porSemana: Int = 3,
        focos: List<String> = emptyList(),
        relaxadas: Boolean = true,
        encerramento: Boolean = true,
        nivel: TrainingLevel = TrainingLevel.INTERMEDIARIO,
        trocas: Map<String, String> = emptyMap()
    ) = ConfigDoPlano(semanas, porSemana, focos, relaxadas, encerramento, nivel, trocas)

    @Test
    fun `semanas seguem Base, Construcao, Pico e a leve`() {
        val c = config(semanas = 8)
        assertEquals(
            listOf("Base", "Construção", "Pico", "Regeneração", "Base", "Construção", "Pico", "Regeneração"),
            (1..8).map { PlanoDeTreino.blocoDaSemana(it, c) }
        )
        val semLeve = config(semanas = 5, relaxadas = false, encerramento = false)
        assertEquals(listOf("Base", "Construção", "Pico", "Base", "Construção"), (1..5).map { PlanoDeTreino.blocoDaSemana(it, semLeve) })
        val soEncerramento = config(semanas = 5, relaxadas = false, encerramento = true)
        assertEquals("Base", PlanoDeTreino.blocoDaSemana(4, soEncerramento))
        assertEquals("Regeneração", PlanoDeTreino.blocoDaSemana(5, soEncerramento))
        assertEquals("plano de 1 semana não é só descanso", "Base", PlanoDeTreino.blocoDaSemana(1, config(semanas = 1)))
    }

    @Test
    fun `o que a pessoa quer melhorar se alterna com as outras capacidades`() {
        assertEquals(listOf("Velocidade", "Técnica", "Velocidade"), PlanoDeTreino.focosDaSemana(1, 3, listOf("Velocidade")))
        assertEquals(listOf("Velocidade", "Resistência", "Velocidade"), PlanoDeTreino.focosDaSemana(2, 3, listOf("Velocidade")))
        assertEquals("todas: uma de cada", PlanoDeTreino.FOCOS, PlanoDeTreino.focosDaSemana(1, 6, emptyList()))
        assertEquals(listOf("Técnica", "Resistência"), PlanoDeTreino.focosDaSemana(1, 2, PlanoDeTreino.FOCOS))
        assertEquals("foco desconhecido vira todas", PlanoDeTreino.FOCOS.take(2), PlanoDeTreino.focosDaSemana(1, 2, listOf("Respiração")))
    }

    @Test
    fun `monta todas as semanas com treinos do Metodo NC do nivel`() {
        val semanas = PlanoDeTreino.montar(config(focos = listOf("Técnica")), treinosNC, "p1")
        assertEquals(8, semanas.size)
        semanas.forEach { s ->
            assertEquals("semana ${s.numero} com 3 treinos", 3, s.treinos.size)
            s.treinos.forEachIndexed { i, t ->
                assertEquals(s.numero, t.workout.plano?.semana)
                assertEquals(i + 1, t.workout.plano?.treino)
                assertEquals("p1", t.workout.plano?.planoId)
                assertEquals(TrainingLevel.INTERMEDIARIO, t.workout.level)
                assertTrue(t.workout.id, t.workout.id.startsWith("nc_") && t.workout.id.endsWith("_intermediario"))
                assertTrue(t.workout.isSuggestion)
                assertTrue(t.metros > 0)
            }
        }
        // Semana 4 é leve: os treinos da Regeneração (dias 22 a 28 do programa).
        assertEquals(listOf("nc_d22_intermediario", "nc_d26_intermediario", "nc_d22_intermediario"), semanas[3].treinos.map { it.workout.id })
        assertTrue(semanas[3].relaxada)
        assertEquals("Plano · Semana 4 · Treino 2", semanas[3].treinos[1].workout.tag)
    }

    @Test
    fun `resumo conta o que foi feito e aponta o proximo`() {
        val c = config()
        val semanas = PlanoDeTreino.montar(c, treinosNC, "p1")
        val r = PlanoDeTreino.resumo(c, semanas, setOf(1 to 1, 1 to 2))
        assertEquals(24, r.total)
        assertEquals(2, r.feitos)
        assertEquals(1, r.proximo?.semana)
        assertEquals(3, r.proximo?.numero)

        val tudo = semanas.flatMap { s -> s.treinos.map { it.semana to it.numero } }.toSet()
        assertNull(PlanoDeTreino.resumo(c, semanas, tudo).proximo)
    }

    @Test
    fun `nadador troca um treino por outra capacidade ou por um treino dele`() {
        val meu = treinosNC.first { it.foco == "Ritmo" && it.bloco == "Base" && it.level == "INICIANTE" }.toDomain()
            .copy(id = "11111111-2222-3333-4444-555555555555", title = "Meu treino de sábado", isSuggestion = false, focus = null)
        val c = config(
            trocas = mapOf(
                "2-3" to PlanoDeTreino.trocaPorFoco("Estilos"),
                "1-1" to PlanoDeTreino.trocaPorMeuTreino(meu.id),
                "3-1" to PlanoDeTreino.trocaPorMeuTreino("apagado"),
                "3-2" to PlanoDeTreino.trocaPorFoco("Respiração")
            )
        )
        val semanas = PlanoDeTreino.montar(c, treinosNC, "p1", listOf(meu))

        val trocado = semanas[1].treinos[2]
        assertEquals(OrigemDoTreino.TROCADO, trocado.origem)
        assertEquals("Estilos", trocado.foco)
        assertEquals("mesma fase da semana 2", "nc_d11_intermediario", trocado.workout.id)

        val seu = semanas[0].treinos[0]
        assertEquals(OrigemDoTreino.MEU_TREINO, seu.origem)
        assertEquals("Seu treino", seu.foco)
        assertEquals(meu.id, seu.workout.id)
        assertEquals("p1", seu.workout.plano?.planoId)
        assertEquals("Plano · Semana 1 · Treino 1", seu.workout.tag)

        assertEquals("treino apagado volta ao sugerido", OrigemDoTreino.METODO_NC, semanas[2].treinos[0].origem)
        assertEquals("capacidade que não existe é ignorada", OrigemDoTreino.METODO_NC, semanas[2].treinos[1].origem)

        val opcoes = PlanoDeTreino.opcoesDoMetodo(c, 4, treinosNC)
        assertEquals(PlanoDeTreino.FOCOS, opcoes.map { it.focus })
        assertTrue(opcoes.all { it.id.startsWith("nc_d2") })

        val roteiro = RoteiroDeTreino(seu.workout)
        val registro = RegistroDoTreino.montar(roteiro, ProgressoExecucao(roteiro.passos.size, 0), 1800, 6, null, "", "2026-09-15")
        assertEquals("treino de Meus treinos grava workout_id", meu.id, registro.workoutId)
        assertNull(registro.treinoCicloId)
        assertEquals(1, registro.planoSemana)
    }

    @Test
    fun `treino do plano salvo leva plano, semana e numero`() {
        val treino = PlanoDeTreino.montar(config(), treinosNC, "11111111-2222-3333-4444-555555555555")[1].treinos[2].workout
        val roteiro = RoteiroDeTreino(treino)
        val registro = RegistroDoTreino.montar(roteiro, ProgressoExecucao(roteiro.passos.size, 0), 1800, 6, null, "", "2026-09-15")
        assertEquals("11111111-2222-3333-4444-555555555555", registro.planoId)
        assertEquals(2, registro.planoSemana)
        assertEquals(3, registro.planoTreino)
        assertEquals(treino.id, registro.treinoCicloId)
    }
}
