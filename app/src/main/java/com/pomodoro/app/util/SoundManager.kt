package com.pomodoro.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Manages sound feedback for the Pomodoro timer using AudioTrack
 * to generate rich, synthesized tones — no external audio files required.
 */
class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 44100
    private var soundEnabled = true

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    /** Crisp double-beep — played when timer starts */
    fun playTimerStart() {
        if (!soundEnabled) return
        scope.launch {
            playTone(frequency = 880.0, durationMs = 80, volume = 0.7f)
            Thread.sleep(60)
            playTone(frequency = 1100.0, durationMs = 120, volume = 0.8f)
        }
    }

    /** Soft single click — for pause */
    fun playTimerPause() {
        if (!soundEnabled) return
        scope.launch {
            playTone(frequency = 600.0, durationMs = 100, volume = 0.5f)
        }
    }

    /** Quick descending pair — for reset */
    fun playTimerReset() {
        if (!soundEnabled) return
        scope.launch {
            playTone(frequency = 800.0, durationMs = 80, volume = 0.6f)
            Thread.sleep(40)
            playTone(frequency = 500.0, durationMs = 120, volume = 0.6f)
        }
    }

    /** Triumphant ascending melody — for focus session complete */
    fun playSessionComplete() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(
                Pair(523.25, 120L),  // C5
                Pair(659.25, 120L),  // E5
                Pair(783.99, 120L),  // G5
                Pair(1046.5, 280L)   // C6
            )
            for ((freq, dur) in notes) {
                playTone(frequency = freq, durationMs = dur.toInt(), volume = 0.75f)
                Thread.sleep(30)
            }
        }
    }

    /** Gentle soft chime — for break start */
    fun playBreakStart() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(
                Pair(880.0, 200L),   // A5
                Pair(1108.7, 200L),  // C#6
                Pair(1318.5, 350L)   // E6
            )
            for ((freq, dur) in notes) {
                playTone(frequency = freq, durationMs = dur.toInt(), volume = 0.55f, fadeOut = true)
                Thread.sleep(50)
            }
        }
    }

    /** Sharp single tick — for skip */
    fun playTimerSkip() {
        if (!soundEnabled) return
        scope.launch {
            playTone(frequency = 1200.0, durationMs = 60, volume = 0.5f)
        }
    }

    /** Soft tick — for button UI interactions */
    fun playButtonClick() {
        if (!soundEnabled) return
        scope.launch {
            playTone(frequency = 1000.0, durationMs = 30, volume = 0.3f)
        }
    }

    /**
     * Synthesizes and plays a pure sine-wave tone.
     * @param frequency Hz
     * @param durationMs duration in milliseconds
     * @param volume 0.0f..1.0f
     * @param fadeOut apply a fade-out envelope for smoother ending
     */
    private fun playTone(
        frequency: Double,
        durationMs: Int,
        volume: Float,
        fadeOut: Boolean = false
    ) {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val sample = sin(2.0 * PI * frequency * i / sampleRate)
            val envelope = when {
                fadeOut && i > numSamples * 0.7 -> {
                    val fadeProgress = (i - numSamples * 0.7) / (numSamples * 0.3)
                    1.0 - fadeProgress
                }
                i < numSamples * 0.05 -> i / (numSamples * 0.05) // attack
                else -> 1.0
            }
            buffer[i] = (sample * envelope * Short.MAX_VALUE * volume).toInt().toShort()
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(buffer.size * 2, minBufferSize)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }

    fun release() {
        // Coroutine scope handles cleanup automatically
    }
}
