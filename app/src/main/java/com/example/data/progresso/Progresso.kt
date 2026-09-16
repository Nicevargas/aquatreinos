package com.example.data.progresso

import com.example.data.ciclo.DataCivil
import com.example.data.compartilhar.TextosDoTreino
import com.example.data.execucao.TreinoRealizadoDto

/** Um treino registrado, só com o que o progresso usa. */
data class Atividade(
    val id: String?,
    val dia: Long, // epoch day
    val titulo: String,
    val foco: String?,
    val metros: Int,
    val duracaoSegundos: Int,
    val completo: Boolean,
    val deuNotas: Boolean,
    // Percepção de esforço (PSE) 0–10; nulo nos treinos salvos antes de ela ser obrigatória.
    val esforco: Int? = null
)

/** Linha com data ilegível não entra: não derruba o histórico inteiro. */
fun TreinoRealizadoDto.paraAtividade(): Atividade? = runCatching {
    Atividade(
        id = id,
        dia = DataCivil.deIso(dataTreino),
        titulo = titulo,
        foco = foco,
        metros = metrosFeitos,
        duracaoSegundos = duracaoSegundos,
        completo = metrosPlanejados > 0 && metrosFeitos >= metrosPlanejados,
        deuNotas = intensidade != null && complexidade != null,
        esforco = intensidade
    )
}.getOrNull()

/**
 * Estatística da percepção de esforço. Só entram treinos com nota.
 * [porNota] tem 11 posições: quantos treinos em cada nota de 0 a 10.
 * [porSemana] traz a média de cada semana (segunda) com nota, da mais antiga à atual.
 */
data class EstatisticaDeEsforco(
    val treinosComNota: Int,
    val media: Double?,
    val mediaUltimas4Semanas: Double?,
    val maior: Int?,
    val ultima: Int?,
    val porNota: List<Int>,
    val porSemana: List<Pair<Long, Double>>
)

/** Um prêmio possível, obtido ou não. */
data class MetaDeConquista(val id: String, val titulo: String, val descricao: String)

data class Conquista(val id: String, val titulo: String, val descricao: String, val dia: Long)

/** [semanas] seguidas até agora; [nadouEstaSemana] diz se a semana atual já conta. */
data class SerieDeSemanas(val semanas: Int, val nadouEstaSemana: Boolean)

data class Totais(val treinos: Int, val metros: Int, val segundos: Long, val metrosNoMes: Int)

/** Tudo o que as telas de progresso mostram, calculado de uma vez. */
data class PainelDoProgresso(
    val hoje: Long,
    // Do mais recente para o mais antigo.
    val atividades: List<Atividade>,
    val totais: Totais,
    val serie: SerieDeSemanas,
    val ultimosSeteDias: List<Pair<Long, Boolean>>,
    val conquistas: List<Conquista>,
    val proximasConquistas: List<MetaDeConquista>,
    val esforco: EstatisticaDeEsforco,
    val nomeMesAtual: String,
    val nomeMesAnterior: String,
    val diasNoMesAtual: Int,
    val metrosMesAtual: List<Int>,
    val metrosMesAnterior: List<Int>,
    val pontos: Int = 0,
    // Tabelas de quanto nadou, do período atual para trás.
    val porSemana: List<LinhaDoPeriodo> = emptyList(),
    val porMes: List<LinhaDoPeriodo> = emptyList()
)

/** Uma linha da tabela: uma semana (começando na segunda) ou um mês. */
data class LinhaDoPeriodo(
    val inicio: Long,
    val rotulo: String,
    val treinos: Int,
    val metros: Int,
    val segundos: Long,
    val esforcoMedio: Double?
)

/**
 * Histórico, série de semanas e conquistas calculados a partir dos treinos
 * registrados. Nada disso é gravado: sai sempre do histórico, então apagar um
 * registro recalcula tudo sem deixar prêmio órfão.
 */
