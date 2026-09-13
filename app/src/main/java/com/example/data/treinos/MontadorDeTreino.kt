package com.example.data.treinos

import com.example.data.ciclo.DataCivil
import com.example.data.supabase.WorkoutPhaseDto
import com.example.data.supabase.WorkoutSetDto
import com.example.data.supabase.WorkoutWriteDto
import com.example.model.TrainingLevel
import com.example.model.Workout
import java.util.Locale
import kotlin.math.round

/** Uma série como a pessoa digita, no formato do carrossel. */
data class SerieDigitada(
    val serie: String = "", // "8x50m Crawl"
    val detalhes: String = "", // "25m forte; 25m leve"
    val intervalo: String = "" // 20"
)

data class TreinoDigitado(
    val titulo: String = "",
    val data: String = "", // dd/mm/aaaa
    val level: TrainingLevel = TrainingLevel.INTERMEDIARIO,
    val fases: Map<String, List<SerieDigitada>> = MontadorDeTreino.FASES.associateWith { emptyList() }
)

sealed interface Montagem {
    data class Pronto(val treino: WorkoutWriteDto) : Montagem
    data class ComErros(val erros: List<String>) : Montagem
}

/**
 * Transforma o formulário de "Meus treinos" em linha de public.workouts.
 *
 * Mesmas regras de scripts/carrossel_para_supabase.py, para um treino do
 * usuário e uma sugestão do carrossel saírem idênticos: a metragem vem do
 * cabeçalho ("8x50m" = 400m), os detalhes não somam, material é detectado no
 * texto, e tempo/calorias são a mesma estimativa.
 */
object MontadorDeTreino {

    val FASES = listOf("Aquecimento", "Principal", "Final")

    private val REPS = Regex("""^(\d+)\s*x\s*(\d+)\s*m\b\s*""", RegexOption.IGNORE_CASE)
    private val SIMPLES = Regex("""^(\d+)\s*m\b\s*""", RegexOption.IGNORE_CASE)
    private val INTERVALO = Regex("""^(?:(\d+)\s*')?\s*(?:(\d+)\s*(?:"|''|s|seg)?)?$""", RegexOption.IGNORE_CASE)

    private val MATERIAIS = listOf(
        "Palmar" to Regex("palmar", RegexOption.IGNORE_CASE),
        "Pull buoy" to Regex("""pull\s*buoy""", RegexOption.IGNORE_CASE),
        "Nadadeira" to Regex("nadadeira", RegexOption.IGNORE_CASE),
        "Prancha" to Regex("prancha", RegexOption.IGNORE_CASE)
    )

    private val RITMO_S_POR_100M = mapOf(
        TrainingLevel.INICIANTE to 150,
        TrainingLevel.INTERMEDIARIO to 130,
        TrainingLevel.AVANCADO to 115
    )
    private const val KCAL_POR_MINUTO = 8

    data class Cabecalho(val repeticoes: Int, val metros: Int, val nado: String)

    /** "8x50m Crawl" -> 8 × 50, "Crawl". Null se não começar pela distância. */
    fun lerCabecalho(texto: String): Cabecalho? {
        val t = texto.trim()
        REPS.find(t)?.let { m ->
            return Cabecalho(m.groupValues[1].toInt(), m.groupValues[2].toInt(), t.substring(m.range.last + 1).trim())
        }
        SIMPLES.find(t)?.let { m ->
            return Cabecalho(1, m.groupValues[1].toInt(), t.substring(m.range.last + 1).trim())
        }
        return null
    }

    /** `20"`, `20`, `20s` ou `1'30"` -> texto normalizado e segundos. Vazio = sem intervalo. */
    fun lerIntervalo(texto: String): Pair<String, Int>? {
        val t = texto.trim()
        if (t.isEmpty()) return "" to 0
        val m = INTERVALO.matchEntire(t) ?: return null
        val minutos = m.groupValues[1]
        val segundos = m.groupValues[2]
        if (minutos.isEmpty() && segundos.isEmpty()) return null
        val min = minutos.toIntOrNull() ?: 0
        val seg = segundos.toIntOrNull() ?: 0
        if (min > 0 && seg >= 60) return null
        val normalizado = if (min > 0) String.format(Locale.US, "%d'%02d\"", min, seg) else "$seg\""
        return normalizado to (min * 60 + seg)
    }

    fun lerDetalhes(texto: String): List<String> =
        texto.split(';', '·', '\n').map { it.trim() }.filter { it.isNotEmpty() }

