package com.clefie.melodies.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val MAX_TAPS = 8

class TapTempo {

    // ── Exposed StateFlows ──────────────────────────────────────────────────
    private val _bpm = MutableStateFlow(0f)    // 0 = not set yet
    val bpm: StateFlow<Float> = _bpm

    private val _synced = MutableStateFlow(false)
    val synced: StateFlow<Boolean> = _synced

    // ── Internal state ──────────────────────────────────────────────────────
    private val tapTimes = ArrayDeque<Long>(MAX_TAPS)
    private var lastSyncTime = 0L

    private val tapTimeoutMs = 2000L   // tap expires after 2s of no input
    private val maxTaps      = MAX_TAPS

    // ── Tap to set BPM ─────────────────────────────────────────────────────
    fun onTap() {
        val now = System.currentTimeMillis()

        if (tapTimes.isNotEmpty() && (now - tapTimes.last()) > tapTimeoutMs) {
            tapTimes.clear()
        }

        tapTimes.addLast(now)

        if (tapTimes.size < 2) return

        while (tapTimes.size > maxTaps) tapTimes.removeFirst()

        var totalInterval = 0L
        for (i in 1 until tapTimes.size) {
            totalInterval += tapTimes[i] - tapTimes[i - 1]
        }
        val avgInterval = totalInterval.toFloat() / (tapTimes.size - 1)
        val tappedBpm = (60000f / avgInterval).coerceIn(40f, 240f)

        _bpm.value = tappedBpm
        _synced.value = false
    }

    // ── Tap to sync phase ──────────────────────────────────────────────────
    fun onSyncTap() {
        lastSyncTime  = System.currentTimeMillis()
        _synced.value = true
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
}
