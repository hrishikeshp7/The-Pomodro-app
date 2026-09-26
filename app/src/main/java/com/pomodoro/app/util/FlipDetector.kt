package com.pomodoro.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects whether the device is resting face-down or face-up from the
 * accelerometer's gravity component on the Z axis, and reports stable
 * transitions via [onFaceDown] / [onFaceUp].
 *
 * Raw readings are debounced: a candidate orientation has to hold for
 * [DEBOUNCE_MS] before it's treated as a real flip, so picking the phone up
 * briefly or pocket jostling doesn't fire spurious events.
 */
class FlipDetector(
    context: Context,
    private val onFaceDown: () -> Unit,
    private val onFaceUp: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var confirmedFaceDown: Boolean? = null
    private var pendingState: Boolean? = null
    private var pendingSince = 0L

    companion object {
        private const val FACE_DOWN_Z_THRESHOLD = -8.5f
        private const val FACE_UP_Z_THRESHOLD = 8.5f
        private const val DEBOUNCE_MS = 600L
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        confirmedFaceDown = null
        pendingState = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val z = event.values[2]
        val candidate = when {
            z < FACE_DOWN_Z_THRESHOLD -> true
            z > FACE_UP_Z_THRESHOLD -> false
            else -> return // ambiguous orientation (device on its side, in motion, etc.)
        }

        val now = System.currentTimeMillis()
        if (candidate != pendingState) {
            pendingState = candidate
            pendingSince = now
            return
        }
        if (candidate == confirmedFaceDown) return
        if (now - pendingSince >= DEBOUNCE_MS) {
            confirmedFaceDown = candidate
            if (candidate) onFaceDown() else onFaceUp()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
