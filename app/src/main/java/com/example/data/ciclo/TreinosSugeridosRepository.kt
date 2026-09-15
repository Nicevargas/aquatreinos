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
 *
 * São vários ciclos embarcados (o antigo e o do Método NC): vale o que já tinha
 * começado na data escolhida, como em public.treinos_sugeridos.
 */
class TreinosSugeridosRepository(lerCiclosEmbarcados: () -> List<String>) {

    private val ciclos: List<CicloDeTreinos> by lazy { lerCiclosEmbarcados().map { CicloDeTreinos.deJson(it) } }

    fun embarcado(epochDay: Long, level: TrainingLevel): Workout =
        CicloDeTreinos.escolher(ciclos, epochDay).sugestao(epochDay, level)
            ?: WorkoutRepository.getWorkoutForLevel(level)

    suspend fun remoto(epochDay: Long, level: TrainingLevel): Workout? =
        SupabaseRepository.getTreinoSugerido(epochDay, level)
}
