package com.clefie.melodies.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class SensorController(
    private val context: Context,
    private val onMotion: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var accelerometer: Sensor? = null

    // Filtering + smoothing
    private var gravity = FloatArray(3)
    private var linearAcceleration = FloatArray(3)

    private var lastOutput = 0f

    // Tunables (feel free to tweak later)
    private val alpha = 0.8f           // low-pass filter strength
    private val motionThreshold = 0.5f // ignore tiny movements
    private val damping = 0.85f        // smooth output decay

    fun start() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        // FIX: explicit cast removes ambiguity
        sensorManager.unregisterListener(this as SensorEventListener)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // --- Low-pass filter to isolate gravity ---
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        // --- Remove gravity to get linear acceleration ---
        linearAcceleration[0] = event.values[0] - gravity[0]
        linearAcceleration[1] = event.values[1] - gravity[1]
        linearAcceleration[2] = event.values[2] - gravity[2]

        // --- Magnitude of movement ---
        val magnitude = sqrt(
            (linearAcceleration[0] * linearAcceleration[0] +
             linearAcceleration[1] * linearAcceleration[1] +
             linearAcceleration[2] * linearAcceleration[2]).toDouble()
        ).toFloat()

        // --- Threshold to kill idle noise ---
        val motion = if (magnitude > motionThreshold) magnitude else 0f

        // --- Smooth output (prevents jitter/looping feeling) ---
        val output = if (motion > lastOutput) {
            motion
        } else {
            lastOutput * damping
        }

        lastOutput = output

        onMotion(output)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}
