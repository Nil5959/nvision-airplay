package io.github.jqssun.airplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NilMint = Color(0xFF3DDBB4)
val NilMintDeep = Color(0xFF14806A)

private val NilColors = darkColorScheme(
    primary = NilMint,
    onPrimary = Color(0xFF00201A),
    primaryContainer = NilMintDeep,
    onPrimaryContainer = Color(0xFFD6FFF3),
    secondary = Color(0xFF9FC7BE),
    onSecondary = Color(0xFF0B1F1B),
    secondaryContainer = Color(0xFF1E2B28),
    onSecondaryContainer = Color(0xFFDCEBE7),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0B0D0D),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF141818),
    onSurfaceVariant = Color(0xFFC3CCCA),
    surfaceContainer = Color(0xFF101313),
    surfaceContainerHigh = Color(0xFF171B1B),
    outline = Color(0xFF3A4442),
    error = Color(0xFFFF8A80),
)

@Composable
fun AirPlayTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = NilColors, content = content)
}
