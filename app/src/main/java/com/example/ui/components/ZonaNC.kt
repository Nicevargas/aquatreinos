package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaYellow

// Mesmas cores das etiquetas do carrossel: das zonas leves (frias) às intensas (quentes).
fun corDaZona(sigla: String?): Color = when (sigla?.trim()?.uppercase()) {
    "A0" -> Color(0xFF8FB4D8)
    "A1" -> AquaGreen
    "A2" -> AquaYellow
    "A3" -> Color(0xFFFF8A00)
    "AN" -> AquaMagenta
    "AA" -> Color(0xFF9B7BFF)
    else -> AquaBorder
}

/** Cor do bloco, igual ao carrossel. Treinos antigos (Aquecimento, Principal, Final) usam a do bloco equivalente. */
fun corDoBloco(titulo: String): Color = when (titulo.trim().lowercase()) {
    "ativação", "ativacao", "aquecimento" -> AquaPrimary
    "preparação", "preparacao", "preparatória", "preparatoria" -> AquaYellow
    "desenvolvimento", "principal" -> AquaMagenta
    "consolidação", "consolidacao" -> AquaGreen
    else -> AquaCyan // Recuperação, Final, Soltura
}

@Composable
fun EtiquetaDeZona(
    sigla: String,
    modifier: Modifier = Modifier,
    texto: String = sigla,
    tamanho: TextUnit = 11.sp
) {
    val textoClaro = sigla.trim().uppercase() in setOf("AN", "AA")
    Text(
        text = texto,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(corDaZona(sigla))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        fontSize = tamanho,
        fontWeight = FontWeight.ExtraBold,
        color = if (textoClaro) Color.White else AquaTextPrimary,
        softWrap = false
    )
}
