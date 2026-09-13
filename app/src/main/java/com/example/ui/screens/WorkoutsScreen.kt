package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.model.Workout
import com.example.model.WorkoutPhase
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
    modifier: Modifier = Modifier
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
                        accentColor = when (phase.title) {
                            "Aquecimento" -> AquaGreen
                            "Principal" -> AquaPrimary
                            "Preparatória" -> AquaCyan
                            else -> AquaYellow
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

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

            Spacer(modifier = Modifier.height(100.dp))
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
                    Text(
                        text = set.header.ifBlank { "${set.repsDistance}m ${set.description}" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AquaTextPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    val cauda = (set.details + listOfNotNull(set.intervalTarget.takeIf { it.isNotBlank() }?.let { "Int: $it" }))
                        .joinToString(" · ")
                    if (cauda.isNotEmpty()) {
                        Text(
                            text = cauda,
                            fontSize = 12.sp,
                            color = AquaTextSecondary
                        )
                    }
                }
            }

            // Percentage pill badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        when (title) {
                            "Aquecimento" -> AquaGreenBg
                            "Principal" -> AquaBlueBg
                            else -> AquaYellowBg
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = percentage,
                    color = when (title) {
                        "Aquecimento" -> AquaGreenText
                        "Principal" -> AquaPrimary
                        else -> AquaYellowText
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
