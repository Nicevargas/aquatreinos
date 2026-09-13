package com.example.data.ciclo

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Datas de calendário como "dias desde 01/01/1970" (epoch day).
 *
 * O minSdk é 24 e java.time só existe a partir do 26, então a conversão é feita
 * à mão (algoritmo days_from_civil de Howard Hinnant). Contar em dias inteiros
 * deixa a regra do ciclo idêntica à do carrossel: (data - âncora) mod dias.
 */
object DataCivil {

    // O carrossel sai às 6h de Brasília; "hoje" é o dia de lá, não o do aparelho.
    private val FUSO_BRASILIA: TimeZone = TimeZone.getTimeZone("America/Sao_Paulo")

    private val SIGLAS = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM")

    fun epochDay(ano: Int, mes: Int, dia: Int): Long {
        val y = (if (mes <= 2) ano - 1 else ano).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val anoDaEra = y - era * 400
        val mesDesdeMarco = (mes + 9) % 12
        val diaDoAno = (153 * mesDesdeMarco + 2) / 5 + dia - 1
        val diaDaEra = anoDaEra * 365 + anoDaEra / 4 - anoDaEra / 100 + diaDoAno
        return era * 146097 + diaDaEra - 719468
    }

    /** Devolve (ano, mês 1-12, dia). */
    fun civil(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val diaDaEra = z - era * 146097
        val anoDaEra = (diaDaEra - diaDaEra / 1460 + diaDaEra / 36524 - diaDaEra / 146096) / 365
        val diaDoAno = diaDaEra - (365 * anoDaEra + anoDaEra / 4 - anoDaEra / 100)
        val mesDesdeMarco = (5 * diaDoAno + 2) / 153
        val dia = diaDoAno - (153 * mesDesdeMarco + 2) / 5 + 1
        val mes = if (mesDesdeMarco < 10) mesDesdeMarco + 3 else mesDesdeMarco - 9
        val ano = anoDaEra + era * 400 + if (mes <= 2) 1 else 0
        return Triple(ano.toInt(), mes.toInt(), dia.toInt())
    }

    fun paraIso(epochDay: Long): String {
        val (ano, mes, dia) = civil(epochDay)
        return String.format(Locale.US, "%04d-%02d-%02d", ano, mes, dia)
    }

    fun deIso(iso: String): Long {
        val partes = iso.trim().split("-")
        require(partes.size == 3) { "Data fora do formato AAAA-MM-DD: $iso" }
        return epochDay(partes[0].toInt(), partes[1].toInt(), partes[2].toInt())
    }

    fun paraBr(epochDay: Long): String {
        val (ano, mes, dia) = civil(epochDay)
        return String.format(Locale.US, "%02d/%02d/%04d", dia, mes, ano)
    }

    /** "13/09/2026" -> epoch day; null se não for uma data de calendário de verdade. */
    fun lerDataBr(texto: String): Long? {
        val partes = texto.trim().split("/")
        if (partes.size != 3 || partes[2].length != 4) return null
        val dia = partes[0].toIntOrNull() ?: return null
        val mes = partes[1].toIntOrNull() ?: return null
        val ano = partes[2].toIntOrNull() ?: return null
        if (mes !in 1..12 || dia !in 1..31) return null
        val epoch = epochDay(ano, mes, dia)
        // 31/02 vira 03/03 na conta; a volta denuncia a data que não existe.
        return epoch.takeIf { civil(it) == Triple(ano, mes, dia) }
    }

    fun hoje(agoraMillis: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance(FUSO_BRASILIA)
        c.timeInMillis = agoraMillis
        return epochDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** 0 = segunda ... 6 = domingo. 01/01/1970 foi uma quinta. */
    fun diaDaSemana(epochDay: Long): Int = (epochDay + 3).mod(7L).toInt()

    fun segundaDaSemana(epochDay: Long): Long = epochDay - diaDaSemana(epochDay)

    fun sigla(epochDay: Long): String = SIGLAS[diaDaSemana(epochDay)]
}
