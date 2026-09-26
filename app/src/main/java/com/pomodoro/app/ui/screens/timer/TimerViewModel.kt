package com.pomodoro.app.ui.screens.timer

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.db.AppDatabase
import com.pomodoro.app.data.model.PomodoroSession
import com.pomodoro.app.data.model.Task
import com.pomodoro.app.data.repository.SessionRepository
import com.pomodoro.app.util.FlipDetector
import com.pomodoro.app.util.HapticManager
import com.pomodoro.app.util.PreferencesManager
import com.pomodoro.app.util.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class TimerUiState(
    val timeLeftSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isBreak: Boolean = false,
    val isLongBreak: Boolean = false,
    val completedSessions: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
    val selectedTask: Task? = null,
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val currentStreak: Int = 0,
    val showSessionComplete: Boolean = false,
    val soundEnabled: Boolean = true,
    val flipToFocusEnabled: Boolean = false,
    val distractionTimerEnabled: Boolean = false,
    val isFaceDown: Boolean = false,
    val isDistracted: Boolean = false,
    val distractedSeconds: Int = 0,
    val hapticMetronomeEnabled: Boolean = false,
    val hapticMetronomeIntervalMinutes: Int = 5,
    val ambientDisplayEnabled: Boolean = false
) {
    /** True whenever the timer face should render as the minimal AOD-style display. */
    val showAmbientDisplay: Boolean get() = (isFaceDown && !showSessionComplete) || ambientDisplayEnabled
}

