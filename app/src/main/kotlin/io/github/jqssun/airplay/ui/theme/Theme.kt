package io.github.jqssun.airplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.jqssun.airplay.R

// N Vision brand tokens (same as the N Vision contract)
val NvInk = Color(0xFF0B1620)
val NvPanel = Color(0xFF0E1A24)
val NvGreen = Color(0xFF10B981)
val NvCyan = Color(0xFF06B6D4)
val NvBlue = Color(0xFF2563EB)
val NvMuted = Color(0xFF94A3B8)

// tvOS dark (graphite) tokens
val TvBgTop = Color(0xFF3A3A3C)
val TvBgMid = Color(0xFF1C1C1E)
val TvBgEdge = Color(0xFF0E0E10)
val TvText = Color(0xFFFFFFFF)
val TvMuted = Color(0xFF98989F)
val TvFill = Color.White.copy(alpha = 0.10f)
val TvFocusText = Color(0xFF1C1C1E)
val TvGreen = Color(0xFF30D158)
val TvRed = Color(0xFFFF453A)

// Hebrew glyphs fall back to the system font
val Inter = FontFamily(
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_black, FontWeight.Black),
)

private val NvColors = darkColorScheme(
    primary = TvGreen,
    onPrimary = Color(0xFF00210B),
    primaryContainer = Color(0xFF1F4D2B),
    onPrimaryContainer = Color(0xFFD1FAE0),
    secondary = Color(0xFF64D2FF),
    onSecondary = Color(0xFF00242B),
    secondaryContainer = Color(0xFF3A3A3C),
    onSecondaryContainer = TvText,
    tertiary = Color(0xFF0A84FF),
    background = TvBgMid,
    onBackground = TvText,
    surface = TvBgMid,
    onSurface = TvText,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = TvMuted,
    surfaceContainer = Color(0xFF242426),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF3A3A3C),
    error = TvRed,
)

private val NvTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Inter),
        displayMedium = displayMedium.copy(fontFamily = Inter),
        displaySmall = displaySmall.copy(fontFamily = Inter),
        headlineLarge = headlineLarge.copy(fontFamily = Inter),
        headlineMedium = headlineMedium.copy(fontFamily = Inter),
        headlineSmall = headlineSmall.copy(fontFamily = Inter),
        titleLarge = titleLarge.copy(fontFamily = Inter),
        titleMedium = titleMedium.copy(fontFamily = Inter),
        titleSmall = titleSmall.copy(fontFamily = Inter),
        bodyLarge = bodyLarge.copy(fontFamily = Inter),
        bodyMedium = bodyMedium.copy(fontFamily = Inter),
        bodySmall = bodySmall.copy(fontFamily = Inter),
        labelLarge = labelLarge.copy(fontFamily = Inter),
        labelMedium = labelMedium.copy(fontFamily = Inter),
        labelSmall = labelSmall.copy(fontFamily = Inter),
    )
}

@Composable
fun AirPlayTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = NvColors, typography = NvTypography, content = content)
}
