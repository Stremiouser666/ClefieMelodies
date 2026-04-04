package com.clefie.melodies.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class FlowStep { INTRO, ACTIVATION, MAGIC, DASHBOARD }

class FlowViewModel : ViewModel() {

    private val _step = MutableStateFlow(FlowStep.INTRO)
    val step: StateFlow<FlowStep> = _step

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen

    private val _isAmbient = MutableStateFlow(false)
    val isAmbient: StateFlow<Boolean> = _isAmbient

    private val _isPulsing = MutableStateFlow(false)
    val isPulsing: StateFlow<Boolean> = _isPulsing

    fun onCreateSound() {
        viewModelScope.launch {
            _step.value = FlowStep.ACTIVATION
            delay(3000)
            _step.value = FlowStep.MAGIC
            delay(4000)
            _step.value = FlowStep.DASHBOARD
        }
    }

    fun onBeatPulse() {
        viewModelScope.launch {
            _isPulsing.value = true
            delay(150)
            _isPulsing.value = false
        }
    }

    fun openFullscreen()  { _isFullscreen.value = true }
    fun closeFullscreen() { _isFullscreen.value = false }
    fun toggleAmbient()   { _isAmbient.value = !_isAmbient.value }
}
