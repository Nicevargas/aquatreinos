package com.example.data.treinos

/** Vocabulário do Método Natação Criativa que as telas mostram. */
object MetodoNC {

    data class Zona(val sigla: String, val nome: String, val pse: String)

    val ZONAS = listOf(
        Zona("A0", "Recuperação ativa", "1-3"),
        Zona("A1", "Base aeróbia", "até 4"),
        Zona("A2", "Resistência aeróbia", "5-7"),
        Zona("A3", "Potência aeróbia", "8-10"),
        Zona("AN", "Anaeróbio", "10"),
        Zona("AA", "Velocidade", "máx, até 10 s")
    )

    fun zona(sigla: String?): Zona? = ZONAS.firstOrNull { it.sigla.equals(sigla?.trim(), ignoreCase = true) }

    /**
     * Intervalo como o aluno lê: aberto "#20\"" = descansa 20 s; fechado
     * "@1'45\"" = sai a cada 1'45". Treinos antigos escrevem só "30\"".
     */
    fun intervaloLegivel(intervalo: String): String? {
        val t = intervalo.trim()
        return when {
            t.isEmpty() -> null
            t.startsWith("#") -> "Descanso ${t.drop(1)}"
            t.startsWith("@") -> "Sai a cada ${t.drop(1)}"
            else -> "Intervalo $t"
        }
    }
}
