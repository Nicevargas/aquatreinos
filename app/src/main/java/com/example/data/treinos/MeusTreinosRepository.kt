package com.example.data.treinos

import com.example.data.Resultado
import com.example.data.supabase.WorkoutWriteDto
import com.example.data.supabase.chamarApi
import com.example.data.supabase.toDomain
import com.example.model.Workout

/** CRUD de public.workouts do usuário logado. Quem garante que é só dele é o RLS. */
object MeusTreinosRepository {

    suspend fun listar(userId: String): Resultado<List<Workout>> =
        chamarApi({ it.getMyWorkouts("eq.$userId") }) { lista ->
            Resultado.Ok(lista.orEmpty().map { dto -> dto.toDomain() })
        }

    suspend fun criar(treino: WorkoutWriteDto): Resultado<Workout> =
        chamarApi({ it.createWorkout(treino) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it.toDomain()) }
                ?: Resultado.Falha("O treino não foi salvo. Tente de novo.")
        }

    suspend fun atualizar(id: String, treino: WorkoutWriteDto): Resultado<Workout> =
        chamarApi({ it.updateWorkout("eq.$id", treino) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it.toDomain()) }
                ?: Resultado.Falha("Treino não encontrado: ele pode ter sido excluído.")
        }

    suspend fun excluir(id: String): Resultado<Unit> =
        chamarApi({ it.deleteWorkout("eq.$id") }) { lista ->
            if (lista.isNullOrEmpty()) {
                Resultado.Falha("Treino não encontrado: ele pode já ter sido excluído.")
            } else {
                Resultado.Ok(Unit)
            }
        }
}
