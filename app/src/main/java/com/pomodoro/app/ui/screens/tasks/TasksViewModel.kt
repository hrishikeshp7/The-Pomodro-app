package com.pomodoro.app.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.db.AppDatabase
import com.pomodoro.app.data.model.DefaultPresets
import com.pomodoro.app.data.model.Task
import com.pomodoro.app.data.model.TimerPreset
import com.pomodoro.app.data.repository.TaskRepository
import com.pomodoro.app.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isAddingTask: Boolean = false,
    val newTaskTitle: String = "",
    val selectedPreset: TimerPreset? = null,
    val customPresets: List<TimerPreset> = emptyList()
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(AppDatabase.getDatabase(application).taskDao())
    private val preferencesManager = PreferencesManager(application)

    private val _isAddingTask = MutableStateFlow(false)
    private val _newTaskTitle = MutableStateFlow("")
    private val _selectedPreset = MutableStateFlow<TimerPreset?>(null)
    
    // Custom presets from PreferencesManager could be added here in the future
    private val _customPresets = MutableStateFlow<List<TimerPreset>>(emptyList())

    val uiState: StateFlow<TasksUiState> = combine(
        repository.getAllTasks(),
        _isAddingTask,
        _newTaskTitle,
        _selectedPreset,
        _customPresets
    ) { tasks, isAdding, title, preset, customPresets ->
        TasksUiState(
            tasks = tasks,
            isAddingTask = isAdding,
            newTaskTitle = title,
            selectedPreset = preset,
            customPresets = customPresets
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    init {
        // Load custom presets
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

    fun showAddTaskDialog() {
        _newTaskTitle.value = ""
        _selectedPreset.value = DefaultPresets.classic
        _isAddingTask.value = true
    }

    fun hideAddTaskDialog() {
        _isAddingTask.value = false
    }

    fun updateNewTaskTitle(title: String) {
        _newTaskTitle.value = title
    }

    fun updateSelectedPreset(preset: TimerPreset) {
        _selectedPreset.value = preset
    }

    fun addTask() {
        val title = _newTaskTitle.value.trim()
        val preset = _selectedPreset.value ?: DefaultPresets.classic

        if (title.isNotEmpty()) {
            viewModelScope.launch {
                repository.insertTask(
                    Task(
                        title = title,
                        focusDuration = preset.focusMinutes,
                        shortBreakDuration = preset.shortBreakMinutes,
                        longBreakDuration = preset.longBreakMinutes,
                        sessionsBeforeLongBreak = preset.sessionsBeforeLongBreak,
                        presetName = preset.name
                    )
                )
                _isAddingTask.value = false
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
}
