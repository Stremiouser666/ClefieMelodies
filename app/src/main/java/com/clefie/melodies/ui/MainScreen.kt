package com.clefie.melodies.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.clefie.melodies.viewmodel.MainViewModel

@Composable
fun MainScreen(vm: MainViewModel) {

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
        Text("Clefie Melodies", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        // BPM display — highlights on beat pulse
        Text(
            text = "BPM: ${bpm.toInt()}  ${if (beatPulse) "●" else "○"}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("Detected: ${detectedBpm.toInt()}  Tapped: ${if (tapBpm > 0f) tapBpm.toInt().toString() else "-"}")

        Spacer(Modifier.height(16.dp))

        // Tap tempo buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.onTapBeat() }) {
                Text("TAP BEAT")
            }
            Button(onClick = { vm.onSyncTap() }) {
                Text("SYNC")
            }
            OutlinedButton(onClick = { vm.resetTapTempo() }) {
                Text("RESET")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("── Sensors ──", style = MaterialTheme.typography.labelSmall)
        Text("Mic: ${"%.2f".format(amp)}")
        Text("Motion: ${"%.2f".format(accel)}")
        Text("Gyro: ${"%.2f".format(gyro)} rad/s")
        Text("Proximity: ${if (proximity) "NEAR" else "FAR"}")
        Text("Shake: ${if (shake) "YES" else "-"}")
        Text("Tilt X: ${"%.1f".format(tiltX)}°  Y: ${"%.1f".format(tiltY)}°")

        Spacer(Modifier.height(12.dp))

        Text("── Touch ──", style = MaterialTheme.typography.labelSmall)
        Text("Fingers: $fingerCount")
        Text("Pitch: ${"%.2f".format(pitch)}")
        Text("Hold: ${"%.2f".format(holdIntensity)}")
        Text("Pressure: ${"%.2f".format(pressure)}")
        Text("Velocity: ${"%.2f".format(swipeVelocity)}")
    }
}
