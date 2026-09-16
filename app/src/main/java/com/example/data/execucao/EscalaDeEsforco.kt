package com.example.data.execucao

/**
 * Escala de esforço percebido (PSE) de 0 a 10, a tabela do professor.
 * A nota vai para treinos_realizados.intensidade em todo treino salvo.
 */
object EscalaDeEsforco {

    val NOMES = listOf(
        "Nada cansado",
        "Muito fácil",
        "Fácil",
        "Moderado",
        "Moderadamente difícil",
        "Difícil",
        "Difícil",
        "Muito difícil",
        "Muito difícil",
        "Muito, muito difícil",
        "Máximo"
    )

    // Cores da tabela, de 0 a 10 (ARGB).
    val CORES = listOf(
        0xFFC5CFD9, 0xFF5B8BD0, 0xFFB7D7A0, 0xFF2E9A48, 0xFF25843C,
        0xFFF5EE1E, 0xFFF5EE1E, 0xFFDC6A1F, 0xFFDC6A1F, 0xFFEA1414, 0xFFA91212
    )

    // Nas cores escuras o texto vai branco para dar leitura.
    private val FUNDO_CLARO = setOf(0, 2, 5, 6)

    fun nome(nota: Int): String = NOMES[nota.coerceIn(0, 10)]

    fun cor(nota: Int): Long = CORES[nota.coerceIn(0, 10)]

    fun textoClaro(nota: Int): Boolean = nota.coerceIn(0, 10) !in FUNDO_CLARO

    fun rotulo(nota: Int): String = "${nota.coerceIn(0, 10)} · ${nome(nota)}"
}
