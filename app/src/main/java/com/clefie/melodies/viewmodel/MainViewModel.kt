package com.clefie.melodies.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clefie.melodies.engine.SequencerEngine
import com.clefie.melodies.sensor.GestureController
import com.clefie.melodies.sensor.SensorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sequencer  = SequencerEngine()
    private val sensors    = SensorController(application)
    val gestures           = GestureController()   // public so MainScreen can forward events

    // ── Sensor StateFlows ───────────────────────────────────────────────────
    private val _bpm       = MutableStateFlow(120f)
    val bpm: StateFlow<Float> = _bpm

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _accel     = MutableStateFlow(0f)
    val accel: StateFlow<Float> = _accel

    private val _gyro      = MutableStateFlow(0f)
    val gyro: StateFlow<Float> = _gyro

    private val _proximity = MutableStateFlow(false)
    val proximity: StateFlow<Boolean> = _proximity

    private val _shake     = MutableStateFlow(false)
    val shake: StateFlow<Boolean> = _shake

    private val _tiltX     = MutableStateFlow(0f)
    val tiltX: StateFlow<Float> = _tiltX

    private val _tiltY     = MutableStateFlow(0f)
    val tiltY: StateFlow<Float> = _tiltY

    // ── Gesture StateFlows (proxied from GestureController) ─────────────────
    val pitch         get() = gestures.pitch
    val holdIntensity get() = gestures.holdIntensity
    val fingerCount   get() = gestures.fingerCount
    val pressure      get() = gestures.pressure
    val swipeVelocity get() = gestures.swipeVelocity

    init { start() }

    private fun start() {
        sensors.start()

        viewModelScope.launch {
            sensors.amplitude.collect { value ->
                _amplitude.value = value
                updateBpm()
            }
        }
        viewModelScope.launch {
            sensors.acceleration.collect { value ->
                _accel.value = value
                updateBpm()
            }
        }
        viewModelScope.launch {
            sensors.gyroscope.collect { value -> _gyro.value = value }
        }
        viewModelScope.launch {
            sensors.proximity.collect { value -> _proximity.value = value }
        }
        viewModelScope.launch {
            sensors.shake.collect { value -> _shake.value = value }
        }
        viewModelScope.launch {
            sensors.tiltX.collect { value -> _tiltX.value = value }
        }
        viewModelScope.launch {
            sensors.tiltY.collect { value -> _tiltY.value = value }
        }

        sequencer.start { /* Phase 4: trigger visual pulses here */ }
    }

    private fun updateBpm() {
        val newBpm = 80f + (_amplitude.value * 80f) + (_accel.value * 60f)
        _bpm.value = newBpm.coerceIn(40f, 240f)
        sequencer.setBpm(newBpm)
    }

    override fun onCleared() {
        super.onCleared()
        sequencer.stop()
        sensors.stop()
    }
}
