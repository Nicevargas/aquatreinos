package com.example.model

// Os três níveis do carrossel "Cada Dia 1 Treino" (🟢 verde, 🟡 amarelo, 🔴 vermelho),
// com os nomes do Método Natação Criativa. O nome do enum não muda: é o que o banco guarda.
enum class TrainingLevel(val label: String, val carouselLabel: String) {
    INICIANTE("Pré-condicionamento", "Ainda construindo o nado contínuo"),
    INTERMEDIARIO("Condicionamento", "Nada contínuo com controle"),
    AVANCADO("Aperfeiçoamento", "Técnica consolidada e ritmo controlado")
}

data class CalendarDay(
    val dayOfWeek: String,
    val dayNumber: Int,
    val epochDay: Long = 0L,
    val isToday: Boolean = false,
    val isSelected: Boolean = false
)

enum class PhaseStatus {
    COMPLETED,
    ACTIVE,
    PENDING
}

data class WorkoutSet(
    val id: String,
    val repsDistance: String, // e.g., "8x100"
    val description: String, // "Crawl com Palmar"
    val intervalTarget: String = "1'45\"",
    val intensity: String = "Z3 (75%)",
    val restSeconds: Int = 30,
    val equipmentName: String? = "Palmar",
    val isCompleted: Boolean = false,
    val header: String = "", // como sai no carrossel: "8x50m Crawl"
    val details: List<String> = emptyList(), // "25m ponta do dedo", "25m nado completo"
    val distanceMeters: Int = 0,
    // Método NC: zona de intensidade (A0, A1, A2, A3, AN, AA), PSE e o corretivo, quando houver.
    val zona: String? = null,
    val pse: String? = null,
    val corretivo: Corretivo? = null
)

/** O antigo "educativo": nasce de um objetivo e volta ao nado completo. */
data class Corretivo(val nome: String, val objetivo: String, val dica: String)

data class WorkoutPhase(
    val id: String,
    val title: String, // Ativação ... Recuperação (Método NC); treinos antigos: Aquecimento, Principal, Final
    val summary: String, // "400m Crawl Relaxado"
    val distanceMeters: Int,
    val percentage: Int,
    val status: PhaseStatus,
    val sets: List<WorkoutSet> = emptyList(),
    val currentSetIndex: Int = 0
)

data class Workout(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val totalDistanceMeters: Int,
    val estimatedMinutes: Int,
    val calories: Int,
    val level: TrainingLevel,
    val phases: List<WorkoutPhase>,
    val motivationalTip: String = "Mantenha a técnica na fase principal! Respiração bilateral e braçadas consistentes.",
    val workoutDate: String? = null, // AAAA-MM-DD
    val isSuggestion: Boolean = false, // veio do ciclo do carrossel, não de public.workouts
    val focus: String? = null, // Técnica, Resistência, Velocidade...
    val cycleDay: Int? = null,
    // Método NC: objetivo do dia, zona predominante e o que ajustar se a sessão fugir do plano.
    val objetivo: String? = null,
    val zona: String? = null,
    val ajuste: String? = null,
    // Treino que faz parte de um plano de treino: concluído, conta no progresso do plano.
    val plano: ReferenciaDoPlano? = null
) {
    /** Material usado em alguma série, na ordem em que aparece. */
    val equipment: List<String>
        get() = phases
            .flatMap { it.sets }
            .flatMap { it.equipmentName?.split("+").orEmpty() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
}

/** Plano (public.planos_treino), semana e número do treino dentro dela. */
data class ReferenciaDoPlano(val planoId: String, val semana: Int, val treino: Int)

enum class AppNavTab(val title: String) {
    HOME("Home"),
    PLAN("Plano"),
    WORKOUTS("Treinos"),
    MY_WORKOUTS("Meus treinos"),
    PROFILE("Perfil")
}

data class CompletedSetRecord(
    val setNumber: Int,
    val timeFormatted: String,
    val pacePer100m: String,
    val splitDifference: String = "",
    val timeMillis: Long = 0L
)
