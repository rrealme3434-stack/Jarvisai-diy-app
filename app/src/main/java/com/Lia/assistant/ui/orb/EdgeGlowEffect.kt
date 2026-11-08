package com.Lia.assistant.ui.orb

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.Lia.assistant.voice.NovaOrbState

@Composable
fun EdgeGlowEffect(
    state: NovaOrbState,
    amplitude: Float,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (state == NovaOrbState.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "EdgeGlowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 3000 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val glowColor1 = when (state) {
        NovaOrbState.SPEAKING -> Color(0xFF00E5FF)
        NovaOrbState.LISTENING -> Color(0xFF38BDF8)
        NovaOrbState.THINKING -> Color(0xFFFFB300)
        NovaOrbState.ERROR -> Color(0xFFF43F5E)
        else -> Color(0xFF00E5FF)
    }

    val glowColor2 = when (state) {
        NovaOrbState.SPEAKING -> Color(0xFF0284C7)
        NovaOrbState.LISTENING -> Color(0xFF00E5FF)
        NovaOrbState.THINKING -> Color(0xFFFF8F00)
        NovaOrbState.ERROR -> Color(0xFFE11D48)
        else -> Color(0xFF0F172A)
    }

    val dynamicAlpha = (glowAlpha * (0.6f + amplitude * 0.4f)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glowColor1.copy(alpha = dynamicAlpha),
                        glowColor2.copy(alpha = dynamicAlpha * 0.35f),
                        glowColor1.copy(alpha = dynamicAlpha * 0.85f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    )
}
