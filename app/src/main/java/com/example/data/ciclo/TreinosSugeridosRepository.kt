package com.example.data.ciclo

import com.example.data.WorkoutRepository
import com.example.data.supabase.SupabaseRepository
import com.example.model.TrainingLevel
import com.example.model.Workout

/**
 * De onde vem o treino sugerido do dia.
 *
 * A cópia embarcada responde na hora e sem rede; o Supabase é a fonte oficial e
 * pode ter recebido um programa novo antes de o app ser atualizado, então,
 * quando conectado, a resposta dele substitui a embarcada.
 */
class TreinosSugeridosRepository(lerCicloEmbarcado: () -> String) {

    private val cicloEmbarcado: CicloDeTreinos by lazy { CicloDeTreinos.deJson(lerCicloEmbarcado()) }

    fun embarcado(epochDay: Long, level: TrainingLevel): Workout =
        cicloEmbarcado.sugestao(epochDay, level) ?: WorkoutRepository.getWorkoutForLevel(level)

    suspend fun remoto(epochDay: Long, level: TrainingLevel): Workout? =
        SupabaseRepository.getTreinoSugerido(epochDay, level)
}
