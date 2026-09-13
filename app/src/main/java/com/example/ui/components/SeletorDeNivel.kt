package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrainingLevel
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPinkText
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow

/** Cor do nível no carrossel: verde, amarelo, vermelho. */
fun corDoNivel(nivel: TrainingLevel): Color = when (nivel) {
    TrainingLevel.INICIANTE -> AquaGreen
    TrainingLevel.INTERMEDIARIO -> AquaYellow
    TrainingLevel.AVANCADO -> AquaMagenta
}

@Composable
fun SeletorDeNivel(
    selecionado: TrainingLevel,
    onSelecionar: (TrainingLevel) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrainingLevel.entries.forEach { nivel ->
            val ativo = nivel == selecionado
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = habilitado) { onSelecionar(nivel) }
                    .testTag("nivel_${nivel.name.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                color = if (ativo) AquaBlueBg else Color.White,
                border = BorderStroke(if (ativo) 2.dp else 1.dp, if (ativo) AquaPrimary else AquaBorder)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(corDoNivel(nivel))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nivel.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ativo) AquaPrimary else AquaTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = nivel.carouselLabel,
                        fontSize = 10.sp,
                        color = AquaTextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/** Faixa de erro (rosa) ou de aviso (verde) dentro das telas. */
@Composable
fun MensagemDeTela(texto: String, erro: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag(if (erro) "mensagem_erro" else "mensagem_aviso"),
        shape = RoundedCornerShape(12.dp),
        color = if (erro) AquaPinkBg else AquaGreenBg
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (erro) AquaPinkText else AquaGreenText
        )
    }
}
