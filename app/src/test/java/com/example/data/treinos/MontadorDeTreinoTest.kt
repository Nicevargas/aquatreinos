package com.example.data.treinos

import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.data.supabase.WorkoutDto
import com.example.data.supabase.toDomain
import com.example.model.TrainingLevel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MontadorDeTreinoTest {

    private val json = File("src/main/assets/programa_nc.json").readText()
    private val cicloAntigo = CicloDeTreinos.deJson(File("src/main/assets/treinos_ciclo.json").readText())

    @Test
    fun `remontar cada sugestao do metodo NC da a mesma estrutura que o script gerou`() {
        // Leitura crua do asset, para comparar com o que o Python gravou no banco.
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val tipo = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        @Suppress("UNCHECKED_CAST")
        val cru = moshi.adapter<Map<String, Any>>(tipo).fromJson(json)!!["treinos"] as List<Map<String, Any>>
        val adapterDto = moshi.adapter(WorkoutDto::class.java)
        val dtos = cru.map { adapterDto.fromJsonValue(it)!! }

        assertEquals(84, dtos.size)
        for (dto in dtos) {
            val rotulo = dto.id!!
            val original = dto.toDomain()
            val digitado = MontadorDeTreino.paraDigitacao(original, DataCivil.deIso("2026-09-28"))
            val montado = (MontadorDeTreino.montar(digitado) as? Montagem.Pronto)?.treino
                ?: error("$rotulo não montou: ${(MontadorDeTreino.montar(digitado) as Montagem.ComErros).erros}")

            // A duração do método usa outra estimativa (por zona); o resto tem de bater.
            assertEquals(rotulo, dto.totalDistanceMeters, montado.totalDistanceMeters)
            assertEquals(rotulo, dto.level, montado.level)
            assertEquals(rotulo, "2026-09-28", montado.workoutDate)

            val esperadas = dto.phases!!
            assertEquals(rotulo, esperadas.map { it.title }, montado.phases.map { it.title })
            esperadas.zip(montado.phases).forEach { (e, m) ->
                assertEquals(rotulo, e.summary, m.summary)
                assertEquals(rotulo, e.distanceMeters, m.distanceMeters)
                assertEquals(rotulo, e.percentage, m.percentage)
                assertEquals(rotulo, e.sets!!.size, m.sets!!.size)
                e.sets!!.zip(m.sets!!).forEach { (se, sm) ->
                    assertEquals(rotulo, se.serie, sm.serie)
                    assertEquals(rotulo, se.repsDescription, sm.repsDescription)
                    assertEquals(rotulo, se.stroke, sm.stroke)
                    assertEquals(rotulo, se.details, sm.details)
                    assertEquals(rotulo, se.interval, sm.interval)
                    assertEquals(rotulo, se.restSeconds, sm.restSeconds)
                    assertEquals(rotulo, se.equipment, sm.equipment)
                    assertEquals(rotulo, se.distanceMeters, sm.distanceMeters)
                    assertEquals(rotulo, se.zona, sm.zona)
                    assertEquals(rotulo, se.pse, sm.pse)
                    assertEquals(rotulo, se.corretivo, sm.corretivo)
                }
            }
        }
    }

    @Test
    fun `treino salvo antes do metodo abre nos blocos equivalentes`() {
        val antigo = cicloAntigo.sugestao(DataCivil.deIso("2026-09-13"), TrainingLevel.INTERMEDIARIO)!!
        assertEquals(listOf("Aquecimento", "Principal", "Final"), antigo.phases.map { it.title })

        val digitado = MontadorDeTreino.paraDigitacao(antigo)
        assertEquals(1, digitado.fases.getValue("Ativação").size)
        assertEquals(2, digitado.fases.getValue("Desenvolvimento").size)
        assertEquals(2, digitado.fases.getValue("Recuperação").size)
        val montado = (MontadorDeTreino.montar(digitado) as Montagem.Pronto).treino
        assertEquals(listOf("Ativação", "Desenvolvimento", "Recuperação"), montado.phases.map { it.title })
        assertEquals(antigo.totalDistanceMeters, montado.totalDistanceMeters)
    }

    @Test
    fun `le cabecalho no formato do carrossel`() {
        assertEquals(MontadorDeTreino.Cabecalho(8, 50, "Crawl"), MontadorDeTreino.lerCabecalho("8x50m Crawl"))
        assertEquals(MontadorDeTreino.Cabecalho(8, 75, ""), MontadorDeTreino.lerCabecalho(" 8 X 75m "))
        assertEquals(MontadorDeTreino.Cabecalho(1, 400, "Crawl leve"), MontadorDeTreino.lerCabecalho("400m Crawl leve"))
        assertNull(MontadorDeTreino.lerCabecalho("Crawl 400m"))
        assertNull(MontadorDeTreino.lerCabecalho("400 Crawl"))
        assertNull(MontadorDeTreino.lerCabecalho("8x50 Crawl"))
    }

    @Test
    fun `le intervalo aberto, fechado ou so o tempo`() {
        assertEquals("" to 0, MontadorDeTreino.lerIntervalo(""))
        assertEquals("#20\"" to 20, MontadorDeTreino.lerIntervalo("#20\""))
        assertEquals("@1'45\"" to 105, MontadorDeTreino.lerIntervalo("@1'45\""))
        assertEquals("#1'" to 60, MontadorDeTreino.lerIntervalo("#1'"))
        assertEquals("20\"" to 20, MontadorDeTreino.lerIntervalo("20\""))
        assertEquals("20\"" to 20, MontadorDeTreino.lerIntervalo("20"))
        assertEquals("45\"" to 45, MontadorDeTreino.lerIntervalo("45s"))
        assertEquals("1'30\"" to 90, MontadorDeTreino.lerIntervalo("1'30\""))
        assertEquals("2'" to 120, MontadorDeTreino.lerIntervalo("2'"))
        assertNull(MontadorDeTreino.lerIntervalo("vinte"))
        assertNull(MontadorDeTreino.lerIntervalo("1'75\""))
        assertNull(MontadorDeTreino.lerIntervalo("#"))
    }

    @Test
    fun `aponta cada erro do formulario`() {
        val digitado = TreinoDigitado(
            titulo = " ",
            data = "31/02/2026",
            level = TrainingLevel.INICIANTE,
            fases = mapOf(
                "Ativação" to listOf(SerieDigitada(serie = "Crawl leve")),
                "Desenvolvimento" to listOf(
                    SerieDigitada(serie = "8x50m Crawl", intervalo = "rápido"),
                    SerieDigitada(serie = "4x100m Crawl", intervalo = "#20\"", zona = "Z3")
                ),
                "Recuperação" to listOf(SerieDigitada()) // linha vazia é ignorada
            )
        )
        val erros = (MontadorDeTreino.montar(digitado) as Montagem.ComErros).erros
        assertEquals(5, erros.size)
        assertTrue(erros[0].contains("nome"))
        assertTrue(erros[1].contains("Data inválida"))
        assertTrue(erros[2].startsWith("Ativação, série 1"))
        assertTrue(erros[3].startsWith("Desenvolvimento, série 1") && erros[3].contains("intervalo"))
        assertTrue(erros[4].startsWith("Desenvolvimento, série 2") && erros[4].contains("zona"))

        val vazio = MontadorDeTreino.montar(TreinoDigitado(titulo = "Nada", data = "13/09/2026"))
        assertEquals(listOf("Adicione pelo menos uma série."), (vazio as Montagem.ComErros).erros)
    }

    @Test
    fun `monta treino digitado com bloco vazio, zona, saida fechada e material`() {
        val digitado = TreinoDigitado(
            titulo = "Treino de sábado",
            data = "19/09/2026",
            level = TrainingLevel.AVANCADO,
            fases = mapOf(
                "Ativação" to emptyList(),
                "Desenvolvimento" to listOf(
                    SerieDigitada("10x100m Crawl c/ Palmar", "50m ritmo; 50m solto", "15", zona = "a2"),
                    SerieDigitada("400m Pernada", "com prancha", ""),
                    SerieDigitada("4x200m Crawl", "", "@3'30\"", zona = "A2")
                ),
                "Recuperação" to listOf(SerieDigitada("200m Costas solto", zona = "A0"))
            )
        )
        val treino = (MontadorDeTreino.montar(digitado) as Montagem.Pronto).treino
        assertEquals("2026-09-19", treino.workoutDate)
        assertEquals("Aperfeiçoamento", treino.subtitle)
        assertEquals(2400, treino.totalDistanceMeters)
        assertEquals(listOf("Desenvolvimento", "Recuperação"), treino.phases.map { it.title })
        assertEquals(listOf(92, 8), treino.phases.map { it.percentage })

        val series = treino.phases[0].sets!!
        assertEquals("15\"", series[0].interval)
        assertEquals(15, series[0].restSeconds)
        assertEquals("A2", series[0].zona)
        assertEquals(listOf("50m ritmo", "50m solto"), series[0].details)
        assertEquals("Palmar", series[0].equipment)
        assertEquals("Prancha", series[1].equipment) // detectado nos detalhes
        assertNull(series[1].zona)
        assertEquals("@3'30\"", series[2].interval)
        assertEquals(0, series[2].restSeconds) // saída fechada: a pausa depende de quem nada
        assertNull(treino.phases[1].sets!![0].interval)
        assertEquals("A0", treino.phases[1].sets!![0].zona)
    }

    @Test
    fun `datas brasileiras`() {
        assertEquals(DataCivil.deIso("2026-09-13"), DataCivil.lerDataBr("13/09/2026"))
        assertEquals("13/09/2026", DataCivil.paraBr(DataCivil.deIso("2026-09-13")))
        assertEquals(DataCivil.deIso("2028-02-29"), DataCivil.lerDataBr("29/02/2028"))
        assertNull(DataCivil.lerDataBr("29/02/2027"))
        assertNull(DataCivil.lerDataBr("2026-09-13"))
        assertNull(DataCivil.lerDataBr("13/9/26"))
        // Sugestão remontada fica igual ao ciclo, mesmo em outra data.
        val sugestao = cicloAntigo.sugestao(DataCivil.deIso("2026-09-13"), TrainingLevel.INTERMEDIARIO)!!
        assertEquals("13/09/2026", MontadorDeTreino.paraDigitacao(sugestao).data)
    }
}
