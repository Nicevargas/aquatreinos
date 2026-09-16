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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import com.example.ui.components.ParQCard
import com.example.ui.screens.ParQScreen
import com.example.viewmodel.ParQViewModel
import com.example.viewmodel.PlanoViewModel
import com.example.viewmodel.ProgressoViewModel
import com.example.viewmodel.RankingViewModel
import com.example.ui.screens.RankingScreen
import com.example.data.lembrete.LembreteDeTreino
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.screens.CriarPlanoScreen
import com.example.ui.screens.PlanoDeTreinoScreen
import com.example.ui.screens.SemPlanoDeTreino
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
import com.example.ui.compartilhar.compartilharTreinoParaFazer
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
import android.content.Intent
import com.example.data.compartilhar.LinkDeTreino
import com.example.viewmodel.CompartilharTreinoViewModel

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
        // Link de treino compartilhado (natacaocriativa://treino/<código>) que abriu o app.
        LinkDeTreino.receber(intent)
        setContent {
            MyApplicationTheme {
                AquagendaRaiz()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LinkDeTreino.receber(intent)
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
    execucao: ExecucaoViewModel = viewModel(),
    parq: ParQViewModel = viewModel(),
    plano: PlanoViewModel = viewModel(),
    progresso: ProgressoViewModel = viewModel(),
    ranking: RankingViewModel = viewModel(),
    compartilhar: CompartilharTreinoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val meus by meusTreinos.ui.collectAsStateWithLifecycle()
    val estadoExecucao by execucao.ui.collectAsStateWithLifecycle()
    val estadoParQ by parq.ui.collectAsStateWithLifecycle()
    val estadoPlano by plano.ui.collectAsStateWithLifecycle()
    val estadoProgresso by progresso.ui.collectAsStateWithLifecycle()
    val estadoRanking by ranking.ui.collectAsStateWithLifecycle()
    val estadoCompartilhar by compartilhar.ui.collectAsStateWithLifecycle()
    val codigoRecebido by LinkDeTreino.pendente.collectAsStateWithLifecycle()
    val nivelDoPlano = estadoConta.perfil?.nivel ?: uiState.selectedLevel
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // "Quando você vai voltar a nadar?": no Android 13+ o aviso precisa da permissão de notificação.
    val pedirPermissaoDeAviso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val agendarLembrete: (Long) -> Unit = { dia ->
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermissaoDeAviso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        LembreteDeTreino.agendar(contexto, dia)
    }

    LaunchedEffect(estadoConta.sessao?.userId) {
        meusTreinos.definirUsuario(estadoConta.sessao?.userId)
        parq.definirUsuario(estadoConta.sessao?.userId)
        plano.definirUsuario(estadoConta.sessao?.userId)
        progresso.definirUsuario(estadoConta.sessao?.userId)
        ranking.definirUsuario(estadoConta.sessao?.userId)
    }

    LaunchedEffect(estadoRanking.mensagem) {
        estadoRanking.mensagem?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            ranking.mensagemMostrada()
        }
    }

    // Treino salvo e execução fechada: o plano marca o que foi feito, o progresso se atualiza
    // e Meus treinos mostra o treino ajustado ou recebido que acabou de ser salvo.
    LaunchedEffect(estadoExecucao.ativo) {
        if (!estadoExecucao.ativo) {
            plano.atualizarFeitos()
            progresso.carregar()
            meusTreinos.carregar()
        }
    }

    // Link de treino que abriu o app: abre depois do login (esta tela só existe logado).
    LaunchedEffect(codigoRecebido) {
        codigoRecebido?.let {
            LinkDeTreino.consumido()
            compartilhar.abrir(it)
        }
    }

    // Treino recebido ou ajustado vira o treino da vez, na aba Treinos.
    LaunchedEffect(estadoCompartilhar.treinoRecebido) {
        estadoCompartilhar.treinoRecebido?.let {
            viewModel.usarTreino(it)
            viewModel.selectTab(AppNavTab.WORKOUTS)
            compartilhar.treinoRecebidoUsado()
        }
    }
    LaunchedEffect(meus.treinoAjustado) {
        meus.treinoAjustado?.let {
            viewModel.usarTreino(it)
            viewModel.selectTab(AppNavTab.WORKOUTS)
            meusTreinos.treinoAjustadoUsado()
        }
    }

    // Treino pronto: abre a janela de compartilhar do Android (WhatsApp, redes...) com imagem e link.
    LaunchedEffect(estadoCompartilhar.envio) {
        estadoCompartilhar.envio?.let { envio ->
            compartilharTreinoParaFazer(contexto, envio.treino, envio.codigo, envio.mensagem)
            compartilhar.textoEnviado()
        }
    }

    LaunchedEffect(estadoCompartilhar.mensagem) {
        estadoCompartilhar.mensagem?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            compartilhar.mensagemMostrada()
        }
    }

    LaunchedEffect(estadoPlano.mensagem) {
        estadoPlano.mensagem?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            plano.mensagemMostrada()
        }
    }

    // "Ver meu plano" ou plano recém-criado: o plano abre na aba Plano.
    LaunchedEffect(estadoPlano.aberto) {
        if (estadoPlano.aberto) {
            plano.fechar()
            viewModel.selectTab(AppNavTab.PLAN)
        }
    }

    // Meus treinos podem entrar no plano no lugar de um treino do Método NC.
    LaunchedEffect(meus.treinos) {
        plano.definirMeusTreinos(meus.treinos)
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

    if (estadoParQ.aberto) {
        // PAR-Q antes de treinar (ou "Minha saúde mudou", pelo Perfil).
        ParQScreen(
            estado = estadoParQ,
            onResponder = parq::responder,
            onDeclaracao = parq::marcarDeclaracao,
            onTermo = parq::marcarTermo,
            onEnviar = parq::enviar,
            onFechar = parq::fechar
        )
    } else if (estadoExecucao.ativo) {
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
            onFechar = execucao::encerrar,
            onLembrete = agendarLembrete
        )
    } else if (estadoRanking.aberto) {
        RankingScreen(
            estado = estadoRanking,
            onVoltar = ranking::fechar,
            onFiltrar = ranking::filtrar,
            onParticipar = { ranking.editarParticipacao(estadoConta.perfil?.nome.orEmpty()) },
            onAlterarFormulario = ranking::alterarFormulario,
            onSalvarFormulario = ranking::salvarParticipacao,
            onCancelarFormulario = ranking::cancelarFormulario,
            onSairDoRanking = ranking::sairDoRanking,
            onTentarDeNovo = ranking::carregar
        )
    } else if (estadoPlano.assistente != null) {
        CriarPlanoScreen(
            assistente = estadoPlano.assistente!!,
            onAlterar = plano::alterarAssistente,
            onProximo = plano::avancar,
            onVoltar = plano::voltarEtapa,
            onFechar = plano::fecharAssistente
        )
    } else if (editor != null) {
        EditorDeTreinoScreen(
            editor = editor,
            onAlterar = meusTreinos::alterarDigitado,
            onSalvar = meusTreinos::salvar,
            onCancelar = meusTreinos::fecharEditor,
            onUsarParaNadar = meusTreinos::usarAjuste
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
                                onDayClick = { viewModel.selectDay(it) },
                                onLevelChange = { viewModel.selectLevel(it) },
                                onStartWorkoutClick = { parq.antesDeTreinar { execucao.iniciar(uiState.currentWorkout) } },
                                onViewWorkoutDetails = { viewModel.selectTab(AppNavTab.WORKOUTS) },
                                plano = estadoPlano.resumo,
                                onPlanoClick = { plano.abrir(nivelDoPlano) },
                                progresso = estadoProgresso.painel,
                                onVerProgresso = { viewModel.selectTab(AppNavTab.PROFILE) }
                            )
                        }

                        AppNavTab.PLAN -> {
                            if (estadoPlano.plano != null) {
                                PlanoDeTreinoScreen(
                                    estado = estadoPlano,
                                    emAba = true,
                                    onVoltar = { viewModel.selectTab(AppNavTab.HOME) },
                                    onTreino = { treino ->
                                        // O treino do plano vira o treino da vez, com os detalhes e o "Iniciar".
                                        viewModel.usarTreino(treino.workout)
                                        viewModel.selectTab(AppNavTab.WORKOUTS)
                                    },
                                    onNovoPlano = { plano.iniciarAssistente(nivelDoPlano) },
                                    onExcluir = plano::pedirExclusao,
                                    onConfirmarExclusao = plano::confirmarExclusao,
                                    onCancelarExclusao = plano::cancelarExclusao,
                                    onTentarDeNovo = plano::carregar,
                                    onTrocar = plano::abrirTroca,
                                    onEscolherTroca = plano::trocar,
                                    onFecharTroca = plano::fecharTroca
                                )
                            } else {
                                SemPlanoDeTreino(
                                    carregando = estadoPlano.carregando,
                                    erro = estadoPlano.erro,
                                    onComecar = { plano.iniciarAssistente(nivelDoPlano) },
                                    onTentarDeNovo = plano::carregar
                                )
                            }
                        }

                        AppNavTab.WORKOUTS -> {
                            WorkoutsScreen(
                                workout = uiState.currentWorkout,
                                onStartWorkoutClick = { parq.antesDeTreinar { execucao.iniciar(uiState.currentWorkout) } },
                                onSaveToMyWorkouts = { meusTreinos.salvarSugestao(uiState.currentWorkout) },
                                onEditar = { meusTreinos.ajustarParaNadar(uiState.currentWorkout) },
                                onCompartilhar = { compartilhar.compartilhar(uiState.currentWorkout) },
                                compartilhando = estadoCompartilhar.gerando
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
                                onTentarDeNovo = meusTreinos::carregar,
                                onAbrirCodigo = compartilhar::abrir,
                                abrindoCodigo = estadoCompartilhar.abrindo
                            )
                        }

                        AppNavTab.PROFILE -> {
                            ProfileScreen(
                                progresso = estadoProgresso.painel,
                                carregando = estadoProgresso.carregando,
                                erro = estadoProgresso.erro,
                                onTentarDeNovo = progresso::carregar,
                                onAbrirRanking = ranking::abrir,
                                cabecalho = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ContaCard(
                                            estado = estadoConta,
                                            onSalvar = conta::salvarPerfil,
                                            onSair = conta::sair,
                                            onExcluirConta = conta::excluirConta
                                        )
                                        ParQCard(
                                            ultimoDia = estadoParQ.ultimoDia,
                                            onResponder = parq::responderDeNovo
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
