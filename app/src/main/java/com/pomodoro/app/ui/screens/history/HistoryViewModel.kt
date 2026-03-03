package com.pomodoro.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.db.AppDatabase
import com.pomodoro.app.data.model.PomodoroSession
import com.pomodoro.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.*

data class HistoryUiState(
    val sessions: List<PomodoroSession> = emptyList()
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(AppDatabase.getDatabase(application).sessionDao())

    val uiState: StateFlow<HistoryUiState> = repository.getAllSessions()
        .map { HistoryUiState(sessions = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
