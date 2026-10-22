package com.marco.vinyl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object VinylColors {
    val Background = Color(0xFF141110)
    val Surface = Color(0xFF221C19)
    val Brass = Color(0xFFD9A55B)
    val Cream = Color(0xFFF3E9DA)
    val Muted = Color(0xFFA89A8A)

    val WoodDark = Color(0xFF3B2618)
    val WoodMid = Color(0xFF5A3A24)
    val WoodLight = Color(0xFF7A5234)

    val VinylBlack = Color(0xFF0E0E0F)
    val Steel = Color(0xFFC9CDD2)
    val SteelDark = Color(0xFF7D838A)
}

private val scheme = darkColorScheme(
    primary = VinylColors.Brass,
    onPrimary = Color(0xFF2A1C0B),
    secondary = VinylColors.Muted,
    background = VinylColors.Background,
    onBackground = VinylColors.Cream,
    surface = VinylColors.Surface,
    onSurface = VinylColors.Cream,
    surfaceVariant = Color(0xFF2E2622),
    onSurfaceVariant = VinylColors.Muted,
)

@Composable
fun VinylTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
