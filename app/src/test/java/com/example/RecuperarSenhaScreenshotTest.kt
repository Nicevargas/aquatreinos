package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.RecuperarSenhaScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ContaUiState
import com.example.viewmodel.EtapaRecuperacao
import com.example.viewmodel.RecuperacaoUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h1100dp-420dpi", sdk = [36])
class RecuperarSenhaScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun login_com_link_esqueci_senha() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AuthScreen(
                    estado = ContaUiState(configurado = true, erro = "E-mail ou senha incorretos."),
                    onEntrar = { _, _ -> },
                    onCadastrar = { _, _, _, _ -> },
                    onLimparMensagens = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/conta_login.png")
    }

    @Test
    fun recuperar_email() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RecuperarSenhaScreen(
                    estado = RecuperacaoUiState(email = "ana@exemplo.com"),
                    configurado = true,
                    onEnviarCodigo = {}, onRedefinir = { _, _, _ -> }, onTrocarEmail = {}, onVoltar = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recuperar_senha_email.png")
    }

    @Test
    fun recuperar_codigo() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RecuperarSenhaScreen(
                    estado = RecuperacaoUiState(
                        etapa = EtapaRecuperacao.CODIGO,
                        email = "ana@exemplo.com",
                        aviso = "Se houver uma conta com ana@exemplo.com, o código chega em instantes. Confira também o spam.",
                        erro = "Código inválido ou vencido. Confira os números ou peça um novo."
                    ),
                    configurado = true,
                    onEnviarCodigo = {}, onRedefinir = { _, _, _ -> }, onTrocarEmail = {}, onVoltar = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recuperar_senha_codigo.png")
    }
}
