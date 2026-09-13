package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.AquagendaConstants
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPinkBg
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainer
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow
import com.example.ui.theme.AquaYellowBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onDownloadWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    cabecalho: @Composable () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Minha conta (perfil, sair, excluir)
        cabecalho()

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Circular Chart Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = AquaPrimary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Gauge
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        // Background track
                        drawCircle(
                            color = Color(0xFFEDF2FC),
                            radius = (size.minDimension - strokeWidth) / 2,
                            style = Stroke(width = strokeWidth)
                        )

                        // Gradient Arc
                        val gradientBrush = Brush.sweepGradient(
                            colors = listOf(
                                AquaGreen,
                                AquaCyan,
                                AquaMagenta,
                                AquaGreen
                            )
                        )

                        drawArc(
                            brush = gradientBrush,
                            startAngle = -90f,
                            sweepAngle = 300f, // 83% of 360 = ~300 deg
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "2500m",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = AquaTextPrimary,
                            letterSpacing = (-1).sp
                        )

                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(AquaPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "META: 3000M",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AquaPrimary,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Daily Performance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaTextPrimary
                )

                // Um texto só: em três pedaços, cada pedaço quebrava a linha separado.
                Text(
                    text = buildAnnotatedString {
                        append("Você atingiu ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AquaPrimary)) { append("83%") }
                        append(" da sua meta diária.")
                    },
                    fontSize = 13.sp,
                    color = AquaTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bento Grid Metrics (4 cards)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Duration
                PerformanceMetricCard(
                    title = "Total Duration",
                    value = "54:20",
                    unit = null,
                    icon = Icons.Outlined.Timer,
                    iconTint = AquaPrimary,
                    iconBg = AquaBlueBg,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Avg Heart Rate
                PerformanceMetricCard(
                    title = "Avg. Heart Rate",
                    value = "142",
                    unit = "bpm",
                    icon = Icons.Outlined.Favorite,
                    iconTint = AquaMagenta,
                    iconBg = AquaPinkBg,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avg Pace
                PerformanceMetricCard(
                    title = "Avg. Pace",
                    value = "1:45",
                    unit = "/100m",
                    icon = Icons.Outlined.Speed,
                    iconTint = AquaYellow,
                    iconBg = AquaYellowBg,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Sync Data Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onDownloadWorkoutClick)
                        .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = AquaPrimary.copy(alpha = 0.2f))
                        .testTag("download_workout_card"),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(AquaPrimary, AquaCyan, AquaMagenta)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Baixar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column {
                                Text(
                                    text = "Sync Data",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Baixar Treino",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Atual Phase Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STATUS ATUAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Fase: Main Set",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaTextPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(AquaPinkBg)
                            .border(1.dp, AquaMagenta.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Endurance",
                            color = AquaMagenta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-Segment Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(AquaSurfaceContainer),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.20f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                            .background(AquaGreen)
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.50f)
                            .height(10.dp)
                            .background(AquaMagenta)
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.30f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                            .background(AquaCyan.copy(alpha = 0.35f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legends
                // Legendas descem para a linha de baixo quando não cabem juntas.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PhaseLegend(color = AquaGreen, label = "Warm-up")
                    PhaseLegend(color = AquaMagenta, label = "Main Set (Ongoing)", isBold = true)
                    PhaseLegend(color = AquaCyan.copy(alpha = 0.5f), label = "Cool Down")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Workout Details Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workout Details",
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { }
            ) {
                Text(
                    text = "VIEW ANALYTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = AquaPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Set 1
            WorkoutDetailItem(
                number = 1,
                title = "Warm-up: Mixed Strokes",
                distance = "400m",
                description = "4 x 100m, Choice, RPE 3",
                status = DetailStatus.COMPLETED
            )

            // Set 2
            WorkoutDetailItem(
                number = 2,
                title = "Technical: Sculling & Drills",
                distance = "200m",
                description = "8 x 25m, Catch focus",
                status = DetailStatus.COMPLETED
            )

            // Set 3 (ACTIVE)
            WorkoutDetailItem(
                number = 3,
                title = "Main Set: Threshold Intervals",
                distance = "1500m / 2000m",
                description = "10 x 200m, Freestyle @ 3:15",
                status = DetailStatus.ACTIVE
            )

            // Set 4
            WorkoutDetailItem(
                number = 4,
                title = "Cool Down",
                distance = "400m",
                description = "Easy backstroke & stretching",
                status = DetailStatus.PENDING
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Highlight Card with direct swimmer photo URL
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(22.dp))
                .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = AquaPrimary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(AquagendaConstants.URL_SWIMMER_WEEKLY_HIGHLIGHT)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Destaque da semana",
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
                                .background(Color(0xFF003D9B))
                        )
                    }
                )

                // Overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF003D9B).copy(alpha = 0.5f),
                                    Color(0xFF002255).copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Highlight content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF7FFC97),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DESTAQUE DA SEMANA",
                            color = Color(0xFF7FFC97),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Text(
                        text = "Weekly Highlight",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = "Você melhorou seu pace nos 200m Livre em 4%.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // A barra de baixo já desconta a própria altura (innerPadding do Scaffold).
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PerformanceMetricCard(
    title: String,
    value: String,
    unit: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = AquaTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = value,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextPrimary
                    )
                    if (unit != null) {
                        Text(
                            text = " $unit",
                            fontSize = 12.sp,
                            color = AquaTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseLegend(
    color: Color,
    label: String,
    isBold: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) color else AquaTextSecondary
        )
    }
}

private enum class DetailStatus { COMPLETED, ACTIVE, PENDING }

@Composable
private fun WorkoutDetailItem(
    number: Int,
    title: String,
    distance: String,
    description: String,
    status: DetailStatus
) {
    val isActive = status == DetailStatus.ACTIVE
    val isCompleted = status == DetailStatus.COMPLETED

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) Color(0xFFFFF0F6) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (isActive) 1.5.dp else 1.dp,
            if (isActive) AquaMagenta else AquaBorder
        ),
        shadowElevation = if (isActive) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isActive -> AquaMagenta
                            isCompleted -> AquaGreenBg
                            else -> AquaSurfaceContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = when {
                        isActive -> Color.White
                        isCompleted -> AquaGreen
                        else -> AquaTextSecondary
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // weight no título: a distância fica inteira à direita em vez de quebrar letra por letra.
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) AquaMagenta else AquaTextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Text(
                        text = distance,
                        softWrap = false,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isActive -> AquaMagenta
                            isCompleted -> AquaGreen
                            else -> AquaTextSecondary
                        }
                    )
                }

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = AquaTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Icon
            when {
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Concluído",
                        tint = AquaGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                isActive -> {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AquaMagenta)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Circle,
                        contentDescription = "Pendente",
                        tint = Color(0xFFC0C7D5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
