package com.example.data.compartilhar

import com.example.model.TrainingLevel
import java.text.NumberFormat
import java.util.Locale

/** O que vai para a imagem e para a legenda de um treino concluído. */
data class ResumoDoTreino(
    val titulo: String,
    val foco: String?,
    val nivel: TrainingLevel,
    val dataIso: String,
    val metrosFeitos: Int,
    val metrosPlanejados: Int,
    val seriesFeitas: Int,
    val seriesPlanejadas: Int,
    val duracaoSegundos: Long,
    val intensidade: Int?,
    val complexidade: Int?,
    val cicloDia: Int?,
    val doCarrossel: Boolean
) {
    val completo: Boolean get() = metrosPlanejados > 0 && metrosFeitos >= metrosPlanejados
}

/** Tamanho da imagem: feed do Instagram (4:5) ou stories (9:16). */
enum class FormatoDoCartao(val largura: Int, val altura: Int, val rotulo: String) {
    FEED(1080, 1350, "Feed"),
    STORIES(1080, 1920, "Stories")
}

object TextosDoTreino {

    private val numeros = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))

    // Mesmas hashtags do carrossel; a do app é a própria #NatacaoCriativa.
    const val HASHTAGS = "#CadaDia1Treino #NatacaoCriativa #TreinoDeNatacao #Natacao"

    /** 1800 -> "1.800m" */
    fun metros(m: Int): String = numeros.format(m) + "m"

    /** 2520 -> "42 min"; 3900 -> "1h05" */
    fun duracao(segundos: Long): String {
        val minutos = segundos / 60
        return when {
            minutos < 1 -> "menos de 1 min"
            minutos < 60 -> "$minutos min"
            else -> String.format(Locale.US, "%dh%02d", minutos / 60, minutos % 60)
        }
    }

    // Só o nome do nível: a descrição ("Ainda construindo o nado contínuo") é longa para legenda e cartão.
    fun nivel(n: TrainingLevel): String = n.label

    fun legenda(r: ResumoDoTreino): String {
        val blocos = mutableListOf<String>()

        blocos += if (r.completo) {
            "🏊 Treino concluído: ${metros(r.metrosFeitos)} em ${duracao(r.duracaoSegundos)}!"
        } else {
            "🏊 Nadei ${metros(r.metrosFeitos)} de ${metros(r.metrosPlanejados)} em ${duracao(r.duracaoSegundos)}."
        }

        blocos += if (r.doCarrossel) {
            val dia = r.cicloDia?.let { "Dia $it" } ?: "Treino do dia"
            "$dia do Cada Dia 1 Treino — foco em ${r.foco ?: r.titulo}, nível ${nivel(r.nivel)}."
        } else {
            "Treino \"${r.titulo}\" — nível ${nivel(r.nivel)}."
        }

        // O carrossel pede exatamente estas duas notas na legenda.
        val notas = listOfNotNull(
            r.intensidade?.let { "Intensidade: $it/10" },
            r.complexidade?.let { "Complexidade: $it/10" }
        )
        if (notas.isNotEmpty()) blocos += notas.joinToString(" · ")

        if (r.doCarrossel) blocos += "Treino de @natacaocriativa"
        blocos += HASHTAGS
        return blocos.joinToString("\n\n")
    }
}
