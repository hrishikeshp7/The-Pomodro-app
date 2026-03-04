package com.pomodoro.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Manages haptic feedback for immersive Pomodoro interactions.
 * Uses modern VibrationEffect API for rich, nuanced haptic patterns.
 */
class HapticManager(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService<VibratorManager>()
            manager?.defaultVibrator ?: context.getSystemService<Vibrator>()!!
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Light tap — for button clicks */
    fun buttonClick() {
        vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** Double tap — for start timer */
    fun timerStart() {
        val timings = longArrayOf(0, 40, 60, 80)
        val amplitudes = intArrayOf(0, 180, 0, 100)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /** Short pulse — for pause */
    fun timerPause() {
        vibrate(VibrationEffect.createOneShot(50, 120))
    }

    /** Three sharp pulses — for reset */
    fun timerReset() {
        val timings = longArrayOf(0, 50, 50, 50, 50, 50)
        val amplitudes = intArrayOf(0, 150, 0, 150, 0, 150)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /** Triumphant buzz pattern — for session complete */
    fun sessionComplete() {
        val timings = longArrayOf(0, 80, 40, 80, 40, 200, 40, 300)
        val amplitudes = intArrayOf(0, 200, 0, 200, 0, 255, 0, 200)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /** Gentle rising pulse — for break start */
    fun breakStart() {
        val timings = longArrayOf(0, 60, 80, 100, 80, 160)
        val amplitudes = intArrayOf(0, 80, 0, 120, 0, 180)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /** Single firm pulse — for skip */
    fun timerSkip() {
        val timings = longArrayOf(0, 40, 30, 80)
        val amplitudes = intArrayOf(0, 120, 0, 200)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /** Milestone tick — for each completed session dot */
    fun sessionTick() {
        vibrate(VibrationEffect.createOneShot(20, 80))
    }

    private fun vibrate(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(effect)
        }
    }

    fun release() {
        vibrator.cancel()
    }
}