    fun montar(digitado: TreinoDigitado): Montagem {
        val erros = mutableListOf<String>()

        val titulo = digitado.titulo.trim()
        if (titulo.isEmpty()) erros += "Dê um nome ao treino."
        val dia = DataCivil.lerDataBr(digitado.data)
        if (dia == null) erros += "Data inválida: use dd/mm/aaaa."

        val ritmo = RITMO_S_POR_100M.getValue(digitado.level)
        var segundos = 0.0

        val fases = FASES.mapNotNull { nome ->
            val linhas = digitado.fases[nome].orEmpty()
                .filterNot { it.serie.isBlank() && it.detalhes.isBlank() && it.intervalo.isBlank() }

            val sets = linhas.mapIndexedNotNull { i, s ->
                val rotulo = "$nome, série ${i + 1}"
                val cab = lerCabecalho(s.serie)
                val intervalo = lerIntervalo(s.intervalo)
                when {
                    cab == null -> {
                        erros += "$rotulo: comece pela distância, como \"8x50m Crawl\" ou \"400m Crawl\"."
                        null
                    }
                    cab.repeticoes == 0 || cab.metros == 0 -> {
                        erros += "$rotulo: a distância não pode ser zero."
                        null
                    }
                    intervalo == null -> {
                        erros += "$rotulo: intervalo inválido; use 20\" ou 1'30\"."
                        null
                    }
                    else -> {
                        val detalhes = lerDetalhes(s.detalhes)
                        val metros = cab.repeticoes * cab.metros
                        val texto = (listOf(s.serie.trim()) + detalhes).joinToString(" ")
                        val materiais = MATERIAIS.filter { it.second.containsMatchIn(texto) }.map { it.first }

                        segundos += metros * ritmo / 100.0
                        segundos += cab.repeticoes * intervalo.second

                        WorkoutSetDto(
                            id = "${nome.lowercase()}_s${i + 1}",
                            repsDescription = "${cab.repeticoes}x${cab.metros}",
                            // "8x75m" sozinho: o que nadar está nos detalhes.
                            stroke = cab.nado.ifEmpty { detalhes.joinToString(" · ") }.ifEmpty { "Nado livre" },
                            interval = intervalo.first.ifEmpty { null },
                            intensity = null,
                            restSeconds = intervalo.second,
                            equipment = materiais.joinToString(" + ").ifEmpty { null },
                            isDone = false,
                            serie = s.serie.trim(),
                            details = detalhes,
                            distanceMeters = metros
                        )
                    }
                }
            }

            if (sets.isEmpty()) {
                null
            } else {
                WorkoutPhaseDto(
                    id = nome.lowercase(),
                    title = nome,
                    summary = sets.joinToString(" + ") { it.serie.orEmpty() },
                    distanceMeters = sets.sumOf { it.distanceMeters ?: 0 },
                    percentage = 0,
                    status = "PENDING",
                    sets = sets
                )
            }
        }

        if (fases.isEmpty() && erros.none { it.contains("série") }) {
            erros += "Adicione pelo menos uma série."
        }
        if (erros.isNotEmpty() || dia == null) return Montagem.ComErros(erros)

        val porcentagens = porcentagens(fases.map { it.distanceMeters ?: 0 })
        val comPorcentagem = fases.zip(porcentagens) { fase, pct -> fase.copy(percentage = pct) }
        val minutos = maxOf(5, (round(segundos / 60 / 5) * 5).toInt())

        return Montagem.Pronto(
            WorkoutWriteDto(
                title = titulo,
                subtitle = digitado.level.label,
                tag = "Meu treino",
                workoutDate = DataCivil.paraIso(dia),
                totalDistanceMeters = comPorcentagem.sumOf { it.distanceMeters ?: 0 },
                estimatedMinutes = minutos,
                calories = (round(minutos * KCAL_POR_MINUTO / 10.0) * 10).toInt(),
                level = digitado.level.name,
                phases = comPorcentagem
            )
        )
    }

    /** Arredonda preservando a soma em 100 (maior resto), como o script do carrossel. */
    fun porcentagens(metros: List<Int>): List<Int> {
        val total = metros.sum()
        if (total == 0) return metros.map { 0 }
        val brutos = metros.map { it * 100.0 / total }
        val base = brutos.map { it.toInt() }.toMutableList()
        val falta = 100 - base.sum()
        brutos.indices.sortedByDescending { brutos[it] - base[it] }.take(falta).forEach { base[it] += 1 }
        return base
    }

    /** Preenche o formulário a partir de um treino: editar um meu ou salvar a sugestão. */
    fun paraDigitacao(workout: Workout, epochDay: Long? = null): TreinoDigitado {
        val dia = epochDay ?: workout.workoutDate?.let { DataCivil.deIso(it) } ?: DataCivil.hoje()
        return TreinoDigitado(
            titulo = workout.title,
            data = DataCivil.paraBr(dia),
            level = workout.level,
            fases = FASES.associateWith { nome ->
                workout.phases
                    .filter { faseDoTitulo(it.title) == nome }
                    .flatMap { it.sets }
                    .map { s ->
                        SerieDigitada(
                            serie = s.header.ifBlank { "${s.repsDistance}m ${s.description}".trim() },
                            detalhes = s.details.joinToString("; "),
                            intervalo = s.intervalTarget
                        )
                    }
            }
        )
    }

    // Treinos antigos usam "Preparatória" e "Soltura".
    private fun faseDoTitulo(titulo: String): String = when (titulo.trim().lowercase()) {
        "aquecimento", "preparatória", "preparatoria" -> "Aquecimento"
        "principal" -> "Principal"
        else -> "Final"
    }
}
