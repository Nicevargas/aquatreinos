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
import androidx.core.content.FileProvider
import com.example.data.ciclo.DataCivil
import com.example.data.compartilhar.FormatoDoCartao
import com.example.data.compartilhar.ResumoDoTreino
import com.example.data.compartilhar.TextosDoTreino
import com.example.model.TrainingLevel
import java.io.File
import java.io.FileOutputStream

/** Imagem do treino concluído, para publicar nas redes. Desenhada direto no Canvas. */
object CartaoDoTreino {

    private const val FUNDO_TOPO = 0xFF071A33.toInt()
    private const val FUNDO_BASE = 0xFF00315C.toInt()
    private const val AZUL = 0xFF0076D1.toInt()
    private const val CIANO = 0xFF00A3FF.toInt()
    private const val MAGENTA = 0xFFE30071.toInt()
    private const val BRANCO = 0xFFFFFFFF.toInt()
    private const val BRANCO_SUAVE = 0xB3FFFFFF.toInt()
    private const val TRILHA = 0x33FFFFFF

    private val DIAS = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

    // Cores dos níveis no carrossel.
    private fun corDoNivel(nivel: TrainingLevel): Int = when (nivel) {
        TrainingLevel.INICIANTE -> 0xFF8DC63F.toInt()
        TrainingLevel.INTERMEDIARIO -> 0xFFF7B90B.toInt()
        TrainingLevel.AVANCADO -> 0xFFE01B62.toInt()
    }

    fun desenhar(r: ResumoDoTreino, formato: FormatoDoCartao): Bitmap {
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

        val margem = 90f
        val largura = w - 2 * margem
        val stories = formato == FormatoDoCartao.STORIES
        // No stories o topo e a base ficam sob os controles do Instagram; o feed
        // (4:5) é mais baixo e precisa ser compacto para caber as duas notas.
        var y = if (stories) 320f else 110f

        y = texto(c, "AQUAGENDA", margem, y, 34f, CIANO, negrito = true, espaco = 0.3f)
        y += 36f
        y = texto(c, if (r.completo) "TREINO CONCLUÍDO" else "TREINO REGISTRADO", margem, y, 46f, MAGENTA, negrito = true, espaco = 0.12f)
        y += 16f
        y = texto(c, r.foco ?: r.titulo, margem, y, 104f, BRANCO, negrito = true, larguraMax = largura)
        y += 8f
        y = texto(c, dataLonga(r.dataIso), margem, y, 40f, BRANCO_SUAVE)

        y += if (stories) 130f else 50f
        y = texto(c, TextosDoTreino.metros(r.metrosFeitos), margem, y, if (stories) 230f else 190f, BRANCO, negrito = true, larguraMax = largura)
        y += 4f
        val subtitulo = if (r.completo) {
            "em ${TextosDoTreino.duracao(r.duracaoSegundos)}"
        } else {
            "de ${TextosDoTreino.metros(r.metrosPlanejados)} · ${TextosDoTreino.duracao(r.duracaoSegundos)}"
        }
        y = texto(c, subtitulo, margem, y, 60f, CIANO, negrito = true, larguraMax = largura)

        if (!r.completo && r.metrosPlanejados > 0) {
            y += 28f
            val fracao = (r.metrosFeitos.toFloat() / r.metrosPlanejados).coerceIn(0f, 1f)
            c.drawRoundRect(RectF(margem, y, margem + largura, y + 20f), 10f, 10f, pincel(TRILHA))
            c.drawRoundRect(RectF(margem, y, margem + largura * fracao, y + 20f), 10f, 10f, pincel(CIANO))
            y += 20f
        }

        y += 50f
        val rotulo = TextosDoTreino.nivel(r.nivel)
        val tinta = texto(38f, FUNDO_TOPO, negrito = true)
        val larguraChip = tinta.measureText(rotulo) + 64f
        c.drawRoundRect(RectF(margem, y, margem + larguraChip, y + 76f), 38f, 38f, pincel(corDoNivel(r.nivel)))
        c.drawText(rotulo, margem + 32f, y + 52f, tinta)
        y += 76f

        if (r.intensidade != null || r.complexidade != null) {
            y += if (stories) 110f else 44f
            val entreNotas = if (stories) 36f else 24f
            r.intensidade?.let { y = barraDeNota(c, "Intensidade", it, margem, y, largura) + entreNotas }
            r.complexidade?.let { y = barraDeNota(c, "Complexidade", it, margem, y, largura) + entreNotas }
        }

        val rodape = if (r.doCarrossel) "CADA DIA 1 TREINO · @natacaocriativa" else "Treino criado no Aquagenda"
        // O rodapé fica embaixo, mas nunca por cima do que já foi desenhado.
        val topoRodape = maxOf(h - if (stories) 330f else 110f, y + 24f)
        texto(c, rodape, margem, topoRodape, 38f, BRANCO, negrito = true, larguraMax = largura)

        return bitmap
    }

    private fun dataLonga(iso: String): String = runCatching {
        val dia = DataCivil.deIso(iso)
        "${DIAS[DataCivil.diaDaSemana(dia)]}, ${DataCivil.paraBr(dia)}"
    }.getOrDefault(iso)

    private fun pincel(cor: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor }

    private fun texto(tamanho: Float, cor: Int, negrito: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        val tinta = texto(tamanho, cor, negrito).apply { letterSpacing = espaco }
        var linha = conteudo
        if (larguraMax != null) {
            while (tinta.measureText(linha) > larguraMax && tinta.textSize > 40f) tinta.textSize -= 4f
            while (tinta.measureText(linha) > larguraMax && linha.length > 2) linha = linha.dropLast(2) + "…"
        }
        val medidas = tinta.fontMetrics
        val base = topo - medidas.ascent
        c.drawText(linha, x, base, tinta)
        return base + medidas.descent
    }

    private fun barraDeNota(c: Canvas, nome: String, nota: Int, x: Float, topo: Float, largura: Float): Float {
        val fim = texto(c, "$nome: $nota/10", x, topo, 40f, BRANCO, negrito = true)
        val y = fim + 14f
        val vao = 10f
        val segmento = (largura - 9 * vao) / 10
        for (i in 0 until 10) {
            val esquerda = x + i * (segmento + vao)
            c.drawRoundRect(RectF(esquerda, y, esquerda + segmento, y + 22f), 11f, 11f, pincel(if (i < nota) MAGENTA else TRILHA))
        }
        return y + 22f
    }

    /** Grava o PNG na pasta de cache compartilhável; apaga os antigos. */
    fun salvar(context: Context, bitmap: Bitmap): File {
        val pasta = File(context.cacheDir, "compartilhar").apply { mkdirs() }
        pasta.listFiles()?.forEach { it.delete() }
        val arquivo = File(pasta, "treino_${System.currentTimeMillis()}.png")
        FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return arquivo
    }
}

/**
 * Abre o menu de compartilhar do Android com a imagem e a legenda.
 * O Instagram ignora o texto que vem junto; por isso a legenda também vai para
 * a área de transferência, e é só colar.
 */
fun compartilharTreino(context: Context, resumo: ResumoDoTreino, formato: FormatoDoCartao) {
    val arquivo = CartaoDoTreino.salvar(context, CartaoDoTreino.desenhar(resumo, formato))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.compartilhar", arquivo)
    val legenda = TextosDoTreino.legenda(resumo)

    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText("Legenda do treino", legenda))

    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, legenda)
        clipData = ClipData.newRawUri("Treino", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val escolha = Intent.createChooser(envio, "Publicar treino")
    if (context !is Activity) escolha.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(escolha)
}
