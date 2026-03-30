package com.clefie.melodies.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clefie.melodies.engine.SequencerEngine
import com.clefie.melodies.sensor.SensorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sequencer = SequencerEngine()
    private val sensors   = SensorController(application)

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
            sensors.gyroscope.collect { value ->
                _gyro.value = value
            }
        }
        viewModelScope.launch {
            sensors.proximity.collect { value ->
                _proximity.value = value
            }
        }

        sequencer.start { /* Phase 2: trigger notes here */ }
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
