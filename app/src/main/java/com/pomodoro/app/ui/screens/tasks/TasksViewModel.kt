package com.pomodoro.app.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.db.AppDatabase
import com.pomodoro.app.data.model.Task
import com.pomodoro.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val newTaskTitle: String = ""
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(AppDatabase.getDatabase(application).taskDao())

    private val _newTaskTitle = MutableStateFlow("")
    
    val uiState: StateFlow<TasksUiState> = combine(
        repository.getAllTasks(),
        _newTaskTitle
    ) { tasks, title ->
        TasksUiState(tasks = tasks, newTaskTitle = title)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun updateNewTaskTitle(title: String) {
        _newTaskTitle.value = title
    }

    fun addTask() {
        val title = _newTaskTitle.value.trim()
        if (title.isNotEmpty()) {
            viewModelScope.launch {
                repository.insertTask(Task(title = title))
                _newTaskTitle.value = ""
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
