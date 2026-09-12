package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompletedSetRecord
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBlueText
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPinkText
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow
import com.example.ui.theme.AquaYellowBg
import com.example.ui.theme.AquaYellowText
import java.util.Locale

enum class SwimChartType {
    LINE_PACE,
    BAR_TIME
}

@Composable
fun SwimSetHistoryChartCard(
    completedLaps: List<CompletedSetRecord>,
    modifier: Modifier = Modifier,
    targetPaceSeconds: Float = 84f // Target 1'24" per 100m
) {
    var chartType by remember { mutableStateOf(SwimChartType.LINE_PACE) }
    // Sort chronological: Set 1, Set 2, Set 3, Set 4...
    val chronologicalLaps = remember(completedLaps) {
        completedLaps.sortedBy { it.setNumber }
    }

    var selectedSetNumber by remember(chronologicalLaps) {
        mutableStateOf<Int?>(chronologicalLaps.lastOrNull()?.setNumber)
    }

    val selectedLap = remember(selectedSetNumber, chronologicalLaps) {
        chronologicalLaps.find { it.setNumber == selectedSetNumber } ?: chronologicalLaps.lastOrNull()
    }

    // Best lap (lowest time)
    val bestLap = remember(chronologicalLaps) {
        chronologicalLaps.minByOrNull { if (it.timeMillis > 0) it.timeMillis else Long.MAX_VALUE }
    }

    // Average time in millis
    val averageMillis = remember(chronologicalLaps) {
        val validLaps = chronologicalLaps.filter { it.timeMillis > 0 }
        if (validLaps.isNotEmpty()) validLaps.map { it.timeMillis }.average().toLong() else 0L
    }

    val averageTimeFormatted = remember(averageMillis) {
        if (averageMillis > 0) {
            val totalSec = averageMillis / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val tenths = (averageMillis % 1000) / 100
            String.format(Locale.US, "%02d:%02d.%d", min, sec, tenths)
        } else {
            "--:--"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = AquaPrimary.copy(alpha = 0.12f)
            )
            .testTag("swim_set_history_chart_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Decorative aquatic accent stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(AquaCyan, AquaPrimary, AquaGreen, AquaYellow)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header: Title and Chart Type Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AquaBlueBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (chartType == SwimChartType.LINE_PACE) {
                                    Icons.Filled.ShowChart
                                } else {
                                    Icons.Filled.BarChart
                                },
                                contentDescription = "Gráfico",
                                tint = AquaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "HISTÓRICO DE SÉRIES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AquaTextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Análise de Ritmo por Série",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AquaTextPrimary
                            )
                        }
                    }

                    // Chart Mode Toggle: Linha vs Barras
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(AquaSurfaceContainerLow)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (chartType == SwimChartType.LINE_PACE) AquaPrimary else Color.Transparent)
                                .clickable { chartType = SwimChartType.LINE_PACE }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Linha",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartType == SwimChartType.LINE_PACE) Color.White else AquaTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (chartType == SwimChartType.BAR_TIME) AquaPrimary else Color.Transparent)
                                .clickable { chartType = SwimChartType.BAR_TIME }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Barras",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartType == SwimChartType.BAR_TIME) Color.White else AquaTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (chronologicalLaps.isEmpty()) {
                    // Empty state when no laps completed yet
                    EmptyChartState()
                } else {
                    // Metrics Strip: Best Lap, Average Time, Constancy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AquaSurfaceContainerLow)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Melhor Série
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = AquaYellowText,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Melhor Série",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AquaTextSecondary
                                )
                            }
                            Text(
                                text = bestLap?.let { "S${it.setNumber} • ${it.timeFormatted}" } ?: "--",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AquaGreenText
                            )
                        }

                        // Média Geral
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = AquaPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Média Parcial",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AquaTextSecondary
                                )
                            }
                            Text(
                                text = averageTimeFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AquaPrimary
                            )
                        }

                        // Total de Séries
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = AquaMagenta,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Séries Feitas",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AquaTextSecondary
                                )
                            }
                            Text(
                                text = "${chronologicalLaps.size} concluídas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AquaMagenta
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // The Canvas Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AquaSurfaceContainerLow.copy(alpha = 0.5f))
                            .border(1.dp, AquaBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        if (chartType == SwimChartType.LINE_PACE) {
                            SwimLinePaceCanvas(
                                laps = chronologicalLaps,
                                selectedSetNumber = selectedSetNumber,
                                onSelectLap = { selectedSetNumber = it },
                                targetSeconds = targetPaceSeconds
                            )
                        } else {
                            SwimBarTimeCanvas(
                                laps = chronologicalLaps,
                                selectedSetNumber = selectedSetNumber,
                                onSelectLap = { selectedSetNumber = it },
                                averageMillis = averageMillis
                            )
                        }
                    }

                    // Legend & Target Guidance Row
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AquaPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (chartType == SwimChartType.LINE_PACE) "Tempo da série" else "Série concluída",
                                fontSize = 10.sp,
                                color = AquaTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.dp)
                                    .background(AquaYellow)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Meta ritmo: 1'24\"",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AquaYellowText
                            )
                        }

                        Text(
                            text = "Toque em um ponto para detalhes",
                            fontSize = 10.sp,
                            color = AquaTextMuted
                        )
                    }

                    // Interactive Tooltip Card for Selected Lap
                    if (selectedLap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SelectedLapDetailCard(
                            lap = selectedLap,
                            averageMillis = averageMillis,
                            isBest = selectedLap.setNumber == bestLap?.setNumber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChartState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AquaBlueBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AquaPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Nenhuma série registrada ainda",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AquaTextPrimary
        )
        Text(
            text = "Inicie o cronômetro e toque em 'Salvar Série' para ver seu gráfico de ritmo!",
            fontSize = 11.sp,
            color = AquaTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SelectedLapDetailCard(
    lap: CompletedSetRecord,
    averageMillis: Long,
    isBest: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isBest) AquaGreenBg else AquaBlueBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isBest) AquaGreen.copy(alpha = 0.4f) else AquaPrimary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isBest) AquaGreen else AquaPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S${lap.setNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Série ${lap.setNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaTextPrimary
                        )
                        if (isBest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MELHOR VOLTA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AquaGreenText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Pace: ${lap.pacePer100m} • Split: ${lap.splitDifference}",
                        fontSize = 11.sp,
                        color = AquaTextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = lap.timeFormatted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isBest) AquaGreenText else AquaPrimary,
                    fontFamily = FontFamily.Monospace
                )

                if (lap.timeMillis > 0 && averageMillis > 0) {
                    val diffFromAvg = (lap.timeMillis - averageMillis).toDouble() / 1000.0
                    val diffText = if (diffFromAvg < 0) {
                        String.format(Locale.US, "%.1fs melhor que a média", -diffFromAvg)
                    } else if (diffFromAvg > 0) {
                        String.format(Locale.US, "+%.1fs que a média", diffFromAvg)
                    } else {
                        "Exatamente na média"
                    }
                    Text(
                        text = diffText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (diffFromAvg <= 0) AquaGreenText else AquaMagenta
                    )
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose Canvas rendering a sleek line graph with cubic bezier curve,
 * gradient shadow, target line, and interactive data markers.
 */
@Composable
private fun SwimLinePaceCanvas(
    laps: List<CompletedSetRecord>,
    selectedSetNumber: Int?,
    onSelectLap: (Int) -> Unit,
    targetSeconds: Float
) {
    val timesSeconds = remember(laps) {
        laps.map { if (it.timeMillis > 0) it.timeMillis.toFloat() / 1000f else 82f }
    }

    val minVal = remember(timesSeconds, targetSeconds) {
        val minTime = timesSeconds.minOrNull() ?: 80f
        minOf(minTime, targetSeconds) - 2f
    }

    val maxVal = remember(timesSeconds, targetSeconds) {
        val maxTime = timesSeconds.maxOrNull() ?: 86f
        maxOf(maxTime, targetSeconds) + 2f
    }

    val valueRange = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(laps) {
                detectTapGestures { offset ->
                    if (laps.isEmpty()) return@detectTapGestures
                    val pointSpacing = size.width / (laps.size.coerceAtLeast(1) + 1)
                    val clickedIndex = ((offset.x / pointSpacing) - 0.5f).toInt()
                    val clampedIndex = clickedIndex.coerceIn(0, laps.size - 1)
                    onSelectLap(laps[clampedIndex].setNumber)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingHorizontal = width / (laps.size.coerceAtLeast(1) + 1)
        val paddingTop = 24.dp.toPx()
        val paddingBottom = 26.dp.toPx()
        val usableHeight = height - paddingTop - paddingBottom

        // Draw horizontal grid guide lines (3 lines)
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = paddingTop + (usableHeight * (i.toFloat() / gridLines))
            drawLine(
                color = AquaBorder.copy(alpha = 0.6f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        // Draw Target Pace Reference Line (Amber dashed)
        val targetY = paddingTop + usableHeight * (1f - ((targetSeconds - minVal) / valueRange))
        if (targetY in paddingTop..(paddingTop + usableHeight)) {
            drawLine(
                color = AquaYellow,
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
        }

        // Calculate (x, y) coordinates for each lap
        val points = laps.mapIndexed { index, lap ->
            val x = (index + 1) * paddingHorizontal
            val timeSec = if (lap.timeMillis > 0) lap.timeMillis.toFloat() / 1000f else 82f
            // Lower time = higher on chart (faster is better)
            val normalized = 1f - ((timeSec - minVal) / valueRange).coerceIn(0f, 1f)
            val y = paddingTop + (usableHeight * normalized)
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            // Build smooth cubic bezier curve
            val strokePath = Path()
            val fillPath = Path()

            strokePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, height - paddingBottom)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                strokePath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }

            fillPath.lineTo(points.last().x, height - paddingBottom)
            fillPath.close()

            // Draw gradient area under the curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AquaCyan.copy(alpha = 0.35f),
                        AquaPrimary.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = height - paddingBottom
                )
            )

            // Draw line curve
            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(AquaCyan, AquaPrimary, Color(0xFF005DA7))
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw Data Points and Labels
            points.forEachIndexed { index, point ->
                val lap = laps[index]
                val isSelected = lap.setNumber == selectedSetNumber

                // Selected outer pulse circle
                if (isSelected) {
                    drawCircle(
                        color = AquaCyan.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = point
                    )
                }

                // White backdrop circle
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                    center = point
                )

                // Colored inner center
                drawCircle(
                    color = if (isSelected) AquaPrimary else AquaCyan,
                    radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                    center = point
                )

                // X-Axis Series Label (S1, S2, S3...)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (isSelected) 0xFF0076D1.toInt() else 0xFF707785.toInt()
                        textSize = 28f
                        isFakeBoldText = isSelected
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText("S${lap.setNumber}", point.x, height - 6.dp.toPx(), paint)
                }

                // Above point: Short time tag (e.g. 1:21.2)
                drawContext.canvas.nativeCanvas.apply {
                    val timePaint = android.graphics.Paint().apply {
                        color = if (isSelected) 0xFF0076D1.toInt() else 0xFF111C2D.toInt()
                        textSize = 24f
                        isFakeBoldText = isSelected
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val shortLabel = lap.timeFormatted.replace("^0".toRegex(), "")
                    drawText(shortLabel, point.x, point.y - 10.dp.toPx(), timePaint)
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose Canvas rendering a modern vertical bar chart
 * with rounded bar caps, subtle background tracks, and performance color cues.
 */
@Composable
private fun SwimBarTimeCanvas(
    laps: List<CompletedSetRecord>,
    selectedSetNumber: Int?,
    onSelectLap: (Int) -> Unit,
    averageMillis: Long
) {
    val timesMillis = remember(laps) {
        laps.map { if (it.timeMillis > 0) it.timeMillis else 82000L }
    }

    val maxMillis = remember(timesMillis) {
        val maxT = timesMillis.maxOrNull() ?: 85000L
        maxT + 2000L
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(laps) {
                detectTapGestures { offset ->
                    if (laps.isEmpty()) return@detectTapGestures
                    val barSlotWidth = size.width / laps.size.coerceAtLeast(1)
                    val clickedIndex = (offset.x / barSlotWidth).toInt().coerceIn(0, laps.size - 1)
                    onSelectLap(laps[clickedIndex].setNumber)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val barSlotWidth = width / laps.size.coerceAtLeast(1)
        val barWidth = (barSlotWidth * 0.48f).coerceIn(16.dp.toPx(), 42.dp.toPx())
        val paddingTop = 26.dp.toPx()
        val paddingBottom = 26.dp.toPx()
        val usableHeight = height - paddingTop - paddingBottom

        // Draw background average baseline line
        if (averageMillis > 0) {
            val avgFraction = (averageMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.1f, 1f)
            val avgY = height - paddingBottom - (usableHeight * avgFraction)
            drawLine(
                color = AquaPrimary.copy(alpha = 0.5f),
                start = Offset(0f, avgY),
                end = Offset(width, avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        laps.forEachIndexed { index, lap ->
            val isSelected = lap.setNumber == selectedSetNumber
            val centerX = (index * barSlotWidth) + (barSlotWidth / 2f)
            val left = centerX - (barWidth / 2f)

            val lapMillis = if (lap.timeMillis > 0) lap.timeMillis else 82000L
            val fraction = (lapMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.15f, 1f)
            val barHeight = usableHeight * fraction
            val top = height - paddingBottom - barHeight

            // Background full track column
            drawRoundRect(
                color = AquaSurfaceContainerLow,
                topLeft = Offset(left, paddingTop),
                size = Size(barWidth, usableHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Performance based gradient: Faster than average = Green/Cyan, Slower = Primary/Magenta
            val isFasterThanAvg = lapMillis <= averageMillis
            val barBrush = Brush.verticalGradient(
                colors = if (isFasterThanAvg) {
                    listOf(AquaGreen, AquaCyan)
                } else {
                    listOf(AquaPrimary, Color(0xFF005DA7))
                },
                startY = top,
                endY = height - paddingBottom
            )

            // Value Bar
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Selected Highlight border
            if (isSelected) {
                drawRoundRect(
                    color = AquaPrimary,
                    topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                    size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Above bar: Exact time label
            drawContext.canvas.nativeCanvas.apply {
                val timePaint = android.graphics.Paint().apply {
                    color = if (isSelected) 0xFF0076D1.toInt() else 0xFF111C2D.toInt()
                    textSize = 24f
                    isFakeBoldText = isSelected
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val shortLabel = lap.timeFormatted.replace("^0".toRegex(), "")
                drawText(shortLabel, centerX, top - 8.dp.toPx(), timePaint)
            }

            // Bottom: Series label (S1, S2, S3...)
            drawContext.canvas.nativeCanvas.apply {
                val labelPaint = android.graphics.Paint().apply {
                    color = if (isSelected) 0xFF0076D1.toInt() else 0xFF707785.toInt()
                    textSize = 28f
                    isFakeBoldText = isSelected
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText("S${lap.setNumber}", centerX, height - 6.dp.toPx(), labelPaint)
            }
        }
    }
}
