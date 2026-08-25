package com.pomodoro.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.model.DefaultPresets
import com.pomodoro.app.data.model.TimerPreset
import com.pomodoro.app.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SettingsUiState(
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val darkMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val presets: List<TimerPreset> = DefaultPresets.all,
    val customPresets: List<TimerPreset> = emptyList(),
    val isCreatingProfile: Boolean = false,
    val newProfileName: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    private val _customPresets = MutableStateFlow<List<TimerPreset>>(emptyList())
    private val _isCreatingProfile = MutableStateFlow(false)
    private val _newProfileName = MutableStateFlow("")

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
    }.combine(_customPresets) { state, customPresets ->
        state.copy(customPresets = customPresets)
    }.combine(_isCreatingProfile) { state, isCreating ->
        state.copy(isCreatingProfile = isCreating)
    }.combine(_newProfileName) { state, name ->
        state.copy(newProfileName = name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        viewModelScope.launch {
            preferencesManager.customProfiles.collect { jsonString ->
                if (jsonString.isNotEmpty() && jsonString != "[]") {
                    try {
                        val jsonArray = JSONArray(jsonString)
                        val profiles = mutableListOf<TimerPreset>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            profiles.add(
                                TimerPreset(
                                    name = obj.getString("name"),
                                    focusMinutes = obj.getInt("focusMinutes"),
                                    shortBreakMinutes = obj.getInt("shortBreakMinutes"),
                                    longBreakMinutes = obj.getInt("longBreakMinutes"),
                                    sessionsBeforeLongBreak = obj.getInt("sessionsBeforeLongBreak")
                                )
                            )
                        }
                        _customPresets.value = profiles
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    _customPresets.value = emptyList()
                }
            }
        }
    }

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

    fun showCreateProfileDialog() {
        _newProfileName.value = ""
        _isCreatingProfile.value = true
    }

    fun hideCreateProfileDialog() {
        _isCreatingProfile.value = false
    }

    fun updateNewProfileName(name: String) {
        _newProfileName.value = name
    }

    fun saveCustomProfile() {
        val name = _newProfileName.value.trim()
        if (name.isNotEmpty()) {
            val currentState = uiState.value
            val newPreset = TimerPreset(
                name = name,
                focusMinutes = currentState.focusDuration,
                shortBreakMinutes = currentState.shortBreakDuration,
                longBreakMinutes = currentState.longBreakDuration,
                sessionsBeforeLongBreak = currentState.sessionsBeforeLongBreak
            )

            val updatedProfiles = _customPresets.value + newPreset
            saveProfilesToPrefs(updatedProfiles)
            _isCreatingProfile.value = false
        }
    }

    private fun saveProfilesToPrefs(profiles: List<TimerPreset>) {
        val jsonArray = JSONArray()
        profiles.forEach { preset ->
            val obj = JSONObject()
            obj.put("name", preset.name)
            obj.put("focusMinutes", preset.focusMinutes)
            obj.put("shortBreakMinutes", preset.shortBreakMinutes)
            obj.put("longBreakMinutes", preset.longBreakMinutes)
            obj.put("sessionsBeforeLongBreak", preset.sessionsBeforeLongBreak)
            jsonArray.put(obj)
        }
        viewModelScope.launch {
            preferencesManager.setCustomProfiles(jsonArray.toString())
        }
    }
}
