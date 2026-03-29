package com.clefie.melodies.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clefie.melodies.viewmodel.MainViewModel

@Composable
fun MainScreen(vm: MainViewModel) {

    val bpm by vm.bpm.collectAsState()
    val amp by vm.amplitude.collectAsState()
    val accel by vm.accel.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Clefie Melodies", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Text("BPM: ${bpm.toInt()}")
        Text("Mic: ${"%.2f".format(amp)}")
        Text("Motion: ${"%.2f".format(accel)}")

        Spacer(modifier = Modifier.height(30.dp))

        Text("Sequencer Running...", style = MaterialTheme.typography.bodyLarge)
    }
}
