package com.example.data.ciclo

import com.example.data.supabase.WorkoutDto
import com.example.data.supabase.toDomain
import com.example.model.TrainingLevel
import com.example.model.Workout
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class CicloDto(
    @Json(name = "id") val id: String,
    @Json(name = "ancora") val ancora: String,
    @Json(name = "dias") val dias: Int,
    @Json(name = "treinos") val treinos: List<WorkoutDto>
)

/**
 * Um ciclo "Cada Dia 1 Treino" do carrossel do @natacaocriativa.
 *
 * Há dois, no Supabase (public.treinos_ciclo) e embarcados: o antigo
 * (assets/treinos_ciclo.json, scripts/carrossel_para_supabase.py) e o do Método NC,
 * que vale desde 15/09/2026 (assets/programa_nc.json, scripts/programa_nc_para_supabase.py).
 * Esta classe e [escolher] fazem, sem rede, a mesma escolha de public.treinos_sugeridos.
 */
class CicloDeTreinos(
    val ancoraEpochDay: Long,
    val dias: Int,
    private val treinos: List<WorkoutDto>
) {
    init {
        require(dias > 0) { "Ciclo sem dias" }
    }

    /** Posição 1..dias. O mod positivo acerta também datas anteriores à âncora. */
    fun diaDoCiclo(epochDay: Long): Int = (epochDay - ancoraEpochDay).mod(dias.toLong()).toInt() + 1

    fun sugestao(epochDay: Long, level: TrainingLevel): Workout? {
        val dia = diaDoCiclo(epochDay)
        return treinos
            .firstOrNull { it.cicloDia == dia && it.level.equals(level.name, ignoreCase = true) }
            ?.copy(workoutDate = DataCivil.paraIso(epochDay), isSuggestion = true)
            ?.toDomain()
    }

    companion object {
        private val adapter by lazy {
            Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(CicloDto::class.java)
        }

        fun deJson(json: String): CicloDeTreinos {
            val dto = requireNotNull(adapter.fromJson(json)) { "ciclo embarcado vazio" }
            return CicloDeTreinos(DataCivil.deIso(dto.ancora), dto.dias, dto.treinos)
        }

        /**
         * O ciclo que vale no dia: o de âncora mais recente que já começou; para
         * datas anteriores a todos, o mais antigo.
         */
        fun escolher(ciclos: List<CicloDeTreinos>, epochDay: Long): CicloDeTreinos =
            ciclos.filter { it.ancoraEpochDay <= epochDay }.maxByOrNull { it.ancoraEpochDay }
                ?: ciclos.minBy { it.ancoraEpochDay }
    }
}
