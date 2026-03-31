package com.clefie.melodies.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class BeatDetector {

    // ── Exposed StateFlows ──────────────────────────────────────────────────
    private val _bpm = MutableStateFlow(120f)
    val bpm: StateFlow<Float> = _bpm

    // Pulses true for one frame on each detected beat
    private val _beatPulse = MutableStateFlow(false)
    val beatPulse: StateFlow<Boolean> = _beatPulse

    // ── Energy history for onset detection ─────────────────────────────────
    // We compare current frame energy against average of recent frames
    private val historySize   = 43        // ~1 second of history at typical buffer rates
    private val energyHistory = FloatArray(historySize)
    private var historyIndex  = 0
    private var historyFilled = false

    // Onset threshold multiplier — how much louder than average triggers a beat
    // Higher = less sensitive, Lower = more sensitive
    private val onsetMultiplier = 1.5f

    // ── BPM smoothing (stable mode) ─────────────────────────────────────────
    // Keep a rolling window of recent inter-onset intervals
    private val intervalWindowSize = 8    // average over last 8 beats for stability
    private val intervalWindow = ArrayDeque<Long>(intervalWindowSize)
    private var lastBeatTime = 0L

    // Smoothed BPM — exponential weighted average for extra stability
    private var smoothedBpm = 120f
    private val bpmSmoothing = 0.15f      // low = very stable, high = more reactive

    // Minimum time between beats (enforces max 240 BPM)
    private val minBeatIntervalMs = 250L

    // ── Called from SensorController mic loop ───────────────────────────────
    fun processPcmBuffer(buffer: ShortArray, read: Int) {
        if (read <= 0) return

        // Calculate RMS energy of this frame
        var sum = 0.0
        for (i in 0 until read) {
            val s = buffer[i].toDouble()
            sum += s * s
        }
        val energy = sqrt(sum / read).toFloat()

        // Store in history
        energyHistory[historyIndex] = energy
        historyIndex = (historyIndex + 1) % historySize
        if (historyIndex == 0) historyFilled = true

        // Need at least a full history window before detecting
        if (!historyFilled) return

        // Average energy over history window
        val avgEnergy = energyHistory.average().toFloat()

        // Onset: current frame energy significantly exceeds average
        val now = System.currentTimeMillis()
        if (energy > avgEnergy * onsetMultiplier &&
            (now - lastBeatTime) > minBeatIntervalMs) {

            onBeatDetected(now)
        }
    }

    private fun onBeatDetected(now: Long) {
        _beatPulse.value = true
        // Reset pulse after a short delay (caller should reset or we reset next frame)

        if (lastBeatTime > 0L) {
            val interval = now - lastBeatTime

            // Only accept intervals in 40–240 BPM range
            val intervalBpm = 60000f / interval
            if (intervalBpm in 40f..240f) {

                // Add to rolling window
                if (intervalWindow.size >= intervalWindowSize) {
                    intervalWindow.removeFirst()
                }
                intervalWindow.addLast(interval)

                // Average interval → BPM
                val avgInterval = intervalWindow.average().toFloat()
                val rawBpm = 60000f / avgInterval

                // Exponential smoothing for stability
                smoothedBpm = bpmSmoothing * rawBpm + (1f - bpmSmoothing) * smoothedBpm
                _bpm.value = smoothedBpm.coerceIn(40f, 240f)
            }
        }

        lastBeatTime = now
    }

    fun resetPulse() {
        _beatPulse.value = false
    }

    fun reset() {
        energyHistory.fill(0f)
        historyIndex  = 0
        historyFilled = false
        intervalWindow.clear()
        lastBeatTime  = 0L
        smoothedBpm   = 120f
        _bpm.value    = 120f
        _beatPulse.value = false
    }
}
