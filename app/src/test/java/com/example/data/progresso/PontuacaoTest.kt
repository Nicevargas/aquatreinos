package com.example.data.progresso

import com.example.data.ciclo.DataCivil
import com.example.data.lembrete.LembreteDeTreino
import com.example.data.ranking.OpcoesDoRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class PontuacaoTest {

    private fun dia(iso: String) = DataCivil.deIso(iso)
    private fun treino(iso: String, metros: Int, completo: Boolean, esforco: Int? = 5) =
        Atividade(null, dia(iso), "Treino", "Técnica", metros, 1800, completo, false, esforco)

    @Test
    fun `pontos por treino e por semana, iguais aos do ranking no banco`() {
        assertEquals(30, Pontuacao.doTreino(2000, true))
        assertEquals(15, Pontuacao.doTreino(1550, false))
        assertEquals(0, Pontuacao.doTreino(-10, false))
        // Mesmo caso do testar_ranking.mjs: 2 treinos na mesma semana = 65.
        val mesmaSemana = listOf(treino("2026-09-15", 2000, true), treino("2026-09-16", 1500, false))
        assertEquals(65, Pontuacao.total(mesmaSemana))
        assertEquals(65 + 20 + 20, Pontuacao.total(mesmaSemana + treino("2026-09-08", 1000, true)))
        assertEquals(0, Pontuacao.total(emptyList()))
    }

    @Test
    fun `niveis`() {
        assertEquals("Bronze", Pontuacao.nivel(0).nome)
        assertEquals("Bronze", Pontuacao.nivel(299).nome)
        assertEquals("Prata", Pontuacao.nivel(300).nome)
        assertEquals("Diamante", Pontuacao.nivel(100_000).nome)
        assertEquals("Prata", Pontuacao.proximoNivel(10)?.nome)
        assertNull(Pontuacao.proximoNivel(8000))
    }

    @Test
    fun `tabela por semana e por mes, do periodo atual para tras`() {
        val hoje = dia("2026-09-15")
        val lista = listOf(
            treino("2026-09-14", 2000, true, 6),
            treino("2026-09-15", 1000, false, 8),
            treino("2026-09-08", 1500, true, null),
            treino("2026-08-20", 3000, true, 4)
        )
        val semanas = Progresso.porSemana(lista, hoje, quantas = 3)
        assertEquals(listOf("14/09", "07/09", "31/08"), semanas.map { it.rotulo })
        assertEquals(2, semanas[0].treinos)
        assertEquals(3000, semanas[0].metros)
        assertEquals(7.0, semanas[0].esforcoMedio!!, 0.001)
        assertNull("semana sem nota não inventa média", semanas[1].esforcoMedio)
        assertEquals(0, semanas[2].treinos)

        val meses = Progresso.porMes(lista, hoje, quantos = 13)
        assertEquals("set/26", meses[0].rotulo)
        assertEquals("ago/26", meses[1].rotulo)
        assertEquals("set/25", meses[12].rotulo)
        assertEquals(4500, meses[0].metros)
        assertEquals(3000, meses[1].metros)
    }

    @Test
    fun `lembrete de voltar a nadar`() {
        val hoje = dia("2026-09-15")
        val dias = LembreteDeTreino.proximosDias(hoje)
        assertEquals(7, dias.size)
        assertEquals(hoje + 1, dias.first())
        assertEquals("na quarta, 16/09", LembreteDeTreino.rotulo(dia("2026-09-16")))
        assertEquals("no sábado, 19/09", LembreteDeTreino.rotulo(dia("2026-09-19")))
        // 7h de 16/09/2026 em Brasília (UTC-3) = 10h UTC.
        assertEquals(
            dia("2026-09-16") * 86_400_000L + 10 * 3_600_000L,
            LembreteDeTreino.horarioDoAviso(dia("2026-09-16"), 7, TimeZone.getTimeZone("America/Sao_Paulo"))
        )
    }

    @Test
    fun `nome sugerido para o ranking`() {
        assertEquals("Ana S.", OpcoesDoRanking.nomeSugerido("Ana Maria dos Santos"))
        assertEquals("Bruno", OpcoesDoRanking.nomeSugerido(" Bruno "))
        assertEquals("", OpcoesDoRanking.nomeSugerido("   "))
    }
}
