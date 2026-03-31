package com.clefie.melodies.sensor

import androidx.compose.ui.input.pointer.PointerEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt
import kotlin.math.sign

class GestureController {

    // ── Exposed StateFlows ──────────────────────────────────────────────────

    // Vertical swipe direction: up = +1.0, down = -1.0, idle = 0.0
    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch

    // Hold duration 0.0–1.0 → intensity/energy boost
    private val _holdIntensity = MutableStateFlow(0f)
    val holdIntensity: StateFlow<Float> = _holdIntensity

    // Fingers currently down → chord complexity
    private val _fingerCount = MutableStateFlow(0)
    val fingerCount: StateFlow<Int> = _fingerCount

    // Pressure 0.0–1.0 (uses touch size as proxy on most devices)
    private val _pressure = MutableStateFlow(0f)
    val pressure: StateFlow<Float> = _pressure

    // Signed swipe velocity: left = negative, right = positive (-1.0 to +1.0)
    private val _swipeVelocity = MutableStateFlow(0f)
    val swipeVelocity: StateFlow<Float> = _swipeVelocity

    // ── Internal state ──────────────────────────────────────────────────────
    private var holdStartTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastEventTime = 0L

    // Smoothing for velocity and pitch to prevent flicker
    private var smoothedVelocityX = 0f
    private var smoothedVelocityY = 0f
    private val velocitySmoothing = 0.25f   // higher = more responsive

    private val maxHoldMs   = 3000L    // hold fully saturates at 3s
    private val maxVelocity = 2000f    // px/s = max swipe speed reference

    fun onPointerEvent(event: PointerEvent, screenWidth: Float) {
        val pointers = event.changes.filter { it.pressed }

        _fingerCount.value = pointers.size

        if (pointers.isEmpty()) {
            holdStartTime = 0L
            // Decay smoothly to zero instead of snapping
            smoothedVelocityX = 0f
            smoothedVelocityY = 0f
            _swipeVelocity.value = 0f
            _pitch.value        = 0f
            _pressure.value     = 0f
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

        // ── Velocity (signed) ──────────────────────────────────────────────
        if (lastEventTime > 0L) {
            val dt = (now - lastEventTime).coerceAtLeast(1L).toFloat()

            val dx = x - lastX
            val dy = y - lastY

            // px/s normalised to -1..+1
            val rawVx = (dx / dt * 1000f) / maxVelocity
            val rawVy = (dy / dt * 1000f) / maxVelocity

            // Exponential smoothing
            smoothedVelocityX = velocitySmoothing * rawVx + (1f - velocitySmoothing) * smoothedVelocityX
            smoothedVelocityY = velocitySmoothing * rawVy + (1f - velocitySmoothing) * smoothedVelocityY

            // Horizontal: left = negative, right = positive
            _swipeVelocity.value = smoothedVelocityX.coerceIn(-1f, 1f)

            // Vertical: up = positive (dy negative), down = negative (dy positive)
            _pitch.value = (-smoothedVelocityY).coerceIn(-1f, 1f)
        }

        // ── Pressure ──────────────────────────────────────────────────────
        // Android MotionEvent.getPressure() is unreliable on most devices (always 1.0)
        // getSize() (touch contact area) is a much better proxy for press force
        val rawPressure  = primary.pressure   // 0.0–1.0 (often stuck at 1.0)
        val rawSize      = primary.size        // 0.0–1.0 touch contact area

        _pressure.value = when {
            // Real pressure sensor available — use it
            rawPressure > 0f && rawPressure < 0.99f -> rawPressure.coerceIn(0f, 1f)
            // Fall back to touch size (wider touch = more pressure)
            rawSize > 0f -> rawSize.coerceIn(0f, 1f)
            // Last resort: hold intensity proxy
            else -> _holdIntensity.value
        }

        lastX = x
        lastY = y
        lastEventTime = now
    }
}
