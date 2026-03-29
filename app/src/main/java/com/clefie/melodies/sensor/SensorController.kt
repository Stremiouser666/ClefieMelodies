package com.clefie.melodies.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class SensorController(private val context: Context) {

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _acceleration = MutableStateFlow(0f)
    val acceleration: StateFlow<Float> = _acceleration

    private var micJob: Job? = null
    private var sensorManager: SensorManager? = null

    // smoothing
    private var lastAccel = 0f
    private val alpha = 0.1f   // smoothing factor
    private val deadzone = 0.02f

    fun start() {
        startMic()
        startAccelerometer()
    }

    private fun startMic() {
        micJob = CoroutineScope(Dispatchers.Default).launch {
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

    private fun startAccelerometer() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager?.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val magnitude = sqrt(x * x + y * y + z * z)

                // remove gravity (~9.8)
                val motion = abs(magnitude - 9.8f) / 9.8f

                // smoothing (low-pass filter)
                val smoothed = lastAccel + alpha * (motion - lastAccel)
                lastAccel = smoothed

                // deadzone (ignore tiny movement)
                val finalValue = if (smoothed < deadzone) 0f else smoothed

                _acceleration.value = finalValue
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }, accel, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        micJob?.cancel()
        sensorManager?.unregisterListener(null)
    }
}
