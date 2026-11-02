package com.pomodoro.app.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.pomodoro.app.R

/**
 * Manages low-latency audio feedback using Android's native SoundPool and
 * bundled uncompressed PCM audio assets in res/raw.
 */
class SoundManager(private val context: Context) {

    private var soundEnabled = true

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(audioAttributes)
        .build()

    // Pre-loaded sound IDs
    private val thockSoundId = soundPool.load(context, R.raw.thock, 1)
    private val buttonClickSoundId = soundPool.load(context, R.raw.button_click, 1)
    private val timerSkipSoundId = soundPool.load(context, R.raw.timer_skip, 1)
    private val timerResetSoundId = soundPool.load(context, R.raw.timer_reset, 1)
    private val sessionCompleteSoundId = soundPool.load(context, R.raw.session_complete, 1)
    private val breakStartSoundId = soundPool.load(context, R.raw.break_start, 1)

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    private fun isDndActive(): Boolean {
        val nm = notificationManager ?: return false
        val filter = nm.currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
               filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private fun canPlaySound(): Boolean {
        if (!soundEnabled) return false
        if (isDndActive()) return false
        val am = audioManager ?: return true
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    private fun playSound(soundId: Int, volume: Float = 1.0f) {
        if (!canPlaySound() || soundId == 0) return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    /** Soft, muted thock — for timer start (matches pause for physical switch feel) */
    fun playTimerStart() {
        playTimerPause()
    }

    /** Soft, muted thock — for pause */
    fun playTimerPause() {
        playSound(thockSoundId)
    }

    /** Quick descending double-tap — for reset */
    fun playTimerReset() {
        playSound(timerResetSoundId)
    }

    /** Ascending marimba run — for focus session complete */
    fun playSessionComplete() {
        playSound(sessionCompleteSoundId)
    }

    /** Gentle marimba arpeggio — for break start */
    fun playBreakStart() {
        playSound(breakStartSoundId)
    }

    /** Crisp single tap — for skip */
    fun playTimerSkip() {
        playSound(timerSkipSoundId)
    }

    /** Light, high tap — for button UI interactions */
    fun playButtonClick() {
        playSound(buttonClickSoundId)
    }

    fun release() {
        soundPool.release()
    }
}
