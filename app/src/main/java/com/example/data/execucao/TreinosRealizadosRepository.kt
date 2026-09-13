package com.example.data.execucao

import com.example.data.Resultado
import com.example.data.supabase.chamarApi

/** "Concluir treino": grava em public.treinos_realizados. As estatísticas o banco atualiza sozinho. */
object TreinosRealizadosRepository {

    suspend fun registrar(registro: TreinoRealizadoDto): Resultado<TreinoRealizadoDto> =
        chamarApi({ it.registrarTreino(registro) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it) }
                ?: Resultado.Falha("O treino não foi salvo. Tente de novo.")
        }
}
