package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.example.data.WorkoutRepository
import com.example.data.auth.Sessao
import com.example.data.ciclo.CicloDeTreinos
import com.example.data.ciclo.DataCivil
import com.example.data.compartilhar.FormatoDoCartao
import com.example.data.conta.Perfil
import com.example.data.execucao.ProgressoExecucao
import com.example.data.execucao.RegistroDoTreino
import com.example.data.execucao.RoteiroDeTreino
import com.example.data.supabase.WorkoutDto
import com.example.data.supabase.toDomain
import com.example.data.treinos.MontadorDeTreino
import com.example.data.treinos.Montagem
import com.example.model.AppNavTab
import com.example.model.SwimSetStopwatchState
import com.example.model.TrainingLevel
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ContaCard
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EditorDeTreinoScreen
import com.example.ui.screens.ExecucaoDeTreinoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MeusTreinosScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecuperarSenhaScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ContaUiState
import com.example.viewmodel.EditorDeTreino
import com.example.viewmodel.EtapaExecucao
import com.example.viewmodel.EtapaRecuperacao
import com.example.viewmodel.ExecucaoUiState
import com.example.viewmodel.MeusTreinosUiState
import com.example.viewmodel.RecuperacaoUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * O pior caso comum: celular de 360dp com a letra grande do Android (1,3×).
 * Se a informação cabe aqui sem cortar nem encavalar, cabe nos outros.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h2600dp-420dpi", sdk = [36])
class LayoutEstreitoScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/treinos_ciclo.json").readText())
    private val dia = DataCivil.deIso("2026-09-13")
    private val intermediario = ciclo.sugestao(dia, TrainingLevel.INTERMEDIARIO)!!
    private val roteiro = RoteiroDeTreino(intermediario)

    private fun capturar(arquivo: String, conteudo: @Composable () -> Unit) {
        composeTestRule.setContent {
            val atual = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(atual.density, 1.3f)) {
                MyApplicationTheme {
                    Box(Modifier.background(AquaBackground)) { conteudo() }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/estreito/$arquivo")
    }

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

    @Test
    fun home() = capturar("home.png") {
        Column {
            AppTopBar()
            HomeScreen(
                workout = ciclo.sugestao(dia, TrainingLevel.INICIANTE)!!,
                selectedLevel = TrainingLevel.INICIANTE,
                calendarDays = WorkoutRepository.semanaDoCalendario(dia, dia),
                stopwatchState = SwimSetStopwatchState(),
                onDayClick = {}, onLevelChange = {}, onStartWorkoutClick = {}, onViewWorkoutDetails = {},
                onToggleStopwatch = {}, onLapStopwatch = {}, onResetStopwatch = {},
                onStopwatchModeChange = {}, onStopwatchPrevSet = {}, onStopwatchNextSet = {}
            )
        }
    }

    @Test
    fun treinos() = capturar("treinos.png") {
        WorkoutsScreen(workout = intermediario, onStartWorkoutClick = {}, onSaveToMyWorkouts = {})
    }

    @Test
    fun meus_treinos() = capturar("meus_treinos.png") {
        Column {
            MeusTreinosScreen(
                estado = MeusTreinosUiState(
                    treinos = listOf(
                        meuTreino("t1", TrainingLevel.INTERMEDIARIO, "Regenerativo de domingo"),
                        meuTreino("t2", TrainingLevel.AVANCADO, "Volume puxado")
                    )
                ),
                onNovo = {}, onEditar = {}, onUsar = {}, onExcluir = {},
                onConfirmarExclusao = {}, onCancelarExclusao = {}, onTentarDeNovo = {},
                modifier = Modifier.weight(1f, fill = false)
            )
            BottomNavBar(selectedTab = AppNavTab.MY_WORKOUTS, onTabSelected = {})
        }
    }

    @Test
    fun editor() = capturar("editor.png") {
        EditorDeTreinoScreen(
            editor = EditorDeTreino(
                digitado = MontadorDeTreino.paraDigitacao(ciclo.sugestao(dia, TrainingLevel.INICIANTE)!!),
                erros = listOf("Final, série 2: comece pela distância, como \"8x50m Crawl\" ou \"400m Crawl\".")
            ),
            onAlterar = {}, onSalvar = {}, onCancelar = {}
        )
    }

    private fun execucao(estado: ExecucaoUiState, arquivo: String) = capturar(arquivo) {
        ExecucaoDeTreinoScreen(
            estado = estado,
            onAlternarPausa = {}, onAvancar = {}, onVoltar = {}, onConcluir = {}, onSairSemSalvar = {},
            onVoltarAoTreino = {}, onIntensidade = {}, onComplexidade = {}, onObservacao = {},
            onSalvar = {}, onCompartilhar = {}, onFechar = {}
        )
    }

    @Test
    fun execucao_ao_vivo() = execucao(
        ExecucaoUiState(ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(2, 3), decorridoSegundos = 1_234, rodando = true),
        "execucao_ao_vivo.png"
    )

    @Test
    fun execucao_resumo() = execucao(
        ExecucaoUiState(
            ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(3, 0), decorridoSegundos = 1_860,
            etapa = EtapaExecucao.RESUMO, intensidade = 6
        ),
        "execucao_resumo.png"
    )

    @Test
    fun execucao_publicar() = execucao(
        ExecucaoUiState(
            ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(5, 0), etapa = EtapaExecucao.PUBLICAR,
            resumo = RegistroDoTreino.resumo(
                RegistroDoTreino.montar(roteiro, ProgressoExecucao(5, 0), 2_520, 6, 4, "", "2026-09-13"),
                roteiro
            )
        ),
        "execucao_publicar.png"
    )

    @Test
    fun login() = capturar("login.png") {
        AuthScreen(
            estado = ContaUiState(configurado = true, erro = "E-mail ou senha incorretos."),
            onEntrar = { _, _ -> }, onCadastrar = { _, _, _, _ -> }, onLimparMensagens = {}
        )
    }

    @Test
    fun cadastro() = capturar("cadastro.png") {
        AuthScreen(
            estado = ContaUiState(configurado = true),
            onEntrar = { _, _ -> }, onCadastrar = { _, _, _, _ -> }, onLimparMensagens = {},
            comecarNoCadastro = true
        )
    }

    @Test
    fun recuperar_codigo() = capturar("recuperar_codigo.png") {
        RecuperarSenhaScreen(
            estado = RecuperacaoUiState(
                etapa = EtapaRecuperacao.CODIGO,
                email = "nadadora.com.nome.comprido@exemplo.com.br",
                aviso = "Se houver uma conta com esse e-mail, o código chega em instantes. Confira também o spam.",
                erro = "Código inválido ou vencido. Confira os números ou peça um novo."
            ),
            configurado = true,
            onEnviarCodigo = {}, onRedefinir = { _, _, _ -> }, onTrocarEmail = {}, onVoltar = {}
        )
    }

    @Test
    fun perfil() = capturar("perfil.png") {
        ProfileScreen(
            onDownloadWorkoutClick = {},
            cabecalho = {
                ContaCard(
                    estado = ContaUiState(
                        configurado = true,
                        sessao = Sessao("a", "r", 0, "u1", "nadadora.com.nome.comprido@exemplo.com.br"),
                        perfil = Perfil("u1", "nadadora.com.nome.comprido@exemplo.com.br", "Ana Maria Nadadora dos Santos", 25, TrainingLevel.AVANCADO),
                        aviso = "Perfil salvo."
                    ),
                    onSalvar = { _, _, _ -> }, onSair = {}, onExcluirConta = {}
                )
            }
        )
    }

    @Test
    fun perfil_editando() = capturar("perfil_editando.png") {
        ContaCard(
            estado = ContaUiState(
                configurado = true,
                sessao = Sessao("a", "r", 0, "u1", "ana@exemplo.com"),
                perfil = Perfil("u1", "ana@exemplo.com", "Ana Nadadora", 25, TrainingLevel.AVANCADO)
            ),
            onSalvar = { _, _, _ -> }, onSair = {}, onExcluirConta = {}
        )
    }
}
