package com.example.data.conta

import com.example.data.Resultado
import com.example.data.auth.Sessao
import com.example.data.supabase.ProfileDto
import com.example.data.supabase.ProfileWriteDto
import com.example.data.supabase.chamarApi
import com.example.model.TrainingLevel

data class Perfil(
    val id: String,
    val email: String,
    val nome: String,
    val piscinaMetros: Int, // 25 ou 50
    val nivel: TrainingLevel
)

fun ProfileDto.paraPerfil(sessao: Sessao): Perfil = Perfil(
    id = id ?: sessao.userId,
    email = email ?: sessao.email,
    nome = fullName.orEmpty(),
    piscinaMetros = if (preferredPoolMeters == 50) 50 else 25,
    nivel = TrainingLevel.entries.firstOrNull { it.name.equals(trainingLevel, ignoreCase = true) }
        ?: TrainingLevel.INTERMEDIARIO
)

object PerfilRepository {

    suspend fun carregar(sessao: Sessao): Resultado<Perfil> {
        val lido = chamarApi({ it.getProfile("eq.${sessao.userId}") }) { lista ->
            Resultado.Ok(lista.orEmpty().firstOrNull())
        }
        return when (lido) {
            is Resultado.Falha -> lido
            is Resultado.Ok -> lido.valor?.let { Resultado.Ok(it.paraPerfil(sessao)) }
                // Conta criada antes do gatilho de perfil, ou cujo perfil sumiu: cria agora.
                ?: salvar(
                    sessao,
                    Perfil(
                        id = sessao.userId,
                        email = sessao.email,
                        nome = sessao.email.substringBefore('@'),
                        piscinaMetros = 25,
                        nivel = TrainingLevel.INTERMEDIARIO
                    )
                )
        }
    }

    suspend fun salvar(sessao: Sessao, perfil: Perfil): Resultado<Perfil> {
        val corpo = ProfileWriteDto(
            id = sessao.userId,
            fullName = perfil.nome.trim().ifEmpty { null },
            preferredPoolMeters = if (perfil.piscinaMetros == 50) 50 else 25,
            trainingLevel = perfil.nivel.name
        )
        return chamarApi({ it.upsertProfile(corpo) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it.paraPerfil(sessao)) }
                ?: Resultado.Falha("O perfil não foi salvo. Tente de novo.")
        }
    }

    /** Perfil, treinos, séries e estatísticas saem junto com a conta (no banco). */
    suspend fun excluirConta(): Resultado<Unit> =
        chamarApi({ it.deleteMyAccount() }) { Resultado.Ok(Unit) }
}
