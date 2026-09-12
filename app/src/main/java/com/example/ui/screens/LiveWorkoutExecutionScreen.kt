package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Workout
import com.example.ui.components.AppTopBar
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainer
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.LiveWorkoutUiState

@Composable
fun LiveWorkoutExecutionScreen(
    workout: Workout,
    liveState: LiveWorkoutUiState,
    onCloseClick: () -> Unit,
    onTogglePauseClick: () -> Unit,
    onNextSetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Format elapsed seconds to 00:42:15
    val hours = liveState.elapsedSeconds / 3600
    val minutes = (liveState.elapsedSeconds % 3600) / 60
    val seconds = liveState.elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    // Calculate progress percentage
    val progressFraction = (liveState.completedMeters.toFloat() / liveState.totalMeters.toFloat())
        .coerceIn(0f, 1f)
    val progressPercent = (progressFraction * 100).toInt()

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Encerrar Treino?", fontWeight = FontWeight.Bold) },
            text = { Text("Você registrou ${(liveState.completedMeters / 1000f)}km até agora. Deseja sair da execução?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onCloseClick()
                    }
                ) {
                    Text("Sim, Sair", color = AquaMagenta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar Treinando", color = AquaPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                isExecutionMode = true,
                onCloseClick = { showExitDialog = true }
            )
        },
        bottomBar = {
            ExecutionBottomBar(
                isTimerRunning = liveState.isTimerRunning,
                isFinished = liveState.isFinished,
                onTogglePause = onTogglePauseClick,
                onNextSet = onNextSetClick
            )
        },
        containerColor = Color(0xFFF9F9FF),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Timer & Progress Segment
            TimerProgressHeader(
                formattedTime = formattedTime,
                isTimerRunning = liveState.isTimerRunning,
                progressFraction = progressFraction,
                progressPercent = progressPercent,
                completedMeters = liveState.completedMeters
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Vertical Timeline Execution
            PhaseExecutionTimeline(
                currentSetNumber = liveState.currentSetNumber,
                totalSets = liveState.totalSetsInPhase
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Motivational Banner
            MotivationalAquagendaBanner(tip = workout.motivationalTip)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TimerProgressHeader(
    formattedTime: String,
    isTimerRunning: Boolean,
    progressFraction: Float,
    progressPercent: Int,
    completedMeters: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label with pulsing dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isTimerRunning) AquaMagenta else Color.Gray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TEMPO DE TREINO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AquaMagenta,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Big Digital Timer
        Text(
            text = formattedTime,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = AquaPrimary,
            letterSpacing = (-1).sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFDEE8FF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AquaPrimary, AquaCyan, AquaMagenta)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Distance and Total Progress Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(completedMeters / 1000f)}km",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AquaPrimary
            )

            Row {
                Text(
                    text = "Progresso Total: ",
                    fontSize = 12.sp,
                    color = AquaTextSecondary
                )
                Text(
                    text = "$progressPercent%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaMagenta
                )
            }
        }
    }
}

