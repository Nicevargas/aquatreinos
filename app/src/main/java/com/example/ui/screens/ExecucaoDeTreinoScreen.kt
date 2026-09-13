package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.compartilhar.FormatoDoCartao
import com.example.data.compartilhar.ResumoDoTreino
import com.example.data.compartilhar.TextosDoTreino
import com.example.data.execucao.PassoDeTreino
import com.example.data.execucao.ProgressoExecucao
import com.example.data.execucao.RoteiroDeTreino
import com.example.data.execucao.SituacaoDaFase
import com.example.ui.compartilhar.CartaoDoTreino
import com.example.ui.components.AppTopBar
import com.example.ui.components.MensagemDeTela
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainer
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.EtapaExecucao
import com.example.viewmodel.ExecucaoUiState
import java.util.Locale
import kotlin.math.roundToInt

private fun tempoFormatado(segundos: Long): String =
    String.format(Locale.US, "%02d:%02d:%02d", segundos / 3600, (segundos % 3600) / 60, segundos % 60)

/** Execução ao vivo do treino escolhido -> resumo com notas -> publicar nas redes. */
@Composable
fun ExecucaoDeTreinoScreen(
    estado: ExecucaoUiState,
    onAlternarPausa: () -> Unit,
    onAvancar: () -> Unit,
    onVoltar: () -> Unit,
    onConcluir: () -> Unit,
    onSairSemSalvar: () -> Unit,
    onVoltarAoTreino: () -> Unit,
    onIntensidade: (Int) -> Unit,
    onComplexidade: (Int) -> Unit,
    onObservacao: (String) -> Unit,
    onSalvar: () -> Unit,
    onCompartilhar: (FormatoDoCartao) -> Unit,
    onFechar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roteiro = estado.roteiro ?: return

    // Na piscina, com a mão molhada, a tela não pode apagar no meio da série.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    when (estado.etapa) {
        EtapaExecucao.EXECUTANDO -> TelaExecutando(
            estado, roteiro, onAlternarPausa, onAvancar, onVoltar, onConcluir, onSairSemSalvar, modifier
        )
        EtapaExecucao.RESUMO -> TelaResumo(
            estado, roteiro, onIntensidade, onComplexidade, onObservacao, onSalvar, onVoltarAoTreino, modifier
        )
        EtapaExecucao.PUBLICAR -> estado.resumo?.let { TelaPublicar(it, onCompartilhar, onFechar, modifier) }
    }
}

// ------------------------------------------------------------------ executando

