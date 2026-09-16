package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.ciclo.DataCivil
import com.example.ui.components.ParQCard
import com.example.ui.screens.ParQScreen
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ParQUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h2200dp-420dpi", sdk = [36])
class ParQScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun tela(estado: ParQUiState, arquivo: String) {
        composeTestRule.setContent {
            MyApplicationTheme {
                ParQScreen(estado = estado, onResponder = { _, _ -> }, onDeclaracao = {}, onTermo = {}, onEnviar = {}, onFechar = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$arquivo")
    }

    @Test
    fun parq_comecando() = tela(
        ParQUiState(aberto = true, vaiTreinar = true, respostas = listOf(false, false, null, null, null, null, null)),
        "parq_comecando.png"
    )

    @Test
    fun parq_com_sim_e_termo() = tela(
        ParQUiState(
            aberto = true, vaiTreinar = true,
            respostas = listOf(false, false, false, false, true, false, false),
            declaracao = true, termo = true
        ),
        "parq_com_sim.png"
    )

    @Test
    fun parq_no_perfil() {
        val hoje = DataCivil.deIso("2026-09-15")
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.background(AquaBackground).padding(16.dp)) {
                    androidx.compose.foundation.layout.Column {
                        ParQCard(ultimoDia = DataCivil.deIso("2026-09-15"), onResponder = {}, hoje = hoje)
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                        ParQCard(ultimoDia = DataCivil.deIso("2025-09-01"), onResponder = {}, hoje = hoje)
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                        ParQCard(ultimoDia = null, onResponder = {}, hoje = hoje)
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/parq_perfil.png")
    }
}
