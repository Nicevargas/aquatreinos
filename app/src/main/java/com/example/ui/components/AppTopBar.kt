package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextSecondary

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    isExecutionMode: Boolean = false,
    showUserAvatar: Boolean = false,
    onCloseClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, spotColor = AquaPrimary.copy(alpha = 0.08f)),
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Logo
            AquagendaLogo(
                height = 52.dp,
                modifier = Modifier.weight(1f)
            )

            // Right Actions
            if (isExecutionMode) {
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AquaSurfaceContainerLow)
                        .testTag("close_execution_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar Treino",
                        tint = AquaTextSecondary
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AquaSurfaceContainerLow)
                            .testTag("notification_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notificações",
                                tint = AquaTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            // Pink badge indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(AquaMagenta)
                            )
                        }
                    }

                    if (showUserAvatar) {
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(AquaPrimary, AquaCyan))
                                )
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JS",
                                color = AquaPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
