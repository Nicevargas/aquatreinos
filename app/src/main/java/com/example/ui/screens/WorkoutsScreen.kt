package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.AquagendaConstants
import com.example.data.treinos.MetodoNC
import com.example.model.Workout
import com.example.model.WorkoutPhase
import com.example.ui.components.EtiquetaDeZona
import com.example.ui.components.corDoBloco
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPinkText
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow
import com.example.ui.theme.AquaYellowBg
import com.example.ui.theme.AquaYellowText

@Composable
fun WorkoutsScreen(
    workout: Workout,
    onStartWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveToMyWorkouts: (() -> Unit)? = null,
    onEditar: (() -> Unit)? = null,
    onCompartilhar: (() -> Unit)? = null,
    compartilhando: Boolean = false
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Hero Image Header with direct image link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(AquagendaConstants.URL_SWIMMER_HERO_DETAILS)
                    .crossfade(true)
                    .build(),
                contentDescription = "Swimmer mid-stroke",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0C243B)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AquaCyan, modifier = Modifier.size(32.dp))
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF003865), Color(0xFF001F3B))
                                )
                            )
                    )
                }
            )

            // Gradient transition to white/background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF0A1424).copy(alpha = 0.2f),
                                Color(0xFFF8FAFF)
                            )
                        )
                    )
            )

            // Floating title banner at bottom of hero
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(AquaMagenta)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (workout.isSuggestion) "SUGESTÃO DO DIA" else "TREINO DO DIA",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Text(
                    text = workout.title,
                    color = AquaTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (workout.subtitle.isNotBlank()) {
                    Text(
                        text = workout.subtitle,
                        color = AquaTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Total Distance & Metrics Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = AquaPrimary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DISTÂNCIA TOTAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextSecondary,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${workout.totalDistanceMeters}",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            color = AquaPrimary,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = "m",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaPrimary,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Duration pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(AquaYellowBg)
                                .border(1.dp, AquaYellow.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = AquaYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${workout.estimatedMinutes} min",
                                color = AquaYellowText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Calories pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(AquaPinkBg)
                                .border(1.dp, AquaMagenta.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = AquaMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${workout.calories} kcal",
                                color = AquaPinkText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Método NC: o objetivo vem antes da metragem.
            workout.objetivo?.let { objetivo ->
                Spacer(modifier = Modifier.height(16.dp))
                CartaoDoObjetivo(objetivo = objetivo, zona = workout.zona, ajuste = workout.ajuste)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Equipamentos Necessários
            Text(
                text = "Equipamentos Necessários",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val materiais = workout.equipment
            if (materiais.isEmpty()) {
                Text(
                    text = "Nenhum material neste treino: só você e a piscina.",
                    fontSize = 13.sp,
                    color = AquaTextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    materiais.forEach { nome ->
                        val (icone, cor) = iconeDoMaterial(nome)
                        EquipmentCard(
                            title = nome,
                            icon = icone,
                            accentColor = cor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Estrutura do Treino
            Text(
                text = "Estrutura do Treino",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                workout.phases.forEach { phase ->
                    WorkoutStructurePhaseCard(
                        phase = phase,
                        accentColor = corDoBloco(phase.title)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            if (workout.salvarAoConcluir) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aviso_salvar_ao_concluir"),
                    shape = RoundedCornerShape(14.dp),
                    color = AquaYellowBg
                ) {
                    Text(
                        text = "${workout.tag}: este treino vai para Meus treinos quando você concluir.",
                        fontSize = 13.sp,
                        color = AquaTextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (onEditar != null || onCompartilhar != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    onEditar?.let { editar ->
                        OutlinedButton(
                            onClick = editar,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("editar_este_treino"),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Edit, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Editar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AquaPrimary, maxLines = 1)
                        }
                    }
                    onCompartilhar?.let { compartilhar ->
                        OutlinedButton(
                            onClick = compartilhar,
                            enabled = !compartilhando,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("compartilhar_este_treino"),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            if (compartilhando) {
                                androidx.compose.material3.CircularProgressIndicator(color = AquaPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(androidx.compose.material.icons.Icons.Outlined.Share, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartilhar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AquaPrimary, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (onSaveToMyWorkouts != null && workout.isSuggestion) {
                OutlinedButton(
                    onClick = onSaveToMyWorkouts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("salvar_nos_meus_treinos"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = AquaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar nos meus treinos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AquaPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Primary CTA: Iniciar Treino
            Button(
                onClick = onStartWorkoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = AquaPrimary.copy(alpha = 0.35f)
                    )
                    .testTag("workouts_start_cta"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Iniciar Treino",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // A barra de baixo já desconta a própria altura (innerPadding do Scaffold).
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Objetivo do dia, zona predominante e o que ajustar: a primeira coisa a ler no Método NC. */
@Composable
private fun CartaoDoObjetivo(objetivo: String, zona: String?, ajuste: String?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cartao_objetivo"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "OBJETIVO DO DIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextSecondary,
                letterSpacing = 1.sp
            )
            // Etiqueta embaixo do título: ao lado, espremia "OBJETIVO DO DIA" no celular estreito.
            MetodoNC.zona(zona)?.let { z ->
                EtiquetaDeZona(
                    sigla = z.sigla,
                    texto = "${z.sigla} · ${z.nome}",
                    tamanho = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text(
                text = objetivo,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AquaTextPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
            MetodoNC.zona(zona)?.let { z ->
                Text(
                    text = "Esforço (PSE) ${z.pse} de 10",
                    fontSize = 12.sp,
                    color = AquaTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            ajuste?.let {
                Text(
                    text = "Ajuste: $it",
                    fontSize = 12.sp,
                    color = AquaTextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EquipmentCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AquaTextPrimary
            )
        }
    }
}

private fun iconeDoMaterial(nome: String): Pair<ImageVector, Color> = when (nome.lowercase()) {
    "palmar" -> Icons.Outlined.FrontHand to AquaPrimary
    "pull buoy" -> Icons.Outlined.WaterDrop to AquaMagenta
    "nadadeira" -> Icons.Outlined.Pool to AquaGreen
    else -> Icons.Outlined.Straighten to AquaYellow
}

/** Uma fase do treino com as séries do jeito que saem no carrossel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutStructurePhaseCard(
    phase: WorkoutPhase,
    accentColor: Color
) {
    val title = phase.title
    val percentage = "${phase.percentage}%"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Colored Vertical Pill Line
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$title · ${phase.distanceMeters}m",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaTextPrimary
                )
                if (phase.sets.isEmpty()) {
                    Text(
                        text = phase.summary,
                        fontSize = 13.sp,
                        color = AquaTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                phase.sets.forEach { set ->
                    // A etiqueta segue o texto; se não couber, desce para a linha de baixo.
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = set.header.ifBlank { "${set.repsDistance}m ${set.description}" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AquaTextPrimary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        set.zona?.let {
                            EtiquetaDeZona(sigla = it, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                    val cauda = (
                        (if (set.corretivo != null) emptyList() else set.details) +
                            listOfNotNull(MetodoNC.intervaloLegivel(set.intervalTarget))
                        ).joinToString(" · ")
                    if (cauda.isNotEmpty()) {
                        Text(
                            text = cauda,
                            fontSize = 12.sp,
                            color = AquaTextSecondary
                        )
                    }
                    // Corretivo: o que corrigir e a dica, e a volta ao nado completo.
                    set.corretivo?.let { c ->
                        Text(
                            text = "Corretivo: ${c.nome} + nado completo — “${c.dica}”",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AquaYellowText
                        )
                    }
                }
            }

            // Percentage pill badge, na cor do bloco
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = percentage,
                    color = AquaTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    softWrap = false
                )
            }
        }
    }
}
