package com.clefie.melodies.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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
    val gyro          by vm.gyro.collectAsState()
    val proximity     by vm.proximity.collectAsState()
    val shake         by vm.shake.collectAsState()
    val tiltX         by vm.tiltX.collectAsState()
    val tiltY         by vm.tiltY.collectAsState()
    val pitch         by vm.pitch.collectAsState()
    val holdIntensity by vm.holdIntensity.collectAsState()
    val fingerCount   by vm.fingerCount.collectAsState()
    val pressure      by vm.pressure.collectAsState()
    val swipeVelocity by vm.swipeVelocity.collectAsState()

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
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Clefie Melodies",
                fontFamily = JackOfGearsFamily,
                fontSize = 28.sp,
                color = Color(0xFFEE80FF)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "BPM: ${bpm.toInt()}  ${if (beatPulse) "●" else "○"}",
                fontFamily = JackOfGearsFamily,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                "Detected: ${detectedBpm.toInt()}  Tapped: ${if (tapBpm > 0f) tapBpm.toInt().toString() else "-"}",
                fontFamily = PacificoFamily,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.onTapBeat() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE526AB))
                ) { Text("TAP BEAT", fontFamily = PacificoFamily) }

                Button(
                    onClick = { vm.onSyncTap() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE80FF))
                ) { Text("SYNC", fontFamily = PacificoFamily, color = Color.Black) }

                Button(
                    onClick = { vm.resetTapTempo() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) { Text("RESET", fontFamily = PacificoFamily) }
            }

            Spacer(Modifier.height(16.dp))

            Text("── Sensors ──", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("Mic: ${"%.2f".format(amp)}", color = Color.White, fontFamily = PacificoFamily)
            Text("Motion: ${"%.2f".format(accel)}", color = Color.White, fontFamily = PacificoFamily)
            Text("Shake: ${if (shake) "YES" else "-"}", color = Color.White, fontFamily = PacificoFamily)
            Text("Proximity: ${if (proximity) "NEAR" else "FAR"}", color = Color.White, fontFamily = PacificoFamily)
            Text("Tilt X: ${"%.1f".format(tiltX)}  Y: ${"%.1f".format(tiltY)}", color = Color.White, fontFamily = PacificoFamily)

            Spacer(Modifier.height(12.dp))

            Text("── Touch ──", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("Fingers: $fingerCount", color = Color.White, fontFamily = PacificoFamily)
            Text("Hold: ${"%.2f".format(holdIntensity)}", color = Color.White, fontFamily = PacificoFamily)
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
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
        }
    }
}
