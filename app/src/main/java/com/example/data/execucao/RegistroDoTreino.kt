package com.example.data.execucao

import com.example.data.compartilhar.ResumoDoTreino
import com.example.model.TrainingLevel
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Linha de public.treinos_realizados. user_id e id vêm do banco. */
@JsonClass(generateAdapter = true)
data class TreinoRealizadoDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "workout_id") val workoutId: String? = null,
    @Json(name = "treino_ciclo_id") val treinoCicloId: String? = null,
    @Json(name = "titulo") val titulo: String,
    @Json(name = "foco") val foco: String? = null,
    @Json(name = "nivel") val nivel: String,
    @Json(name = "data_treino") val dataTreino: String,
    @Json(name = "metros_planejados") val metrosPlanejados: Int,
    @Json(name = "metros_feitos") val metrosFeitos: Int,
    @Json(name = "series_planejadas") val seriesPlanejadas: Int,
    @Json(name = "series_feitas") val seriesFeitas: Int,
    @Json(name = "duracao_segundos") val duracaoSegundos: Int,
    @Json(name = "intensidade") val intensidade: Int? = null,
    @Json(name = "complexidade") val complexidade: Int? = null,
    @Json(name = "observacao") val observacao: String? = null,
    // Treino de um plano: qual plano, semana e número do treino.
    @Json(name = "plano_id") val planoId: String? = null,
    @Json(name = "plano_semana") val planoSemana: Int? = null,
    @Json(name = "plano_treino") val planoTreino: Int? = null,
    // Só na leitura; no registro o banco preenche.
    @Json(name = "created_at") val criadoEm: String? = null
)

object RegistroDoTreino {

    private val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun montar(
        roteiro: RoteiroDeTreino,
        progresso: ProgressoExecucao,
        duracaoSegundos: Long,
        intensidade: Int?,
        complexidade: Int?,
        observacao: String,
        dataIso: String
    ): TreinoRealizadoDto {
        val treino = roteiro.workout
        return TreinoRealizadoDto(
            // As chaves estrangeiras só aceitam ids que existem no banco: sugestão
            // do ciclo antigo ("ciclo_d14_...") ou do Método NC ("nc_d01_..."), ou
            // treino de Meus treinos (uuid). O treino de exemplo embarcado não é nenhum.
            workoutId = treino.id.takeIf { !treino.isSuggestion && UUID.matches(it) },
            treinoCicloId = treino.id.takeIf { treino.isSuggestion && (it.startsWith("ciclo_") || it.startsWith("nc_")) },
            titulo = treino.title.take(200),
            foco = treino.focus,
            nivel = treino.level.name,
            dataTreino = dataIso,
            metrosPlanejados = roteiro.metrosTotais,
            metrosFeitos = roteiro.metrosFeitos(progresso).coerceIn(0, roteiro.metrosTotais),
            seriesPlanejadas = roteiro.seriesTotais,
            seriesFeitas = roteiro.seriesFeitas(progresso),
            duracaoSegundos = duracaoSegundos.coerceIn(0L, 86_400L).toInt(),
            intensidade = intensidade?.coerceIn(0, 10),
            complexidade = complexidade?.coerceIn(0, 10),
            observacao = observacao.trim().take(500).ifEmpty { null },
            planoId = treino.plano?.planoId,
            planoSemana = treino.plano?.semana,
            planoTreino = treino.plano?.treino
        )
    }

    fun resumo(registro: TreinoRealizadoDto, roteiro: RoteiroDeTreino): ResumoDoTreino {
        val treino = roteiro.workout
        return ResumoDoTreino(
            titulo = registro.titulo,
            foco = registro.foco,
            nivel = TrainingLevel.entries.firstOrNull { it.name == registro.nivel } ?: treino.level,
            dataIso = registro.dataTreino,
            metrosFeitos = registro.metrosFeitos,
            metrosPlanejados = registro.metrosPlanejados,
            seriesFeitas = registro.seriesFeitas,
            seriesPlanejadas = registro.seriesPlanejadas,
            duracaoSegundos = registro.duracaoSegundos.toLong(),
            intensidade = registro.intensidade,
            complexidade = registro.complexidade,
            cicloDia = treino.cycleDay,
            doCarrossel = treino.isSuggestion
        )
    }
}
