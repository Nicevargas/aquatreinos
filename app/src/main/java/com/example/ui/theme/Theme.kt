package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = AquaPrimary,
    secondary = AquaMagenta,
    tertiary = AquaGreen,
    background = AquaBackground,
    surface = AquaSurface,
    surfaceVariant = AquaSurfaceContainer,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AquaTextPrimary,
    onSurface = AquaTextPrimary,
    outline = AquaOutline,
    outlineVariant = AquaBorder
  )

/**
 * Sempre o tema claro com as cores da marca. As telas pintam fundos e cartões com
 * cores fixas (Aqua*): com cor dinâmica (Android 12+) ou com o celular no modo escuro,
 * campos e botões do Material ganhavam texto claro sobre esses fundos claros e sumiam.
 */
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
