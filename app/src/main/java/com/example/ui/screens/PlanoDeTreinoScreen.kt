package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.compartilhar.TextosDoTreino
import com.example.data.plano.OrigemDoTreino
import com.example.data.plano.PlanoDeTreino
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.data.plano.SemanaDoPlano
import com.example.data.plano.TreinoDoPlano
import com.example.model.TrainingLevel
import com.example.ui.components.MensagemDeTela
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellowText
import com.example.viewmodel.AssistenteDoPlano
import com.example.viewmodel.EtapaDoPlano
import com.example.viewmodel.PlanoUiState
import kotlin.math.roundToInt

/** Cor de cada capacidade do Método NC nas etiquetas do plano. */
fun corDoFoco(foco: String): Color = when (foco) {
    "Técnica" -> AquaPrimary
    "Resistência" -> AquaGreenText
    "Velocidade" -> AquaMagenta
    "Estilos" -> Color(0xFF7B1FA2)
    "Ritmo" -> AquaYellowText
    "Força específica" -> Color(0xFFD84315)
    else -> AquaTextSecondary
}

private val DESCRICAO_DO_FOCO = mapOf(
    "Técnica" to "Braçada, rolamento e respiração mais eficientes.",
    "Resistência" to "Nadar mais tempo sem cansar.",
    "Velocidade" to "Tiros curtos, mais rápidos.",
    "Estilos" to "Costas, peito e borboleta, além do crawl.",
    "Ritmo" to "Manter o mesmo ritmo do começo ao fim.",
    "Força específica" to "Mais força na água, com e sem material."
)

// ------------------------------------------------------------------ o plano

