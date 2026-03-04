package com.pomodoro.app.ui.screens.timer

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.db.AppDatabase
import com.pomodoro.app.data.model.PomodoroSession
import com.pomodoro.app.data.model.Task
import com.pomodoro.app.data.repository.SessionRepository
import com.pomodoro.app.util.PreferencesManager
import kotlinx.coroutines.flow.*
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
    val showSessionComplete: Boolean = false
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sessionRepository = SessionRepository(db.sessionDao())
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.focusDuration,
                preferencesManager.shortBreak,
                preferencesManager.longBreak,
                preferencesManager.sessionsBeforeLongBreak,
                preferencesManager.currentStreak
            ) { focus, shortBreak, longBreak, sessions, streak ->
                _uiState.value.copy(
                    focusDuration = focus,
                    shortBreakDuration = shortBreak,
                    longBreakDuration = longBreak,
                    sessionsBeforeLongBreak = sessions,
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
    }

    fun selectTask(task: Task?) {
        _uiState.value = _uiState.value.copy(selectedTask = task)
    }

    fun startTimer() {
        val state = _uiState.value
        val millisLeft = state.timeLeftSeconds * 1000L

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millisLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _uiState.value = _uiState.value.copy(
                    timeLeftSeconds = (millisUntilFinished / 1000).toInt(),
                    isRunning = true,
                    isPaused = false
                )
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
        moveToNextPhase()
    }

    fun dismissSessionComplete() {
        _uiState.value = _uiState.value.copy(showSessionComplete = false)
    }

    private fun onTimerComplete() {
        val state = _uiState.value

        if (!state.isBreak) {
            // Completed a focus session
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
            // Move to break
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
    }
}
