package com.example.data.compartilhar

import android.content.Intent
import com.example.data.Resultado
import com.example.data.supabase.WorkoutDto
import com.example.data.supabase.chamarApi
import com.example.data.supabase.toDomain
import com.example.data.treinos.paraDto
import com.example.model.Workout
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@JsonClass(generateAdapter = true)
data class NovoTreinoCompartilhadoDto(
    @Json(name = "titulo") val titulo: String,
    @Json(name = "treino") val treino: WorkoutDto
)

/** Linha de public.treinos_compartilhados ou resposta de abrir_treino_compartilhado(). */
@JsonClass(generateAdapter = true)
data class TreinoCompartilhadoDto(
    @Json(name = "codigo") val codigo: String,
    @Json(name = "titulo") val titulo: String? = null,
    @Json(name = "treino") val treino: WorkoutDto? = null,
    @Json(name = "enviado_por") val enviadoPor: String? = null
)

@JsonClass(generateAdapter = true)
data class AbrirTreinoParams(@Json(name = "p_codigo") val codigo: String)

/**
 * Código e link de um treino compartilhado. O mesmo link serve para mandar a
 * outro usuário do app, no WhatsApp ou nas redes: a página abre o app no treino.
 */
object CodigoDoTreino {

    const val PAGINA = "https://nicevargas.github.io/natacao-treinos/treino.html"
    const val ESQUEMA = "natacaocriativa"

    private val SO_CODIGO = Regex("^[a-f0-9]{10}$")
    private val NO_TEXTO = Regex("""(?:#|treino/|c[oó]digo(?:\s+do\s+treino)?:?\s*)([a-f0-9]{10})(?![a-f0-9])""", RegexOption.IGNORE_CASE)

    fun link(codigo: String) = "$PAGINA#$codigo"

    /** Código a partir do que a pessoa colou: o link, a mensagem inteira ou só o código. */
    fun extrair(texto: String?): String? {
        val t = texto?.trim()?.lowercase() ?: return null
        if (SO_CODIGO.matches(t)) return t
        return NO_TEXTO.find(t)?.groupValues?.get(1)
    }
}

/** Mensagem pronta para o WhatsApp: o treino inteiro para ler, o link e o código. */
object MensagemDoTreino {

    fun paraCompartilhar(treino: Workout, codigo: String): String = buildString {
        append("🏊 *").append(treino.title).append("*\n")
        append(TextosDoTreino.metros(treino.totalDistanceMeters))
        append(" · ~").append(treino.estimatedMinutes).append(" min · ")
        append(TextosDoTreino.nivel(treino.level)).append('\n')
        treino.phases.forEach { fase ->
            append("\n*").append(fase.title).append("* · ").append(TextosDoTreino.metros(fase.distanceMeters)).append('\n')
            fase.sets.forEach { s ->
                val serie = s.header.ifBlank { "${s.repsDistance}m ${s.description}".trim() }
                append("• ").append(serie)
                s.zona?.let { append(" (").append(it).append(')') }
                if (s.intervalTarget.isNotBlank()) append(' ').append(s.intervalTarget)
                append('\n')
                if (s.details.isNotEmpty()) append("   ").append(s.details.joinToString("; ")).append('\n')
            }
        }
        append("\nFaça este treino no app Natação Criativa: ").append(CodigoDoTreino.link(codigo)).append('\n')
        append("Código do treino: ").append(codigo)
    }
}

/** Link de treino que abriu o app (natacaocriativa://treino/<código>), esperando o login. */
object LinkDeTreino {

    private val _pendente = MutableStateFlow<String?>(null)
    val pendente: StateFlow<String?> = _pendente.asStateFlow()

    fun receber(intent: Intent?) {
        val dados = intent?.data ?: return
        val codigo = when {
            dados.scheme == CodigoDoTreino.ESQUEMA && dados.host == "treino" -> CodigoDoTreino.extrair(dados.lastPathSegment)
            else -> CodigoDoTreino.extrair(dados.toString())
        }
        if (codigo != null) _pendente.value = codigo
    }

    fun consumido() {
        _pendente.value = null
    }
}

object TreinosCompartilhadosRepository {

    /** Guarda uma cópia do treino e devolve o código gerado pelo banco. */
    suspend fun compartilhar(treino: Workout): Resultado<String> {
        val corpo = NovoTreinoCompartilhadoDto(titulo = treino.title.take(200), treino = treino.paraDto())
        return chamarApi({ it.compartilharTreino(corpo) }) { lista ->
            lista?.firstOrNull()?.let { Resultado.Ok(it.codigo) }
                ?: Resultado.Falha("Não consegui gerar o link do treino. Tente de novo.")
        }
    }

    /** O treino do código, pronto para nadar; entra em Meus treinos ao concluir. */
    suspend fun abrir(codigo: String): Resultado<Workout> =
        chamarApi({ it.abrirTreinoCompartilhado(AbrirTreinoParams(codigo)) }) { lista ->
            val dto = lista?.firstOrNull()
            val treino = dto?.treino
            if (dto == null || treino == null) {
                Resultado.Falha("Treino não encontrado. Confira o código.")
            } else {
                Resultado.Ok(
                    treino.copy(id = "recebido_${dto.codigo}", workoutDate = null, isSuggestion = false)
                        .toDomain()
                        .copy(
                            tag = dto.enviadoPor?.let { "Recebido de $it" } ?: "Treino recebido",
                            salvarAoConcluir = true
                        )
                )
            }
        }
}
