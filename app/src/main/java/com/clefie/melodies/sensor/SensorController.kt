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
import kotlin.math.sqrt

class SensorController(
    private val context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    // ── Public state ──────────────────────────────────────────────
    private val _acceleration = MutableStateFlow(0f)
    val acceleration: StateFlow<Float> = _acceleration

    private val _gyro = MutableStateFlow(0f)
    val gyro: StateFlow<Float> = _gyro

    private val _proximity = MutableStateFlow(false)
    val proximity: StateFlow<Boolean> = _proximity

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    // ── Accelerometer filter ──────────────────────────────────────
    private var gravity = FloatArray(3)
    private var linear = FloatArray(3)
    private var lastAccel = 0f
    private val accelAlpha = 0.8f
    private val accelThreshold = 0.5f
    private val damping = 0.85f

    // ── Gyroscope filter ──────────────────────────────────────────
    private var gyroFiltered = FloatArray(3)
    private var lastGyro = 0f
    private val gyroAlpha = 0.7f
    private val gyroThreshold = 0.05f

    // ── Mic ───────────────────────────────────────────────────────
    private var audioRecord: AudioRecord? = null
    private var micJob: Job? = null
    private val micScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ─────────────────────────────────────────────────────────────
    fun start() {
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor     = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximitySensor     = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        startMic()
    }

    fun stop() {
        sensorManager.unregisterListener(this as SensorEventListener)
        stopMic()
        micScope.cancel()
    }

    // ── Sensor events ─────────────────────────────────────────────
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE     -> handleGyroscope(event)
            Sensor.TYPE_PROXIMITY     -> handleProximity(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun handleAccelerometer(event: SensorEvent) {
        gravity[0] = accelAlpha * gravity[0] + (1 - accelAlpha) * event.values[0]
        gravity[1] = accelAlpha * gravity[1] + (1 - accelAlpha) * event.values[1]
        gravity[2] = accelAlpha * gravity[2] + (1 - accelAlpha) * event.values[2]

        linear[0] = event.values[0] - gravity[0]
        linear[1] = event.values[1] - gravity[1]
        linear[2] = event.values[2] - gravity[2]

        val mag = sqrt(
            (linear[0] * linear[0] + linear[1] * linear[1] + linear[2] * linear[2]).toDouble()
        ).toFloat()

        val motion = if (mag > accelThreshold) mag else 0f
        val out = if (motion > lastAccel) motion else lastAccel * damping
        lastAccel = out
        _acceleration.value = out
    }

    private fun handleGyroscope(event: SensorEvent) {
        gyroFiltered[0] = gyroAlpha * gyroFiltered[0] + (1 - gyroAlpha) * event.values[0]
        gyroFiltered[1] = gyroAlpha * gyroFiltered[1] + (1 - gyroAlpha) * event.values[1]
        gyroFiltered[2] = gyroAlpha * gyroFiltered[2] + (1 - gyroAlpha) * event.values[2]

        val mag = sqrt(
            (gyroFiltered[0] * gyroFiltered[0] +
             gyroFiltered[1] * gyroFiltered[1] +
             gyroFiltered[2] * gyroFiltered[2]).toDouble()
        ).toFloat()

        val rot = if (mag > gyroThreshold) mag else 0f
        val out = if (rot > lastGyro) rot else lastGyro * damping
        lastGyro = out
        _gyro.value = out
    }

    private fun handleProximity(event: SensorEvent) {
        val maxRange = proximitySensor?.maximumRange ?: 5f
        _proximity.value = event.values[0] < maxRange
    }

    // ── Microphone ────────────────────────────────────────────────
    private fun startMic() {
        val sampleRate = 44100
        val bufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord = null
                return
            }
            audioRecord?.startRecording()

            micJob = micScope.launch {
                val buf = ShortArray(bufSize)
                while (isActive) {
                    val read = audioRecord?.read(buf, 0, bufSize) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) sum += buf[i] * buf[i].toDouble()
                        val rms = sqrt(sum / read).toFloat()
                        _amplitude.value = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not yet granted — amplitude stays 0f
        }
    }

    private fun stopMic() {
        micJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
