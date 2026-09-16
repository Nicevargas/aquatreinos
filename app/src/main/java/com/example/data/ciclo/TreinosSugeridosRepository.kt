package com.example.data.ciclo

import com.example.data.WorkoutRepository
import com.example.data.supabase.SupabaseRepository
import com.example.model.ModoDeTreino
import com.example.model.TrainingLevel
import com.example.model.Workout

/**
 * De onde vem o treino sugerido do dia.
 *
 * A cópia embarcada responde na hora e sem rede; o Supabase é a fonte oficial e
 * pode ter recebido um programa novo antes de o app ser atualizado, então,
 * quando conectado, a resposta dele substitui a embarcada.
 *
 * Cada modo tem os seus ciclos embarcados (piscina: o antigo e o do Método NC;
 * águas abertas: o de águas abertas). Dentro do modo, vale o que já tinha
 * começado na data escolhida, como em public.treinos_sugeridos.
 */
class TreinosSugeridosRepository(lerCiclosEmbarcados: (ModoDeTreino) -> List<String>) {

    private val ciclos: Map<ModoDeTreino, List<CicloDeTreinos>> by lazy {
        ModoDeTreino.entries.associateWith { modo -> lerCiclosEmbarcados(modo).map { CicloDeTreinos.deJson(it) } }
    }

    fun embarcado(epochDay: Long, level: TrainingLevel): Workout =
        CicloDeTreinos.escolher(ciclos.getValue(ModoDeTreino.PISCINA), epochDay).sugestao(epochDay, level)
            ?: WorkoutRepository.getWorkoutForLevel(level)

    /** Treino do modo; null quando o modo não tem treino para o nível (águas abertas no Pré-condicionamento). */
    fun embarcado(epochDay: Long, level: TrainingLevel, modo: ModoDeTreino): Workout? {
        if (modo == ModoDeTreino.PISCINA) return embarcado(epochDay, level)
        val doModo = ciclos.getValue(modo)
        if (!modo.temTreinoPara(level) || doModo.isEmpty()) return null
        return CicloDeTreinos.escolher(doModo, epochDay).sugestao(epochDay, level)
    }

    suspend fun remoto(epochDay: Long, level: TrainingLevel, modo: ModoDeTreino = ModoDeTreino.PISCINA): Workout? =
        if (modo.temTreinoPara(level)) SupabaseRepository.getTreinoSugerido(epochDay, level, modo) else null
}
