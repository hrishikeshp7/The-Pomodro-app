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
import kotlin.random.Random

/**
 * Manages sound feedback for the Pomodoro timer using AudioTrack to
 * synthesize two distinct, purpose-built timbres — no external audio files.
 *
 * This mirrors how real productivity/timer apps (and platform UI sound
 * kits) actually split their sound palette:
 *  - UI taps (button/pause/reset/skip) are short *percussive* sounds — a
 *    filtered noise transient plus a low sine "thump", the same recipe
 *    behind system click/tap sounds. They are not musical notes at all.
 *  - Completion chimes (session complete/break start) use a "marimba"
 *    timbre: a fundamental plus a bright, fast-decaying *inharmonic*
 *    overtone near 4x the fundamental. That inharmonic overtone — not a
 *    stacked harmonic series like a bell — is what reads as a pleasant
 *    wooden mallet "ding" instead of a lab-tone beep.
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

    /** Soft, muted thock — for pause */
    fun playTimerPause() {
        if (!soundEnabled) return
        scope.launch {
            playBuffer(renderClick(pitchHz = 300.0, durationMs = 75, volume = 0.4f, crispness = 0.2f), 75)
        }
    }

    /** Quick descending double-tap — for reset */
    fun playTimerReset() {
        if (!soundEnabled) return
        scope.launch {
            playBuffer(renderClick(pitchHz = 650.0, durationMs = 45, volume = 0.4f, crispness = 0.55f), 45)
            Thread.sleep(35)
            playBuffer(renderClick(pitchHz = 380.0, durationMs = 70, volume = 0.42f, crispness = 0.35f), 70)
        }
    }

    /** Ascending marimba run — for focus session complete */
    fun playSessionComplete() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(
                Triple(523.25, 150L, 0.7f),  // C5
                Triple(659.25, 150L, 0.75f), // E5
                Triple(783.99, 150L, 0.8f),  // G5
                Triple(1046.5, 320L, 0.85f)  // C6 — resonant finish
            )
            for ((freq, dur, vol) in notes) {
                playBuffer(renderMarimba(frequency = freq, durationMs = dur.toInt(), volume = vol), dur.toInt())
                Thread.sleep(40)
            }
        }
    }

    /** Gentle marimba arpeggio — for break start */
    fun playBreakStart() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(
                Triple(659.25, 200L, 0.5f),  // E5
                Triple(880.0, 200L, 0.55f),  // A5
                Triple(1108.7, 340L, 0.55f)  // C#6
            )
            for ((freq, dur, vol) in notes) {
                playBuffer(renderMarimba(frequency = freq, durationMs = dur.toInt(), volume = vol), dur.toInt())
                Thread.sleep(55)
            }
        }
    }

    /** Crisp single tap — for skip */
    fun playTimerSkip() {
        if (!soundEnabled) return
        scope.launch {
            playBuffer(renderClick(pitchHz = 1050.0, durationMs = 40, volume = 0.35f, crispness = 0.7f), 40)
        }
    }

    /** Light, high tap — for button UI interactions */
    fun playButtonClick() {
        if (!soundEnabled) return
        scope.launch {
            playBuffer(renderClick(pitchHz = 900.0, durationMs = 30, volume = 0.22f, crispness = 0.75f), 30)
        }
    }

    /**
     * Renders a short percussive UI tap: a band-limited noise transient
     * (the "tick") layered with a low sine "thump" (the body/weight), both
     * with a hard instant attack and a fast exponential decay — the same
     * shape as a real click/tap sound, not a musical tone.
     *
     * @param pitchHz frequency of the low thump component
     * @param crispness 0.0f..1.0f — higher lets more high-frequency noise through
     */
    private fun renderClick(
        pitchHz: Double,
        durationMs: Int,
        volume: Float,
        crispness: Float
    ): ShortArray {
        val numSamples = max(1, (sampleRate * durationMs / 1000.0).toInt())
        val buffer = ShortArray(numSamples)
        val maxAmplitude = Short.MAX_VALUE * volume
        val durationSec = durationMs / 1000.0

        val noiseCutoffHz = 1500.0 + crispness * 6000.0
        val lpCoeff = exp(-2.0 * PI * noiseCutoffHz / sampleRate)
        var lpState = 0.0
        val random = Random(System.nanoTime())

        val thumpDecayRate = 18.0
        val noiseDecayRate = 32.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val thump = sin(2.0 * PI * pitchHz * t) * exp(-thumpDecayRate * t / durationSec)

            val raw = random.nextDouble(-1.0, 1.0)
            lpState = lpState * lpCoeff + raw * (1.0 - lpCoeff)
            val noise = lpState * exp(-noiseDecayRate * t / durationSec)

            val mix = thump * 0.55 + noise * (0.45 + 0.3 * crispness)
            buffer[i] = (mix * maxAmplitude)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return buffer
    }

    /**
     * Renders a marimba-like mallet note: a fundamental plus a bright,
     * fast-decaying *inharmonic* overtone near 3.93x the fundamental (the
     * real acoustic ratio for a marimba bar's second mode), giving it a
     * "wood" character instead of the stacked harmonic series of a bell.
     */
    private fun renderMarimba(
        frequency: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val numSamples = max(1, (sampleRate * durationMs / 1000.0).toInt())
        val buffer = ShortArray(numSamples)
        val maxAmplitude = Short.MAX_VALUE * volume
        val durationSec = durationMs / 1000.0

        data class Partial(val ratio: Double, val weight: Double, val decayRate: Double)
        val partials = listOf(
            Partial(1.0, 1.0, 3.0),
            Partial(3.93, 0.28, 14.0),
            Partial(9.4, 0.07, 26.0)
        )
        val totalWeight = partials.sumOf { it.weight }
        val attackSamples = max(1, (numSamples * 0.01).toInt()) // near-instant mallet strike

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attackEnvelope = if (i < attackSamples) i.toDouble() / attackSamples else 1.0

            var sample = 0.0
            for (p in partials) {
                val decay = exp(-p.decayRate * t / durationSec)
                sample += (p.weight / totalWeight) * decay * sin(2.0 * PI * frequency * p.ratio * t)
            }

            buffer[i] = (sample * attackEnvelope * maxAmplitude)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return buffer
    }

    private fun playBuffer(buffer: ShortArray, durationMs: Int) {
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
