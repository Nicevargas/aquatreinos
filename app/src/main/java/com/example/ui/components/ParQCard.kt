package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ciclo.DataCivil
import com.example.data.parq.ParQ
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaPinkText
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary

/** Situação do PAR-Q no Perfil, com o atalho para responder de novo. */
@Composable
fun ParQCard(
    ultimoDia: Long?,
    onResponder: () -> Unit,
    modifier: Modifier = Modifier,
    hoje: Long = DataCivil.hoje()
) {
    val emDia = !ParQ.precisaResponder(ultimoDia, hoje)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parq_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "PAR-Q · PRONTIDÃO PARA ATIVIDADE FÍSICA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextSecondary,
                letterSpacing = 0.6.sp
            )
            Text(
                text = when {
                    ultimoDia == null -> "Ainda não respondido"
                    emDia -> "Em dia até ${DataCivil.paraBr(ParQ.venceEm(ultimoDia) - 1)}"
                    else -> "Vencido: responda antes do próximo treino"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    ultimoDia == null -> AquaTextPrimary
                    emDia -> AquaGreenText
                    else -> AquaPinkText
                },
                modifier = Modifier.padding(top = 4.dp)
            )
            ultimoDia?.let {
                Text(
                    text = "Respondido em ${DataCivil.paraBr(it)}",
                    fontSize = 13.sp,
                    color = AquaTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            TextButton(
                onClick = onResponder,
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .testTag("parq_responder_de_novo")
            ) {
                Text(if (emDia) "Minha saúde mudou: responder de novo" else "Responder agora")
            }
        }
    }
}
