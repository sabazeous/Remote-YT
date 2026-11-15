package com.musicremote.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF09090B)
val ObsidianSurface = Color(0xFF141418)
val ObsidianBorder = Color(0x14FFFFFF)
val CrimsonAccent = Color(0xFFE11D48)
val TextPrimary = Color(0xFFFAFAFA)
val TextSecondary = Color(0xFFA1A1AA)

private val DarkScheme = darkColorScheme(
    primary = CrimsonAccent,
    onPrimary = Color.White,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1C1C22),
    onSurfaceVariant = TextSecondary
)

@Composable
fun MusicRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