@Composable
private fun PhaseExecutionTimeline(
    currentSetNumber: Int,
    totalSets: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Phase 1: Aquecimento (Concluído)
        TimelinePhaseItem(
            title = "Aquecimento",
            statusLabel = "CONCLUÍDO",
            isCompleted = true,
            isActive = false,
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AquaSurfaceContainerLow.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "400m Crawl Relaxado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AquaTextSecondary,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Phase 2: Preparatória (Concluído)
        TimelinePhaseItem(
            title = "Preparatória",
            statusLabel = "CONCLUÍDO",
            isCompleted = true,
            isActive = false,
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AquaSurfaceContainerLow.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "200m Educativos Medley",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AquaTextSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Phase 3: Principal (ACTIVE!)
        TimelinePhaseItem(
            title = "Principal",
            statusLabel = "Série $currentSetNumber/$totalSets",
            isCompleted = false,
            isActive = true,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Set Card 1 (Active)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = AquaPrimary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, AquaPrimary.copy(alpha = 0.3f))
                    ) {
                        Column {
                            // Top rainbow accent
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AquaPrimary, AquaCyan, AquaMagenta)
                                        )
                                    )
                            )

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = "8x100",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AquaPrimary,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "Crawl com Palmar",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AquaTextPrimary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    // Equipment tag
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AquaPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FrontHand,
                                            contentDescription = "Palmar",
                                            tint = AquaPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Interval and Intensity Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = AquaBorder.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = AquaPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sair para ",
                                            fontSize = 12.sp,
                                            color = AquaTextSecondary
                                        )
                                        Text(
                                            text = "1'45\"",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AquaTextPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Speed,
                                            contentDescription = null,
                                            tint = AquaMagenta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Intensidade: ",
                                            fontSize = 12.sp,
                                            color = AquaTextSecondary
                                        )
                                        Text(
                                            text = "Z3 (75%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AquaMagenta
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Rest Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(AquaBorder)
                        )

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(AquaPinkBg)
                                .border(1.dp, AquaMagenta.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = AquaMagenta,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Intervalo: 30s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AquaMagenta
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(AquaBorder)
                        )
                    }

                    // Set Card 2 (Upcoming)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "1x600",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AquaTextSecondary
                                )
                                Text(
                                    text = "Crawl completo com Nadadeira",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AquaTextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AquaSurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pool,
                                    contentDescription = "Nadadeira",
                                    tint = AquaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Phase 4: Final (Pending)
        TimelinePhaseItem(
            title = "Final",
            statusLabel = "Aguardando",
            isCompleted = false,
            isActive = false,
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AquaSurfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
                ) {
                    Text(
                        text = "200m Soltura / Alongamento",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AquaTextMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun TimelinePhaseItem(
    title: String,
    statusLabel: String,
    isCompleted: Boolean,
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon step circle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            isActive -> Modifier
                                .background(
                                    Brush.linearGradient(listOf(AquaPrimary, AquaCyan))
                                )
                                .border(3.dp, Color.White, CircleShape)
                                .shadow(8.dp, CircleShape)
                            isCompleted -> Modifier
                                .background(Color.White)
                                .border(2.dp, Color(0xFF34D399), CircleShape)
                            else -> Modifier
                                .background(AquaSurfaceContainer)
                                .border(2.dp, AquaBorder, CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isActive -> Icon(
                        imageVector = Icons.Default.Pool,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    isCompleted -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Concluído",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(22.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = AquaTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Header & Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = if (isActive) 19.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isActive -> AquaPrimary
                        isCompleted -> Color(0xFF065F46)
                        else -> AquaTextMuted
                    }
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            when {
                                isActive -> AquaMagenta
                                isCompleted -> Color(0xFFD1FAE5)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isActive -> Color.White
                            isCompleted -> Color(0xFF047857)
                            else -> AquaTextMuted
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            content()
        }
    }
}

@Composable
private fun MotivationalAquagendaBanner(tip: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = AquaPrimary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(AquaPrimary, AquaCyan))
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Mantenha a técnica na fase principal!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = tip,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutionBottomBar(
    isTimerRunning: Boolean,
    isFinished: Boolean,
    onTogglePause: () -> Unit,
    onNextSet: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pausar / Continuar Button
            OutlinedButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("pause_resume_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AquaSurfaceContainerLow
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AquaMagenta,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTimerRunning) "Pausar" else "Retomar",
                        color = AquaTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Próximo Set Button
            Button(
                onClick = onNextSet,
                modifier = Modifier
                    .weight(1.3f)
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = AquaPrimary.copy(alpha = 0.3f))
                    .testTag("next_set_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(AquaPrimary, Color(0xFF0070E6), AquaMagenta)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isFinished) "Finalizar Treino" else "Próximo Set",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
