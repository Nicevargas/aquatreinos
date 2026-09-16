package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.compartilhar.TextosDoTreino
import com.example.data.plano.ResumoDoPlano
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary

/** "Plano de Treino" em destaque na tela inicial: começar um plano ou ver o andamento. */
@Composable
fun PlanoCard(
    resumo: ResumoDoPlano?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = AquaPrimary.copy(alpha = 0.3f))
            .testTag("plano_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(AquaPrimary, Color(0xFF0070E6), AquaMagenta)))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "PLANO DE TREINO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (resumo == null) "Monte o seu plano"
                        else "${resumo.semanas} ${if (resumo.semanas == 1) "semana" else "semanas"} · ${resumo.treinosPorSemana} por semana",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            if (resumo == null) {
                Text(
                    text = "Diga quantas semanas e quantas vezes quer nadar. O app monta com o Método NC, ou você escolhe cada treino.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                LinearProgressIndicator(
                    progress = { if (resumo.total == 0) 0f else resumo.feitos.toFloat() / resumo.total },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "${resumo.feitos} de ${resumo.total} treinos feitos",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 6.dp)
                )
                val proximo = resumo.proximo
                Text(
                    text = if (proximo == null) "Plano concluído. Parabéns!"
                    else "Próximo: Semana ${proximo.semana} · Treino ${proximo.numero} · ${proximo.foco} · ${TextosDoTreino.metros(proximo.metros)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(48.dp)
                    .testTag("plano_card_botao"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AquaPrimary)
            ) {
                Text(if (resumo == null) "Criar meu plano" else "Ver meu plano", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