private data class FocusFeatureSettings(
    val flipToFocusEnabled: Boolean,
    val distractionTimerEnabled: Boolean,
    val hapticMetronomeEnabled: Boolean,
    val hapticMetronomeIntervalMinutes: Int,
    val ambientDisplayEnabled: Boolean
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sessionRepository = SessionRepository(db.sessionDao())
    private val preferencesManager = PreferencesManager(application)

    val hapticManager = HapticManager(application)
    val soundManager = SoundManager(application)

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var distractionJob: Job? = null

    private val flipDetector = FlipDetector(
        context = application,
        onFaceDown = { onDeviceFaceDown() },
        onFaceUp = { onDeviceFaceUp() }
    )

    // Track global settings vs task specific settings
    private var globalFocusDuration = 25
    private var globalShortBreak = 5
    private var globalLongBreak = 15
    private var globalSessionsBeforeLongBreak = 4

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.focusDuration,
                preferencesManager.shortBreak,
                preferencesManager.longBreak,
                preferencesManager.sessionsBeforeLongBreak,
                preferencesManager.currentStreak
            ) { focus, shortBreak, longBreak, sessions, streak ->
                globalFocusDuration = focus
                globalShortBreak = shortBreak
                globalLongBreak = longBreak
                globalSessionsBeforeLongBreak = sessions

                // If a task is selected, we use the task's settings, otherwise global
                val currentTask = _uiState.value.selectedTask
                val activeFocus = currentTask?.focusDuration ?: focus
                val activeShortBreak = currentTask?.shortBreakDuration ?: shortBreak
                val activeLongBreak = currentTask?.longBreakDuration ?: longBreak
                val activeSessions = currentTask?.sessionsBeforeLongBreak ?: sessions

                _uiState.value.copy(
                    focusDuration = activeFocus,
                    shortBreakDuration = activeShortBreak,
                    longBreakDuration = activeLongBreak,
                    sessionsBeforeLongBreak = activeSessions,
                    currentStreak = streak
                )
            }.collect { state ->
                if (!_uiState.value.isRunning && !_uiState.value.isPaused) {
                    val totalSec = if (_uiState.value.isBreak) {
                        if (_uiState.value.isLongBreak) state.longBreakDuration * 60
                        else state.shortBreakDuration * 60
                    } else {
                        state.focusDuration * 60
                    }
                    _uiState.value = state.copy(
                        timeLeftSeconds = totalSec,
                        totalSeconds = totalSec
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        focusDuration = state.focusDuration,
                        shortBreakDuration = state.shortBreakDuration,
                        longBreakDuration = state.longBreakDuration,
                        sessionsBeforeLongBreak = state.sessionsBeforeLongBreak,
                        currentStreak = state.currentStreak
                    )
                }
            }
        }

        // Observe soundEnabled preference
        viewModelScope.launch {
            preferencesManager.soundEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(soundEnabled = enabled)
                soundManager.setSoundEnabled(enabled)
            }
        }

        // Observe flip-to-focus, distraction timer, haptic metronome and ambient display settings
        viewModelScope.launch {
            combine(
                preferencesManager.flipToFocusEnabled,
                preferencesManager.distractionTimerEnabled,
                preferencesManager.hapticMetronomeEnabled,
                preferencesManager.hapticMetronomeIntervalMinutes,
                preferencesManager.ambientDisplayEnabled
            ) { flip, distraction, metronome, interval, ambient ->
                FocusFeatureSettings(flip, distraction, metronome, interval, ambient)
            }.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    flipToFocusEnabled = settings.flipToFocusEnabled,
                    distractionTimerEnabled = settings.distractionTimerEnabled,
                    hapticMetronomeEnabled = settings.hapticMetronomeEnabled,
                    hapticMetronomeIntervalMinutes = settings.hapticMetronomeIntervalMinutes,
                    ambientDisplayEnabled = settings.ambientDisplayEnabled
                )
                if (!settings.flipToFocusEnabled && _uiState.value.isFaceDown) {
                    stopDistractionTimer()
                    _uiState.value = _uiState.value.copy(isFaceDown = false)
                }
            }
        }
    }

    /** Starts listening to the accelerometer; call from the Timer screen's onResume. */
    fun startFlipMonitoring() {
        flipDetector.start()
    }

    /** Stops listening to the accelerometer; call from the Timer screen's onPause. */
    fun stopFlipMonitoring() {
        flipDetector.stop()
        if (_uiState.value.isFaceDown) {
            stopDistractionTimer()
            _uiState.value = _uiState.value.copy(isFaceDown = false)
        }
    }

    private fun onDeviceFaceDown() {
        val state = _uiState.value
        if (!state.flipToFocusEnabled || state.isFaceDown) return

        hapticManager.timerStart()
        val wasDistracted = state.isDistracted
        _uiState.value = state.copy(isFaceDown = true)
        if (wasDistracted) {
            stopDistractionTimer()
        }
        if (!_uiState.value.isRunning && !_uiState.value.showSessionComplete) {
            startTimer()
        }
    }

    private fun onDeviceFaceUp() {
        val state = _uiState.value
        if (!state.flipToFocusEnabled || !state.isFaceDown) return

        _uiState.value = state.copy(isFaceDown = false)
        if (state.isRunning) {
            if (state.distractionTimerEnabled) {
                startDistractionTimer()
            } else {
                pauseTimer()
            }
        }
    }

    private fun startDistractionTimer() {
        pauseTimer()
        distractionJob?.cancel()
        _uiState.value = _uiState.value.copy(isDistracted = true, distractedSeconds = 0)
        distractionJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.value = _uiState.value.copy(distractedSeconds = _uiState.value.distractedSeconds + 1)
            }
        }
    }

    private fun stopDistractionTimer() {
        distractionJob?.cancel()
        distractionJob = null
        if (_uiState.value.isDistracted) {
            _uiState.value = _uiState.value.copy(isDistracted = false, distractedSeconds = 0)
        }
    }

    private fun maybeFireHapticMetronome(secondsLeft: Int) {
        val state = _uiState.value
        if (!state.hapticMetronomeEnabled || state.isBreak) return
        val intervalSeconds = state.hapticMetronomeIntervalMinutes * 60
        if (intervalSeconds <= 0) return
        val elapsed = state.totalSeconds - secondsLeft
        if (elapsed > 0 && elapsed % intervalSeconds == 0) {
            hapticManager.metronomeTick()
        }
    }

    fun selectTask(task: Task?) {
        val focus = task?.focusDuration ?: globalFocusDuration
        val shortBreak = task?.shortBreakDuration ?: globalShortBreak
        val longBreak = task?.longBreakDuration ?: globalLongBreak
        val sessions = task?.sessionsBeforeLongBreak ?: globalSessionsBeforeLongBreak

        val totalSec = focus * 60

        _uiState.value = _uiState.value.copy(
            selectedTask = task,
            focusDuration = focus,
            shortBreakDuration = shortBreak,
            longBreakDuration = longBreak,
            sessionsBeforeLongBreak = sessions,
            timeLeftSeconds = totalSec,
            totalSeconds = totalSec,
            isBreak = false,
            isLongBreak = false,
            isRunning = false,
            isPaused = false
        )
        countDownTimer?.cancel()
    }

    fun startTimer() {
        val state = _uiState.value
        val millisLeft = state.timeLeftSeconds * 1000L

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millisLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                _uiState.value = _uiState.value.copy(
                    timeLeftSeconds = secondsLeft,
                    isRunning = true,
                    isPaused = false
                )
                maybeFireHapticMetronome(secondsLeft)
            }

            override fun onFinish() {
                onTimerComplete()
            }
        }.start()

        _uiState.value = state.copy(isRunning = true, isPaused = false)
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false, isPaused = true)
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        stopDistractionTimer()
        val state = _uiState.value
        val totalSec = if (state.isBreak) {
            if (state.isLongBreak) state.longBreakDuration * 60
            else state.shortBreakDuration * 60
        } else {
            state.focusDuration * 60
        }
        _uiState.value = state.copy(
            timeLeftSeconds = totalSec,
            totalSeconds = totalSec,
            isRunning = false,
            isPaused = false
        )
    }

    fun skipToNext() {
        countDownTimer?.cancel()
        stopDistractionTimer()
        moveToNextPhase()
    }

    fun dismissSessionComplete() {
        hapticManager.buttonClick()
        _uiState.value = _uiState.value.copy(showSessionComplete = false)
    }

    private fun onTimerComplete() {
        val state = _uiState.value

        if (!state.isBreak) {
            // Completed a focus session — triumphant feedback
            hapticManager.sessionComplete()
            soundManager.playSessionComplete()

            viewModelScope.launch {
                sessionRepository.insertSession(
                    PomodoroSession(
                        taskId = state.selectedTask?.id,
                        taskTitle = state.selectedTask?.title ?: "",
                        durationMinutes = state.focusDuration,
                        sessionType = "focus"
                    )
                )
                updateStreak()
            }
            _uiState.value = state.copy(
                completedSessions = state.completedSessions + 1,
                showSessionComplete = true,
                isRunning = false,
                isPaused = false
            )
        } else {
            // Break ended — gentle nudge back to focus
            hapticManager.timerStart()
            _uiState.value = state.copy(
                isRunning = false,
                isPaused = false
            )
        }
        moveToNextPhase()
    }

    private fun moveToNextPhase() {
        val state = _uiState.value
        if (state.isBreak) {
            // Move to focus
            val totalSec = state.focusDuration * 60
            _uiState.value = state.copy(
                isBreak = false,
                isLongBreak = false,
                timeLeftSeconds = totalSec,
                totalSeconds = totalSec,
                isRunning = false,
                isPaused = false
            )
        } else {
            // Move to break — play break sound after session complete sound
            viewModelScope.launch {
                kotlinx.coroutines.delay(800)
                hapticManager.breakStart()
                soundManager.playBreakStart()
            }
            val completed = state.completedSessions
            val isLong = completed > 0 && completed % state.sessionsBeforeLongBreak == 0
            val breakDuration = if (isLong) state.longBreakDuration else state.shortBreakDuration
            val totalSec = breakDuration * 60
            _uiState.value = state.copy(
                isBreak = true,
                isLongBreak = isLong,
                timeLeftSeconds = totalSec,
                totalSeconds = totalSec,
                isRunning = false,
                isPaused = false
            )
        }
    }

    private suspend fun updateStreak() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastDate = preferencesManager.lastSessionDate.first()
        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        val newStreak = when {
            lastDate == today -> _uiState.value.currentStreak // Already counted today
            lastDate == yesterday -> _uiState.value.currentStreak + 1
            else -> 1 // Streak broken (gap > 1 day or first session)
        }

        preferencesManager.setCurrentStreak(newStreak)
        preferencesManager.setLastSessionDate(today)
        _uiState.value = _uiState.value.copy(currentStreak = newStreak)
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        distractionJob?.cancel()
        flipDetector.stop()
        hapticManager.release()
        soundManager.release()
    }
}
