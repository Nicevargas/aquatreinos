package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNavTab
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextSecondary

@Composable
fun BottomNavBar(
    selectedTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                spotColor = AquaPrimary.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        // Cinco abas com a mesma largura: cabem em celular estreito sem empurrar a última para fora.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Aba(AppNavTab.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
                Aba(AppNavTab.PLAN, "Plano", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_plan"),
                Aba(AppNavTab.WORKOUTS, "Treinos", Icons.Filled.Pool, Icons.Outlined.Pool, "nav_tab_workouts"),
                Aba(AppNavTab.MY_WORKOUTS, "Meus treinos", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_tab_my_workouts"),
                Aba(AppNavTab.PROFILE, "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
            ).forEach { aba ->
                NavBarItem(
                    label = aba.rotulo,
                    selectedIcon = aba.icone,
                    unselectedIcon = aba.iconeVazio,
                    isSelected = selectedTab == aba.tab,
                    testTag = aba.tag,
                    onClick = { onTabSelected(aba.tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class Aba(
    val tab: AppNavTab,
    val rotulo: String,
    val icone: ImageVector,
    val iconeVazio: ImageVector,
    val tag: String
)

@Composable
private fun NavBarItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) AquaBlueBg else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (isSelected) AquaPrimary else AquaTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AquaPrimary else AquaTextSecondary,
            // Duas linhas com letra grande: "Meus treinos" aparece inteiro em vez de "Meus tr…".
            maxLines = 2,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
