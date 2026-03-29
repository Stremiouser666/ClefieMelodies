package com.clefie.melodies.sensor

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class SensorController {

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _acceleration = MutableStateFlow(0f)
    val acceleration: StateFlow<Float> = _acceleration

    private var job: Job? = null

    fun start() {
        startMic()
        startFakeAccel() // replace later with real sensor
    }

    private fun startMic() {
        job = CoroutineScope(Dispatchers.Default).launch {
            val bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val buffer = ShortArray(bufferSize)
            recorder.startRecording()

            while (isActive) {
                recorder.read(buffer, 0, buffer.size)
                val amp = buffer.map { abs(it.toInt()) }.average().toFloat() / 32767f
                _amplitude.value = amp
            }

            recorder.stop()
            recorder.release()
        }
    }

    private fun startFakeAccel() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                _acceleration.value = (0..10).random() / 10f
                delay(200)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
