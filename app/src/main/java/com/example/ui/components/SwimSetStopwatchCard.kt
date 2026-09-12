package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompletedSetRecord
import com.example.model.StopwatchMode
import com.example.model.SwimSetStopwatchState
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import java.util.Locale

@Composable
fun SwimSetStopwatchCard(
    stopwatchState: SwimSetStopwatchState,
    onToggleStartPause: () -> Unit,
    onLapSet: () -> Unit,
    onReset: () -> Unit,
    onModeChange: (StopwatchMode) -> Unit,
    onPreviousSet: () -> Unit,
    onNextSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHistoryExpanded by remember { mutableStateOf(true) }

    // Pulsing animation for active live recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Calculate time breakdown
    val totalSeconds = stopwatchState.elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val deciseconds = (stopwatchState.elapsedMillis % 1000) / 100

    val minutesFormatted = String.format(Locale.US, "%02d", minutes)
    val secondsFormatted = String.format(Locale.US, "%02d", seconds)
    val deciFormatted = deciseconds.toString()

    // Interval progress calculation
    val targetSeconds = if (stopwatchState.mode == StopwatchMode.SERIE) {
        stopwatchState.targetIntervalSeconds.toFloat()
    } else {
        stopwatchState.restDurationSeconds.toFloat()
    }
    val progressFraction = (totalSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = AquaPrimary.copy(alpha = 0.12f)
            )
            .testTag("swim_set_stopwatch_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Top decorative aquatic accent banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (stopwatchState.mode == StopwatchMode.SERIE) {
                                listOf(AquaPrimary, AquaCyan, Color(0xFF0070E6))
                            } else {
                                listOf(AquaMagenta, Color(0xFFFF7597), AquaCyan)
                            }
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Row: Badge, Title, and Live Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (stopwatchState.mode == StopwatchMode.SERIE) AquaBlueBg else AquaPinkBg
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (stopwatchState.mode == StopwatchMode.SERIE) {
                                    Icons.Outlined.Timer
                                } else {
                                    Icons.Default.HourglassTop
                                },
                                contentDescription = "Cronômetro",
                                tint = if (stopwatchState.mode == StopwatchMode.SERIE) AquaPrimary else AquaMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "CRONÔMETRO DE SÉRIE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AquaTextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Monitoramento em Tempo Real",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AquaTextPrimary
                            )
                        }
                    }

                    // Live Status Pill
                    val statusText = when {
                        stopwatchState.isRunning -> "AO VIVO"
                        stopwatchState.elapsedMillis > 0 -> "PAUSADO"
                        else -> "PRONTO"
                    }
                    val statusBg = when {
                        stopwatchState.isRunning -> AquaGreenBg
                        stopwatchState.elapsedMillis > 0 -> Color(0xFFFEF3C7)
                        else -> AquaSurfaceContainerLow
                    }
                    val statusColor = when {
                        stopwatchState.isRunning -> AquaGreenText
                        stopwatchState.elapsedMillis > 0 -> Color(0xFFB45309)
                        else -> AquaTextSecondary
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (stopwatchState.isRunning) {
                                        AquaGreen.copy(alpha = pulseAlpha)
                                    } else {
                                        statusColor
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Series Navigator and Mode Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Series Badge with step arrows
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AquaSurfaceContainerLow)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        IconButton(
                            onClick = onPreviousSet,
                            enabled = stopwatchState.currentSetNumber > 1,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("stopwatch_prev_set")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Série anterior",
                                tint = if (stopwatchState.currentSetNumber > 1) AquaPrimary else AquaTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Série ${stopwatchState.currentSetNumber} / ${stopwatchState.totalSets}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = onNextSet,
                            enabled = stopwatchState.currentSetNumber < stopwatchState.totalSets,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("stopwatch_next_set")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Próxima série",
                                tint = if (stopwatchState.currentSetNumber < stopwatchState.totalSets) AquaPrimary else AquaTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Mode Toggle: Nado vs Descanso
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(AquaSurfaceContainerLow)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSerie = stopwatchState.mode == StopwatchMode.SERIE
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSerie) AquaPrimary else Color.Transparent)
                                .clickable { onModeChange(StopwatchMode.SERIE) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSerie) Color.White else AquaTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (!isSerie) AquaMagenta else Color.Transparent)
                                .clickable { onModeChange(StopwatchMode.DESCANSO) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Descanso",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSerie) Color.White else AquaTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Display Center Panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = AquaSurfaceContainerLow.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (stopwatchState.mode == StopwatchMode.SERIE) {
                                "${stopwatchState.setRepDescription} • Sair a cada 1'45\""
                            } else {
                                "Intervalo de Descanso • 30 segundos"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AquaTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Monospaced Tabular Digits Display
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$minutesFormatted:$secondsFormatted",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = if (stopwatchState.mode == StopwatchMode.SERIE) AquaPrimary else AquaMagenta,
                                letterSpacing = (-1).sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = ".$deciFormatted",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (stopwatchState.mode == StopwatchMode.SERIE) AquaCyan else AquaMagenta.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Gauge Bar (Relative to target interval)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (stopwatchState.mode == StopwatchMode.SERIE) {
                                                listOf(AquaCyan, AquaPrimary)
                                            } else {
                                                listOf(AquaMagenta, Color(0xFFFF7597))
                                            }
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtext Pace Target
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = AquaTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val paceInfo = if (totalSeconds > 5) {
                                val currentPaceSec = ((totalSeconds.toDouble() / stopwatchState.setDistanceMeters) * 100).toInt()
                                val pMin = currentPaceSec / 60
                                val pSec = currentPaceSec % 60
                                String.format(Locale.US, "Ritmo atual: %02d'%02d\"/100m", pMin, pSec)
                            } else {
                                "Meta de Ritmo: 1'24\"/100m (Sair no 1'45\")"
                            }
                            Text(
                                text = paceInfo,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AquaTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stopwatch_reset_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AquaSurfaceContainerLow
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Zerar cronômetro",
                            tint = AquaTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Primary Play/Pause Button
                    Button(
                        onClick = onToggleStartPause,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .shadow(
                                6.dp,
                                RoundedCornerShape(14.dp),
                                spotColor = AquaPrimary.copy(alpha = 0.3f)
                            )
                            .testTag("stopwatch_play_pause_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        if (stopwatchState.isRunning) {
                                            listOf(Color(0xFFE11D48), AquaMagenta)
                                        } else {
                                            listOf(AquaPrimary, Color(0xFF0070E6))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (stopwatchState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (stopwatchState.isRunning) "Pausar" else "Iniciar Série",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Record Set / Lap Button
                    Button(
                        onClick = onLapSet,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("stopwatch_lap_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AquaBlueBg,
                            contentColor = AquaPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = AquaPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Salvar Série",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AquaPrimary
                            )
                        }
                    }
                }

                // Completed Sets History Section
                if (stopwatchState.completedLaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHistoryExpanded = !showHistoryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AquaGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SÉRIES REGISTRADAS (${stopwatchState.completedLaps.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AquaTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = if (showHistoryExpanded) "Recolher" else "Expandir",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AquaPrimary
                        )
                    }

                    AnimatedVisibility(
                        visible = showHistoryExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stopwatchState.completedLaps.take(4).forEach { lap ->
                                CompletedLapRow(lap = lap)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedLapRow(lap: CompletedSetRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AquaSurfaceContainerLow)
            .border(1.dp, AquaBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, AquaBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${lap.setNumber}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AquaPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Série ${lap.setNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaTextPrimary
                )
                Text(
                    text = lap.pacePer100m,
                    fontSize = 11.sp,
                    color = AquaTextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (lap.splitDifference.isNotEmpty() && lap.splitDifference != "Base") {
                val isFaster = lap.splitDifference.startsWith("-")
                Text(
                    text = lap.splitDifference,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFaster) AquaGreenText else AquaMagenta,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFaster) AquaGreenBg else AquaPinkBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = lap.timeFormatted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = AquaTextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
