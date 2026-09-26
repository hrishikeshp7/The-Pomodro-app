package com.pomodoro.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects "phone flipped face-down" using the proximity sensor rather than
 * the accelerometer. When the phone is set face-down on a desk, the
 * front-facing proximity sensor (next to the earpiece) is covered by the
 * surface and reports "near"; face-up, nothing is in front of it and it
 * reports "far". This is the same signal most OEM "flip to shhh" / table
 * mode features use — it's a cheap, low-power binary sensor with its own
 * hardware-level threshold/hysteresis, so unlike the accelerometer it
 * doesn't need software smoothing to avoid jitter.
 *
 * Trade-off: it can't tell "resting face-down on a table" apart from
 * "something else is covering the sensor" (a hand, a pocket). That's an
 * accepted limitation of proximity-based flip detection, not a bug.
 */
class FlipDetector(
    context: Context,
    private val onFaceDown: () -> Unit,
    private val onFaceUp: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isNear: Boolean? = null

    fun start() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isNear = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Proximity sensors report a distance in cm, usually clamped to either
        // 0 (covered) or the sensor's maximumRange (uncovered) rather than a
        // continuous value — treat anything short of that max as "covered".
        val near = event.values[0] < event.sensor.maximumRange
        if (near == isNear) return
        isNear = near
        if (near) onFaceDown() else onFaceUp()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
