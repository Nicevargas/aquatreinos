package com.example.data

import com.example.model.CalendarDay
import com.example.model.PhaseStatus
import com.example.model.TrainingLevel
import com.example.model.Workout
import com.example.model.WorkoutPhase
import com.example.model.WorkoutSet

object WorkoutRepository {

    fun getInitialCalendarDays(): List<CalendarDay> = listOf(
        CalendarDay("SEG", 12, isToday = false, isSelected = false),
        CalendarDay("TER", 13, isToday = true, isSelected = true),
        CalendarDay("QUA", 14, isToday = false, isSelected = false),
        CalendarDay("QUI", 15, isToday = false, isSelected = false),
        CalendarDay("SEX", 16, isToday = false, isSelected = false),
        CalendarDay("SÁB", 17, isToday = false, isSelected = false)
    )

    fun getWorkoutForLevel(level: TrainingLevel): Workout {
        return if (level == TrainingLevel.INTERMEDIARIO) {
            Workout(
                id = "workout_inter_13",
                title = "Performance Day",
                subtitle = "Série técnica de resistência",
                tag = "Treino Principal",
                totalDistanceMeters = 2500,
                estimatedMinutes = 55,
                calories = 480,
                level = TrainingLevel.INTERMEDIARIO,
                phases = listOf(
                    WorkoutPhase(
                        id = "p1",
                        title = "Aquecimento",
                        summary = "400m Crawl Relaxado",
                        distanceMeters = 400,
                        percentage = 15,
                        status = PhaseStatus.COMPLETED,
                        sets = listOf(
                            WorkoutSet("s1", "1x400", "Crawl Relaxado", "8'00\"", "Z1 (60%)", 30, null, true)
                        )
                    ),
                    WorkoutPhase(
                        id = "p2",
                        title = "Preparatória",
                        summary = "200m Educativos Medley",
                        distanceMeters = 200,
                        percentage = 10,
                        status = PhaseStatus.COMPLETED,
                        sets = listOf(
                            WorkoutSet("s2", "4x50", "Educativos Medley", "1'15\"", "Z2 (65%)", 20, "Pull Buoy", true)
                        )
                    ),
                    WorkoutPhase(
                        id = "p3",
                        title = "Principal",
                        summary = "8x100m Crawl c/ Palmar + 1x600m c/ Nadadeira",
                        distanceMeters = 1400,
                        percentage = 60,
                        status = PhaseStatus.ACTIVE,
                        sets = listOf(
                            WorkoutSet("s3", "8x100", "Crawl com Palmar", "1'45\"", "Z3 (75%)", 30, "Palmar", false),
                            WorkoutSet("s4", "1x600", "Crawl completo com Nadadeira", "10'30\"", "Z3 (75%)", 45, "Nadadeira", false)
                        ),
                        currentSetIndex = 0
                    ),
                    WorkoutPhase(
                        id = "p4",
                        title = "Final",
                        summary = "200m Soltura / Alongamento",
                        distanceMeters = 200,
                        percentage = 15,
                        status = PhaseStatus.PENDING,
                        sets = listOf(
                            WorkoutSet("s5", "1x200", "Soltura / Alongamento", "5'00\"", "Z1 (50%)", 0, null, false)
                        )
                    )
                ),
                motivationalTip = "Mantenha a técnica na fase principal! Respiração bilateral e braçadas consistentes."
            )
        } else {
            Workout(
                id = "workout_adv_13",
                title = "Aeróbico Intensivo",
                subtitle = "Ritmo de prova e limiar de lactato",
                tag = "Treino Avançado",
                totalDistanceMeters = 3200,
                estimatedMinutes = 70,
                calories = 650,
                level = TrainingLevel.AVANCADO,
                phases = listOf(
                    WorkoutPhase(
                        id = "p1_adv",
                        title = "Aquecimento",
                        summary = "600m Variado (Crawl + Costas)",
                        distanceMeters = 600,
                        percentage = 18,
                        status = PhaseStatus.COMPLETED,
                        sets = listOf(
                            WorkoutSet("s1_a", "1x600", "Variado Crawl e Costas", "11'00\"", "Z1 (60%)", 30, null, true)
                        )
                    ),
                    WorkoutPhase(
                        id = "p2_adv",
                        title = "Preparatória",
                        summary = "400m Pernada com Prancha",
                        distanceMeters = 400,
                        percentage = 12,
                        status = PhaseStatus.COMPLETED,
                        sets = listOf(
                            WorkoutSet("s2_a", "8x50", "Pernada c/ Prancha", "1'10\"", "Z2 (70%)", 15, "Prancha", true)
                        )
                    ),
                    WorkoutPhase(
                        id = "p3_adv",
                        title = "Principal",
                        summary = "10x150m Crawl progressivo c/ Nadadeira",
                        distanceMeters = 1500,
                        percentage = 55,
                        status = PhaseStatus.ACTIVE,
                        sets = listOf(
                            WorkoutSet("s3_a", "10x150", "Crawl c/ Nadadeira", "2'15\"", "Z4 (85%)", 30, "Nadadeira", false),
                            WorkoutSet("s4_a", "4x100", "Velocidade pura c/ Palmar", "1'30\"", "Z5 (95%)", 45, "Palmar", false)
                        ),
                        currentSetIndex = 0
                    ),
                    WorkoutPhase(
                        id = "p4_adv",
                        title = "Final",
                        summary = "300m Soltura e recuperação",
                        distanceMeters = 300,
                        percentage = 15,
                        status = PhaseStatus.PENDING,
                        sets = listOf(
                            WorkoutSet("s5_a", "1x300", "Soltura e nado livre", "6'00\"", "Z1 (50%)", 0, null, false)
                        )
                    )
                ),
                motivationalTip = "Foque na eficiência do alcance subaquático e na finalização da braçada!"
            )
        }
    }
}
