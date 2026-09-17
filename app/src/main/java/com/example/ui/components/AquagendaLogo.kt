package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/** Logo "Natação Criativa Treinos" no topo. Embarcado no app: aparece mesmo sem internet. O ícone do app é outro (natacao_criativa_icone). */
@Composable
fun AquagendaLogo(
    modifier: Modifier = Modifier,
    height: Dp = 36.dp
) {
    Image(
        painter = painterResource(R.drawable.natacao_criativa_logo),
        contentDescription = "Natação Criativa Treinos",
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
        modifier = modifier
            .height(height)
            .testTag("logo")
    )
}
