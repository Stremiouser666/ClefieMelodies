package com.clefie.melodies.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clefie.melodies.viewmodel.FlowViewModel
import com.clefie.melodies.viewmodel.MainViewModel

@Composable
fun MainScreen(vm: MainViewModel, flowVm: FlowViewModel) {

    val bpm           by vm.bpm.collectAsState()
    val detectedBpm   by vm.detectedBpm.collectAsState()
    val tapBpm        by vm.tapBpm.collectAsState()
    val beatPulse     by vm.beatPulse.collectAsState()
    val amp           by vm.amplitude.collectAsState()
    val accel         by vm.accel.collectAsState()
    val shake         by vm.shake.collectAsState()
    val proximity     by vm.proximity.collectAsState()
    val tiltX         by vm.tiltX.collectAsState()
    val tiltY         by vm.tiltY.collectAsState()
    val holdIntensity by vm.holdIntensity.collectAsState()
    val swipeVelocity by vm.swipeVelocity.collectAsState()
    val fingerCount   by vm.fingerCount.collectAsState()

    var screenWidth by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF361F30))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { screenWidth = it.size.width.toFloat() }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            vm.gestures.onPointerEvent(event, screenWidth)
                        }
                    }
                }
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title — twice the size, bold, at the top
            Text(
                text  = "Clefie Melodies",
                style = TextStyle(
                    fontFamily  = JackOfGearsFamily,
                    fontSize    = 48.sp,
                    fontWeight  = FontWeight.Bold,
                    color       = Color(0xFFEE80FF),
                    textAlign   = TextAlign.Center,
                    shadow      = Shadow(
                        color      = Color(0xFFE526AB),
                        offset     = Offset(0f, 4f),
                        blurRadius = 16f
                    )
                )
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "BPM: ${bpm.toInt()}  ${if (beatPulse) "●" else "○"}",
                fontFamily = JackOfGearsFamily,
                fontSize   = 22.sp,
                color      = Color.White
            )
            Text(
                "Detected: ${detectedBpm.toInt()}  Tapped: ${if (tapBpm > 0f) tapBpm.toInt().toString() else "-"}",
                fontFamily = PacificoFamily,
                color      = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.onTapBeat() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE526AB))
                ) { Text("TAP BEAT", fontFamily = PacificoFamily, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { vm.onSyncTap() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE80FF))
                ) { Text("SYNC", fontFamily = PacificoFamily, color = Color.Black, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { vm.resetTapTempo() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) { Text("RESET", fontFamily = PacificoFamily, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(20.dp))

            Text("── Sensors ──", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("Mic: ${"%.2f".format(amp)}", color = Color.White, fontFamily = PacificoFamily)
            Text("Motion: ${"%.2f".format(accel)}", color = Color.White, fontFamily = PacificoFamily)
            Text("Shake: ${if (shake) "YES" else "-"}", color = Color.White, fontFamily = PacificoFamily)
            Text("Proximity: ${if (proximity) "NEAR" else "FAR"}", color = Color.White, fontFamily = PacificoFamily)
            Text("Tilt X: ${"%.1f".format(tiltX)}  Y: ${"%.1f".format(tiltY)}", color = Color.White, fontFamily = PacificoFamily)
            Text("Fingers: $fingerCount  Hold: ${"%.2f".format(holdIntensity)}", color = Color.White, fontFamily = PacificoFamily)
            Text("Velocity: ${"%.2f".format(swipeVelocity)}", color = Color.White, fontFamily = PacificoFamily)
        }

        // Fullscreen button
        IconButton(
            onClick  = { flowVm.openFullscreen() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(Color(0xFFE526AB), shape = CircleShape)
                .size(56.dp)
        ) {
            Icon(Icons.Default.OpenInFull, contentDescription = "Fullscreen", tint = Color.White)
        }
    }
}
