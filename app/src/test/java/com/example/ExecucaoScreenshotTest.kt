package com.example

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.data.compartilhar.FormatoDoCartao
import com.example.data.execucao.ProgressoExecucao
import com.example.data.execucao.RegistroDoTreino
import com.example.data.execucao.RoteiroDeTreino
import com.example.data.progresso.Conquista
import com.example.data.progresso.SerieDeSemanas
import com.example.model.TrainingLevel
import com.example.ui.compartilhar.CartaoDoTreino
import com.example.ui.screens.ExecucaoDeTreinoScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EtapaExecucao
import com.example.viewmodel.ExecucaoUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h1500dp-420dpi", sdk = [36])
class ExecucaoScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/programa_nc.json").readText())
    private val roteiro = RoteiroDeTreino(ciclo.sugestao(DataCivil.deIso("2026-09-15"), TrainingLevel.INTERMEDIARIO)!!)
    private val resumoCompleto = RegistroDoTreino.resumo(
        RegistroDoTreino.montar(roteiro, ProgressoExecucao(roteiro.passos.size, 0), 2_520, 6, 4, "", "2026-09-13"),
        roteiro
    )

    private fun tela(estado: ExecucaoUiState, arquivo: String) {
        composeTestRule.setContent {
            MyApplicationTheme {
                ExecucaoDeTreinoScreen(
                    estado = estado,
                    onAlternarPausa = {}, onAvancar = {}, onVoltar = {}, onConcluir = {}, onSairSemSalvar = {},
                    onVoltarAoTreino = {}, onIntensidade = {}, onComplexidade = {}, onObservacao = {},
                    onSalvar = {}, onCompartilhar = {}, onFechar = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$arquivo")
    }

    @Test
    fun execucao_no_meio_do_treino() {
        tela(
            ExecucaoUiState(ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(2, 3), decorridoSegundos = 1_234, rodando = true),
            "execucao_ao_vivo.png"
        )
    }

    @Test
    fun resumo_com_uma_nota() {
        tela(
            ExecucaoUiState(
                ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(3, 0), decorridoSegundos = 1_860,
                etapa = EtapaExecucao.RESUMO, intensidade = 6
            ),
            "execucao_resumo.png"
        )
    }

    @Test
    fun publicar_nas_redes() {
        tela(
            ExecucaoUiState(
                ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(roteiro.passos.size, 0),
                etapa = EtapaExecucao.PUBLICAR, resumo = resumoCompleto,
                serie = SerieDeSemanas(3, true), estendeuSerie = true, pontosGanhos = 50, nivelDePontos = "Prata",
                conquistasNovas = listOf(Conquista("treino-2000", "Treino de 2.000m", "Completou um treino de 2.000m.", DataCivil.deIso("2026-09-15")))
            ),
            "execucao_publicar.png"
        )
    }

    @Test
    fun imagens_para_as_redes() {
        val parcial = RegistroDoTreino.resumo(
            RegistroDoTreino.montar(roteiro, ProgressoExecucao(2, 3), 1_500, 7, null, "", "2026-09-13"),
            roteiro
        )
        listOf(
            Triple(resumoCompleto, FormatoDoCartao.FEED, "cartao_feed.png"),
            Triple(resumoCompleto, FormatoDoCartao.STORIES, "cartao_stories.png"),
            Triple(parcial, FormatoDoCartao.FEED, "cartao_parcial.png")
        ).forEach { (resumo, formato, arquivo) ->
            val imagem = CartaoDoTreino.desenhar(resumo, formato)
            assertEquals(formato.largura, imagem.width)
            assertEquals(formato.altura, imagem.height)
            FileOutputStream("src/test/screenshots/$arquivo").use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
