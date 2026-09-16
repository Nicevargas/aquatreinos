package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.data.lembrete.LembreteDeTreino
import com.example.data.progresso.Pontuacao
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ciclo.DataCivil
import com.example.data.compartilhar.ResumoDoTreino
import com.example.data.compartilhar.TextosDoTreino
import com.example.data.execucao.EscalaDeEsforco
import com.example.data.progresso.Atividade
import com.example.data.progresso.Conquista
import com.example.data.progresso.PainelDoProgresso
import com.example.data.progresso.SerieDeSemanas
import com.example.ui.screens.corDoFoco
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow
import com.example.ui.theme.AquaYellowBg
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")

private fun semanasSeguidas(n: Int) = if (n == 1) "1 semana seguida" else "$n semanas seguidas"

private fun decimal(valor: Double) = String.format(PT_BR, "%.1f", valor)

@Composable
private fun CartaoBranco(modifier: Modifier = Modifier, padding: Int = 18, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(padding.dp), content = conteudo)
    }
}

@Composable
private fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AquaTextSecondary,
        letterSpacing = 0.6.sp,
        modifier = modifier
    )
}

@Composable
private fun Etiqueta(texto: String, cor: Color, fundo: Color = cor.copy(alpha = 0.12f)) {
    Surface(shape = RoundedCornerShape(8.dp), color = fundo) {
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
fun EtiquetaDeEsforco(nota: Int) {
    Etiqueta(
        texto = "Esforço $nota",
        cor = if (EscalaDeEsforco.textoClaro(nota)) Color.White else AquaTextPrimary,
        fundo = Color(EscalaDeEsforco.cor(nota))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinhaDeAtividade(atividade: Atividade, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(DataCivil.paraBr(atividade.dia), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary)
            // Em tela estreita a etiqueta de esforço desce para a linha de baixo em vez de cortar.
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val foco = atividade.foco ?: atividade.titulo
                Etiqueta(foco, corDoFoco(foco))
                atividade.esforco?.let { EtiquetaDeEsforco(it) }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            Text(TextosDoTreino.metros(atividade.metros), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AquaPrimary, softWrap = false)
            Text(TextosDoTreino.duracao(atividade.duracaoSegundos.toLong()), fontSize = 12.sp, color = AquaTextSecondary, softWrap = false)
        }
    }
}

// ------------------------------------------------------------------ tela inicial

/** As bolinhas dos últimos 7 dias e a série de semanas. */
@Composable
fun SuaSemanaCard(painel: PainelDoProgresso, onVerProgresso: () -> Unit, modifier: Modifier = Modifier) {
    CartaoBranco(modifier.testTag("sua_semana")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Rotulo("SUA SEMANA", Modifier.weight(1f))
            TextButton(onClick = onVerProgresso) { Text("Ver progresso") }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            painel.ultimosSeteDias.forEach { (dia, nadou) ->
                val hoje = dia == painel.hoje
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (nadou) AquaPrimary else AquaSurfaceContainerLow)
                            .then(if (hoje) Modifier.border(2.dp, AquaPrimary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (nadou) Icon(Icons.Filled.Check, contentDescription = "Nadou", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = DataCivil.sigla(dia),
                        fontSize = 11.sp,
                        fontWeight = if (hoje) FontWeight.Bold else FontWeight.Normal,
                        color = if (hoje) AquaPrimary else AquaTextMuted,
                        softWrap = false,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        val s = painel.serie
        Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (s.semanas > 0) AquaMagenta else AquaTextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    s.semanas == 0 -> "Nade esta semana para começar sua série."
                    s.nadouEstaSemana -> "${semanasSeguidas(s.semanas)} nadando. Continue assim!"
                    else -> "Sua série: ${semanasSeguidas(s.semanas)}. Nade até domingo para manter."
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AquaTextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun UltimasAtividadesCard(atividades: List<Atividade>, onVerTudo: () -> Unit, modifier: Modifier = Modifier) {
    CartaoBranco(modifier.testTag("ultimas_atividades")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Rotulo("ÚLTIMAS ATIVIDADES", Modifier.weight(1f))
            if (atividades.isNotEmpty()) TextButton(onClick = onVerTudo) { Text("Ver tudo") }
        }
        if (atividades.isEmpty()) {
            Text(
                text = "Nenhum treino registrado ainda. Conclua o seu primeiro treino e ele aparece aqui.",
                fontSize = 14.sp,
                color = AquaTextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        atividades.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(color = AquaBorder)
            LinhaDeAtividade(a)
        }
    }
}

// ------------------------------------------------------------------ meu progresso

@Composable
fun CartaoDeTotais(painel: PainelDoProgresso, modifier: Modifier = Modifier) {
    val t = painel.totais
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numero("TREINOS", "${t.treinos}", Modifier.weight(1f).fillMaxHeight())
            Numero("DISTÂNCIA", TextosDoTreino.metros(t.metros), Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numero("TEMPO", TextosDoTreino.duracao(t.segundos), Modifier.weight(1f).fillMaxHeight())
            Numero("EM ${painel.nomeMesAtual.uppercase(PT_BR)}", TextosDoTreino.metros(t.metrosNoMes), Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, AquaBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(rotulo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AquaTextSecondary, letterSpacing = 0.6.sp)
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AquaPrimary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun CartaoDaSerie(serie: SerieDeSemanas, modifier: Modifier = Modifier) {
    CartaoBranco(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (serie.semanas > 0) AquaMagenta.copy(alpha = 0.12f) else AquaSurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (serie.semanas > 0) AquaMagenta else AquaTextMuted,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Rotulo("SÉRIE DE SEMANAS")
                Text(semanasSeguidas(serie.semanas), fontSize = 20.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
                Text(
                    text = when {
                        serie.semanas == 0 -> "Nade pelo menos uma vez na semana para começar."
                        serie.nadouEstaSemana -> "Esta semana já conta."
                        else -> "Nade até domingo para não perder a série."
                    },
                    fontSize = 13.sp,
                    color = AquaTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/** Metros acumulados no mês atual contra o mês anterior. */
@Composable
fun GraficoDoMes(painel: PainelDoProgresso, modifier: Modifier = Modifier) {
    val atual = painel.metrosMesAtual
    val anterior = painel.metrosMesAnterior
    val maior = maxOf(atual.maxOrNull() ?: 0, anterior.maxOrNull() ?: 0, 1000)
    val dias = maxOf(painel.diasNoMesAtual, anterior.size, 2)
    CartaoBranco(modifier.testTag("grafico_do_mes")) {
        Rotulo("METROS EM ${painel.nomeMesAtual.uppercase(PT_BR)}")
        Text(
            text = TextosDoTreino.metros(atual.lastOrNull() ?: 0),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = AquaTextPrimary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "Até o fim de ${painel.nomeMesAnterior}: ${TextosDoTreino.metros(anterior.lastOrNull() ?: 0)}",
            fontSize = 13.sp,
            color = AquaTextSecondary
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            (0..3).forEach { i ->
                val y = h * i / 3
                drawLine(AquaBorder, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }
            fun linha(valores: List<Int>, cor: Color, espessura: Float) {
                if (valores.isEmpty()) return
                val caminho = Path()
                valores.forEachIndexed { i, v ->
                    val x = w * i / (dias - 1)
                    val y = h - h * v / maior
                    if (i == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
                }
                drawPath(caminho, cor, style = Stroke(width = espessura, cap = StrokeCap.Round))
            }
            linha(anterior, AquaTextMuted.copy(alpha = 0.6f), 2.dp.toPx())
            linha(atual, AquaPrimary, 3.dp.toPx())
        }
        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Legenda(AquaPrimary, "Mês atual")
            Spacer(modifier = Modifier.width(16.dp))
            Legenda(AquaTextMuted, "Mês anterior")
        }
    }
}

@Composable
private fun Legenda(cor: Color, texto: String) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(cor))
    Spacer(modifier = Modifier.width(6.dp))
    Text(texto, fontSize = 12.sp, color = AquaTextSecondary, softWrap = false)
}

/** Média e distribuição da percepção de esforço, com as cores da escala do professor. */
@Composable
fun CartaoDeEsforco(painel: PainelDoProgresso, modifier: Modifier = Modifier) {
    val e = painel.esforco
    CartaoBranco(modifier.testTag("cartao_esforco")) {
        Rotulo("PERCEPÇÃO DE ESFORÇO")
        if (e.treinosComNota == 0 || e.media == null) {
            Text(
                text = "Ao salvar cada treino você marca o esforço de 0 a 10. A estatística aparece aqui.",
                fontSize = 14.sp,
                color = AquaTextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            return@CartaoBranco
        }
        val mediaArredondada = e.media.let { kotlin.math.round(it).toInt() }
        Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(decimal(e.media), fontSize = 30.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Etiqueta(
                texto = EscalaDeEsforco.nome(mediaArredondada),
                cor = if (EscalaDeEsforco.textoClaro(mediaArredondada)) Color.White else AquaTextPrimary,
                fundo = Color(EscalaDeEsforco.cor(mediaArredondada))
            )
        }
        Text(
            text = "Média de ${e.treinosComNota} ${if (e.treinosComNota == 1) "treino" else "treinos"}" +
                (e.mediaUltimas4Semanas?.let { " · últimas 4 semanas: ${decimal(it)}" } ?: ""),
            fontSize = 13.sp,
            color = AquaTextSecondary,
            lineHeight = 18.sp
        )

        val maior = e.porNota.maxOrNull()?.takeIf { it > 0 } ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            e.porNota.forEachIndexed { nota, quantos ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (quantos > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(quantos.toFloat() / maior)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(EscalaDeEsforco.cor(nota)))
                            )
                        }
                    }
                    Text("$nota", fontSize = 10.sp, color = AquaTextMuted, softWrap = false)
                }
            }
        }
        Text(
            text = "Quantos treinos em cada nota",
            fontSize = 12.sp,
            color = AquaTextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CartaoDeConquistas(painel: PainelDoProgresso, modifier: Modifier = Modifier) {
    CartaoBranco(modifier.testTag("conquistas")) {
        Rotulo("PRÊMIOS · ${painel.conquistas.size} DE ${com.example.data.progresso.Progresso.CATALOGO.size}")
        if (painel.conquistas.isEmpty()) {
            Text(
                text = "Conclua treinos para ganhar os primeiros prêmios.",
                fontSize = 14.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        painel.conquistas.asReversed().forEach { c ->
            LinhaDePremio(c.titulo, "${c.descricao} ${DataCivil.paraBr(c.dia)}", obtido = true)
        }
        if (painel.proximasConquistas.isNotEmpty()) {
            Text(
                text = "Próximos",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 12.dp)
            )
            painel.proximasConquistas.forEach { LinhaDePremio(it.titulo, it.descricao, obtido = false) }
        }
    }
}

@Composable
private fun LinhaDePremio(titulo: String, descricao: String, obtido: Boolean) {
    Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (obtido) AquaYellowBg else AquaSurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (obtido) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = if (obtido) AquaYellow else AquaTextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (obtido) AquaTextPrimary else AquaTextMuted)
            Text(descricao, fontSize = 12.sp, color = if (obtido) AquaTextSecondary else AquaTextMuted, lineHeight = 17.sp)
        }
    }
}

@Composable
fun CartaoDoHistorico(atividades: List<Atividade>, modifier: Modifier = Modifier) {
    CartaoBranco(modifier.testTag("historico")) {
        Rotulo("HISTÓRICO")
        if (atividades.isEmpty()) {
            Text(
                text = "Os treinos que você concluir ficam guardados aqui.",
                fontSize = 14.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        atividades.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(color = AquaBorder)
            LinhaDeAtividade(a)
        }
    }
}

/** Pontos de gamificação e o caminho até o próximo nível. */
@Composable
fun CartaoDePontos(painel: PainelDoProgresso, modifier: Modifier = Modifier) {
    val nivel = Pontuacao.nivel(painel.pontos)
    val proximo = Pontuacao.proximoNivel(painel.pontos)
    CartaoBranco(modifier.testTag("cartao_pontos")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AquaYellowBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Stars, contentDescription = null, tint = AquaYellow, modifier = Modifier.size(30.dp))
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Rotulo("PONTOS · NÍVEL ${nivel.nome.uppercase(PT_BR)}")
                Text("${painel.pontos} pontos", fontSize = 22.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
            }
        }
        if (proximo != null) {
            val faixa = (proximo.minimo - nivel.minimo).coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { ((painel.pontos - nivel.minimo).toFloat() / faixa).coerceIn(0f, 1f) },
                color = AquaYellow,
                trackColor = AquaBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = "Faltam ${proximo.minimo - painel.pontos} pontos para ${proximo.nome}",
                fontSize = 13.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Text("Nível máximo. Parabéns!", fontSize = 13.sp, color = AquaTextSecondary, modifier = Modifier.padding(top = 8.dp))
        }
        Text(
            text = "1 ponto a cada ${Pontuacao.METROS_POR_PONTO} m · +${Pontuacao.BONUS_TREINO_COMPLETO} por treino completo · +${Pontuacao.BONUS_SEMANA_COM_TREINO} por semana com treino",
            fontSize = 12.sp,
            color = AquaTextMuted,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun CartaoDoRanking(onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    CartaoBranco(modifier.testTag("cartao_ranking")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(28.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text("Ranking dos nadadores", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
                Text(
                    text = "Compare seus pontos por idade, sexo, horário e local. Só aparece quem aceita.",
                    fontSize = 13.sp,
                    color = AquaTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
        androidx.compose.material3.Button(
            onClick = onAbrir,
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AquaPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("abrir_ranking")
        ) { Text("Ver ranking", fontWeight = FontWeight.Bold) }
    }
}

/** Quanto nadou por semana ou por mês, em tabela. */
@Composable
fun TabelaDoQuantoNadou(painel: PainelDoProgresso, modifier: Modifier = Modifier) {
    var porMes by rememberSaveable { mutableStateOf(false) }
    val linhas = if (porMes) painel.porMes else painel.porSemana
    CartaoBranco(modifier.testTag("tabela_quanto_nadou")) {
        Rotulo("QUANTO VOCÊ NADOU")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(AquaSurfaceContainerLow)
                .padding(4.dp)
        ) {
            listOf(false to "Por semana", true to "Por mês").forEach { (mes, rotulo) ->
                val ativo = porMes == mes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (ativo) Color.White else Color.Transparent)
                        .clickableSemEfeito { porMes = mes }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rotulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (ativo) AquaPrimary else AquaTextSecondary)
                }
            }
        }
        Row(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
            Celula(if (porMes) "Mês" else "Semana", 1.1f, cabecalho = true)
            Celula("Treinos", 0.9f, cabecalho = true, direita = true)
            Celula("Metros", 1.3f, cabecalho = true, direita = true)
            Celula("Esforço", 1.0f, cabecalho = true, direita = true)
        }
        HorizontalDivider(color = AquaBorder)
        linhas.forEachIndexed { i, l ->
            if (i > 0) HorizontalDivider(color = AquaBorder.copy(alpha = 0.6f))
            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Celula(l.rotulo, 1.1f, destaque = i == 0)
                Celula("${l.treinos}", 0.9f, direita = true, apagado = l.treinos == 0)
                Celula(TextosDoTreino.metros(l.metros), 1.3f, direita = true, apagado = l.treinos == 0, destaque = i == 0)
                Celula(l.esforcoMedio?.let { decimal(it) } ?: "–", 1.0f, direita = true, apagado = l.esforcoMedio == null)
            }
        }
        Text(
            text = if (porMes) "Mês atual no alto · esforço médio de 0 a 10" else "Semanas começam na segunda · esforço médio de 0 a 10",
            fontSize = 12.sp,
            color = AquaTextMuted,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Celula(
    texto: String,
    peso: Float,
    cabecalho: Boolean = false,
    direita: Boolean = false,
    apagado: Boolean = false,
    destaque: Boolean = false
) {
    Text(
        text = texto,
        fontSize = if (cabecalho) 12.sp else 14.sp,
        fontWeight = when {
            cabecalho -> FontWeight.Bold
            destaque -> FontWeight.Bold
            else -> FontWeight.Normal
        },
        color = when {
            cabecalho -> AquaTextSecondary
            apagado -> AquaTextMuted
            else -> AquaTextPrimary
        },
        textAlign = if (direita) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start,
        softWrap = false,
        // Folga à esquerda: sem ela "Metros" e "Esforço" encostavam no cabeçalho.
        modifier = Modifier
            .weight(peso)
            .padding(start = if (direita) 6.dp else 0.dp)
    )
}

private fun Modifier.clickableSemEfeito(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

/** Depois do treino: a pessoa combina o dia de voltar e o celular lembra às 7h. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuandoVoltarANadar(hoje: Long, onEscolher: (Long) -> Unit, modifier: Modifier = Modifier) {
    var escolhido by rememberSaveable { mutableStateOf<Long?>(null) }
    var pulou by rememberSaveable { mutableStateOf(false) }
    if (pulou) return
    CartaoBranco(modifier.testTag("quando_voltar")) {
        Text("Quando você vai voltar a nadar?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
        val dia = escolhido
        if (dia == null) {
            Text(
                text = "Escolha o dia e a gente te lembra às ${LembreteDeTreino.HORA_DO_AVISO}h.",
                fontSize = 13.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LembreteDeTreino.proximosDias(hoje).forEach { d ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                escolhido = d
                                onEscolher(d)
                            }
                            .testTag("voltar_$d"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, AquaBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(DataCivil.sigla(d), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaPrimary)
                            Text(DataCivil.paraBr(d).take(5), fontSize = 13.sp, color = AquaTextPrimary)
                        }
                    }
                }
            }
            TextButton(onClick = { pulou = true }, modifier = Modifier.padding(top = 2.dp)) { Text("Pular") }
        } else {
            Text(
                text = "Combinado! Vamos te lembrar ${LembreteDeTreino.rotulo(dia)}, às ${LembreteDeTreino.HORA_DO_AVISO}h.",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AquaTextPrimary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// ------------------------------------------------------------------ treino salvo

/** "Muito bem!" depois de salvar: série de semanas e prêmios que o treino liberou. */
@Composable
fun CartaoMuitoBem(
    resumo: ResumoDoTreino,
    serie: SerieDeSemanas?,
    estendeuSerie: Boolean,
    conquistas: List<Conquista>,
    modifier: Modifier = Modifier,
    pontos: Int? = null,
    nivel: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("muito_bem"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(AquaPrimary, Color(0xFF0070E6), AquaMagenta)))
                .padding(18.dp)
        ) {
            Text("Muito bem!", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                text = "${TextosDoTreino.metros(resumo.metrosFeitos)} em ${TextosDoTreino.duracao(resumo.duracaoSegundos)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f)
            )
            if (pontos != null && pontos > 0) {
                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Stars, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+$pontos pontos" + (nivel?.let { " · nível $it" } ?: ""),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (serie != null && serie.semanas > 0) {
                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (estendeuSerie) "Estendeu sua série: ${semanasSeguidas(serie.semanas)}!"
                        else "Sua série: ${semanasSeguidas(serie.semanas)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            conquistas.forEach { c ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AquaYellowBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AquaYellow, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Você ganhou um prêmio!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaMagenta)
                            Text(c.titulo, fontSize = 16.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
                            Text(c.descricao, fontSize = 13.sp, color = AquaTextSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
