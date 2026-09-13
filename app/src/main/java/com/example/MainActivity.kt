package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthRepository
import com.example.model.AppNavTab
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ContaCard
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EditorDeTreinoScreen
import com.example.ui.screens.HomeScreen
import androidx.compose.ui.platform.LocalContext
import com.example.ui.compartilhar.compartilharTreino
import com.example.ui.screens.ExecucaoDeTreinoScreen
import com.example.viewmodel.ExecucaoViewModel
import com.example.ui.screens.MeusTreinosScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecuperarSenhaScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AquagendaViewModel
import com.example.viewmodel.ContaUiState
import com.example.viewmodel.ContaViewModel
import com.example.viewmodel.MeusTreinosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Antes de qualquer chamada ao Supabase: é daqui que sai o token do usuário.
        AuthRepository.init(applicationContext)
        // Ícones escuros na barra de status sempre: o app é claro mesmo com o celular no modo escuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                AquagendaRaiz()
            }
        }
    }
}

/** Sem sessão, só a tela de login/cadastro. */
@Composable
fun AquagendaRaiz(
    conta: ContaViewModel = viewModel()
) {
    val estadoConta by conta.ui.collectAsStateWithLifecycle()

    val recuperacao = estadoConta.recuperacao

    if (estadoConta.sessao == null && recuperacao != null) {
        RecuperarSenhaScreen(
            estado = recuperacao,
            configurado = estadoConta.configurado,
            onEnviarCodigo = { conta.enviarCodigo(it) },
            onRedefinir = conta::redefinirSenha,
            onTrocarEmail = conta::trocarEmailDaRecuperacao,
            onVoltar = conta::fecharRecuperacao
        )
    } else if (estadoConta.sessao == null) {
        AuthScreen(
            estado = estadoConta,
            onEntrar = conta::entrar,
            onCadastrar = conta::cadastrar,
            onLimparMensagens = conta::limparMensagens,
            onEsqueciSenha = conta::abrirRecuperacao
        )
    } else {
        AquagendaApp(conta = conta, estadoConta = estadoConta)
    }
}

@Composable
fun AquagendaApp(
    conta: ContaViewModel,
    estadoConta: ContaUiState,
    viewModel: AquagendaViewModel = viewModel(),
    meusTreinos: MeusTreinosViewModel = viewModel(),
    execucao: ExecucaoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val meus by meusTreinos.ui.collectAsStateWithLifecycle()
    val estadoExecucao by execucao.ui.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(estadoConta.sessao?.userId) {
        meusTreinos.definirUsuario(estadoConta.sessao?.userId)
    }

    // O nível do perfil vira o nível da sugestão do dia.
    LaunchedEffect(estadoConta.perfil?.nivel) {
        estadoConta.perfil?.nivel?.let { viewModel.selectLevel(it) }
    }

    // Show user notification snackbar when needed
    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    LaunchedEffect(meus.mensagem) {
        meus.mensagem?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            meusTreinos.mensagemMostrada()
        }
    }

    val editor = meus.editor

    if (estadoExecucao.ativo) {
        // Execução ao vivo do treino escolhido -> concluir -> publicar
        ExecucaoDeTreinoScreen(
            estado = estadoExecucao,
            onAlternarPausa = execucao::alternarPausa,
            onAvancar = execucao::avancar,
            onVoltar = execucao::voltar,
            onConcluir = execucao::irParaResumo,
            onSairSemSalvar = execucao::encerrar,
            onVoltarAoTreino = execucao::voltarAoTreino,
            onIntensidade = execucao::definirIntensidade,
            onComplexidade = execucao::definirComplexidade,
            onObservacao = execucao::definirObservacao,
            onSalvar = { execucao.salvar() },
            onCompartilhar = { formato ->
                estadoExecucao.resumo?.let { compartilharTreino(contexto, it, formato) }
            },
            onFechar = execucao::encerrar
        )
    } else if (editor != null) {
        EditorDeTreinoScreen(
            editor = editor,
            onAlterar = meusTreinos::alterarDigitado,
            onSalvar = meusTreinos::salvar,
            onCancelar = meusTreinos::fecharEditor
        )
    } else {
        // Main App Layout with TopBar, Content and BottomNav
        Scaffold(
            topBar = {
                AppTopBar(
                    isExecutionMode = false,
                    showUserAvatar = uiState.selectedTab == AppNavTab.PROFILE,
                    onNotificationClick = {
                        viewModel.selectToday()
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = AquaBackground,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        AppNavTab.HOME -> {
                            HomeScreen(
                                workout = uiState.currentWorkout,
                                selectedLevel = uiState.selectedLevel,
                                calendarDays = uiState.calendarDays,
                                stopwatchState = uiState.stopwatch,
                                onDayClick = { viewModel.selectDay(it) },
                                onLevelChange = { viewModel.selectLevel(it) },
                                onStartWorkoutClick = { execucao.iniciar(uiState.currentWorkout) },
                                onViewWorkoutDetails = { viewModel.selectTab(AppNavTab.WORKOUTS) },
                                onToggleStopwatch = { viewModel.toggleStopwatch() },
                                onLapStopwatch = { viewModel.recordSetLap() },
                                onResetStopwatch = { viewModel.resetStopwatch() },
                                onStopwatchModeChange = { viewModel.setStopwatchMode(it) },
                                onStopwatchPrevSet = { viewModel.decrementStopwatchSet() },
                                onStopwatchNextSet = { viewModel.incrementStopwatchSet() }
                            )
                        }

                        AppNavTab.WORKOUTS -> {
                            WorkoutsScreen(
                                workout = uiState.currentWorkout,
                                onStartWorkoutClick = { execucao.iniciar(uiState.currentWorkout) },
                                onSaveToMyWorkouts = { meusTreinos.salvarSugestao(uiState.currentWorkout) }
                            )
                        }

                        AppNavTab.MY_WORKOUTS -> {
                            MeusTreinosScreen(
                                estado = meus,
                                onNovo = { meusTreinos.novo(uiState.selectedEpochDay, uiState.selectedLevel) },
                                onEditar = meusTreinos::editar,
                                onUsar = viewModel::usarTreino,
                                onExcluir = meusTreinos::pedirExclusao,
                                onConfirmarExclusao = meusTreinos::confirmarExclusao,
                                onCancelarExclusao = meusTreinos::cancelarExclusao,
                                onTentarDeNovo = meusTreinos::carregar
                            )
                        }

                        AppNavTab.PROFILE -> {
                            ProfileScreen(
                                onDownloadWorkoutClick = { viewModel.downloadWorkout() },
                                cabecalho = {
                                    ContaCard(
                                        estado = estadoConta,
                                        onSalvar = conta::salvarPerfil,
                                        onSair = conta::sair,
                                        onExcluirConta = conta::excluirConta
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
