package io.github.jqssun.airplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// N Vision brand tokens (same as the N Vision contract)
val NvInk = Color(0xFF0B1620)
val NvPanel = Color(0xFF0E1A24)
val NvGreen = Color(0xFF10B981)
val NvCyan = Color(0xFF06B6D4)
val NvBlue = Color(0xFF2563EB)
val NvMuted = Color(0xFF94A3B8)

private val NvColors = darkColorScheme(
    primary = NvGreen,
    onPrimary = Color(0xFF02281C),
    primaryContainer = Color(0xFF0B4F3C),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = NvCyan,
    onSecondary = Color(0xFF00242B),
    secondaryContainer = Color(0xFF123445),
    onSecondaryContainer = Color(0xFFCFF4FC),
    tertiary = NvBlue,
    background = NvInk,
    onBackground = Color(0xFFF1F5F9),
    surface = NvInk,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NvPanel,
    onSurfaceVariant = NvMuted,
    surfaceContainer = Color(0xFF0D1823),
    surfaceContainerHigh = Color(0xFF13212D),
    surfaceContainerHighest = Color(0xFF182938),
    outline = Color(0xFF1E2C3A),
    outlineVariant = Color(0xFF1E2C3A),
    error = Color(0xFFF87171),
)

@Composable
fun AirPlayTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = NvColors, content = content)
}
