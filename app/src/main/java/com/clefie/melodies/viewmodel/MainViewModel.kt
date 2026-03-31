package com.clefie.melodies.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clefie.melodies.engine.SequencerEngine
import com.clefie.melodies.engine.TapTempo
import com.clefie.melodies.sensor.GestureController
import com.clefie.melodies.sensor.SensorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sequencer  = SequencerEngine()
    private val sensors    = SensorController(application)
    private val tapTempo   = TapTempo()
    val gestures           = GestureController()

    // ── BPM source tracking ─────────────────────────────────────────────────
    // Priority: tap tempo > beat detection > motion/mic fallback
    private val _bpm = MutableStateFlow(120f)
    val bpm: StateFlow<Float> = _bpm

    private val _beatPulse = MutableStateFlow(false)
    val beatPulse: StateFlow<Boolean> = _beatPulse

    private val _tapBpm = MutableStateFlow(0f)
    val tapBpm: StateFlow<Float> = _tapBpm

    private val _detectedBpm = MutableStateFlow(120f)
    val detectedBpm: StateFlow<Float> = _detectedBpm

    // ── Sensor StateFlows ───────────────────────────────────────────────────
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

    // ── Gesture proxies ─────────────────────────────────────────────────────
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
        viewModelScope.launch {
            sensors.beatDetector.bpm.collect { value ->
                _detectedBpm.value = value
                updateBpm()
            }
        }
        viewModelScope.launch {
            sensors.beatDetector.beatPulse.collect { value ->
                _beatPulse.value = value
            }
        }
        viewModelScope.launch {
            tapTempo.bpm.collect { value ->
                _tapBpm.value = value
                updateBpm()
            }
        }
        viewModelScope.launch {
            gestures.swipeVelocity.collect { updateBpm() }
        }

        sequencer.start { /* Phase 4: trigger visual pulse on step */ }
    }

    // ── BPM priority logic ──────────────────────────────────────────────────
    private fun updateBpm() {
        val finalBpm = when {
            // Tap tempo takes priority when set
            _tapBpm.value > 0f -> _tapBpm.value

            // Beat detection is available — use it, modulated by motion/velocity
            _detectedBpm.value > 0f -> {
                val modulation = (_accel.value * 10f) +
                                 (gestures.swipeVelocity.value * 20f)
                (_detectedBpm.value + modulation).coerceIn(40f, 240f)
            }

            // Fallback: mic + motion only (no beat detected yet)
            else -> {
                80f + (_amplitude.value * 80f) +
                      (_accel.value * 60f) +
                      (gestures.swipeVelocity.value * 40f)
            }
        }

        _bpm.value = finalBpm.coerceIn(40f, 240f)
        sequencer.setBpm(finalBpm)
    }

    // ── Public tap actions ──────────────────────────────────────────────────
    fun onTapBeat() {
        tapTempo.onTap()
    }

    fun onSyncTap() {
        tapTempo.onSyncTap()
    }

    fun resetTapTempo() {
        tapTempo.reset()
        updateBpm()
    }

    override fun onCleared() {
        super.onCleared()
        sequencer.stop()
        sensors.stop()
    }
}
