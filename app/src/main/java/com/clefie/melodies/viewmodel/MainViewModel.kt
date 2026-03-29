package com.clefie.melodies.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clefie.melodies.engine.SequencerEngine
import com.clefie.melodies.sensor.SensorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val sequencer = SequencerEngine()
    private val sensors = SensorController()

    private val _bpm = MutableStateFlow(120f)
    val bpm: StateFlow<Float> = _bpm

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _accel = MutableStateFlow(0f)
    val accel: StateFlow<Float> = _accel

    init {
        start()
    }

    private fun start() {
        sensors.start()

        viewModelScope.launch {
            sensors.amplitude.collect {
                _amplitude.value = it
                updateBpm()
            }
        }

        viewModelScope.launch {
            sensors.acceleration.collect {
                _accel.value = it
                updateBpm()
            }
        }

        sequencer.start { step ->
            // later: trigger notes here
        }
    }

    private fun updateBpm() {
        val newBpm = 80f + (_amplitude.value * 80f) + (_accel.value * 40f)
        _bpm.value = newBpm
        sequencer.setBpm(newBpm)
    }

    override fun onCleared() {
        super.onCleared()
        sequencer.stop()
        sensors.stop()
    }
}
