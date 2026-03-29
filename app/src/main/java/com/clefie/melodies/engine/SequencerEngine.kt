package com.clefie.melodies.engine

import kotlinx.coroutines.*
import kotlin.math.roundToLong

class SequencerEngine {

    private var bpm: Float = 120f
    private var job: Job? = null
    private var step = 0

    fun start(onStep: (Int) -> Unit) {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val interval = (60000f / bpm / 4f).roundToLong() // 16th notes
                onStep(step)

                step = (step + 1) % 16
                delay(interval)
            }
        }
    }

    fun setBpm(newBpm: Float) {
        bpm = newBpm.coerceIn(40f, 240f)
    }

    fun stop() {
        job?.cancel()
    }
}
