package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseStatus
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaGreenBg
import com.example.ui.theme.AquaGreenText
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.ui.theme.AquaYellow
import com.example.ui.theme.AquaYellowBg
import com.example.ui.theme.AquaYellowText

@Composable
fun SupabaseSyncBar(
    status: SupabaseStatus,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showDialog = true }
            .testTag("supabase_sync_bar"),
        shape = RoundedCornerShape(16.dp),
        color = when (status) {
            SupabaseStatus.CONNECTED -> AquaGreenBg
            SupabaseStatus.CONFIG_NEEDED -> AquaBlueBg
            SupabaseStatus.OFFLINE_LOCAL -> AquaYellowBg
            SupabaseStatus.CONNECTING -> AquaSurfaceContainerLow
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (status) {
                SupabaseStatus.CONNECTED -> AquaGreen.copy(alpha = 0.35f)
                SupabaseStatus.CONFIG_NEEDED -> AquaPrimary.copy(alpha = 0.25f)
                SupabaseStatus.OFFLINE_LOCAL -> AquaYellow.copy(alpha = 0.35f)
                SupabaseStatus.CONNECTING -> AquaBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status Icon with Pulse / Indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                SupabaseStatus.CONNECTED -> AquaGreen
                                SupabaseStatus.CONFIG_NEEDED -> AquaPrimary
                                SupabaseStatus.OFFLINE_LOCAL -> AquaYellow
                                SupabaseStatus.CONNECTING -> AquaPrimary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            SupabaseStatus.CONNECTED -> Icons.Default.CloudDone
                            SupabaseStatus.CONFIG_NEEDED -> Icons.Default.Storage
                            SupabaseStatus.OFFLINE_LOCAL -> Icons.Default.CloudQueue
                            SupabaseStatus.CONNECTING -> Icons.Default.Cloud
                        },
                        contentDescription = "Status Supabase",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (status) {
                                SupabaseStatus.CONNECTED -> "Supabase Conectado"
                                SupabaseStatus.CONFIG_NEEDED -> "Banco Supabase Configurado"
                                SupabaseStatus.OFFLINE_LOCAL -> "Modo Offline (Cache)"
                                SupabaseStatus.CONNECTING -> "Conectando ao Supabase..."
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                SupabaseStatus.CONNECTED -> AquaGreenText
                                SupabaseStatus.CONFIG_NEEDED -> AquaPrimary
                                SupabaseStatus.OFFLINE_LOCAL -> AquaYellowText
                                SupabaseStatus.CONNECTING -> AquaTextPrimary
                            }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (status) {
                                        SupabaseStatus.CONNECTED -> AquaGreen
                                        SupabaseStatus.CONFIG_NEEDED -> AquaPrimary
                                        SupabaseStatus.OFFLINE_LOCAL -> AquaYellow
                                        SupabaseStatus.CONNECTING -> AquaCyan
                                    }
                                )
                        )
                    }

                    Text(
                        text = when (status) {
                            SupabaseStatus.CONNECTED -> "Treinos & séries salvos na nuvem"
                            SupabaseStatus.CONFIG_NEEDED -> "Migrações SQL prontas • Toque p/ detalhes"
                            SupabaseStatus.OFFLINE_LOCAL -> "Sincronização pendente • Toque p/ reconectar"
                            SupabaseStatus.CONNECTING -> "Verificando endpoints REST..."
                        },
                        fontSize = 11.sp,
                        color = AquaTextSecondary
                    )
                }
            }

            // Sync Button
            IconButton(
                onClick = onSyncClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sincronizar",
                    tint = AquaPrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .then(if (isSyncing) Modifier.rotate(angle) else Modifier)
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = AquaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Conexão com Supabase",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "O aplicativo Aquagenda possui arquitetura híbrida com suporte completo ao Supabase e migrações SQL:",
                        fontSize = 13.sp,
                        color = AquaTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Migrations status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = AquaGreenText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Migrações SQL geradas em /supabase/migrations",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AquaTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Tabelas: profiles, workouts, swim_set_records, swimmer_stats\n• Segurança RLS & triggers de updated_at inclusos",
                        fontSize = 11.sp,
                        color = AquaTextMuted,
                        modifier = Modifier.padding(start = 22.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Configuração das Chaves:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = AquaSurfaceContainerLow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "SUPABASE_URL: ${if (SupabaseClient.isConfigured) SupabaseClient.supabaseUrl else "Aguardando chave real"}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AquaTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SUPABASE_ANON_KEY: ${if (SupabaseClient.isConfigured) "Configurada (●●●●●●●●)" else "Aguardando chave real"}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AquaTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Para conectar a um projeto ativo, adicione as variáveis no painel Secrets do AI Studio ou no arquivo .env.",
                        fontSize = 11.sp,
                        color = AquaTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSyncClick()
                        showDialog = false
                    }
                ) {
                    Text(
                        text = "Testar Conexão Agora",
                        color = AquaPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Fechar", color = AquaTextSecondary)
                }
            }
        )
    }
}
