package com.example.data.progresso

import com.example.data.ciclo.DataCivil
import com.example.data.execucao.TreinoRealizadoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressoTest {

    private fun dia(iso: String) = DataCivil.deIso(iso)
    private fun treino(iso: String, metros: Int = 1500, notas: Boolean = false, id: String? = null) =
        Atividade(id, dia(iso), "Treino", "Técnica", metros, 1800, true, notas, 5)

    // 15/09/2026 é uma terça; a semana começa na segunda 14/09.
    private val hoje = dia("2026-09-15")

    @Test
    fun `serie conta semanas seguidas e a semana atual ainda nao quebra`() {
        val dias = listOf(dia("2026-09-01"), dia("2026-09-08"))
        assertEquals(SerieDeSemanas(2, false), Progresso.serieDeSemanas(dias, hoje))
        assertEquals(SerieDeSemanas(3, true), Progresso.serieDeSemanas(dias + hoje, hoje))
        assertEquals("buraco zera", SerieDeSemanas(1, false), Progresso.serieDeSemanas(listOf(dia("2026-08-20"), dia("2026-09-08")), hoje))
        assertEquals(SerieDeSemanas(0, false), Progresso.serieDeSemanas(emptyList(), hoje))
        assertEquals("duas semanas sem nadar", 0, Progresso.serieDeSemanas(listOf(dia("2026-08-31")), hoje).semanas)
    }

    @Test
    fun `sete dias ate hoje`() {
        val sete = Progresso.ultimosSeteDias(listOf(dia("2026-09-10"), hoje), hoje)
        assertEquals(7, sete.size)
        assertEquals(dia("2026-09-09"), sete.first().first)
        assertEquals(hoje to true, sete.last())
        assertTrue(sete[1].second)
        assertFalse(sete[0].second)
    }

    @Test
    fun `meses e metros acumulados`() {
        assertEquals(28, Progresso.diasNoMes(2026, 2))
        assertEquals(29, Progresso.diasNoMes(2028, 2))
        assertEquals(31, Progresso.diasNoMes(2026, 12))
        val acumulado = Progresso.metrosAcumuladosNoMes(
            listOf(treino("2026-09-02", 1000), treino("2026-09-02", 500), treino("2026-09-04", 2000), treino("2026-08-31", 9000)),
            2026, 9, ateDia = 5
        )
        assertEquals(listOf(0, 1500, 1500, 3500, 3500), acumulado)
    }

    @Test
    fun `conquistas com o dia de cada uma`() {
        val lista = listOf(
            treino("2026-08-25", 1000, notas = true),
            treino("2026-09-01", 2100, notas = true),
            treino("2026-09-08", 1500, notas = true),
            treino("2026-09-09", 1500, notas = true),
            treino("2026-09-15", 5000, notas = true)
        )
        val c = Progresso.conquistas(lista).associateBy { it.id }
        assertEquals(dia("2026-08-25"), c.getValue("primeiro-treino").dia)
        assertEquals(dia("2026-08-25"), c.getValue("treino-1000").dia)
        assertEquals(dia("2026-09-01"), c.getValue("treino-2000").dia)
        assertEquals(dia("2026-09-01"), c.getValue("semanas-2").dia)
        assertEquals(dia("2026-09-15"), c.getValue("treinos-5").dia)
        assertEquals(dia("2026-09-15"), c.getValue("treino-5000").dia)
        assertEquals(dia("2026-09-15"), c.getValue("mes-10km").dia)
        assertEquals(dia("2026-09-15"), c.getValue("semanas-4").dia)
        assertEquals(dia("2026-09-15"), c.getValue("notas-5").dia)
        assertFalse("20 km no mês ainda não", "mes-20km" in c)
        assertTrue("todo prêmio está no catálogo", c.keys.all { id -> Progresso.CATALOGO.any { it.id == id } })
        assertEquals("ids do catálogo não se repetem", Progresso.CATALOGO.size, Progresso.CATALOGO.map { it.id }.toSet().size)
    }

    @Test
    fun `premios novos sao so os que o ultimo treino liberou`() {
        val antes = listOf(treino("2026-09-08", 1000))
        val depois = antes + treino("2026-09-15", 2000)
        assertEquals(listOf("treino-2000", "semanas-2"), Progresso.novasConquistas(antes, depois).map { it.id })
        assertTrue(Progresso.novasConquistas(depois, depois).isEmpty())
    }

    @Test
    fun `proximos premios sao o proximo de cada trilha`() {
        val proximos = Progresso.proximasConquistas(listOf("primeiro-treino", "treino-1000", "semanas-2")).map { it.id }
        assertEquals(listOf("treinos-5", "treino-2000", "semanas-4", "mes-10km", "notas-5"), proximos)
    }

    @Test
    fun `painel junta tudo com o historico do mais recente ao mais antigo`() {
        val lista = listOf(
            treino("2026-08-28", 3000, id = "a"),
            treino("2026-09-15", 2000, id = "b"),
            treino("2026-09-03", 1000, id = "c")
        )
        val p = Progresso.painel(lista, hoje)
        assertEquals(listOf("b", "c", "a"), p.atividades.map { it.id })
        assertEquals(Totais(3, 6000, 5400, 3000), p.totais)
        assertEquals("setembro", p.nomeMesAtual)
        assertEquals("agosto", p.nomeMesAnterior)
        assertEquals(30, p.diasNoMesAtual)
        assertEquals(15, p.metrosMesAtual.size)
        assertEquals(3000, p.metrosMesAtual.last())
        assertEquals(31, p.metrosMesAnterior.size)
        assertEquals(3000, p.metrosMesAnterior.last())
        assertTrue(p.serie.nadouEstaSemana)
        assertTrue(p.proximasConquistas.none { prox -> p.conquistas.any { it.id == prox.id } })

        val vazio = Progresso.painel(emptyList(), hoje)
        assertTrue(vazio.atividades.isEmpty())
        assertNull(vazio.esforco.media)
        assertEquals("primeiro-treino", vazio.proximasConquistas.first().id)
    }

    @Test
    fun `linha do banco vira atividade e data ruim nao derruba`() {
        val dto = TreinoRealizadoDto(
            id = "x", titulo = "Técnica", foco = "Técnica", nivel = "INICIANTE", dataTreino = "2026-09-15",
            metrosPlanejados = 1300, metrosFeitos = 1300, seriesPlanejadas = 5, seriesFeitas = 5,
            duracaoSegundos = 3000, intensidade = 7
        )
        val a = dto.paraAtividade()!!
        assertEquals(hoje, a.dia)
        assertTrue(a.completo)
        assertEquals(7, a.esforco)
        assertFalse("sem complexidade não conta como notas completas", a.deuNotas)
        assertNull(dto.copy(dataTreino = "15/09/2026").paraAtividade())
    }
}
