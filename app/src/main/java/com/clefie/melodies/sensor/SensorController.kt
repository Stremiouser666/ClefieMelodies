package com.clefie.melodies.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorController(private val context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    // ── Exposed StateFlows ──────────────────────────────────────────────────
    private val _acceleration = MutableStateFlow(0f)
    val acceleration: StateFlow<Float> = _acceleration

    private val _gyroscope = MutableStateFlow(0f)
    val gyroscope: StateFlow<Float> = _gyroscope

    private val _proximity = MutableStateFlow(false)
    val proximity: StateFlow<Boolean> = _proximity

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    // Phase 3 additions
    private val _shake = MutableStateFlow(false)
    val shake: StateFlow<Boolean> = _shake

    private val _tiltX = MutableStateFlow(0f)   // roll  -180 to 180 degrees
    val tiltX: StateFlow<Float> = _tiltX

    private val _tiltY = MutableStateFlow(0f)   // pitch -90  to 90 degrees
    val tiltY: StateFlow<Float> = _tiltY

    // ── Accelerometer filter state ──────────────────────────────────────────
    private val accelGravity = FloatArray(3)
    private var accelLastOutput = 0f
    private val accelAlpha = 0.8f
    private val accelThreshold = 0.5f
    private val accelDamping = 0.85f

    // ── Shake detection ─────────────────────────────────────────────────────
    private var lastMagnitude = 0f
    private val shakeThreshold = 12f     // m/s² delta to trigger shake
    private var shakeResetJob: Job? = null
    private val shakeScope = CoroutineScope(Dispatchers.Default)

    // ── Gyroscope filter state ──────────────────────────────────────────────
    private val gyroFiltered = FloatArray(3)
    private var gyroLastOutput = 0f
    private val gyroAlpha = 0.7f
    private val gyroThreshold = 0.05f
    private val gyroDamping = 0.90f

    // ── AudioRecord ─────────────────────────────────────────────────────────
    private var audioRecord: AudioRecord? = null
    private var micJob: Job? = null
    private val micScope = CoroutineScope(Dispatchers.IO)

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    private var micSmoothed = 0f
    private val micSmoothing = 0.15f

    fun start() {
        startSensors()
        startMic()
    }

    fun stop() {
        sensorManager.unregisterListener(this as SensorEventListener)
        stopMic()
        shakeResetJob?.cancel()
    }

    // ── Sensors ─────────────────────────────────────────────────────────────

    private fun startSensors() {
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
    }

    // ── Mic ─────────────────────────────────────────────────────────────────

    private fun startMic() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return
        }

        audioRecord?.startRecording()

        micJob = micScope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val sample = buffer[i].toDouble()
                        sum += sample * sample
                    }
                    val rms = sqrt(sum / read)
                    val normalised = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                    micSmoothed = micSmoothing * normalised + (1f - micSmoothing) * micSmoothed
                    _amplitude.value = micSmoothed
                }
            }
        }
    }

    private fun stopMic() {
        micJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    // ── Sensor callbacks ────────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE     -> handleGyroscope(event)
            Sensor.TYPE_PROXIMITY     -> handleProximity(event)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        // Low-pass filter → gravity
        accelGravity[0] = accelAlpha * accelGravity[0] + (1 - accelAlpha) * event.values[0]
        accelGravity[1] = accelAlpha * accelGravity[1] + (1 - accelAlpha) * event.values[1]
        accelGravity[2] = accelAlpha * accelGravity[2] + (1 - accelAlpha) * event.values[2]

        // Linear acceleration
        val lx = event.values[0] - accelGravity[0]
        val ly = event.values[1] - accelGravity[1]
        val lz = event.values[2] - accelGravity[2]

        val magnitude = sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()
        val motion = if (magnitude > accelThreshold) magnitude else 0f
        val output = if (motion > accelLastOutput) motion else accelLastOutput * accelDamping
        accelLastOutput = output
        _acceleration.value = output

        // ── Shake detection ────────────────────────────────────────────────
        val delta = magnitude - lastMagnitude
        if (delta > shakeThreshold && !_shake.value) {
            _shake.value = true
            shakeResetJob?.cancel()
            shakeResetJob = shakeScope.launch {
                kotlinx.coroutines.delay(500L)
                _shake.value = false
            }
        }
        lastMagnitude = magnitude

        // ── Tilt (from gravity vector) ─────────────────────────────────────
        // Roll: rotation around Z axis
        val roll  = Math.toDegrees(
            atan2(accelGravity[1].toDouble(), accelGravity[2].toDouble())
        ).toFloat()

        // Pitch: rotation around X axis
        val pitch = Math.toDegrees(
            atan2(
                (-accelGravity[0]).toDouble(),
                sqrt((accelGravity[1] * accelGravity[1] +
                      accelGravity[2] * accelGravity[2]).toDouble())
            )
        ).toFloat()

        _tiltX.value = roll
        _tiltY.value = pitch
    }

    private fun handleGyroscope(event: SensorEvent) {
        gyroFiltered[0] = gyroAlpha * gyroFiltered[0] + (1 - gyroAlpha) * event.values[0]
        gyroFiltered[1] = gyroAlpha * gyroFiltered[1] + (1 - gyroAlpha) * event.values[1]
        gyroFiltered[2] = gyroAlpha * gyroFiltered[2] + (1 - gyroAlpha) * event.values[2]

        val magnitude = sqrt(
            (gyroFiltered[0] * gyroFiltered[0] +
             gyroFiltered[1] * gyroFiltered[1] +
             gyroFiltered[2] * gyroFiltered[2]).toDouble()
        ).toFloat()

        val spin = if (magnitude > gyroThreshold) magnitude else 0f
        val output = if (spin > gyroLastOutput) spin else gyroLastOutput * gyroDamping
        gyroLastOutput = output
        _gyroscope.value = output
    }

    private fun handleProximity(event: SensorEvent) {
        val maxRange = proximitySensor?.maximumRange ?: 5f
        _proximity.value = event.values[0] < maxRange
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