object Progresso {

    val METAS_DE_TREINOS = listOf(5, 10, 25, 50, 100)
    val METAS_DE_METROS = listOf(1000, 2000, 3000, 5000)
    val METAS_DE_SEMANAS = listOf(2, 4, 8, 12)
    val METAS_DO_MES_KM = listOf(10, 20, 50)
    const val TREINOS_COM_NOTA = 5

    private val MESES = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    )

    /** Todos os prêmios, na ordem de cada trilha (do mais fácil ao mais difícil). */
    val CATALOGO: List<MetaDeConquista> = buildList {
        add(MetaDeConquista("primeiro-treino", "Primeiro treino", "Registrou o primeiro treino."))
        METAS_DE_TREINOS.forEach { add(MetaDeConquista("treinos-$it", "$it treinos", "Terminou $it treinos.")) }
        METAS_DE_METROS.forEach {
            val m = TextosDoTreino.metros(it)
            add(MetaDeConquista("treino-$it", "Treino de $m", "Completou um treino de $m."))
        }
        METAS_DE_SEMANAS.forEach { add(MetaDeConquista("semanas-$it", "$it semanas seguidas", "Nadou em $it semanas seguidas.")) }
        METAS_DO_MES_KM.forEach { add(MetaDeConquista("mes-${it}km", "$it km no mês", "Nadou $it km no mesmo mês.")) }
        add(
            MetaDeConquista(
                "notas-$TREINOS_COM_NOTA", "Conhece o próprio esforço",
                "Deu nota de esforço e complexidade em $TREINOS_COM_NOTA treinos."
            )
        )
    }

    private val CATALOGO_POR_ID = CATALOGO.associateBy { it.id }

    fun nomeDoMes(mes: Int): String = MESES[(mes - 1).coerceIn(0, 11)]

    /**
     * Semanas seguidas (segunda a domingo) com pelo menos um treino. A semana
     * atual ainda não quebra a série: sem treino nela, conta até a anterior.
     */
    fun serieDeSemanas(dias: Collection<Long>, hoje: Long): SerieDeSemanas {
        val semanas = dias.map { DataCivil.segundaDaSemana(it) }.toSet()
        val atual = DataCivil.segundaDaSemana(hoje)
        val nadou = atual in semanas
        var semana = if (nadou) atual else atual - 7
        var seguidas = 0
        while (semana in semanas) {
            seguidas++
            semana -= 7
        }
        return SerieDeSemanas(seguidas, nadou)
    }

    /** Os 7 dias até hoje, do mais antigo ao de hoje: (dia, nadou). */
    fun ultimosSeteDias(dias: Collection<Long>, hoje: Long): List<Pair<Long, Boolean>> {
        val comTreino = dias.toSet()
        return (6 downTo 0).map { hoje - it }.map { it to (it in comTreino) }
    }

    fun diasNoMes(ano: Int, mes: Int): Int {
        val (anoSeguinte, mesSeguinte) = if (mes == 12) ano + 1 to 1 else ano to mes + 1
        return (DataCivil.epochDay(anoSeguinte, mesSeguinte, 1) - DataCivil.epochDay(ano, mes, 1)).toInt()
    }

    /** Metros acumulados dia a dia no mês (posição 0 = dia 1), até [ateDia] ou o fim do mês. */
    fun metrosAcumuladosNoMes(atividades: List<Atividade>, ano: Int, mes: Int, ateDia: Int? = null): List<Int> {
        val tamanho = diasNoMes(ano, mes)
        val porDia = IntArray(tamanho)
        atividades.forEach { a ->
            val (y, m, d) = DataCivil.civil(a.dia)
            if (y == ano && m == mes) porDia[d - 1] += a.metros
        }
        var soma = 0
        return (0 until (ateDia ?: tamanho).coerceIn(0, tamanho)).map { soma += porDia[it]; soma }
    }

    fun totais(atividades: List<Atividade>, hoje: Long): Totais {
        val (ano, mes, _) = DataCivil.civil(hoje)
        return Totais(
            treinos = atividades.size,
            metros = atividades.sumOf { it.metros },
            segundos = atividades.sumOf { it.duracaoSegundos.toLong() },
            metrosNoMes = atividades.filter { DataCivil.civil(it.dia).let { (y, m, _) -> y == ano && m == mes } }
                .sumOf { it.metros }
        )
    }

    /** Conquistas na ordem em que foram liberadas, cada uma com o dia do treino que a liberou. */
    fun conquistas(atividades: List<Atividade>): List<Conquista> {
        val obtidas = linkedMapOf<String, Conquista>()
        fun ganhar(id: String, dia: Long) {
            val meta = CATALOGO_POR_ID.getValue(id)
            if (id !in obtidas) obtidas[id] = Conquista(id, meta.titulo, meta.descricao, dia)
        }

        var treinos = 0
        var comNota = 0
        val metrosPorMes = mutableMapOf<Pair<Int, Int>, Int>()
        val dias = mutableListOf<Long>()

        // sortedBy é estável: no mesmo dia, vale a ordem em que chegaram.
        for (a in atividades.sortedBy { it.dia }) {
            treinos++
            dias += a.dia
            if (treinos == 1) ganhar("primeiro-treino", a.dia)
            METAS_DE_TREINOS.filter { treinos == it }.forEach { ganhar("treinos-$it", a.dia) }
            METAS_DE_METROS.filter { a.metros >= it }.forEach { ganhar("treino-$it", a.dia) }
            if (a.deuNotas && ++comNota == TREINOS_COM_NOTA) ganhar("notas-$TREINOS_COM_NOTA", a.dia)
            val (ano, mes, _) = DataCivil.civil(a.dia)
            val noMes = (metrosPorMes[ano to mes] ?: 0) + a.metros
            metrosPorMes[ano to mes] = noMes
            METAS_DO_MES_KM.filter { noMes >= it * 1000 }.forEach { ganhar("mes-${it}km", a.dia) }
            val seguidas = serieDeSemanas(dias, a.dia).semanas
            METAS_DE_SEMANAS.filter { seguidas >= it }.forEach { ganhar("semanas-$it", a.dia) }
        }
        return obtidas.values.toList()
    }

    /** O que o treino que acabou de ser salvo liberou. */
    fun novasConquistas(antes: List<Atividade>, depois: List<Atividade>): List<Conquista> {
        val jaTinha = conquistas(antes).map { it.id }.toSet()
        return conquistas(depois).filter { it.id !in jaTinha }
    }

    /** O próximo prêmio de cada trilha (treinos, metros, semanas, mês, notas) ainda não obtido. */
    fun proximasConquistas(obtidas: Collection<String>): List<MetaDeConquista> =
        CATALOGO.filter { it.id !in obtidas }
            .groupBy { it.id.substringBefore('-') }
            .values
            .map { it.first() }

    /** Médias e distribuição da PSE; as últimas 4 semanas contam a atual e as 3 anteriores. */
    fun esforco(atividades: List<Atividade>, hoje: Long): EstatisticaDeEsforco {
        // sortedBy é estável: no mesmo dia, o último que chegou é o mais recente.
        val comNota = atividades.filter { it.esforco != null }.sortedBy { it.dia }
        val notas = comNota.map { it.esforco!!.coerceIn(0, 10) }
        val inicio4Semanas = DataCivil.segundaDaSemana(hoje) - 21
        val recentes = comNota.filter { it.dia in inicio4Semanas..hoje }.map { it.esforco!!.coerceIn(0, 10) }
        return EstatisticaDeEsforco(
            treinosComNota = notas.size,
            media = notas.takeIf { it.isNotEmpty() }?.average(),
            mediaUltimas4Semanas = recentes.takeIf { it.isNotEmpty() }?.average(),
            maior = notas.maxOrNull(),
            ultima = notas.lastOrNull(),
            porNota = (0..10).map { n -> notas.count { it == n } },
            porSemana = comNota.groupBy { DataCivil.segundaDaSemana(it.dia) }
                .toSortedMap()
                .map { (semana, lista) -> semana to lista.map { it.esforco!!.coerceIn(0, 10) }.average() }
        )
    }

    private fun linha(inicio: Long, rotulo: String, lista: List<Atividade>) = LinhaDoPeriodo(
        inicio = inicio,
        rotulo = rotulo,
        treinos = lista.size,
        metros = lista.sumOf { it.metros },
        segundos = lista.sumOf { it.duracaoSegundos.toLong() },
        esforcoMedio = lista.mapNotNull { it.esforco }.takeIf { it.isNotEmpty() }?.average()
    )

    /** As [quantas] semanas até a atual, da atual para trás. Rótulo: a segunda-feira ("14/09"). */
    fun porSemana(atividades: List<Atividade>, hoje: Long, quantas: Int = 8): List<LinhaDoPeriodo> {
        val atual = DataCivil.segundaDaSemana(hoje)
        return (0 until quantas).map { i ->
            val segunda = atual - 7L * i
            val (_, mes, dia) = DataCivil.civil(segunda)
            linha(segunda, String.format(java.util.Locale.US, "%02d/%02d", dia, mes), atividades.filter { it.dia in segunda..segunda + 6 })
        }
    }

    /** Os [quantos] meses até o atual, do atual para trás. Rótulo: "set/26". */
    fun porMes(atividades: List<Atividade>, hoje: Long, quantos: Int = 6): List<LinhaDoPeriodo> {
        val (anoHoje, mesHoje, _) = DataCivil.civil(hoje)
        return (0 until quantos).map { i ->
            val indice = anoHoje * 12 + (mesHoje - 1) - i
            val ano = indice / 12
            val mes = indice % 12 + 1
            val primeiro = DataCivil.epochDay(ano, mes, 1)
            val ultimo = primeiro + diasNoMes(ano, mes) - 1
            linha(primeiro, "${nomeDoMes(mes).take(3)}/${String.format(java.util.Locale.US, "%02d", ano % 100)}", atividades.filter { it.dia in primeiro..ultimo })
        }
    }

    fun painel(atividades: List<Atividade>, hoje: Long): PainelDoProgresso {
        val (ano, mes, dia) = DataCivil.civil(hoje)
        val (anoAnterior, mesAnterior) = if (mes == 1) ano - 1 to 12 else ano to mes - 1
        val dias = atividades.map { it.dia }
        val conquistas = conquistas(atividades)
        return PainelDoProgresso(
            hoje = hoje,
            // Estável: no mesmo dia, o que chegou por último (o mais recente) vem antes.
            atividades = atividades.withIndex().sortedWith(compareByDescending<IndexedValue<Atividade>> { it.value.dia }.thenBy { it.index }).map { it.value },
            totais = totais(atividades, hoje),
            serie = serieDeSemanas(dias, hoje),
            ultimosSeteDias = ultimosSeteDias(dias, hoje),
            conquistas = conquistas,
            proximasConquistas = proximasConquistas(conquistas.map { it.id }),
            esforco = esforco(atividades, hoje),
            nomeMesAtual = nomeDoMes(mes),
            nomeMesAnterior = nomeDoMes(mesAnterior),
            diasNoMesAtual = diasNoMes(ano, mes),
            metrosMesAtual = metrosAcumuladosNoMes(atividades, ano, mes, ateDia = dia),
            metrosMesAnterior = metrosAcumuladosNoMes(atividades, anoAnterior, mesAnterior),
            pontos = Pontuacao.total(atividades),
            porSemana = porSemana(atividades, hoje),
            porMes = porMes(atividades, hoje)
        )
    }
}