@Composable
private fun TelaExecutando(
    estado: ExecucaoUiState,
    roteiro: RoteiroDeTreino,
    onAlternarPausa: () -> Unit,
    onAvancar: () -> Unit,
    onVoltar: () -> Unit,
    onConcluir: () -> Unit,
    onSairSemSalvar: () -> Unit,
    modifier: Modifier
) {
    var confirmarSaida by remember { mutableStateOf(false) }
    BackHandler { confirmarSaida = true }

    val progresso = estado.progresso
    val metrosFeitos = roteiro.metrosFeitos(progresso)
    val fracao = if (roteiro.metrosTotais > 0) metrosFeitos.toFloat() / roteiro.metrosTotais else 0f
    val terminou = roteiro.terminou(progresso)

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair do treino?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (metrosFeitos > 0) {
                        "Você nadou ${TextosDoTreino.metros(metrosFeitos)} de ${TextosDoTreino.metros(roteiro.metrosTotais)}. Dá para salvar o que já fez."
                    } else {
                        "Nenhuma série foi marcada como feita ainda."
                    }
                )
            },
            confirmButton = {
                if (metrosFeitos > 0) {
                    TextButton(
                        onClick = { confirmarSaida = false; onConcluir() },
                        modifier = Modifier.testTag("sair_salvando")
                    ) { Text("Salvar o que fiz", color = AquaPrimary, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = { confirmarSaida = false }) {
                        Text("Continuar treinando", color = AquaPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmarSaida = false; onSairSemSalvar() },
                    modifier = Modifier.testTag("sair_sem_salvar")
                ) { Text("Sair sem salvar", color = AquaMagenta) }
            }
        )
    }

    Scaffold(
        topBar = { AppTopBar(isExecutionMode = true, onCloseClick = { confirmarSaida = true }) },
        bottomBar = {
            BarraDeExecucao(
                rodando = estado.rodando,
                terminou = terminou,
                repeticoes = roteiro.passoAtual(progresso)?.repeticoes ?: 1,
                podeVoltar = progresso != ProgressoExecucao(),
                onVoltar = onVoltar,
                onAlternarPausa = onAlternarPausa,
                onAvancar = onAvancar,
                onConcluir = onConcluir
            )
        },
        containerColor = Color(0xFFF9F9FF),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            CabecalhoDeTempo(
                tempo = tempoFormatado(estado.decorridoSegundos),
                rodando = estado.rodando,
                fracao = fracao,
                metrosFeitos = metrosFeitos,
                metrosTotais = roteiro.metrosTotais
            )

            Spacer(modifier = Modifier.height(20.dp))

            val atual = roteiro.passoAtual(progresso)
            if (terminou || atual == null) {
                CartaoTreinoCompleto(metrosFeitos)
            } else {
                CartaoSerieAtual(atual, progresso, roteiro)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Roteiro do treino", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            LinhaDoTempo(roteiro, progresso)

            Spacer(modifier = Modifier.height(24.dp))

            FaixaDeDica(
                titulo = roteiro.workout.focus?.let { "Foco de hoje: $it" } ?: "Dica do treino",
                dica = roteiro.workout.motivationalTip
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CabecalhoDeTempo(
    tempo: String,
    rodando: Boolean,
    fracao: Float,
    metrosFeitos: Int,
    metrosTotais: Int
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (rodando) AquaMagenta else AquaTextMuted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (rodando) "TEMPO DE TREINO" else "PAUSADO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (rodando) AquaMagenta else AquaTextMuted,
                letterSpacing = 1.2.sp
            )
        }

        Text(
            text = tempo,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = AquaPrimary,
            letterSpacing = (-1).sp,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("tempo_de_treino")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFDEE8FF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fracao.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(AquaPrimary, AquaCyan, AquaMagenta)))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${TextosDoTreino.metros(metrosFeitos)} de ${TextosDoTreino.metros(metrosTotais)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AquaPrimary
            )
            Text(
                text = "${(fracao * 100).toInt()}% do treino",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AquaMagenta
            )
        }
    }
}

@Composable
private fun CartaoSerieAtual(passo: PassoDeTreino, progresso: ProgressoExecucao, roteiro: RoteiroDeTreino) {
    val proximo = roteiro.proximoPasso(progresso)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = AquaPrimary.copy(alpha = 0.15f))
            .testTag("serie_atual"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, AquaPrimary.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Brush.horizontalGradient(listOf(AquaPrimary, AquaCyan, AquaMagenta)))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${passo.fase.uppercase()} · SÉRIE ${progresso.passo + 1} DE ${roteiro.seriesTotais}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaMagenta,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = passo.cabecalho,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = AquaPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                passo.serie.details.forEach { detalhe ->
                    Text(
                        text = "• $detalhe",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AquaTextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                val etiquetas: List<Pair<ImageVector, String>> = listOfNotNull(
                    passo.serie.intervalTarget.takeIf { it.isNotBlank() }?.let { Icons.Outlined.Timer to "Intervalo $it" },
                    passo.serie.equipmentName?.let { Icons.Outlined.FrontHand to it }
                )
                if (etiquetas.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        etiquetas.forEach { (icone, texto) ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(AquaBlueBg)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icone, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (passo.repeticoes > 1) {
                    Text(
                        text = "Repetição ${progresso.repeticoesFeitas + 1} de ${passo.repeticoes}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextPrimary,
                        modifier = Modifier.testTag("contador_repeticoes")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ContadorDeRepeticoes(total = passo.repeticoes, feitas = progresso.repeticoesFeitas)
                } else {
                    Text(
                        text = "Série única de ${TextosDoTreino.metros(passo.metros)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextPrimary
                    )
                }

                proximo?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = AquaBorder)
                    Text(
                        text = "Depois: ${it.cabecalho}",
                        fontSize = 13.sp,
                        color = AquaTextSecondary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContadorDeRepeticoes(total: Int, feitas: Int) {
    if (total > 20) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(AquaBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(feitas.toFloat() / total)
                    .height(10.dp)
                    .background(AquaPrimary)
            )
        }
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when {
                            i < feitas -> AquaPrimary
                            i == feitas -> AquaMagenta
                            else -> AquaBorder
                        }
                    )
            )
        }
    }
}

