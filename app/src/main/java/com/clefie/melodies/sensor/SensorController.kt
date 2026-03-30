package com.clefie.melodies.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // Mic amplitude stub — Phase 2 will replace this with real AudioRecord data
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    // ── Accelerometer filter state ──────────────────────────────────────────
    private val accelGravity = FloatArray(3)
    private var accelLastOutput = 0f
    private val accelAlpha = 0.8f
    private val accelThreshold = 0.5f
    private val accelDamping = 0.85f

    // ── Gyroscope filter state ──────────────────────────────────────────────
    private val gyroFiltered = FloatArray(3)
    private var gyroLastOutput = 0f
    private val gyroAlpha = 0.7f       // slightly more responsive than accel
    private val gyroThreshold = 0.05f  // rad/s — kills micro-drift noise
    private val gyroDamping = 0.90f

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
    }

    fun stop() {
        sensorManager.unregisterListener(this as SensorEventListener)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE     -> handleGyroscope(event)
            Sensor.TYPE_PROXIMITY     -> handleProximity(event)
        }
    }

    // ── Handlers ────────────────────────────────────────────────────────────

    private fun handleAccelerometer(event: SensorEvent) {
        // Low-pass filter → isolate gravity component
        accelGravity[0] = accelAlpha * accelGravity[0] + (1 - accelAlpha) * event.values[0]
        accelGravity[1] = accelAlpha * accelGravity[1] + (1 - accelAlpha) * event.values[1]
        accelGravity[2] = accelAlpha * accelGravity[2] + (1 - accelAlpha) * event.values[2]

        // Subtract gravity → linear acceleration only
        val lx = event.values[0] - accelGravity[0]
        val ly = event.values[1] - accelGravity[1]
        val lz = event.values[2] - accelGravity[2]

        val magnitude = sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()
        val motion = if (magnitude > accelThreshold) magnitude else 0f

        // Rising edge: pass through; falling edge: decay smoothly
        val output = if (motion > accelLastOutput) motion else accelLastOutput * accelDamping
        accelLastOutput = output
        _acceleration.value = output
    }

    private fun handleGyroscope(event: SensorEvent) {
        // Low-pass filter on each axis (rad/s)
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
        // maximumRange varies by device (1cm, 5cm, etc.) — near = below max range
        val maxRange = proximitySensor?.maximumRange ?: 5f
        _proximity.value = event.values[0] < maxRange
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}
