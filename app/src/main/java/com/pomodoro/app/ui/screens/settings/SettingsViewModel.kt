package com.pomodoro.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.model.DefaultPresets
import com.pomodoro.app.data.model.TimerPreset
import com.pomodoro.app.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val darkMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val presets: List<TimerPreset> = DefaultPresets.all
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.focusDuration,
        preferencesManager.shortBreak,
        preferencesManager.longBreak,
        preferencesManager.sessionsBeforeLongBreak,
        preferencesManager.darkMode
    ) { focus, shortBreak, longBreak, sessions, darkMode ->
        SettingsUiState(
            focusDuration = focus,
            shortBreakDuration = shortBreak,
            longBreakDuration = longBreak,
            sessionsBeforeLongBreak = sessions,
            darkMode = darkMode
        )
    }.combine(preferencesManager.soundEnabled) { state, soundEnabled ->
        state.copy(soundEnabled = soundEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setFocusDuration(minutes: Int) {
        viewModelScope.launch { preferencesManager.setFocusDuration(minutes) }
    }

    fun setShortBreak(minutes: Int) {
        viewModelScope.launch { preferencesManager.setShortBreak(minutes) }
    }

    fun setLongBreak(minutes: Int) {
        viewModelScope.launch { preferencesManager.setLongBreak(minutes) }
    }

    fun setSessionsBeforeLongBreak(count: Int) {
        viewModelScope.launch { preferencesManager.setSessionsBeforeLongBreak(count) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDarkMode(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSoundEnabled(enabled) }
    }

    fun applyPreset(preset: TimerPreset) {
        viewModelScope.launch {
            preferencesManager.setFocusDuration(preset.focusMinutes)
            preferencesManager.setShortBreak(preset.shortBreakMinutes)
            preferencesManager.setLongBreak(preset.longBreakMinutes)
            preferencesManager.setSessionsBeforeLongBreak(preset.sessionsBeforeLongBreak)
        }
    }
}
