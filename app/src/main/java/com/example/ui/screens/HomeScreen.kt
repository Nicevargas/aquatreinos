package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.ui.components.corDoNivel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.AquagendaConstants
import com.example.model.CalendarDay
import com.example.model.StopwatchMode
import com.example.model.SwimSetStopwatchState
import com.example.model.TrainingLevel
import com.example.model.Workout
import com.example.ui.components.SwimSetHistoryChartCard
import com.example.ui.components.SwimSetStopwatchCard
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow

@Composable
fun HomeScreen(
    workout: Workout,
    selectedLevel: TrainingLevel,
    calendarDays: List<CalendarDay>,
    stopwatchState: SwimSetStopwatchState,
    onDayClick: (Long) -> Unit,
    onLevelChange: (TrainingLevel) -> Unit,
    onStartWorkoutClick: () -> Unit,
    onViewWorkoutDetails: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onStopwatchModeChange: (StopwatchMode) -> Unit,
    onStopwatchPrevSet: () -> Unit,
    onStopwatchNextSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Calendar Strip
        CalendarStrip(
            days = calendarDays,
            onDayClick = onDayClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Nível logo acima do treino: trocar o nível troca o cartão de baixo.
        TrainingLevelToggle(
            selectedLevel = selectedLevel,
            onLevelChange = onLevelChange
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Card with direct image link from HTML
        HeroSwimmerCard(
            workout = workout,
            onClick = onViewWorkoutDetails
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Info Cards (Distância & Tempo Estimado)
        WorkoutMetricBentoCards(workout = workout)

        Spacer(modifier = Modifier.height(16.dp))

        // O botão principal fica junto do treino que ele inicia, não depois do cronômetro e do gráfico.
        BotaoIniciarTreino(onClick = onStartWorkoutClick)

        Spacer(modifier = Modifier.height(24.dp))

        // Real-Time Swimming Set Stopwatch Component
        SwimSetStopwatchCard(
            stopwatchState = stopwatchState,
            onToggleStartPause = onToggleStopwatch,
            onLapSet = onLapStopwatch,
            onReset = onResetStopwatch,
            onModeChange = onStopwatchModeChange,
            onPreviousSet = onStopwatchPrevSet,
            onNextSet = onStopwatchNextSet
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Chart: Swim Set History (Line Pace / Bar Time)
        SwimSetHistoryChartCard(
            completedLaps = stopwatchState.completedLaps,
            targetPaceSeconds = 84f
        )

        Spacer(modifier = Modifier.height(20.dp))

        // A barra de baixo já desconta a própria altura (innerPadding do Scaffold).
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun BotaoIniciarTreino(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = AquaPrimary.copy(alpha = 0.35f)
            )
            .testTag("start_workout_cta"),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AquaPrimary,
                            Color(0xFF0070E6),
                            AquaMagenta
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Iniciar Treino",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun CalendarStrip(
    days: List<CalendarDay>,
    onDayClick: (Long) -> Unit
) {
    // Semana inteira (seg a dom) sem rolagem: o domingo não pode ficar escondido
    // fora da tela quando é o dia selecionado.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            val isSelected = day.isSelected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onDayClick(day.epochDay) }
                    .testTag("calendar_day_${day.dayNumber}"),
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) Color.Transparent else Color.White,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
                shadowElevation = if (isSelected) 6.dp else 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(AquaCyan, AquaPrimary)
                                    )
                                )
                            } else {
                                Modifier.background(Color.White)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day.dayOfWeek,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else AquaTextSecondary
                        )

                        Text(
                            text = day.dayNumber.toString(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else AquaTextPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (day.isToday && isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AquaYellow)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSwimmerCard(
    workout: Workout,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = AquaPrimary.copy(alpha = 0.15f))
            .testTag("hero_workout_card"),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Direct Image URL from the HTML using Coil
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(AquagendaConstants.URL_SWIMMER_HERO_HOME)
                    .crossfade(true)
                    .build(),
                contentDescription = "Nadador em treino de alta performance",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0C243B)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AquaCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                error = {
                    // Aesthetic aquatic gradient fallback
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

            // Cinematic gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0F172A).copy(alpha = 0.35f),
                                Color(0xFF0A0F1D).copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Content at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                // Category Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = workout.tag.uppercase(),
                        color = Color(0xFFA5F3FC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = workout.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = workout.subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutMetricBentoCards(workout: Workout) {
    // Altura pela maior: os dois cartões ficam iguais mesmo com a letra grande.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Distance Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = AquaPrimary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AquaBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Straighten,
                            contentDescription = "Distância",
                            tint = AquaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "DISTÂNCIA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextSecondary,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${workout.totalDistanceMeters}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = AquaTextPrimary,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "m",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaPrimary,
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                    )
                }
            }
        }

        // Duration Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = AquaPrimary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AquaPinkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Tempo Estimado",
                            tint = AquaMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "TEMPO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextSecondary,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${workout.estimatedMinutes}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = AquaTextPrimary,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "min",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaMagenta,
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingLevelToggle(
    selectedLevel: TrainingLevel,
    onLevelChange: (TrainingLevel) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .testTag("training_level_toggle"),
        shape = RoundedCornerShape(50.dp),
        color = Color(0xFFE8F1FC),
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            TrainingLevel.entries.forEach { level ->
                val isSelected = selectedLevel == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(Color.White)
                            } else {
                                Modifier.clickable { onLevelChange(level) }
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 2.dp)
                        .testTag("training_level_${level.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    // Bolinha em cima do nome: lado a lado, "Intermediário" não cabia no celular estreito.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(corDoNivel(level))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = level.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = if (isSelected) AquaPrimary else AquaTextSecondary
                        )
                    }
                }
            }
        }
    }
}
