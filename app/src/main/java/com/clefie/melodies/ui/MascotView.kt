package com.clefie.melodies.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

// Mascot state priority: DANCE > TALKING > WAVE > IDLE
enum class MascotState { IDLE, WAVE, TALKING, DANCE }

@Composable
fun MascotView(
    amplitude: Float,
    bpm: Float,
    beatPulse: Boolean,
    holdIntensity: Float,
    tiltX: Float,
    tiltY: Float,
    forceWave: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determine state based on priority
    val state = when {
        bpm > 100f || beatPulse -> MascotState.DANCE
        amplitude > 0.15f       -> MascotState.TALKING
        forceWave               -> MascotState.WAVE
        else                    -> MascotState.IDLE
    }

    // Scale driven by hold intensity + beat pulse
    val scale = 1f + (holdIntensity * 0.25f) + (if (beatPulse) 0.05f else 0f)

    // Position drift from tilt — max 40dp drift
    val maxDrift = 40f
    val offsetX = (tiltX / 180f * maxDrift).dp
    val offsetY = (tiltY / 90f * maxDrift).dp

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            MascotState.DANCE -> {
                VideoPlayer(
                    assetPath = "images/Mascot_dance.webm",
                    modifier = Modifier.fillMaxSize(),
                    loop = true,
                    speed = (bpm / 120f).coerceIn(0.5f, 2f)
                )
            }
            MascotState.TALKING -> {
                VideoPlayer(
                    assetPath = "images/Mascot_talk.webm",
                    modifier = Modifier.fillMaxSize(),
                    loop = true,
                    speed = 1f + amplitude
                )
            }
            MascotState.WAVE -> {
                VideoPlayer(
                    assetPath = "images/Mascot_wave.webm",
                    modifier = Modifier.fillMaxSize(),
                    loop = true,
                    speed = 1f
                )
            }
            MascotState.IDLE -> {
                // Static face — layered body + face
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/images/Mascot_body.webp")
                        .build(),
                    contentDescription = "Mascot Body",
                    modifier = Modifier.fillMaxSize()
                )
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/images/Mascot_face.webp")
                        .build(),
                    contentDescription = "Mascot Face",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
