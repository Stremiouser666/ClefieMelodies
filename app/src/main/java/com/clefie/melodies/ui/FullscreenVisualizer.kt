package com.clefie.melodies.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clefie.melodies.viewmodel.MainViewModel
import com.clefie.melodies.viewmodel.FlowViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val isPrimary: Boolean
)

@Composable
fun FullscreenVisualizer(
    mainVm: MainViewModel,
    flowVm: FlowViewModel,
    onClose: () -> Unit
) {
    val bpm           by mainVm.bpm.collectAsState()
    val amplitude     by mainVm.amplitude.collectAsState()
    val beatPulse     by mainVm.beatPulse.collectAsState()
    val shake         by mainVm.shake.collectAsState()
    val tiltX         by mainVm.tiltX.collectAsState()
    val tiltY         by mainVm.tiltY.collectAsState()
    val holdIntensity by mainVm.holdIntensity.collectAsState()
    val isAmbient     by flowVm.isAmbient.collectAsState()

    val isDancing = bpm > 100f || beatPulse
    val bgSpeed   = (bpm / 120f).coerceIn(0.25f, 3f)

    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(beatPulse, shake) {
        if (beatPulse || shake) {
            repeat(30) { i ->
                particles.add(
                    Particle(
                        x = 0f, y = 0f,
                        vx = (Math.random().toFloat() - 0.5f) * 20f,
                        vy = (Math.random().toFloat() - 0.5f) * 20f,
                        life = 1f,
                        isPrimary = i % 2 == 0
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            particles.removeAll { it.life <= 0f }
            particles.forEach { p ->
                p.x  += p.vx; p.y  += p.vy
                p.vx *= 0.97f; p.vy *= 0.97f
                p.life -= 0.018f
            }
            delay(16L)
        }
    }

    LaunchedEffect(beatPulse) {
        if (beatPulse) flowVm.onBeatPulse()
    }

    val ambientAlpha = if (isAmbient) 0.5f else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF361F30))
    ) {
        // Animated background
        VideoPlayer(
            assetPath = if (isDancing) "images/Background_dance.webm"
                        else           "images/Background_animated.webm",
            modifier  = Modifier.fillMaxSize(),
            loop      = true,
            speed     = bgSpeed
        )

        // Ambient dim overlay
        if (isAmbient) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Canvas — aura, rings, particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width  / 2f
            val cy = size.height / 2f
            val baseRadius = 180f + amplitude * 80f
            val time = System.currentTimeMillis() / 1000f

            // Radial aura bars
            val barCount = 128
            for (i in 0 until barCount) {
                val angle     = (i.toFloat() / barCount) * 2f * PI.toFloat()
                val barHeight = amplitude * 180f * (0.4f + 0.6f * abs(sin(i * 0.15f)))
                val x1 = cx + cos(angle) * baseRadius
                val y1 = cy + sin(angle) * baseRadius
                val x2 = cx + cos(angle) * (baseRadius + barHeight)
                val y2 = cy + sin(angle) * (baseRadius + barHeight)
                val barAlpha = (amplitude * 0.9f).coerceIn(0f, 1f) * ambientAlpha
                drawLine(
                    color       = (if (i % 2 == 0) Color(0xFFE526AB) else Color(0xFFEE80FF)).copy(alpha = barAlpha),
                    start       = Offset(x1, y1),
                    end         = Offset(x2, y2),
                    strokeWidth = 4f
                )
            }

            // Rotating energy rings
            val ringAlpha = (0.1f + amplitude * 0.25f) * ambientAlpha
            rotate(degrees = time * 36f % 360f, pivot = Offset(cx, cy)) {
                drawOval(
                    color   = Color.White.copy(alpha = ringAlpha),
                    topLeft = Offset(cx - baseRadius - 100f, cy - baseRadius - 120f),
                    size    = Size((baseRadius + 100f) * 2f, (baseRadius + 120f) * 2f),
                    style   = Stroke(width = 2f)
                )
            }
            rotate(degrees = -(time * 22f % 360f), pivot = Offset(cx, cy)) {
                drawOval(
                    color   = Color(0xFFEE80FF).copy(alpha = ringAlpha * 0.7f),
                    topLeft = Offset(cx - baseRadius - 140f, cy - baseRadius - 100f),
                    size    = Size((baseRadius + 140f) * 2f, (baseRadius + 100f) * 2f),
                    style   = Stroke(width = 1.5f)
                )
            }

            // Heart gem glow on beat
            if (beatPulse) {
                drawCircle(
                    color  = Color(0xFFE526AB).copy(alpha = 0.6f * ambientAlpha),
                    radius = baseRadius * 0.3f,
                    center = Offset(cx, cy)
                )
            }

            // Particles
            particles.forEach { p ->
                drawCircle(
                    color  = if (p.isPrimary) Color(0xFFE526AB).copy(alpha = p.life)
                             else Color.White.copy(alpha = p.life),
                    radius = 4f * p.life,
                    center = Offset(cx + p.x, cy + p.y)
                )
            }
        }

        // Mascot
        MascotView(
            amplitude     = amplitude,
            bpm           = bpm,
            beatPulse     = beatPulse,
            holdIntensity = holdIntensity,
            tiltX         = tiltX,
            tiltY         = tiltY,
            modifier      = Modifier
                .size(260.dp)
                .align(Alignment.Center)
        )

        // 16-step sequencer dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(16) { i ->
                val isActive = (System.currentTimeMillis() /
                    (60000L / bpm.toLong().coerceAtLeast(1L)) % 16).toInt() == i
                Box(
                    modifier = Modifier
                        .size(if (isActive) 14.dp else 8.dp)
                        .background(
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )
            }
        }

        // Top right controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick  = { flowVm.toggleAmbient() },
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
            ) {
                Icon(Icons.Default.NightsStay, contentDescription = "Ambient", tint = Color.White)
            }
            IconButton(
                onClick  = onClose,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Close", tint = Color.White)
            }
        }

        // BPM top left
        Text(
            text       = "${bpm.toInt()} BPM  ${if (beatPulse) "●" else "○"}",
            fontFamily = JackOfGearsFamily,
            fontSize   = 18.sp,
            color      = Color(0xFFEE80FF).copy(alpha = ambientAlpha),
            modifier   = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        )

        // Logo watermark
        Text(
            text       = "Clefie",
            fontFamily = JackOfGearsFamily,
            fontSize   = 14.sp,
            color      = Color.White.copy(alpha = 0.15f * ambientAlpha),
            modifier   = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
