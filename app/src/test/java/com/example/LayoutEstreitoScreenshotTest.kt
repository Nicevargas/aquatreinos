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
import com.example.data.progresso.Atividade
import com.example.data.progresso.Progresso
import com.example.model.TrainingLevel
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ContaCard
import com.example.ui.components.ParQCard
import com.example.ui.screens.ParQScreen
import com.example.viewmodel.ParQUiState
import com.example.data.ranking.LinhaDoRanking
import com.example.data.ranking.ParticipacaoDto
import com.example.ui.screens.RankingScreen
import com.example.viewmodel.FiltrosDoRanking
import com.example.viewmodel.FormularioDoRanking
import com.example.viewmodel.RankingUiState
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.example.data.plano.ConfigDoPlano
import com.example.data.plano.PlanoDeTreino
import com.example.data.plano.PlanoDto
import com.example.ui.screens.PlanoDeTreinoScreen
import com.example.ui.screens.SemPlanoDeTreino
import com.example.viewmodel.PlanoUiState
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

    private val ciclo = CicloDeTreinos.deJson(File("src/main/assets/programa_nc.json").readText())
    private val dia = DataCivil.deIso("2026-10-01")
    private val intermediario = ciclo.sugestao(dia, TrainingLevel.INTERMEDIARIO)!!
    private val roteiro = RoteiroDeTreino(intermediario)

    // Histórico de exemplo: série de 3 semanas, prêmios e notas de esforço.
    private val painel = Progresso.painel(
        listOf(
            Atividade("a1", DataCivil.deIso("2026-09-15"), "Técnica", "Técnica", 2000, 2940, true, true, 6),
            Atividade("a2", DataCivil.deIso("2026-09-17"), "Resistência", "Resistência", 2300, 3300, true, false, 8),
            Atividade("a3", DataCivil.deIso("2026-09-22"), "Velocidade", "Velocidade", 1890, 2700, true, true, 9),
            Atividade("a4", DataCivil.deIso("2026-09-29"), "Força específica", "Força específica", 1500, 2400, false, false, 4)
        ),
        dia
    )

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
                calendarDays = WorkoutRepository.diasDoCalendario(DataCivil.deIso("2026-10-03"), dia),
                onDayClick = {}, onLevelChange = {}, onStartWorkoutClick = {}, onViewWorkoutDetails = {},
                progresso = painel
            )
        }
    }

    @Test
    fun treinos() = capturar("treinos.png") {
        WorkoutsScreen(
            workout = intermediario.copy(tag = "Recebido de Ana", salvarAoConcluir = true),
            onStartWorkoutClick = {}, onSaveToMyWorkouts = {}, onEditar = {}, onCompartilhar = {}
        )
    }

    @Test
    fun editor_ajustar_para_nadar() = capturar("editor_ajustar.png") {
        EditorDeTreinoScreen(
            editor = EditorDeTreino(
                digitado = MontadorDeTreino.paraDigitacao(intermediario, dia),
                paraNadar = true,
                origem = intermediario
            ),
            onAlterar = {}, onSalvar = {}, onCancelar = {}, onUsarParaNadar = {}
        )
    }

    @Test
    fun meus_treinos_vazio_com_codigo() = capturar("meus_treinos_codigo.png") {
        MeusTreinosScreen(
            estado = MeusTreinosUiState(),
            onNovo = {}, onEditar = {}, onUsar = {}, onExcluir = {},
            onConfirmarExclusao = {}, onCancelarExclusao = {}, onTentarDeNovo = {},
            onAbrirCodigo = {}
        )
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
            ativo = true, roteiro = roteiro, progresso = ProgressoExecucao(roteiro.passos.size, 0), etapa = EtapaExecucao.PUBLICAR,
            resumo = RegistroDoTreino.resumo(
                RegistroDoTreino.montar(roteiro, ProgressoExecucao(roteiro.passos.size, 0), 2_520, 6, 4, "", "2026-09-13"),
                roteiro
            )
        ),
        "execucao_publicar.png"
    )

    @Test
    fun parq_com_sim() = capturar("parq_com_sim.png") {
        ParQScreen(
            estado = ParQUiState(
                aberto = true, vaiTreinar = true,
                respostas = listOf(false, true, false, false, false, false, null),
                termo = true
            ),
            onResponder = { _, _ -> }, onDeclaracao = {}, onTermo = {}, onEnviar = {}, onFechar = {}
        )
    }

    @Test
    fun parq_no_perfil() = capturar("parq_perfil.png") {
        ParQCard(ultimoDia = DataCivil.deIso("2026-09-15"), onResponder = {}, hoje = dia)
    }

    @Test
    fun plano_na_aba() = capturar("plano_aba.png") {
        val config = ConfigDoPlano(6, 3, listOf("Resistência"), true, true, TrainingLevel.INICIANTE, mapOf("1-2" to PlanoDeTreino.trocaPorFoco("Estilos")))
        val treinosNC = CicloDeTreinos.treinosDoJson(File("src/main/assets/programa_nc.json").readText())
        Column {
            PlanoDeTreinoScreen(
                estado = PlanoUiState(
                    plano = PlanoDto.de(config, manual = true).copy(id = "p1", trocas = config.trocas),
                    semanas = PlanoDeTreino.montar(config, treinosNC, "p1"),
                    feitos = setOf(1 to 1)
                ),
                emAba = true,
                onVoltar = {}, onTreino = {}, onNovoPlano = {}, onExcluir = {},
                onConfirmarExclusao = {}, onCancelarExclusao = {}, onTentarDeNovo = {},
                modifier = Modifier.height(1900.dp)
            )
            BottomNavBar(selectedTab = AppNavTab.PLAN, onTabSelected = {})
        }
    }

    @Test
    fun sem_plano() = capturar("sem_plano.png") {
        SemPlanoDeTreino(carregando = false, erro = null, onComecar = {}, onTentarDeNovo = {})
    }

    @Test
    fun ranking() = capturar("ranking.png") {
        RankingScreen(
            estado = RankingUiState(
                aberto = true,
                participacao = ParticipacaoDto(publico = true, nome = "Ana S.", anoNascimento = 1990, sexo = "F", cidade = "São Paulo", local = "Clube Anhembi"),
                filtros = FiltrosDoRanking(faixa = "30-39", soMinhaCidade = true),
                linhas = listOf(
                    LinhaDoRanking(1, "Mariana Albuquerque C.", 1240, 98_500, 42, 12, false),
                    LinhaDoRanking(2, "Ana S.", 865, 64_200, 30, 10, true),
                    LinhaDoRanking(3, "Rafa T.", 610, 45_000, 21, 8, false),
                    LinhaDoRanking(4, "João P.", 120, 8_000, 4, 2, false)
                )
            ),
            onVoltar = {}, onFiltrar = {}, onParticipar = {}, onAlterarFormulario = {},
            onSalvarFormulario = {}, onCancelarFormulario = {}, onSairDoRanking = {}, onTentarDeNovo = {}
        )
    }

    @Test
    fun ranking_participar() = capturar("ranking_participar.png") {
        RankingScreen(
            estado = RankingUiState(
                aberto = true,
                formulario = FormularioDoRanking(nome = "Ana S.", ano = "1990", sexo = "F", cidade = "São Paulo", erro = "Marque que você aceita aparecer no ranking.")
            ),
            onVoltar = {}, onFiltrar = {}, onParticipar = {}, onAlterarFormulario = {},
            onSalvarFormulario = {}, onCancelarFormulario = {}, onSairDoRanking = {}, onTentarDeNovo = {}
        )
    }

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
            progresso = painel,
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
