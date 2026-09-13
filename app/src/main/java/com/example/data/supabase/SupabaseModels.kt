package com.example.data.supabase

import com.example.model.CompletedSetRecord
import com.example.model.PhaseStatus
import com.example.model.TrainingLevel
import com.example.model.Workout
import com.example.model.WorkoutPhase
import com.example.model.WorkoutSet
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkoutDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "tag") val tag: String? = "Treino Principal",
    @Json(name = "workout_date") val workoutDate: String? = null,
    @Json(name = "total_distance_meters") val totalDistanceMeters: Int? = null,
    @Json(name = "estimated_minutes") val estimatedMinutes: Int? = null,
    @Json(name = "calories") val calories: Int? = null,
    @Json(name = "level") val level: String? = "INTERMEDIARIO",
    @Json(name = "is_completed") val isCompleted: Boolean? = false,
    @Json(name = "phases") val phases: List<WorkoutPhaseDto>? = null,
    // Campos de public.treinos_sugeridos (o ciclo do carrossel).
    @Json(name = "ciclo_dia") val cicloDia: Int? = null,
    @Json(name = "bloco") val bloco: String? = null,
    @Json(name = "foco") val foco: String? = null,
    @Json(name = "motivational_tip") val motivationalTip: String? = null,
    @Json(name = "is_suggestion") val isSuggestion: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class WorkoutPhaseDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "distanceMeters") val distanceMeters: Int? = null,
    @Json(name = "percentage") val percentage: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "sets") val sets: List<WorkoutSetDto>? = null
)

@JsonClass(generateAdapter = true)
data class WorkoutSetDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "repsDescription") val repsDescription: String? = null,
    @Json(name = "stroke") val stroke: String? = null,
    @Json(name = "interval") val interval: String? = null,
    @Json(name = "intensity") val intensity: String? = null,
    @Json(name = "restSeconds") val restSeconds: Int? = null,
    @Json(name = "equipment") val equipment: String? = null,
    @Json(name = "isDone") val isDone: Boolean? = false,
    @Json(name = "serie") val serie: String? = null,
    @Json(name = "details") val details: List<String>? = null,
    @Json(name = "distanceMeters") val distanceMeters: Int? = null
)

@JsonClass(generateAdapter = true)
data class TreinosSugeridosParams(
    @Json(name = "p_data") val data: String,
    @Json(name = "p_level") val level: String?
)

@JsonClass(generateAdapter = true)
data class SwimSetRecordDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "workout_id") val workoutId: String? = null,
    @Json(name = "set_number") val setNumber: Int? = null,
    @Json(name = "rep_description") val repDescription: String? = null,
    @Json(name = "distance_meters") val distanceMeters: Int? = 100,
    @Json(name = "time_formatted") val timeFormatted: String? = null,
    @Json(name = "time_millis") val timeMillis: Long? = null,
    @Json(name = "pace_per_100m") val pacePer100m: String? = null,
    @Json(name = "split_difference") val splitDifference: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "preferred_pool_meters") val preferredPoolMeters: Int? = 25,
    @Json(name = "training_level") val trainingLevel: String? = "INTERMEDIARIO"
)

// "8x100" -> 800; "400" -> 400.
private fun metrosDe(repsDescription: String): Int {
    val partes = repsDescription.lowercase().removeSuffix("m").split("x")
    return when (partes.size) {
        2 -> (partes[0].trim().toIntOrNull() ?: 1) * (partes[1].trim().toIntOrNull() ?: 0)
        1 -> partes[0].trim().toIntOrNull() ?: 0
        else -> 0
    }
}

// Extension functions for DTO mapping
fun WorkoutDto.toDomain(): Workout {
    val domainLevel = TrainingLevel.entries.firstOrNull { it.name.equals(level, ignoreCase = true) }
        ?: TrainingLevel.INTERMEDIARIO

    val domainPhases = phases?.mapIndexed { index, phaseDto ->
        val phaseStatus = when (phaseDto.status?.uppercase()) {
            "COMPLETED" -> PhaseStatus.COMPLETED
            "CURRENT", "ACTIVE" -> PhaseStatus.ACTIVE
            else -> PhaseStatus.PENDING
        }

        WorkoutPhase(
            id = phaseDto.id ?: "phase_$index",
            title = phaseDto.title ?: "Fase de Nado",
            summary = phaseDto.summary ?: "",
            distanceMeters = phaseDto.distanceMeters ?: 400,
            percentage = phaseDto.percentage ?: 25,
            status = phaseStatus,
            sets = phaseDto.sets?.mapIndexed { setIndex, setDto ->
                val reps = setDto.repsDescription ?: "1x100"
                WorkoutSet(
                    id = setDto.id ?: "set_${index}_$setIndex",
                    repsDistance = reps,
                    description = setDto.stroke ?: "Crawl",
                    // Série contínua do carrossel não tem intervalo; não inventar um.
                    intervalTarget = setDto.interval ?: "",
                    intensity = setDto.intensity ?: "Z2 (70%)",
                    restSeconds = setDto.restSeconds ?: 30,
                    equipmentName = setDto.equipment,
                    isCompleted = setDto.isDone ?: false,
                    header = setDto.serie ?: "${reps}m ${setDto.stroke.orEmpty()}".trim(),
                    details = setDto.details.orEmpty(),
                    distanceMeters = setDto.distanceMeters ?: metrosDe(reps)
                )
            } ?: emptyList()
        )
    } ?: emptyList()

    val workout = Workout(
        id = id ?: "workout_custom",
        title = title ?: "Treino de Natação",
        subtitle = subtitle ?: "",
        tag = tag ?: "Treino Principal",
        totalDistanceMeters = totalDistanceMeters ?: 2000,
        estimatedMinutes = estimatedMinutes ?: 50,
        calories = calories ?: 450,
        level = domainLevel,
        phases = domainPhases,
        workoutDate = workoutDate,
        isSuggestion = isSuggestion ?: false,
        focus = foco,
        cycleDay = cicloDia
    )
    return motivationalTip?.let { workout.copy(motivationalTip = it) } ?: workout
}

fun SwimSetRecordDto.toDomain(): CompletedSetRecord {
    return CompletedSetRecord(
        setNumber = setNumber ?: 1,
        timeFormatted = timeFormatted ?: "00:00.0",
        pacePer100m = pacePer100m ?: "1'22\"/100m",
        splitDifference = splitDifference ?: "Base",
        timeMillis = timeMillis ?: 0L
    )
}

fun CompletedSetRecord.toDto(
    workoutId: String? = null,
    repDescription: String = "8x100m Crawl",
    distanceMeters: Int = 100
): SwimSetRecordDto {
    return SwimSetRecordDto(
        workoutId = workoutId,
        setNumber = setNumber,
        repDescription = repDescription,
        distanceMeters = distanceMeters,
        timeFormatted = timeFormatted,
        timeMillis = timeMillis,
        pacePer100m = pacePer100m,
        splitDifference = splitDifference
    )
}
