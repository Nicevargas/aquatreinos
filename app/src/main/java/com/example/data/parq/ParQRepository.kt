package com.example.data.parq

import com.example.data.Resultado
import com.example.data.ciclo.DataCivil
import com.example.data.supabase.chamarApi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Linha de public.parq_respostas. id, user_id e as datas vêm do banco. */
@JsonClass(generateAdapter = true)
data class ParQRespostaDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "versao") val versao: String,
    @Json(name = "respostas") val respostas: List<Boolean>,
    @Json(name = "algum_sim") val algumSim: Boolean,
    @Json(name = "declaracao_aceita") val declaracaoAceita: Boolean,
    @Json(name = "termo_aceito") val termoAceito: Boolean,
    // Só na leitura: o gatilho do banco preenche com a data de Brasília.
    @Json(name = "respondido_no_dia") val respondidoNoDia: String? = null
) {
    fun dia(): Long? = respondidoNoDia?.let { runCatching { DataCivil.deIso(it) }.getOrNull() }
}

object ParQRepository {

    /** A resposta mais recente de quem está logado, ou nulo se nunca respondeu. */
    suspend fun ultima(): Resultado<ParQRespostaDto?> =
        chamarApi({ it.ultimoParQ() }) { lista -> Resultado.Ok(lista?.firstOrNull()) }

    suspend fun registrar(respostas: List<Boolean>, declaracao: Boolean, termo: Boolean): Resultado<ParQRespostaDto> {
        val algumSim = respostas.any { it }
        val corpo = ParQRespostaDto(
            versao = ParQ.VERSAO,
            respostas = respostas,
            algumSim = algumSim,
            declaracaoAceita = declaracao,
            // Sem SIM, o termo nem aparece: não registrar um aceite que não houve.
            termoAceito = algumSim && termo
        )
        return chamarApi({ it.registrarParQ(corpo) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it) }
                ?: Resultado.Falha("Suas respostas não foram salvas. Tente de novo.")
        }
    }
}
