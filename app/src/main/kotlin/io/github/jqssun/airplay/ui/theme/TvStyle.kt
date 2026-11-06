package io.github.jqssun.airplay.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Full-screen tvOS graphite backdrop: lighter at the top centre, darker at the edges. */
@Composable
fun TvBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBgEdge)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to TvBgTop, 0.55f to TvBgMid, 1f to TvBgEdge,
                        center = Offset(size.width / 2, 0f),
                        radius = maxOf(size.width, size.height) * 0.9f
                    )
                )
            },
        content = content
    )
}

/**
 * tvOS focus: unfocused = translucent white fill; focused = solid white, dark text,
 * slight scale-up and soft shadow. Content colour follows via LocalContentColor.
 */
@Composable
fun TvFocusSurface(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.05f,
    content: @Composable (focused: Boolean) -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, tween(180), label = "tvScale")
    val bg by animateColorAsState(if (focused) Color.White else TvFill, tween(180), label = "tvBg")
    val fg = if (focused) TvFocusText else TvText
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                if (focused) 14.dp else 0.dp, shape, clip = false,
                ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(bg)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) { content(focused) }
    }
}

/** Small fully-rounded pill (status / settings in the idle top bar). */
@Composable
fun TvPill(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.(focused: Boolean) -> Unit) {
    TvFocusSurface(onClick = onClick, shape = CircleShape, modifier = modifier, focusedScale = 1.06f) { focused ->
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content(focused)
        }
    }
}

@Composable
fun StatusDot(color: Color) {
    Box(
        Modifier
            .size(9.dp)
            .drawBehind {
                drawCircle(color.copy(alpha = 0.35f), radius = size.minDimension)
                drawCircle(color)
            }
    )
    Spacer(Modifier.width(9.dp))
}

/** Soft circle with the AirPlay glyph; optional slow 4 s "breathing" glow. */
@Composable
fun AirPlayRing(size: Dp, breathe: Boolean, modifier: Modifier = Modifier) {
    val t = if (breathe) {
        val transition = rememberInfiniteTransition(label = "breathe")
        transition.animateFloat(
            0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breatheT"
        ).value
    } else 0f
    Box(
        modifier = modifier
            .size(size)
            // glow first, before the clip, so it spills outside the circle
            .drawBehind {
                val glow = androidx.compose.ui.graphics.lerp(
                    NvCyan.copy(alpha = 0.16f), NvGreen.copy(alpha = 0.24f), t
                )
                drawCircle(
                    Brush.radialGradient(
                        0f to glow, 1f to Color.Transparent,
                        center = center, radius = this.size.minDimension * (0.95f + 0.15f * t)
                    ),
                    radius = this.size.minDimension * (0.95f + 0.15f * t)
                )
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f + 0.04f * t), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Airplay, contentDescription = null, tint = TvText, modifier = Modifier.size(size * 0.45f))
    }
}
