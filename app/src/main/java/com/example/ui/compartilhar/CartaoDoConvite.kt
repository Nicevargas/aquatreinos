package com.example.ui.compartilhar

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.compartilhar.FormatoDoCartao
import com.example.data.compartilhar.TextosDoTreino
import com.example.model.TrainingLevel
import com.example.model.Workout

/**
 * Imagem do treino para fazer, que vai junto com o link ao compartilhar.
 * Mostra as fases com as séries e zonas; o código fica em destaque no rodapé.
 */
object CartaoDoConvite {

    private const val FUNDO_TOPO = 0xFF071A33.toInt()
    private const val FUNDO_BASE = 0xFF00315C.toInt()
    private const val AZUL = 0xFF0076D1.toInt()
    private const val CIANO = 0xFF00A3FF.toInt()
    private const val MAGENTA = 0xFFE30071.toInt()
    private const val AMARELO = 0xFFFFB800.toInt()
    private const val VERDE = 0xFF48CA1B.toInt()
    private const val BRANCO = 0xFFFFFFFF.toInt()
    private const val BRANCO_SUAVE = 0xB3FFFFFF.toInt()
    private const val CARTAO = 0x1AFFFFFF

    private val formato = FormatoDoCartao.FEED

    private fun corDoNivel(nivel: TrainingLevel): Int = when (nivel) {
        TrainingLevel.INICIANTE -> 0xFF8DC63F.toInt()
        TrainingLevel.INTERMEDIARIO -> 0xFFF7B90B.toInt()
        TrainingLevel.AVANCADO -> 0xFFE01B62.toInt()
    }

    // Mesmas cores do app e do carrossel.
    private fun corDoBloco(titulo: String): Int = when (titulo.trim().lowercase()) {
        "ativação", "ativacao", "aquecimento" -> AZUL
        "preparação", "preparacao", "preparatória", "preparatoria" -> AMARELO
        "desenvolvimento", "principal" -> MAGENTA
        "consolidação", "consolidacao" -> VERDE
        else -> CIANO
    }

    private fun corDaZona(sigla: String): Int = when (sigla.trim().uppercase()) {
        "A0" -> 0xFF8FB4D8.toInt()
        "A1" -> VERDE
        "A2" -> AMARELO
        "A3" -> 0xFFFF8A00.toInt()
        "AN" -> MAGENTA
        "AA" -> 0xFF9B7BFF.toInt()
        else -> BRANCO_SUAVE
    }

