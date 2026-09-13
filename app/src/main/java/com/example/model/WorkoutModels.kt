package com.example.model

// Os três níveis do carrossel "Cada Dia 1 Treino": 🟢 verde, 🟡 amarelo e 🔴 vermelho.
enum class TrainingLevel(val label: String, val carouselLabel: String) {
    INICIANTE("Iniciante", "Menor volume"),
    INTERMEDIARIO("Intermediário", "Intermediário"),
    AVANCADO("Avançado", "Maior volume + técnica")
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
    val distanceMeters: Int = 0
)

data class WorkoutPhase(
    val id: String,
    val title: String, // Aquecimento, Preparatória, Principal, Final
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
    val focus: String? = null, // Técnica, Aeróbico, Velocidade...
    val cycleDay: Int? = null
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

enum class AppNavTab(val title: String) {
    HOME("Home"),
    WORKOUTS("Treinos"),
    PROFILE("Perfil")
}

enum class StopwatchMode(val label: String) {
    SERIE("Nado (Série)"),
    DESCANSO("Descanso")
}

data class CompletedSetRecord(
    val setNumber: Int,
    val timeFormatted: String,
    val pacePer100m: String,
    val splitDifference: String = "",
    val timeMillis: Long = 0L
)

data class SwimSetStopwatchState(
    val isRunning: Boolean = false,
    val elapsedMillis: Long = 48500L, // Initial sample time for realistic visual display (00:48.5)
    val currentSetNumber: Int = 5,
    val totalSets: Int = 8,
    val setRepDescription: String = "8x100m Crawl",
    val setDistanceMeters: Int = 100,
    val targetIntervalSeconds: Int = 105, // 1'45" (105 seconds)
    val restDurationSeconds: Int = 30, // 30s
    val mode: StopwatchMode = StopwatchMode.SERIE,
    val completedLaps: List<CompletedSetRecord> = listOf(
        CompletedSetRecord(setNumber = 4, timeFormatted = "01:21.2", pacePer100m = "1'21\"/100m", splitDifference = "-0.7s", timeMillis = 81200L),
        CompletedSetRecord(setNumber = 3, timeFormatted = "01:21.9", pacePer100m = "1'21\"/100m", splitDifference = "-0.8s", timeMillis = 81900L),
        CompletedSetRecord(setNumber = 2, timeFormatted = "01:22.7", pacePer100m = "1'22\"/100m", splitDifference = "-0.9s", timeMillis = 82700L),
        CompletedSetRecord(setNumber = 1, timeFormatted = "01:23.6", pacePer100m = "1'23\"/100m", splitDifference = "Base", timeMillis = 83600L)
    ),
    val lastRecordedTime: String? = "01:21.2"
)
