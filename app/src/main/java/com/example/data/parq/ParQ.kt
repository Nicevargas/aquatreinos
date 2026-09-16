package com.example.data.parq

import com.example.data.ciclo.DataCivil

/**
 * PAR-Q: Questionário de Prontidão para Atividade Física.
 *
 * Antes de treinar, a pessoa responde as 7 perguntas. Com algum SIM, ela só
 * treina aceitando o termo de responsabilidade. O registro protege quem
 * prescreve o treino: fica guardado que a pessoa se declarou apta.
 *
 * Mudou o texto de uma pergunta ou do termo? Troque [VERSAO]: cada resposta
 * guarda a versão que a pessoa viu.
 */
object ParQ {

    const val VERSAO = "parq-br-2026-09"

    // Até o professor decidir a validade; trocar aqui muda o app inteiro.
    const val VALIDADE_MESES = 12

    val INTRODUCAO = listOf(
        "Este questionário tem o objetivo de identificar a necessidade de avaliação por um médico antes do início da atividade física.",
        "Caso você responda SIM a uma ou mais perguntas, converse com seu médico ANTES de aumentar seu nível atual de atividade física.",
        "Mencione este questionário e as perguntas às quais você respondeu SIM. Por favor, assinale SIM ou NÃO às seguintes perguntas:"
    )

    val PERGUNTAS = listOf(
        "Algum médico já disse que você possui algum problema de coração e que só deveria realizar atividade física supervisionado por profissionais de saúde?",
        "Você sente dores no peito quando pratica atividade física?",
        "No último mês, você sentiu dores no peito quando praticou atividade física?",
        "Você apresenta desequilíbrio devido à tontura e/ou perda de consciência?",
        "Você possui algum problema ósseo ou articular que poderia ser piorado pela atividade física?",
        "Você toma atualmente algum medicamento para pressão arterial e/ou problema de coração?",
        "Sabe de alguma outra razão pela qual você não deve praticar atividade física?"
    )

    val TERMO = listOf(
        "Estou ciente de que é recomendável conversar com um médico antes de aumentar meu nível atual de atividade física, por ter respondido SIM a uma ou mais perguntas do Questionário de Prontidão para Atividade Física (PAR-Q).",
        "Assumo plena responsabilidade por qualquer atividade física praticada sem o atendimento a essa recomendação. Avaliação médica não dispensa a avaliação física que deverá ser feita por Profissional de Educação Física, a fim de mensurar a carga de exercícios."
    )

    const val ACEITE_DO_TERMO = "Li e assumo essa responsabilidade."

    const val DECLARACAO =
        "Declaro que respondi este questionário com a verdade e autorizo que minhas respostas fiquem guardadas na minha conta."

    fun algumSim(respostas: List<Boolean?>): Boolean = respostas.any { it == true }

    fun faltam(respostas: List<Boolean?>): Int = PERGUNTAS.size - respostas.count { it != null }

    /** Todas respondidas, declaração marcada e, com algum SIM, termo aceito. */
    fun podeEnviar(respostas: List<Boolean?>, declaracao: Boolean, termo: Boolean): Boolean =
        respostas.size == PERGUNTAS.size &&
            respostas.all { it != null } &&
            declaracao &&
            (!algumSim(respostas) || termo)

    /**
     * Primeiro dia em que é preciso responder de novo: o mesmo dia, [meses] depois.
     * Dia que não existe no mês final (31, ou 29/02) vira o último dia do mês.
     */
    fun venceEm(respondidoNoDia: Long, meses: Int = VALIDADE_MESES): Long {
        val (ano, mes, dia) = DataCivil.civil(respondidoNoDia)
        val indice = ano * 12 + (mes - 1) + meses
        val anoFinal = indice / 12
        val mesFinal = indice % 12 + 1
        val (anoSeguinte, mesSeguinte) = if (mesFinal == 12) anoFinal + 1 to 1 else anoFinal to mesFinal + 1
        val ultimoDoMes = (DataCivil.epochDay(anoSeguinte, mesSeguinte, 1) - DataCivil.epochDay(anoFinal, mesFinal, 1)).toInt()
        return DataCivil.epochDay(anoFinal, mesFinal, minOf(dia, ultimoDoMes))
    }

    fun precisaResponder(ultimoDia: Long?, hoje: Long): Boolean =
        ultimoDia == null || hoje >= venceEm(ultimoDia)
}
