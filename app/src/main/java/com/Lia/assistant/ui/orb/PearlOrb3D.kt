package com.Lia.assistant.ui.orb

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Lia.assistant.voice.NovaOrbState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PearlOrb3D(
    state: NovaOrbState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    orbSize: Dp = 270.dp,
    reducedMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransitions")

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 6000 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ArcBreath"
    )

    // Primary clockwise telemetry rotation
    val primaryRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 24000 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ArcRotationCW"
    )

    // Inner counter-clockwise rotation
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 30000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ArcRotationCCW"
    )

    Box(
        modifier = modifier.size(orbSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.72f
            val activeAmpScale = 1f + (amplitude.coerceIn(0f, 1f) * 0.40f)
            val radius = baseRadius * breathScale * (if (state == NovaOrbState.SPEAKING || state == NovaOrbState.LISTENING) activeAmpScale else 1f)

            // 1. Radiant Ambient Arc Aura
            drawArcAura(center, radius, state, amplitude)

            // 2. Outer Holographic Telemetry Rings (Tick marks & arcs)
            drawTelemetryRings(center, radius, primaryRotation, counterRotation, state, amplitude)

            // 3. Middle Titanium Housing & Circuit Segment Ring
            drawReactorHousing(center, radius, counterRotation, state)

            // 4. Central Plasma Core (Glowing cyan/blue & gold reaction)
            drawPlasmaCore(center, radius, state, amplitude, primaryRotation)

            // 5. Specular Holographic Lens Flare & Highlights
            drawCoreFlares(center, radius)
        }
    }
}

private fun DrawScope.drawArcAura(
    center: Offset,
    radius: Float,
    state: NovaOrbState,
    amplitude: Float
) {
    val auraColor = when (state) {
        NovaOrbState.IDLE -> Color(0xFF00E5FF).copy(alpha = 0.20f)
        NovaOrbState.CONNECTING -> Color(0xFF38BDF8).copy(alpha = 0.30f)
        NovaOrbState.LISTENING -> Color(0xFF00E5FF).copy(alpha = 0.30f + amplitude * 0.45f)
        NovaOrbState.THINKING -> Color(0xFFFFB300).copy(alpha = 0.40f)
        NovaOrbState.SPEAKING -> Color(0xFF00E5FF).copy(alpha = 0.35f + amplitude * 0.50f)
        NovaOrbState.ERROR -> Color(0xFFF43F5E).copy(alpha = 0.35f)
    }

    val maxRadius = radius * (1.38f + amplitude * 0.25f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(auraColor, Color.Transparent),
            center = center,
            radius = maxRadius
        ),
        radius = maxRadius,
        center = center
    )
}

private fun DrawScope.drawTelemetryRings(
    center: Offset,
    radius: Float,
    cwAngle: Float,
    ccwAngle: Float,
    state: NovaOrbState,
    amplitude: Float
) {
    val cyanGlow = Color(0xFF00E5FF)
    val goldAccent = Color(0xFFFFB300)

    // Outer subtle circle
    drawCircle(
        color = cyanGlow.copy(alpha = 0.25f),
        radius = radius * 1.18f + (amplitude * 14f),
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Telemetry tick marks
    val tickRadius = radius * 1.12f
    val tickCount = 24
    for (i in 0 until tickCount) {
        val angle = Math.toRadians((cwAngle + i * (360f / tickCount)).toDouble())
        val isMajor = i % 4 == 0
        val len = if (isMajor) 14f else 7f
        val startX = (center.x + cos(angle) * (tickRadius - len)).toFloat()
        val startY = (center.y + sin(angle) * (tickRadius - len)).toFloat()
        val endX = (center.x + cos(angle) * tickRadius).toFloat()
        val endY = (center.y + sin(angle) * tickRadius).toFloat()

        drawLine(
            color = if (isMajor) goldAccent.copy(alpha = 0.65f) else cyanGlow.copy(alpha = 0.35f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isMajor) 2.5f else 1.2f
        )
    }

    // Counter-rotating segmented circle
    drawCircle(
        color = cyanGlow.copy(alpha = 0.40f),
        radius = radius * 1.04f,
        center = center,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawReactorHousing(
    center: Offset,
    radius: Float,
    angleDeg: Float,
    state: NovaOrbState
) {
    // Titanium housing base gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A),
                Color(0xFF020617)
            ),
            center = center,
            radius = radius * 0.98f
        ),
        radius = radius * 0.98f,
        center = center
    )

    // 10 Radial Reactor Segments
    val segmentCount = 10
    val segInner = radius * 0.52f
    val segOuter = radius * 0.88f
    for (i in 0 until segmentCount) {
        val a = Math.toRadians((angleDeg + i * (360f / segmentCount)).toDouble())
        val x1 = (center.x + cos(a) * segInner).toFloat()
        val y1 = (center.y + sin(a) * segInner).toFloat()
        val x2 = (center.x + cos(a) * segOuter).toFloat()
        val y2 = (center.y + sin(a) * segOuter).toFloat()

        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.55f),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawPlasmaCore(
    center: Offset,
    radius: Float,
    state: NovaOrbState,
    amplitude: Float,
    rotationDeg: Float
) {
    val coreRadius = radius * (0.46f + (amplitude * 0.28f))

    val coreColors = when (state) {
        NovaOrbState.SPEAKING -> listOf(
            Color.White,
            Color(0xFF67EEFF),
            Color(0xFF00E5FF),
            Color(0xFF0284C7),
            Color.Transparent
        )
        NovaOrbState.THINKING -> listOf(
            Color(0xFFFFFBEB),
            Color(0xFFFFD54F),
            Color(0xFFFFB300),
            Color(0xFFD97706),
            Color.Transparent
        )
        NovaOrbState.ERROR -> listOf(
            Color(0xFFFFF1F2),
            Color(0xFFFB7185),
            Color(0xFFE11D48),
            Color.Transparent
        )
        else -> listOf(
            Color(0xFFE0F7FA),
            Color(0xFF26C6DA),
            Color(0xFF0097A7),
            Color(0xFF006064),
            Color.Transparent
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = coreColors,
            center = center,
            radius = coreRadius
        ),
        radius = coreRadius,
        center = center
    )
}

private fun DrawScope.drawCoreFlares(
    center: Offset,
    radius: Float
) {
    // Central Arc spark
    drawCircle(
        color = Color.White.copy(alpha = 0.90f),
        radius = radius * 0.08f,
        center = center
    )

    // Specular glass highlight
    val specCenter = center - Offset(radius * 0.28f, radius * 0.28f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                Color.Transparent
            ),
            center = specCenter,
            radius = radius * 0.22f
        ),
        radius = radius * 0.22f,
        center = specCenter
    )
}