@Composable
private fun CartaoTreinoCompleto(metrosFeitos: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("treino_completo"),
        shape = RoundedCornerShape(20.dp),
        color = AquaGreenBg,
        border = androidx.compose.foundation.BorderStroke(2.dp, AquaGreen.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AquaGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Treino completo!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AquaGreenText)
                Text(
                    text = "Você nadou ${TextosDoTreino.metros(metrosFeitos)}. Toque em Concluir treino para registrar e dar suas notas.",
                    fontSize = 13.sp,
                    color = AquaTextPrimary
                )
            }
        }
    }
}

@Composable
private fun LinhaDoTempo(roteiro: RoteiroDeTreino, progresso: ProgressoExecucao) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        roteiro.workout.phases.forEachIndexed { indiceFase, fase ->
            val passosDaFase = roteiro.passos.withIndex().filter { it.value.indiceFase == indiceFase }
            if (passosDaFase.isEmpty()) return@forEachIndexed
            val situacao = roteiro.situacaoDaFase(indiceFase, progresso)

            ItemDaLinhaDoTempo(
                titulo = "${fase.title} · ${TextosDoTreino.metros(passosDaFase.sumOf { it.value.metros })}",
                rotulo = when (situacao) {
                    SituacaoDaFase.CONCLUIDA -> "CONCLUÍDA"
                    SituacaoDaFase.ATUAL -> "AGORA"
                    SituacaoDaFase.PENDENTE -> "DEPOIS"
                },
                situacao = situacao
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    passosDaFase.forEach { (indice, passo) ->
                        val doPasso = roteiro.situacaoDoPasso(indice, progresso)
                        Text(
                            text = if (doPasso == SituacaoDaFase.ATUAL && passo.repeticoes > 1) {
                                "${passo.cabecalho} · ${progresso.repeticoesFeitas}/${passo.repeticoes}"
                            } else {
                                passo.cabecalho
                            },
                            fontSize = 14.sp,
                            fontWeight = if (doPasso == SituacaoDaFase.ATUAL) FontWeight.Bold else FontWeight.Medium,
                            color = when (doPasso) {
                                SituacaoDaFase.CONCLUIDA -> AquaTextSecondary
                                SituacaoDaFase.ATUAL -> AquaPrimary
                                SituacaoDaFase.PENDENTE -> AquaTextMuted
                            },
                            textDecoration = if (doPasso == SituacaoDaFase.CONCLUIDA) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDaLinhaDoTempo(
    titulo: String,
    rotulo: String,
    situacao: SituacaoDaFase,
    conteudo: @Composable () -> Unit
) {
    val atual = situacao == SituacaoDaFase.ATUAL
    val concluida = situacao == SituacaoDaFase.CONCLUIDA
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .then(
                    when {
                        atual -> Modifier.background(Brush.linearGradient(listOf(AquaPrimary, AquaCyan)))
                        concluida -> Modifier
                            .background(Color.White)
                            .border(2.dp, Color(0xFF34D399), CircleShape)
                        else -> Modifier
                            .background(AquaSurfaceContainer)
                            .border(2.dp, AquaBorder, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                atual -> Icon(Icons.Filled.Pool, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                concluida -> Icon(Icons.Filled.Check, contentDescription = "Concluída", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                else -> Icon(Icons.Filled.Flag, contentDescription = null, tint = AquaTextMuted, modifier = Modifier.size(18.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    fontSize = if (atual) 17.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        atual -> AquaPrimary
                        concluida -> Color(0xFF065F46)
                        else -> AquaTextMuted
                    }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            when {
                                atual -> AquaMagenta
                                concluida -> Color(0xFFD1FAE5)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = rotulo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            atual -> Color.White
                            concluida -> Color(0xFF047857)
                            else -> AquaTextMuted
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            conteudo()
        }
    }
}

@Composable
private fun FaixaDeDica(titulo: String, dica: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(AquaPrimary, AquaCyan)))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = dica,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraDeExecucao(
    rodando: Boolean,
    terminou: Boolean,
    repeticoes: Int,
    podeVoltar: Boolean,
    onVoltar: () -> Unit,
    onAlternarPausa: () -> Unit,
    onAvancar: () -> Unit,
    onConcluir: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onVoltar,
                enabled = podeVoltar,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AquaSurfaceContainerLow)
                    .testTag("desfazer_toque")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Desfazer o último toque",
                    tint = if (podeVoltar) AquaTextPrimary else AquaTextMuted
                )
            }

            OutlinedButton(
                onClick = onAlternarPausa,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("pausar_retomar"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = AquaSurfaceContainerLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (rodando) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AquaMagenta,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (rodando) "Pausar" else "Retomar", color = AquaTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = if (terminou) onConcluir else onAvancar,
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
                    .testTag(if (terminou) "concluir_treino" else "marcar_feito"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (terminou) {
                                Brush.horizontalGradient(listOf(AquaGreen, Color(0xFF2E7D32)))
                            } else {
                                Brush.horizontalGradient(listOf(AquaPrimary, Color(0xFF0070E6), AquaMagenta))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                terminou -> "Concluir treino"
                                repeticoes > 1 -> "Repetição feita"
                                else -> "Série feita"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (terminou) Icons.Filled.Check else Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ resumo

@Composable
private fun TelaResumo(
    estado: ExecucaoUiState,
    roteiro: RoteiroDeTreino,
    onIntensidade: (Int) -> Unit,
    onComplexidade: (Int) -> Unit,
    onObservacao: (String) -> Unit,
    onSalvar: () -> Unit,
    onVoltarAoTreino: () -> Unit,
    modifier: Modifier
) {
    BackHandler(enabled = !estado.salvando, onBack = onVoltarAoTreino)
    val progresso = estado.progresso
    val metros = roteiro.metrosFeitos(progresso)
    val completo = roteiro.terminou(progresso)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Como foi o treino?", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AquaPrimary)
        Text(
            text = "${roteiro.workout.title} · ${TextosDoTreino.nivel(roteiro.workout.level)}",
            fontSize = 14.sp,
            color = AquaTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BlocoDeNumero("Metros", TextosDoTreino.metros(metros), "de ${TextosDoTreino.metros(roteiro.metrosTotais)}", Modifier.weight(1f))
            BlocoDeNumero("Tempo", TextosDoTreino.duracao(estado.decorridoSegundos), tempoFormatado(estado.decorridoSegundos), Modifier.weight(1f))
            BlocoDeNumero(
                "Séries",
                "${roteiro.seriesFeitas(progresso)}/${roteiro.seriesTotais}",
                if (completo) "completo" else "parcial",
                Modifier.weight(1f)
            )
        }

        if (!completo && metros > 0) {
            MensagemDeTela(
                texto = "Você fez parte do treino, e tudo bem: fica registrado o que você nadou.",
                erro = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        NotaDeZeroADez(
            titulo = "Intensidade",
            descricao = "Quanto o treino puxou do seu corpo.",
            valor = estado.intensidade,
            onValor = onIntensidade,
            minimo = "0 · muito leve",
            maximo = "10 · no limite",
            tag = "nota_intensidade",
            habilitado = !estado.salvando
        )

        Spacer(modifier = Modifier.height(20.dp))

        NotaDeZeroADez(
            titulo = "Complexidade",
            descricao = "Quão difícil foi executar a técnica e as séries.",
            valor = estado.complexidade,
            onValor = onComplexidade,
            minimo = "0 · muito simples",
            maximo = "10 · muito difícil",
            tag = "nota_complexidade",
            habilitado = !estado.salvando
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = estado.observacao,
            onValueChange = onObservacao,
            label = { Text("Observação (opcional)") },
            placeholder = { Text("Como você se sentiu, o que foi mais difícil…") },
            minLines = 2,
            maxLines = 4,
            enabled = !estado.salvando,
            supportingText = { Text("${estado.observacao.length}/500") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("observacao")
        )

        estado.erro?.let { MensagemDeTela(texto = it, erro = true) }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSalvar,
            enabled = !estado.salvando,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("salvar_treino_realizado"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
        ) {
            if (estado.salvando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text("Salvar treino", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        TextButton(
            onClick = onVoltarAoTreino,
            enabled = !estado.salvando,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp)
        ) {
            Text("Voltar ao treino")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BlocoDeNumero(rotulo: String, valor: String, detalhe: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(rotulo.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaTextSecondary, letterSpacing = 0.8.sp)
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Black, color = AquaPrimary, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            Text(detalhe, fontSize = 11.sp, color = AquaTextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun NotaDeZeroADez(
    titulo: String,
    descricao: String,
    valor: Int?,
    onValor: (Int) -> Unit,
    minimo: String,
    maximo: String,
    tag: String,
    habilitado: Boolean
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary, modifier = Modifier.weight(1f))
            Text(
                text = valor?.let { "$it/10" } ?: "sem nota",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = if (valor != null) AquaMagenta else AquaTextMuted
            )
        }
        Text(descricao, fontSize = 12.sp, color = AquaTextSecondary)
        Slider(
            value = (valor ?: 5).toFloat(),
            onValueChange = { onValor(it.roundToInt()) },
            valueRange = 0f..10f,
            steps = 9,
            enabled = habilitado,
            colors = SliderDefaults.colors(
                thumbColor = if (valor != null) AquaMagenta else AquaTextMuted,
                activeTrackColor = if (valor != null) AquaMagenta else AquaBorder,
                inactiveTrackColor = AquaBorder
            ),
            modifier = Modifier.testTag(tag)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minimo, fontSize = 11.sp, color = AquaTextMuted)
            Text(maximo, fontSize = 11.sp, color = AquaTextMuted)
        }
    }
}

// ------------------------------------------------------------------ publicar

@Composable
private fun TelaPublicar(
    resumo: ResumoDoTreino,
    onCompartilhar: (FormatoDoCartao) -> Unit,
    onFechar: () -> Unit,
    modifier: Modifier
) {
    BackHandler(onBack = onFechar)
    var formato by rememberSaveable { mutableStateOf(FormatoDoCartao.FEED) }
    val imagem = remember(resumo, formato) { CartaoDoTreino.desenhar(resumo, formato).asImageBitmap() }
    val legenda = remember(resumo) { TextosDoTreino.legenda(resumo) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Treino salvo!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = AquaGreenText,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Mostre o seu treino: publique a imagem nas redes e marque quem nada com você.",
            fontSize = 14.sp,
            color = AquaTextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp)),
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFFE8F1FC),
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                FormatoDoCartao.entries.forEach { opcao ->
                    val ativo = opcao == formato
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (ativo) Color.White else Color.Transparent)
                            .clickable { formato = opcao }
                            .padding(vertical = 10.dp)
                            .testTag("formato_${opcao.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (opcao == FormatoDoCartao.FEED) "Feed (4:5)" else "Stories (9:16)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ativo) AquaPrimary else AquaTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            bitmap = imagem,
            contentDescription = "Imagem do treino para publicar",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(if (formato == FormatoDoCartao.STORIES) 0.62f else 0.86f)
                .aspectRatio(formato.largura.toFloat() / formato.altura)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, AquaBorder, RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Legenda", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
                Text(legenda, fontSize = 13.sp, color = AquaTextSecondary, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onCompartilhar(formato) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publicar_nas_redes"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publicar nas redes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "A legenda é copiada junto: no Instagram, é só colar.",
            fontSize = 12.sp,
            color = AquaTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(
            onClick = onFechar,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("fechar_publicacao")
        ) {
            Text("Agora não")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