@Composable
fun PlanoDeTreinoScreen(
    estado: PlanoUiState,
    onVoltar: () -> Unit,
    onTreino: (TreinoDoPlano) -> Unit,
    onNovoPlano: () -> Unit,
    onExcluir: () -> Unit,
    onConfirmarExclusao: () -> Unit,
    onCancelarExclusao: () -> Unit,
    onTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
    onTrocar: (TreinoDoPlano) -> Unit = {},
    onEscolherTroca: (TreinoDoPlano, String?) -> Unit = { _, _ -> },
    onFecharTroca: () -> Unit = {},
    // Na aba "Plano" a tela fica dentro do app, com topo e barra de baixo: sem seta de voltar.
    emAba: Boolean = false
) {
    val trocando = estado.trocando
    if (trocando != null) {
        TrocaDeTreino(estado, trocando, onEscolherTroca, onFecharTroca, modifier)
        return
    }
    BackHandler(enabled = !emAba, onBack = onVoltar)
    val plano = estado.plano
    val resumo = estado.resumo

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .then(if (emAba) Modifier else Modifier.systemBarsPadding())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emAba) {
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                IconButton(onClick = onVoltar, modifier = Modifier.testTag("plano_voltar")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
            Text(
                text = "Plano de Treino",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (plano != null) {
                IconButton(onClick = onExcluir, modifier = Modifier.testTag("plano_excluir")) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir plano", tint = AquaTextSecondary)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            estado.erro?.let {
                MensagemDeTela(texto = it, erro = true)
                TextButton(onClick = onTentarDeNovo) { Text("Tentar de novo") }
            }

            if (estado.carregando && plano == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AquaPrimary)
                }
            }

            if (plano != null && resumo != null) {
                Cartao {
                    Text(
                        text = "${resumo.semanas} ${if (resumo.semanas == 1) "SEMANA" else "SEMANAS"} · ${resumo.treinosPorSemana} ${if (resumo.treinosPorSemana == 1) "TREINO" else "TREINOS"}/SEMANA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextSecondary,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = TextosDoTreino.nivel(plano.config().nivel),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AquaTextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = if (plano.focos.isEmpty()) "Melhorar: todas as capacidades" else "Melhorar: ${plano.focos.joinToString(", ")}",
                        fontSize = 14.sp,
                        color = AquaTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    GraficoDasSemanas(estado.semanas, estado.feitos)

                    LinearProgressIndicator(
                        progress = { if (resumo.total == 0) 0f else resumo.feitos.toFloat() / resumo.total },
                        color = AquaGreen,
                        trackColor = AquaBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = if (resumo.proximo == null) "Plano concluído: ${resumo.feitos} de ${resumo.total} treinos. Parabéns!"
                        else "${resumo.feitos} de ${resumo.total} treinos feitos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (resumo.proximo == null) AquaGreenText else AquaTextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AquaBlueBg
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = AquaPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (plano.manual) "Você monta este plano: toque em Trocar para escolher cada treino, do Método NC ou de Meus treinos."
                            else "Quer outro treino em algum dia? Toque em Trocar e escolha do Método NC ou de Meus treinos.",
                            fontSize = 13.sp,
                            color = AquaTextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }

                estado.semanas.forEach { semana ->
                    Spacer(modifier = Modifier.height(12.dp))
                    CartaoDaSemana(semana, estado.feitos, resumo.proximo, onTreino, onTrocar)
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onNovoPlano,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("plano_novo"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Criar outro plano")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (estado.confirmarExclusao) {
        AlertDialog(
            onDismissRequest = onCancelarExclusao,
            title = { Text("Excluir o plano?") },
            text = { Text("Os treinos que você já fez continuam no seu histórico.") },
            confirmButton = {
                TextButton(onClick = onConfirmarExclusao, enabled = !estado.excluindo) {
                    Text("Excluir", color = AquaMagenta)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarExclusao, enabled = !estado.excluindo) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun GraficoDasSemanas(semanas: List<SemanaDoPlano>, feitos: Set<Pair<Int, Int>>) {
    val maior = semanas.maxOfOrNull { it.metros }?.takeIf { it > 0 } ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        semanas.forEach { s ->
            val completa = s.treinos.isNotEmpty() && s.treinos.all { (it.semana to it.numero) in feitos }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // A altura da barra é a metragem da semana perto da maior do plano.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight((s.metros.toFloat() / maior).coerceIn(0.08f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    completa -> AquaGreen
                                    s.relaxada -> AquaCyan.copy(alpha = 0.45f)
                                    else -> AquaPrimary
                                }
                            )
                    )
                }
                Text("${s.numero}", fontSize = 10.sp, color = AquaTextMuted, softWrap = false)
            }
        }
    }
}

@Composable
private fun CartaoDaSemana(
    semana: SemanaDoPlano,
    feitos: Set<Pair<Int, Int>>,
    proximo: TreinoDoPlano?,
    onTreino: (TreinoDoPlano) -> Unit,
    onTrocar: (TreinoDoPlano) -> Unit
) {
    Cartao(padding = 0.dp) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SEMANA ${semana.numero}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = AquaTextPrimary,
                letterSpacing = 0.6.sp,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            Etiqueta(
                texto = if (semana.relaxada) "Semana leve" else semana.bloco,
                cor = if (semana.relaxada) AquaCyan else AquaTextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(TextosDoTreino.metros(semana.metros), fontSize = 13.sp, color = AquaTextSecondary, softWrap = false)
        }
        semana.treinos.forEachIndexed { i, treino ->
            if (i > 0) HorizontalDivider(color = AquaBorder, modifier = Modifier.padding(horizontal = 16.dp))
            val feito = (treino.semana to treino.numero) in feitos
            val eOProximo = proximo?.semana == treino.semana && proximo.numero == treino.numero
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTreino(treino) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("plano_treino_${treino.semana}_${treino.numero}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (feito) AquaGreen else AquaBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (feito) Icons.Filled.Check else Icons.Outlined.Schedule,
                        contentDescription = if (feito) "Feito" else "Por fazer",
                        tint = if (feito) Color.White else AquaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Treino ${treino.numero} · ${TextosDoTreino.metros(treino.metros)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AquaTextPrimary
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Etiqueta(treino.foco, if (treino.origem == OrigemDoTreino.MEU_TREINO) AquaPrimary else corDoFoco(treino.foco))
                        if (treino.origem != OrigemDoTreino.METODO_NC) Etiqueta("Sua escolha", AquaTextSecondary)
                        if (eOProximo) Etiqueta("Próximo", AquaMagenta)
                    }
                }
                if (!feito) {
                    TextButton(
                        onClick = { onTrocar(treino) },
                        modifier = Modifier.testTag("plano_trocar_${treino.semana}_${treino.numero}")
                    ) { Text("Trocar", fontSize = 13.sp) }
                } else {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir treino", tint = AquaTextMuted)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun Etiqueta(texto: String, cor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = cor.copy(alpha = 0.12f)) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = cor,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun Cartao(padding: androidx.compose.ui.unit.Dp = 16.dp, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(padding), content = conteudo)
    }
}

/** Aba "Plano" de quem ainda não tem plano: o convite e como funciona. */
@Composable
fun SemPlanoDeTreino(
    carregando: Boolean,
    erro: String?,
    onComecar: () -> Unit,
    onTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        erro?.let {
            MensagemDeTela(texto = it, erro = true)
            TextButton(onClick = onTentarDeNovo) { Text("Tentar de novo") }
        }
        if (carregando) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AquaPrimary)
            }
            return@Column
        }
        com.example.ui.components.PlanoCard(resumo = null, onClick = onComecar)
        Spacer(modifier = Modifier.height(16.dp))
        Cartao {
            Text("Como funciona", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
            listOf(
                "Escolha se o app monta o plano ou se você escolhe cada treino.",
                "Diga por quantas semanas e quantas vezes por semana quer nadar.",
                "Marque o que quer melhorar e o seu nível.",
                "Troque qualquer treino quando quiser: do Método NC ou de Meus treinos."
            ).forEachIndexed { i, passo ->
                Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(AquaBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaPrimary)
                    }
                    Text(
                        text = passo,
                        fontSize = 14.sp,
                        color = AquaTextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 3.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ trocar um treino

@Composable
private fun TrocaDeTreino(
    estado: PlanoUiState,
    vaga: TreinoDoPlano,
    onEscolher: (TreinoDoPlano, String?) -> Unit,
    onFechar: () -> Unit,
    modifier: Modifier
) {
    BackHandler(enabled = !estado.salvandoTroca, onBack = onFechar)
    val habilitado = !estado.salvandoTroca
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        IconButton(
            onClick = onFechar,
            enabled = habilitado,
            modifier = Modifier
                .padding(top = 8.dp)
                .offset(x = (-12).dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar sem trocar")
        }
        Text("Trocar treino", fontSize = 26.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
        Text(
            text = "Semana ${vaga.semana} · Treino ${vaga.numero} · agora: ${vaga.foco} (${TextosDoTreino.metros(vaga.metros)})",
            fontSize = 14.sp,
            color = AquaTextSecondary,
            lineHeight = 20.sp
        )
        if (estado.salvandoTroca) {
            LinearProgressIndicator(color = AquaPrimary, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("DO MÉTODO NC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaTextSecondary, letterSpacing = 0.6.sp)
        Text(
            text = "Mesma fase da semana e o seu nível.",
            fontSize = 13.sp,
            color = AquaTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Cartao(padding = 0.dp) {
            estado.opcoesDaTroca.forEachIndexed { i, w ->
                if (i > 0) HorizontalDivider(color = AquaBorder, modifier = Modifier.padding(horizontal = 16.dp))
                val foco = w.focus.orEmpty()
                OpcaoDeTroca(
                    titulo = foco,
                    detalhe = "${TextosDoTreino.metros(w.totalDistanceMeters)} · ~${w.estimatedMinutes} min",
                    cor = corDoFoco(foco),
                    atual = vaga.origem != OrigemDoTreino.MEU_TREINO && foco == vaga.foco,
                    habilitado = habilitado,
                    tag = "troca_foco_$foco"
                ) { onEscolher(vaga, PlanoDeTreino.trocaPorFoco(foco)) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("MEUS TREINOS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaTextSecondary, letterSpacing = 0.6.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (estado.meusTreinos.isEmpty()) {
            Cartao {
                Text(
                    text = "Você ainda não tem treinos seus. Crie um na aba Meus treinos e ele aparece aqui para entrar no plano.",
                    fontSize = 14.sp,
                    color = AquaTextSecondary,
                    lineHeight = 20.sp
                )
            }
        } else {
            Cartao(padding = 0.dp) {
                estado.meusTreinos.forEachIndexed { i, w ->
                    if (i > 0) HorizontalDivider(color = AquaBorder, modifier = Modifier.padding(horizontal = 16.dp))
                    OpcaoDeTroca(
                        titulo = w.title,
                        detalhe = "${TextosDoTreino.metros(w.totalDistanceMeters)} · ${TextosDoTreino.nivel(w.level)}",
                        cor = AquaPrimary,
                        atual = vaga.origem == OrigemDoTreino.MEU_TREINO && vaga.workout.id == w.id,
                        habilitado = habilitado,
                        tag = "troca_meu_${w.id}"
                    ) { onEscolher(vaga, PlanoDeTreino.trocaPorMeuTreino(w.id)) }
                }
            }
        }

        if (vaga.origem != OrigemDoTreino.METODO_NC) {
            TextButton(
                onClick = { onEscolher(vaga, null) },
                enabled = habilitado,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .testTag("troca_voltar_sugerido")
            ) { Text("Voltar ao treino sugerido pelo app") }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OpcaoDeTroca(
    titulo: String,
    detalhe: String,
    cor: Color,
    atual: Boolean,
    habilitado: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado && !atual, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary)
            Text(detalhe, fontSize = 13.sp, color = AquaTextSecondary)
        }
        if (atual) {
            Etiqueta("Atual", AquaGreenText)
        } else {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Escolher", tint = AquaTextMuted)
        }
    }
}

// ------------------------------------------------------------------ criar o plano

@Composable
fun CriarPlanoScreen(
    assistente: AssistenteDoPlano,
    onAlterar: ((AssistenteDoPlano) -> AssistenteDoPlano) -> Unit,
    onProximo: () -> Unit,
    onVoltar: () -> Unit,
    onFechar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !assistente.salvando, onBack = onVoltar)
    val passo = assistente.etapa.ordinal + 1
    val totalDePassos = EtapaDoPlano.entries.size
    val habilitado = !assistente.salvando

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        IconButton(
            onClick = onFechar,
            enabled = habilitado,
            modifier = Modifier
                .padding(top = 8.dp)
                .offset(x = (-12).dp)
                .testTag("plano_assistente_fechar")
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar sem criar")
        }
        Text("Criar plano de treino", fontSize = 26.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
        Text("Passo $passo de $totalDePassos", fontSize = 14.sp, color = AquaTextSecondary)

        LinearProgressIndicator(
            progress = { passo / totalDePassos.toFloat() },
            color = AquaPrimary,
            trackColor = AquaBorder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 16.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Cartao {
            when (assistente.etapa) {
                EtapaDoPlano.MODO -> {
                    Pergunta("Como você quer montar o seu plano?")
                    Opcao(
                        titulo = "O app monta para mim",
                        descricao = "Os treinos do Método NC são escolhidos pelo que você quer melhorar. Dá para trocar depois.",
                        marcado = !assistente.manual,
                        habilitado = habilitado,
                        tag = "plano_modo_automatico",
                        onMudar = { onAlterar { it.copy(manual = false) } }
                    )
                    Opcao(
                        titulo = "Eu escolho cada treino",
                        descricao = "O app dá um ponto de partida e você troca cada treino do plano: por outro do Método NC ou por um de Meus treinos.",
                        marcado = assistente.manual,
                        habilitado = habilitado,
                        tag = "plano_modo_manual",
                        onMudar = { onAlterar { it.copy(manual = true) } }
                    )
                }
                EtapaDoPlano.SEMANAS -> {
                    Pergunta("Durante quantas semanas você quer nadar?")
                    Contador(
                        valor = assistente.semanas,
                        maximo = PlanoDeTreino.MAX_SEMANAS,
                        habilitado = habilitado,
                        tag = "plano_semanas",
                        onValor = { v -> onAlterar { it.copy(semanas = v) } }
                    )
                    Opcao(
                        titulo = "Planejar semanas leves",
                        descricao = "A cada 4 semanas, uma semana mais leve para o corpo assimilar o treino.",
                        marcado = assistente.semanasRelaxadas,
                        habilitado = habilitado,
                        tag = "plano_relaxadas",
                        onMudar = { v -> onAlterar { it.copy(semanasRelaxadas = v) } }
                    )
                    Opcao(
                        titulo = "Última semana leve",
                        descricao = "Termina o plano com uma semana de regeneração.",
                        marcado = assistente.encerramentoRelaxado,
                        habilitado = habilitado,
                        tag = "plano_encerramento",
                        onMudar = { v -> onAlterar { it.copy(encerramentoRelaxado = v) } }
                    )
                }
                EtapaDoPlano.VEZES -> {
                    Pergunta("Quantas vezes por semana você quer nadar?")
                    Contador(
                        valor = assistente.treinosPorSemana,
                        maximo = PlanoDeTreino.MAX_TREINOS_POR_SEMANA,
                        habilitado = habilitado,
                        tag = "plano_vezes",
                        onValor = { v -> onAlterar { it.copy(treinosPorSemana = v) } }
                    )
                    Text(
                        text = "Você não precisa nadar em dias fixos: o plano segue a ordem dos treinos.",
                        fontSize = 13.sp,
                        color = AquaTextSecondary,
                        lineHeight = 19.sp
                    )
                }
                EtapaDoPlano.FOCOS -> {
                    Pergunta("O que você quer melhorar?")
                    Opcao(
                        titulo = "Todas",
                        descricao = "Um pouco de cada capacidade.",
                        marcado = assistente.focos.isEmpty(),
                        habilitado = habilitado,
                        tag = "plano_foco_todas",
                        onMudar = { onAlterar { it.copy(focos = emptySet()) } }
                    )
                    PlanoDeTreino.FOCOS.forEach { foco ->
                        Opcao(
                            titulo = foco,
                            descricao = DESCRICAO_DO_FOCO[foco].orEmpty(),
                            marcado = foco in assistente.focos,
                            habilitado = habilitado,
                            tag = "plano_foco_$foco",
                            cor = corDoFoco(foco),
                            onMudar = { v ->
                                onAlterar { a -> a.copy(focos = if (v) a.focos + foco else a.focos - foco) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Seu nível", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrainingLevel.entries.forEach { nivel ->
                            val marcado = assistente.nivel == nivel
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = habilitado) { onAlterar { it.copy(nivel = nivel) } }
                                    .testTag("plano_nivel_${nivel.name}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (marcado) AquaBlueBg else Color.White,
                                border = BorderStroke(if (marcado) 2.dp else 1.dp, if (marcado) AquaPrimary else AquaBorder)
                            ) {
                                Text(
                                    text = TextosDoTreino.nivel(nivel),
                                    fontSize = 15.sp,
                                    fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal,
                                    color = if (marcado) AquaPrimary else AquaTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val total = assistente.semanas * assistente.treinosPorSemana
        Text(
            text = "${assistente.semanas} ${if (assistente.semanas == 1) "semana" else "semanas"} · " +
                "${assistente.treinosPorSemana} ${if (assistente.treinosPorSemana == 1) "treino" else "treinos"} por semana · $total ${if (total == 1) "treino" else "treinos"}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AquaTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        )

        assistente.erro?.let { MensagemDeTela(texto = it, erro = true) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onVoltar,
                enabled = habilitado,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (assistente.etapa == EtapaDoPlano.MODO) "Cancelar" else "Voltar")
            }
            Button(
                onClick = onProximo,
                enabled = habilitado,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("plano_proximo"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
            ) {
                if (assistente.salvando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (assistente.etapa == EtapaDoPlano.FOCOS) "Criar plano" else "Próximo",
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Pergunta(texto: String) {
    Text(texto, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary, lineHeight = 24.sp)
}

@Composable
private fun Contador(valor: Int, maximo: Int, habilitado: Boolean, tag: String, onValor: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotaoRedondo("−", habilitado && valor > 1, "Menos") { onValor(valor - 1) }
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .size(96.dp)
                .clip(CircleShape)
                .background(AquaPrimary)
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Text("$valor", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White, softWrap = false)
        }
        BotaoRedondo("+", habilitado && valor < maximo, "Mais") { onValor(valor + 1) }
    }
    Slider(
        value = valor.toFloat(),
        onValueChange = { onValor(it.roundToInt()) },
        valueRange = 1f..maximo.toFloat(),
        steps = (maximo - 2).coerceAtLeast(0),
        enabled = habilitado,
        colors = SliderDefaults.colors(thumbColor = AquaPrimary, activeTrackColor = AquaPrimary, inactiveTrackColor = AquaBorder),
        modifier = Modifier.padding(top = 8.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("1", fontSize = 12.sp, color = AquaTextMuted)
        Text("$maximo", fontSize = 12.sp, color = AquaTextMuted)
    }
}

@Composable
private fun BotaoRedondo(texto: String, habilitado: Boolean, descricao: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        color = if (habilitado) AquaBlueBg else AquaBorder,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = texto,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (habilitado) AquaPrimary else AquaTextMuted,
                modifier = Modifier.semantics { contentDescription = descricao }
            )
        }
    }
}

@Composable
private fun Opcao(
    titulo: String,
    descricao: String,
    marcado: Boolean,
    habilitado: Boolean,
    tag: String,
    cor: Color = AquaTextPrimary,
    onMudar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = marcado, enabled = habilitado, role = Role.Checkbox, onValueChange = onMudar)
            .padding(vertical = 2.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = marcado,
            onCheckedChange = null,
            enabled = habilitado,
            colors = CheckboxDefaults.colors(checkedColor = AquaPrimary),
            modifier = Modifier.padding(10.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = cor)
            if (descricao.isNotEmpty()) {
                Text(descricao, fontSize = 13.sp, color = AquaTextSecondary, lineHeight = 18.sp)
            }
        }
    }
}
