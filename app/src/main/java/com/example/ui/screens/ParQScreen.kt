package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.parq.ParQ
import com.example.ui.components.MensagemDeTela
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPinkText
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.ParQUiState

/** PAR-Q: as 7 perguntas, o termo quando há SIM e a declaração, antes de treinar. */
@Composable
fun ParQScreen(
    estado: ParQUiState,
    onResponder: (pergunta: Int, sim: Boolean) -> Unit,
    onDeclaracao: (Boolean) -> Unit,
    onTermo: (Boolean) -> Unit,
    onEnviar: () -> Unit,
    onFechar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !estado.enviando, onBack = onFechar)
    val habilitado = !estado.enviando
    val algumSim = ParQ.algumSim(estado.respostas)
    val faltam = ParQ.faltam(estado.respostas)

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
                .testTag("parq_fechar")
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar sem responder")
        }

        Text("PAR-Q", fontSize = 32.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
        Text(
            text = "Questionário de Prontidão para Atividade Física",
            fontSize = 14.sp,
            color = AquaTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Cartao {
            ParQ.INTRODUCAO.forEachIndexed { i, paragrafo ->
                if (i > 0) Spacer(modifier = Modifier.height(10.dp))
                Text(paragrafo, fontSize = 14.sp, color = AquaTextPrimary, lineHeight = 21.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Cartao {
            ParQ.PERGUNTAS.forEachIndexed { i, pergunta ->
                if (i > 0) HorizontalDivider(color = AquaBorder, modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "${i + 1}) $pergunta",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AquaTextPrimary,
                    lineHeight = 22.sp
                )
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OpcaoSimNao("Sim", estado.respostas[i] == true, habilitado, "parq_${i + 1}_sim") { onResponder(i, true) }
                    OpcaoSimNao("Não", estado.respostas[i] == false, habilitado, "parq_${i + 1}_nao") { onResponder(i, false) }
                }
            }
        }

        if (algumSim) {
            Spacer(modifier = Modifier.height(12.dp))
            Cartao(cor = AquaPinkBg, borda = AquaPinkText.copy(alpha = 0.35f)) {
                Text(
                    text = "Você respondeu SIM a pelo menos uma pergunta",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaPinkText
                )
                ParQ.TERMO.forEach { paragrafo ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(paragrafo, fontSize = 14.sp, color = AquaTextPrimary, lineHeight = 21.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinhaDeAceite(ParQ.ACEITE_DO_TERMO, estado.termo, habilitado, "parq_termo", onTermo)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        LinhaDeAceite(ParQ.DECLARACAO, estado.declaracao, habilitado, "parq_declaracao", onDeclaracao)

        estado.erro?.let { MensagemDeTela(texto = it, erro = true) }

        Spacer(modifier = Modifier.height(16.dp))

        if (faltam > 0) {
            Text(
                text = if (faltam == 1) "Falta 1 resposta" else "Faltam $faltam respostas",
                fontSize = 13.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = onEnviar,
            enabled = habilitado && ParQ.podeEnviar(estado.respostas, estado.declaracao, estado.termo),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("parq_enviar"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
        ) {
            if (estado.enviando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = if (estado.vaiTreinar) "Enviar e começar o treino" else "Enviar respostas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Suas respostas valem por ${ParQ.VALIDADE_MESES} meses. Se sua saúde mudar antes disso, responda de novo pelo Perfil.",
            fontSize = 12.sp,
            color = AquaTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Cartao(
    cor: Color = Color.White,
    borda: Color = AquaBorder,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cor,
        border = BorderStroke(1.dp, borda)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = conteudo)
    }
}

@Composable
private fun OpcaoSimNao(texto: String, marcada: Boolean, habilitado: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .selectable(selected = marcada, enabled = habilitado, role = Role.RadioButton, onClick = onClick)
            .padding(end = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = marcada,
            onClick = null,
            enabled = habilitado,
            colors = RadioButtonDefaults.colors(selectedColor = AquaPrimary),
            modifier = Modifier.padding(12.dp)
        )
        Text(texto, fontSize = 15.sp, color = AquaTextPrimary)
    }
}

@Composable
private fun LinhaDeAceite(texto: String, marcado: Boolean, habilitado: Boolean, tag: String, onMudar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = marcado, enabled = habilitado, role = Role.Checkbox, onValueChange = onMudar)
            .padding(vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = marcado,
            onCheckedChange = null,
            enabled = habilitado,
            colors = CheckboxDefaults.colors(checkedColor = AquaPrimary),
            modifier = Modifier.padding(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            fontSize = 14.sp,
            color = AquaTextPrimary,
            lineHeight = 21.sp,
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp, end = 8.dp)
        )
    }
}
