package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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

/**
 * Um nível por linha. Em três colunas, "Intermediário" era cortado nos celulares
 * estreitos e as descrições de tamanhos diferentes deixavam os cartões desalinhados.
 */
@Composable
fun SeletorDeNivel(
    selecionado: TrainingLevel,
    onSelecionar: (TrainingLevel) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrainingLevel.entries.forEach { nivel ->
            val ativo = nivel == selecionado
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .selectable(
                        selected = ativo,
                        enabled = habilitado,
                        role = Role.RadioButton,
                        onClick = { onSelecionar(nivel) }
                    )
                    .testTag("nivel_${nivel.name.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                color = if (ativo) AquaBlueBg else Color.White,
                border = BorderStroke(if (ativo) 2.dp else 1.dp, if (ativo) AquaPrimary else AquaBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(corDoNivel(nivel))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nivel.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ativo) AquaPrimary else AquaTextPrimary
                        )
                        // "Intermediário · Intermediário" não diz nada: só mostra quando acrescenta.
                        if (nivel.carouselLabel != nivel.label) {
                            Text(
                                text = nivel.carouselLabel,
                                fontSize = 12.sp,
                                color = AquaTextSecondary
                            )
                        }
                    }
                    if (ativo) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AquaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
