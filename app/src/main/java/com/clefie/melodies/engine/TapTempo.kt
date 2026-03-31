package com.clefie.melodies.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TapTempo {

    // ── Exposed StateFlows ──────────────────────────────────────────────────
    private val _bpm = MutableStateFlow(0f)    // 0 = not set yet
    val bpm: StateFlow<Float> = _bpm

    private val _synced = MutableStateFlow(false)
    val synced: StateFlow<Boolean> = _synced

    // ── Internal state ──────────────────────────────────────────────────────
    private val tapTimes = ArrayDeque<Long>(maxTaps)
    private var lastSyncTime = 0L

    // Tap expires after 2 seconds of no input — fresh start
    private val tapTimeoutMs = 2000L
    private val maxTaps      = 8     // average over last 8 taps

    // ── Tap to set BPM ─────────────────────────────────────────────────────
    // Call this every time the user taps the TAP BEAT button
    fun onTap() {
        val now = System.currentTimeMillis()

        // If last tap was too long ago, reset and start fresh
        if (tapTimes.isNotEmpty() && (now - tapTimes.last()) > tapTimeoutMs) {
            tapTimes.clear()
        }

        tapTimes.addLast(now)

        // Need at least 2 taps to calculate interval
        if (tapTimes.size < 2) return

        // Keep only last maxTaps entries
        while (tapTimes.size > maxTaps) tapTimes.removeFirst()

        // Average interval across all stored taps
        var totalInterval = 0L
        for (i in 1 until tapTimes.size) {
            totalInterval += tapTimes[i] - tapTimes[i - 1]
        }
        val avgInterval = totalInterval.toFloat() / (tapTimes.size - 1)
        val tappedBpm = (60000f / avgInterval).coerceIn(40f, 240f)

        _bpm.value = tappedBpm
        _synced.value = false   // tapping sets BPM, sync is separate
    }

    // ── Tap to sync phase ──────────────────────────────────────────────────
    // Call this when user taps once to sync visuals to the beat they hear
    fun onSyncTap() {
        lastSyncTime   = System.currentTimeMillis()
        _synced.value  = true
    }

    // Returns ms offset since last sync tap — used by visual engine for phase alignment
    fun syncOffsetMs(): Long {
        if (lastSyncTime == 0L) return 0L
        val bpmValue = _bpm.value.takeIf { it > 0f } ?: return 0L
        val beatIntervalMs = (60000f / bpmValue).toLong()
        return (System.currentTimeMillis() - lastSyncTime) % beatIntervalMs
    }

    fun reset() {
        tapTimes.clear()
        lastSyncTime  = 0L
        _bpm.value    = 0f
        _synced.value = false
    }

    companion object {
        private const val maxTaps = 8
    }
}
