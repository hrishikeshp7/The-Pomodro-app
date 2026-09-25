package com.pomodoro.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * Manages sound feedback for the Pomodoro timer using AudioTrack
 * to synthesize rich, layered tones — no external audio files required.
 *
 * Every tone is additive-synthesized from a fundamental plus a handful of
 * quieter harmonics with independent decay rates, which is what makes a
 * synthesized note sound like a bell/chime instead of a flat lab-tone beep.
 */
class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 44100
    private var soundEnabled = true

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    /** No sound played when timer starts, only haptics will be used now */
    fun playTimerStart() {
        // User requested no sound, just soft haptic vibration.
        // HapticManager handles the vibration.
    }

    /** Soft muted knock — for pause */
    fun playTimerPause() {
        if (!soundEnabled) return
        scope.launch {
            playChime(frequency = 480.0, durationMs = 160, volume = 0.5f, brightness = 0.35f)
        }
    }

    /** Quick descending pair — for reset */
    fun playTimerReset() {
        if (!soundEnabled) return
        scope.launch {
            playChime(frequency = 720.0, durationMs = 90, volume = 0.55f, brightness = 0.4f)
            Thread.sleep(35)
            playChime(frequency = 440.0, durationMs = 140, volume = 0.55f, brightness = 0.4f)
        }
    }

    /** Triumphant ascending bell melody — for focus session complete */
    fun playSessionComplete() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(
                Triple(523.25, 160L, 0.7f),  // C5
                Triple(659.25, 160L, 0.75f), // E5
                Triple(783.99, 160L, 0.8f),  // G5
                Triple(1046.5, 420L, 0.85f)  // C6 — long resonant finish
            )
            for ((freq, dur, vol) in notes) {
                playChime(frequency = freq, durationMs = dur.toInt(), volume = vol, brightness = 0.55f)
                Thread.sleep(45)
            }
        }
    }

    /** Warm layered chime with a soft echo tail — for break start */
    fun playBreakStart() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(
                Triple(659.25, 240L, 0.5f),  // E5
                Triple(880.0, 260L, 0.55f),  // A5
                Triple(1108.7, 420L, 0.6f)   // C#6 — long shimmering tail
            )
            for ((freq, dur, vol) in notes) {
                playChime(frequency = freq, durationMs = dur.toInt(), volume = vol, brightness = 0.6f)
                Thread.sleep(60)
            }
        }
    }

    /** Crisp double-tap tick — for skip */
    fun playTimerSkip() {
        if (!soundEnabled) return
        scope.launch {
            playChime(frequency = 950.0, durationMs = 55, volume = 0.45f, brightness = 0.3f)
        }
    }

    /** Soft, woody tick — for button UI interactions */
    fun playButtonClick() {
        if (!soundEnabled) return
        scope.launch {
            playChime(frequency = 1050.0, durationMs = 35, volume = 0.28f, brightness = 0.2f)
        }
    }

    /**
     * Synthesizes and plays a bell/chime-like tone using additive synthesis:
     * a fundamental plus decaying harmonic partials, shaped by a smooth
     * raised-cosine attack and an exponential decay release. This is what
     * separates a "chime" from a single flat sine beep.
     *
     * @param frequency fundamental frequency in Hz
     * @param durationMs total duration in milliseconds
     * @param volume 0.0f..1.0f overall loudness
     * @param brightness 0.0f..1.0f how much of the higher harmonics are audible
     */
    private fun playChime(
        frequency: Double,
        durationMs: Int,
        volume: Float,
        brightness: Float = 0.4f
    ) {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(numSamples)

        // Fundamental + harmonics, each with its own weight and decay rate —
        // higher partials fade faster, giving the tone a natural "ring".
        data class Partial(val ratio: Double, val weight: Double, val decayRate: Double)
        val partials = listOf(
            Partial(1.0, 1.0, 2.2),
            Partial(2.0, 0.30 * brightness.toDouble(), 4.5),
            Partial(3.0, 0.14 * brightness.toDouble(), 6.5),
            Partial(4.0, 0.06 * brightness.toDouble(), 8.5)
        )
        val totalWeight = partials.sumOf { it.weight }
        val maxAmplitude = Short.MAX_VALUE * volume

        val attackSamples = max(1, (numSamples * 0.04).toInt())
        val durationSec = durationMs / 1000.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples

            // Raised-cosine (Hann-style) attack avoids the click/pop of a linear ramp.
            val attackEnvelope = if (i < attackSamples) {
                0.5 * (1.0 - kotlin.math.cos(PI * i / attackSamples))
            } else 1.0

            var sample = 0.0
            for (p in partials) {
                val partialDecay = exp(-p.decayRate * t / durationSec)
                val angularFrequency = 2.0 * PI * frequency * p.ratio
                sample += (p.weight / totalWeight) * partialDecay * sin(angularFrequency * t)
            }

            // Gentle overall tail-off so the buffer ends at silence, not mid-wave.
            val releaseEnvelope = if (progress > 0.85) {
                1.0 - ((progress - 0.85) / 0.15)
            } else 1.0

            buffer[i] = (sample * attackEnvelope * releaseEnvelope * maxAmplitude)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

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
