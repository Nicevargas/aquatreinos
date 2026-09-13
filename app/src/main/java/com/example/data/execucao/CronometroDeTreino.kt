package com.example.data.execucao

/**
 * Tempo de treino que não atrasa nem pula: guarda instantes, em vez de somar
 * um segundo a cada volta de laço. O relógio deve ser monotônico
 * (SystemClock.elapsedRealtime), para não mudar quando o celular acerta a hora.
 */
data class CronometroDeTreino(
    val acumuladoMs: Long = 0L,
    val iniciadoEm: Long? = null
) {
    val rodando: Boolean get() = iniciadoEm != null

    fun decorridoMs(agora: Long): Long = acumuladoMs + (iniciadoEm?.let { (agora - it).coerceAtLeast(0L) } ?: 0L)

    fun iniciar(agora: Long): CronometroDeTreino = if (rodando) this else copy(iniciadoEm = agora)

    fun pausar(agora: Long): CronometroDeTreino = if (!rodando) this else CronometroDeTreino(decorridoMs(agora), null)
}
