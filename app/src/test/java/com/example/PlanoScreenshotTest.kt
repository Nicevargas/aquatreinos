package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.ciclo.CicloDeTreinos
import com.example.data.plano.ConfigDoPlano
import com.example.data.plano.PlanoDeTreino
import com.example.data.plano.PlanoDto
import com.example.model.TrainingLevel
import com.example.ui.components.PlanoCard
import com.example.ui.screens.CriarPlanoScreen
import com.example.ui.screens.PlanoDeTreinoScreen
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AssistenteDoPlano
import com.example.viewmodel.EtapaDoPlano
import com.example.viewmodel.PlanoUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h2400dp-420dpi", sdk = [36])
class PlanoScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val treinosNC = CicloDeTreinos.treinosDoJson(File("src/main/assets/programa_nc.json").readText())
    private val config = ConfigDoPlano(8, 3, listOf("Técnica", "Velocidade"), true, true, TrainingLevel.INTERMEDIARIO)
    private val dto = PlanoDto.de(config).copy(id = "p1")
    private val semanas = PlanoDeTreino.montar(config, treinosNC, "p1")
    private val feitos = setOf(1 to 1, 1 to 2, 1 to 3, 2 to 1)
    private val estado = PlanoUiState(plano = dto, semanas = semanas, feitos = feitos, aberto = true)

    private fun capturar(arquivo: String, conteudo: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent { MyApplicationTheme { conteudo() } }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$arquivo")
    }

    @Test
    fun plano_modo() = capturar("plano_0_modo.png") {
        CriarPlanoScreen(AssistenteDoPlano(manual = true), onAlterar = {}, onProximo = {}, onVoltar = {}, onFechar = {})
    }

    @Test
    fun plano_semanas() = capturar("plano_1_semanas.png") {
        CriarPlanoScreen(AssistenteDoPlano(etapa = EtapaDoPlano.SEMANAS), onAlterar = {}, onProximo = {}, onVoltar = {}, onFechar = {})
    }

    @Test
    fun plano_trocar_treino() = capturar("plano_troca.png") {
        val vaga = semanas[1].treinos[1]
        PlanoDeTreinoScreen(
            estado = estado.copy(
                trocando = vaga,
                opcoesDaTroca = PlanoDeTreino.opcoesDoMetodo(config, 2, treinosNC),
                meusTreinos = listOf(
                    semanas[0].treinos[0].workout.copy(id = "m1", title = "Meu treino de sábado", level = TrainingLevel.INICIANTE)
                )
            ),
            onVoltar = {}, onTreino = {}, onNovoPlano = {}, onExcluir = {},
            onConfirmarExclusao = {}, onCancelarExclusao = {}, onTentarDeNovo = {}
        )
    }

    @Test
    fun plano_vezes() = capturar("plano_2_vezes.png") {
        CriarPlanoScreen(AssistenteDoPlano(etapa = EtapaDoPlano.VEZES), onAlterar = {}, onProximo = {}, onVoltar = {}, onFechar = {})
    }

    @Test
    fun plano_focos() = capturar("plano_3_focos.png") {
        CriarPlanoScreen(
            AssistenteDoPlano(etapa = EtapaDoPlano.FOCOS, focos = setOf("Técnica", "Velocidade")),
            onAlterar = {}, onProximo = {}, onVoltar = {}, onFechar = {}
        )
    }

    @Test
    fun plano_lista() = capturar("plano_lista.png") {
        PlanoDeTreinoScreen(
            estado = estado,
            onVoltar = {}, onTreino = {}, onNovoPlano = {}, onExcluir = {},
            onConfirmarExclusao = {}, onCancelarExclusao = {}, onTentarDeNovo = {}
        )
    }

    @Test
    fun plano_na_tela_inicial() = capturar("plano_card.png") {
        Box(Modifier.background(AquaBackground).padding(16.dp)) {
            Column {
                PlanoCard(resumo = null, onClick = {})
                Spacer(Modifier.height(12.dp))
                PlanoCard(resumo = estado.resumo, onClick = {})
            }
        }
    }
}
