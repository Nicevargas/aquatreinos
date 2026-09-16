package com.example.data.treinos

import com.example.data.supabase.CorretivoDto
import com.example.data.supabase.WorkoutDto
import com.example.data.supabase.WorkoutPhaseDto
import com.example.data.supabase.WorkoutSetDto
import com.example.data.supabase.WorkoutWriteDto
import com.example.data.supabase.toDomain
import com.example.model.Workout

/**
 * O caminho de volta de toDomain(): um treino da tela vira JSON de novo, para
 * ser compartilhado (treinos_compartilhados.treino) ou salvo em Meus treinos.
 */
fun Workout.paraDto(): WorkoutDto = WorkoutDto(
    title = title,
    subtitle = subtitle,
    tag = tag,
    totalDistanceMeters = totalDistanceMeters,
    estimatedMinutes = estimatedMinutes,
    calories = calories,
    level = level.name,
    phases = phases.map { fase ->
        WorkoutPhaseDto(
            id = fase.id,
            title = fase.title,
            summary = fase.summary,
            distanceMeters = fase.distanceMeters,
            percentage = fase.percentage,
            status = "PENDING",
            sets = fase.sets.map { s ->
                WorkoutSetDto(
                    id = s.id,
                    repsDescription = s.repsDistance,
                    stroke = s.description,
                    interval = s.intervalTarget.ifBlank { null },
                    intensity = s.intensity,
                    restSeconds = s.restSeconds,
                    equipment = s.equipmentName,
                    isDone = false,
                    serie = s.header.ifBlank { null },
                    details = s.details,
                    distanceMeters = s.distanceMeters,
                    zona = s.zona,
                    pse = s.pse,
                    corretivo = s.corretivo?.let { CorretivoDto(it.nome, it.objetivo, it.dica) }
                )
            }
        )
    },
    foco = focus,
    motivationalTip = motivationalTip,
    objetivo = objetivo,
    zona = zona,
    ajuste = ajuste
)

/** Linha de public.workouts para salvar este treino em Meus treinos. */
fun Workout.paraGravacao(dataIso: String, tagDoTreino: String = "Meu treino"): WorkoutWriteDto {
    val dto = paraDto()
    return WorkoutWriteDto(
        title = title.take(200),
        subtitle = subtitle.ifBlank { null },
        tag = tagDoTreino,
        workoutDate = dataIso,
        totalDistanceMeters = totalDistanceMeters,
        estimatedMinutes = estimatedMinutes,
        calories = calories,
        level = level.name,
        phases = dto.phases.orEmpty()
    )
}

/** Treino montado no editor, pronto para nadar sem ter sido salvo. */
fun WorkoutWriteDto.paraWorkout(id: String): Workout = WorkoutDto(
    id = id,
    title = title,
    subtitle = subtitle,
    tag = tag,
    workoutDate = workoutDate,
    totalDistanceMeters = totalDistanceMeters,
    estimatedMinutes = estimatedMinutes,
    calories = calories,
    level = level,
    phases = phases
).toDomain()
