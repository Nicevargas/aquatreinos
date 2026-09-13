package com.example.data.supabase

import android.util.Log
import com.example.data.Resultado
import com.example.data.auth.MensagensAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/** Faz a chamada no Supabase e traduz falha de rede, HTTP e RLS em mensagem para a tela. */
internal suspend fun <T, R> chamarApi(
    chamada: suspend (SupabaseApi) -> Response<T>,
    sucesso: (T?) -> Resultado<R>
): Resultado<R> = withContext(Dispatchers.IO) {
    val api = SupabaseClient.api ?: return@withContext Resultado.Falha(MensagensAuth.SEM_CONFIGURACAO)
    try {
        val resposta = chamada(api)
        if (resposta.isSuccessful) {
            sucesso(resposta.body())
        } else {
            Resultado.Falha(MensagensAuth.deErroDaApi(resposta.code(), resposta.errorBody()?.string()))
        }
    } catch (e: IOException) {
        Resultado.Falha(MensagensAuth.SEM_REDE)
    } catch (e: Exception) {
        Log.e("ChamadaApi", "Falha inesperada na chamada ao Supabase", e)
        Resultado.Falha("Não foi possível concluir. Tente de novo.")
    }
}
