package com.clefie.melodies.sensor

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GestureController {

    // ── Exposed StateFlows ──────────────────────────────────────────────────

    // Swipe X position normalised 0.0–1.0 → maps to pitch
    private val _pitch = MutableStateFlow(0.5f)
    val pitch: StateFlow<Float> = _pitch

    // Hold duration normalised 0.0–1.0 → maps to intensity/energy boost
    private val _holdIntensity = MutableStateFlow(0f)
    val holdIntensity: StateFlow<Float> = _holdIntensity

    // Number of fingers currently down → maps to chord complexity
    private val _fingerCount = MutableStateFlow(0)
    val fingerCount: StateFlow<Int> = _fingerCount

    // Pressure 0.0–1.0 (falls back gracefully on devices with no pressure sensor)
    private val _pressure = MutableStateFlow(0f)
    val pressure: StateFlow<Float> = _pressure

    // Swipe velocity magnitude → maps to expression/dynamics
    private val _swipeVelocity = MutableStateFlow(0f)
    val swipeVelocity: StateFlow<Float> = _swipeVelocity

    // ── Internal state ──────────────────────────────────────────────────────
    private var holdStartTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastEventTime = 0L

    private val maxHoldMs = 3000L      // hold fully saturates at 3 seconds
    private val maxVelocity = 3000f    // px/s considered max swipe speed

    // ── Called from MainScreen pointerInput ─────────────────────────────────
    fun onPointerEvent(event: PointerEvent, screenWidth: Float) {
        val pointers = event.changes.filter { it.pressed }

        _fingerCount.value = pointers.size

        if (pointers.isEmpty()) {
            // All fingers lifted — decay hold intensity, reset velocity
            holdStartTime = 0L
            _holdIntensity.value = 0f
            _swipeVelocity.value = 0f
            _pressure.value = 0f
            return
        }

        val primary = pointers.first()
        val now = System.currentTimeMillis()

        // ── Pitch from X position ──────────────────────────────────────────
        val x = primary.position.x
        if (screenWidth > 0f) {
            _pitch.value = (x / screenWidth).coerceIn(0f, 1f)
        }

        // ── Hold intensity ─────────────────────────────────────────────────
        if (holdStartTime == 0L) {
            holdStartTime = now
        }
        val heldMs = (now - holdStartTime).coerceAtLeast(0L)
        _holdIntensity.value = (heldMs.toFloat() / maxHoldMs).coerceIn(0f, 1f)

        // ── Swipe velocity ─────────────────────────────────────────────────
        if (lastEventTime > 0L) {
            val dt = (now - lastEventTime).coerceAtLeast(1L).toFloat()
            val dx = x - lastX
            val dy = primary.position.y - lastY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val velocityPxPerSec = dist / dt * 1000f
            _swipeVelocity.value = (velocityPxPerSec / maxVelocity).coerceIn(0f, 1f)
        }

        // ── Pressure (graceful fallback) ───────────────────────────────────
        // Most devices return 1.0 constantly — we detect that and normalise
        val rawPressure = primary.pressure
        _pressure.value = if (rawPressure <= 0f || rawPressure == 1f && pointers.size == 1) {
            // Device doesn't support pressure — use hold intensity as proxy
            _holdIntensity.value
        } else {
            rawPressure.coerceIn(0f, 1f)
        }

        lastX = x
        lastY = primary.position.y
        lastEventTime = now
    }
}
