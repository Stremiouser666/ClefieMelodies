package com.clefie.melodies.sensor

import androidx.compose.ui.input.pointer.PointerEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GestureController {

    // ── Exposed StateFlows ──────────────────────────────────────────────────

    // Vertical swipe: up = +1.0, down = -1.0, idle = 0.0
    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch

    // Hold duration 0.0–1.0
    private val _holdIntensity = MutableStateFlow(0f)
    val holdIntensity: StateFlow<Float> = _holdIntensity

    // Fingers currently down
    private val _fingerCount = MutableStateFlow(0)
    val fingerCount: StateFlow<Int> = _fingerCount

    // Pressure 0.0–1.0
    private val _pressure = MutableStateFlow(0f)
    val pressure: StateFlow<Float> = _pressure

    // Signed horizontal velocity: left = -1.0, right = +1.0
    private val _swipeVelocity = MutableStateFlow(0f)
    val swipeVelocity: StateFlow<Float> = _swipeVelocity

    // ── Internal state ──────────────────────────────────────────────────────
    private var holdStartTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastEventTime = 0L

    private var smoothedVelocityX = 0f
    private var smoothedVelocityY = 0f
    private val velocitySmoothing = 0.25f

    private val maxHoldMs   = 3000L
    private val maxVelocity = 2000f

    fun onPointerEvent(event: PointerEvent, screenWidth: Float) {
        val pointers = event.changes.filter { it.pressed }

        _fingerCount.value = pointers.size

        if (pointers.isEmpty()) {
            holdStartTime      = 0L
            smoothedVelocityX  = 0f
            smoothedVelocityY  = 0f
            _swipeVelocity.value = 0f
            _pitch.value         = 0f
            _pressure.value      = 0f
            return
        }

        val primary = pointers.first()
        val now     = System.currentTimeMillis()
        val x       = primary.position.x
        val y       = primary.position.y

        // ── Hold intensity ─────────────────────────────────────────────────
        if (holdStartTime == 0L) holdStartTime = now
        val heldMs = (now - holdStartTime).coerceAtLeast(0L)
        _holdIntensity.value = (heldMs.toFloat() / maxHoldMs).coerceIn(0f, 1f)

        // ── Velocity + pitch (signed) ──────────────────────────────────────
        if (lastEventTime > 0L) {
            val dt   = (now - lastEventTime).coerceAtLeast(1L).toFloat()
            val dx   = x - lastX
            val dy   = y - lastY

            val rawVx = (dx / dt * 1000f) / maxVelocity
            val rawVy = (dy / dt * 1000f) / maxVelocity

            smoothedVelocityX = velocitySmoothing * rawVx + (1f - velocitySmoothing) * smoothedVelocityX
            smoothedVelocityY = velocitySmoothing * rawVy + (1f - velocitySmoothing) * smoothedVelocityY

            // Horizontal: left = negative, right = positive
            _swipeVelocity.value = smoothedVelocityX.coerceIn(-1f, 1f)

            // Vertical: up = positive (dy is negative when swiping up)
            _pitch.value = (-smoothedVelocityY).coerceIn(-1f, 1f)
        }

        // ── Pressure ──────────────────────────────────────────────────────
        // Compose only exposes primary.pressure (0.0–1.0)
        // Many devices lock it at 1.0 — fall back to hold intensity as proxy
        val rawPressure = primary.pressure
        _pressure.value = if (rawPressure > 0f && rawPressure < 0.99f) {
            rawPressure.coerceIn(0f, 1f)
        } else {
            _holdIntensity.value
        }

        lastX = x
        lastY = y
        lastEventTime = now
    }
}