    fun desenhar(treino: Workout, codigo: String): Bitmap {
        val w = formato.largura.toFloat()
        val h = formato.altura.toFloat()
        val bitmap = Bitmap.createBitmap(formato.largura, formato.altura, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)

        c.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, w * 0.4f, h, FUNDO_TOPO, FUNDO_BASE, Shader.TileMode.CLAMP)
        })
        c.drawRect(0f, 0f, w, 16f, Paint().apply {
            shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(AZUL, CIANO, MAGENTA), null, Shader.TileMode.CLAMP)
        })

        val margem = 80f
        val largura = w - 2 * margem
        var y = 90f

        y = texto(c, "NATAÇÃO CRIATIVA", margem, y, 32f, CIANO, negrito = true, espaco = 0.3f)
        y += 28f
        y = texto(c, "TREINO PARA VOCÊ NADAR", margem, y, 42f, MAGENTA, negrito = true, espaco = 0.12f)
        y += 12f
        y = texto(c, treino.focus ?: treino.title, margem, y, 92f, BRANCO, negrito = true, larguraMax = largura)
        y += 14f

        // Metros e tempo, com o nível ao lado.
        val metros = "${TextosDoTreino.metros(treino.totalDistanceMeters)} · ~${treino.estimatedMinutes} min"
        val fimMetros = texto(c, metros, margem, y, 60f, CIANO, negrito = true, larguraMax = largura * 0.62f)
        val rotulo = TextosDoTreino.nivel(treino.level)
        val tintaChip = tinta(32f, FUNDO_TOPO, negrito = true)
        val larguraChip = tintaChip.measureText(rotulo) + 52f
        val topoChip = y + 4f
        c.drawRoundRect(RectF(w - margem - larguraChip, topoChip, w - margem, topoChip + 62f), 31f, 31f, pincel(corDoNivel(treino.level)))
        c.drawText(rotulo, w - margem - larguraChip + 26f, topoChip + 43f, tintaChip)
        y = maxOf(fimMetros, topoChip + 62f) + 36f

        // Fases: cada uma num cartão, com as séries e as zonas.
        val topoRodape = h - 200f
        val fases = treino.phases.filter { it.sets.isNotEmpty() || it.distanceMeters > 0 }
        val vao = 16f
        val alturaFase = if (fases.isEmpty()) 0f else ((topoRodape - 30f - y - vao * (fases.size - 1)) / fases.size).coerceAtMost(150f)
        fases.forEach { fase ->
            val caixa = RectF(margem, y, margem + largura, y + alturaFase)
            c.drawRoundRect(caixa, 24f, 24f, pincel(CARTAO))
            c.drawRoundRect(RectF(margem + 22f, y + 22f, margem + 32f, y + alturaFase - 22f), 5f, 5f, pincel(corDoBloco(fase.title)))

            val x = margem + 56f
            val larguraTexto = largura - 56f - 28f
            val titulo = "${fase.title} · ${TextosDoTreino.metros(fase.distanceMeters)}"
            val tamanhoTitulo = if (alturaFase < 110f) 36f else 42f
            val fimTitulo = texto(c, titulo, x, y + 20f, tamanhoTitulo, BRANCO, negrito = true, larguraMax = larguraTexto * 0.62f)

            // Zonas da fase como etiquetas coloridas, à direita.
            var direita = margem + largura - 24f
            fase.sets.mapNotNull { it.zona?.takeIf(String::isNotBlank) }.distinct().reversed().forEach { zona ->
                val tz = tinta(28f, FUNDO_TOPO, negrito = true)
                val lz = tz.measureText(zona) + 28f
                c.drawRoundRect(RectF(direita - lz, y + 22f, direita, y + 66f), 12f, 12f, pincel(corDaZona(zona)))
                c.drawText(zona, direita - lz + 14f, y + 55f, tz)
                direita -= lz + 10f
            }

            val series = fase.sets.map { s -> s.header.ifBlank { "${s.repsDistance}m ${s.description}".trim() } }
            if (series.isNotEmpty() && y + alturaFase - fimTitulo > 44f) {
                texto(c, series.joinToString(" + "), x, fimTitulo + 8f, 32f, BRANCO_SUAVE, larguraMax = larguraTexto)
            }
            y += alturaFase + vao
        }

        // Rodapé: onde fazer e o código.
        c.drawRoundRect(RectF(margem, topoRodape, margem + largura, h - 70f), 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(margem, 0f, margem + largura, 0f, AZUL, MAGENTA, Shader.TileMode.CLAMP)
        })
        texto(c, "Faça no app Natação Criativa", margem + 40f, topoRodape + 24f, 40f, BRANCO, negrito = true, larguraMax = largura - 80f)
        texto(c, "Código do treino: $codigo", margem + 40f, topoRodape + 80f, 36f, BRANCO, larguraMax = largura - 80f)

        return bitmap
    }

    private fun pincel(cor: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor }

    private fun tinta(tamanho: Float, cor: Int, negrito: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamanho
        color = cor
        typeface = if (negrito) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    /** Escreve com o topo da letra em [topo] e devolve onde a linha termina. Encolhe para caber. */
    private fun texto(
        c: Canvas,
        conteudo: String,
        x: Float,
        topo: Float,
        tamanho: Float,
        cor: Int,
        negrito: Boolean = false,
        larguraMax: Float? = null,
        espaco: Float = 0f
    ): Float {
        val t = tinta(tamanho, cor, negrito).apply { letterSpacing = espaco }
        var linha = conteudo
        if (larguraMax != null) {
            while (t.measureText(linha) > larguraMax && t.textSize > 28f) t.textSize -= 2f
            while (t.measureText(linha) > larguraMax && linha.length > 2) linha = linha.dropLast(2) + "…"
        }
        val medidas = t.fontMetrics
        val base = topo - medidas.ascent
        c.drawText(linha, x, base, t)
        return base + medidas.descent
    }
}

/**
 * Abre o menu de compartilhar com a imagem do treino e a mensagem com o link.
 * O WhatsApp manda a mensagem como legenda da imagem; o Instagram ignora o texto,
 * por isso ele também vai para a área de transferência. Se a imagem falhar, vai só o texto.
 */
fun compartilharTreinoParaFazer(context: Context, treino: Workout, codigo: String, mensagem: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText("Treino", mensagem))
    Toast.makeText(context, "Mensagem copiada. No Instagram, cole na legenda.", Toast.LENGTH_LONG).show()

    val envio = runCatching {
        val arquivo = CartaoDoTreino.salvar(context, CartaoDoConvite.desenhar(treino, codigo))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.compartilhar", arquivo)
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, mensagem)
            clipData = ClipData.newRawUri("Treino", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrElse {
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, mensagem)
    }
    val escolha = Intent.createChooser(envio, "Compartilhar treino")
    if (context !is Activity) escolha.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(escolha)
}
