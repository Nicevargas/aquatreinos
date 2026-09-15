package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.auth.Sessao
import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.data.conta.Perfil
import com.example.data.treinos.MontadorDeTreino
import com.example.data.treinos.Montagem
import com.example.data.supabase.toDomain
import com.example.data.supabase.WorkoutDto
import com.example.model.TrainingLevel
import com.example.ui.components.ContaCard
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EditorDeTreinoScreen
import com.example.ui.screens.MeusTreinosScreen
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ContaUiState
import com.example.viewmodel.EditorDeTreino
import com.example.viewmodel.MeusTreinosUiState
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
@Config(qualifiers = "w412dp-h1400dp-420dpi", sdk = [36])
class TelasDeContaScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/programa_nc.json").readText())
    private val dia = DataCivil.deIso("2026-09-15")
    private val sessao = Sessao("a", "r", 0, "u1", "ana@exemplo.com")

    // Um treino "meu" de verdade: a sugestão remontada como o formulário faz.
    private fun meuTreino(id: String, level: TrainingLevel, titulo: String) =
        (MontadorDeTreino.montar(
            MontadorDeTreino.paraDigitacao(ciclo.sugestao(dia, level)!!).copy(titulo = titulo)
        ) as Montagem.Pronto).treino.let { w ->
            WorkoutDto(
                id = id, title = w.title, subtitle = w.subtitle, tag = w.tag, workoutDate = w.workoutDate,
                totalDistanceMeters = w.totalDistanceMeters, estimatedMinutes = w.estimatedMinutes,
                calories = w.calories, level = w.level, phases = w.phases
            ).toDomain()
        }

    // A tela de login (com "Esqueci minha senha") fica em RecuperarSenhaScreenshotTest.

    @Test
    fun cadastro() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AuthScreen(
                    estado = ContaUiState(configurado = true),
                    onEntrar = { _, _ -> },
                    onCadastrar = { _, _, _, _ -> },
                    onLimparMensagens = {},
                    comecarNoCadastro = true
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/conta_cadastro.png")
    }

    @Test
    fun meus_treinos() {
        val estado = MeusTreinosUiState(
            treinos = listOf(
                meuTreino("t1", TrainingLevel.INTERMEDIARIO, "Regenerativo de domingo"),
                meuTreino("t2", TrainingLevel.AVANCADO, "Volume puxado")
            )
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.background(AquaBackground)) {
                    MeusTreinosScreen(
                        estado = estado,
                        onNovo = {}, onEditar = {}, onUsar = {}, onExcluir = {},
                        onConfirmarExclusao = {}, onCancelarExclusao = {}, onTentarDeNovo = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/meus_treinos.png")
    }

    @Test
    fun editor_com_sugestao_e_erro() {
        val digitado = MontadorDeTreino.paraDigitacao(ciclo.sugestao(dia, TrainingLevel.INICIANTE)!!).let {
            it.copy(fases = it.fases + ("Recuperação" to it.fases.getValue("Recuperação") + com.example.data.treinos.SerieDigitada("Costas 200m", "", "vinte")))
        }
        composeTestRule.setContent {
            MyApplicationTheme {
                EditorDeTreinoScreen(
                    editor = EditorDeTreino(digitado = digitado, erros = listOf("Recuperação, série 2: comece pela distância, como \"8x50m Crawl\" ou \"400m Crawl\".")),
                    onAlterar = {}, onSalvar = {}, onCancelar = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/editor_de_treino.png")
    }

    @Test
    fun conta_no_perfil() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.background(AquaBackground).padding(16.dp)) {
                    ContaCard(
                        estado = ContaUiState(
                            configurado = true,
                            sessao = sessao,
                            perfil = Perfil("u1", "ana@exemplo.com", "Ana Nadadora", 25, TrainingLevel.INTERMEDIARIO),
                            aviso = "Perfil salvo."
                        ),
                        onSalvar = { _, _, _ -> }, onSair = {}, onExcluirConta = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/conta_perfil.png")
    }
}
