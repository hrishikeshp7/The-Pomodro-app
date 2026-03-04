package com.pomodoro.app.ui.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.db.AppDatabase
import com.pomodoro.app.data.model.PomodoroSession
import com.pomodoro.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class AnalyticsUiState(
    val todaySessions: Int = 0,
    val todayMinutes: Int = 0,
    val weekSessions: Int = 0,
    val weekMinutes: Int = 0,
    val dailyCounts: List<Int> = List(7) { 0 }
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(AppDatabase.getDatabase(application).sessionDao())

    val uiState: StateFlow<AnalyticsUiState> = run {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val weekStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis

        combine(
            repository.getFocusSessionCountSince(todayStart),
            repository.getTotalFocusMinutesSince(todayStart),
            repository.getFocusSessionCountSince(weekStart),
            repository.getTotalFocusMinutesSince(weekStart),
            repository.getSessionsSince(weekStart)
        ) { todaySessions, todayMinutes, weekSessions, weekMinutes, weeklySessionsList ->
            val dailyCounts = calculateDailyCounts(weeklySessionsList, weekStart)
            AnalyticsUiState(
                todaySessions = todaySessions,
                todayMinutes = todayMinutes ?: 0,
                weekSessions = weekSessions,
                weekMinutes = weekMinutes ?: 0,
                dailyCounts = dailyCounts
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
    }

    private fun calculateDailyCounts(sessions: List<PomodoroSession>, weekStart: Long): List<Int> {
        val counts = MutableList(7) { 0 }
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        for (session in sessions) {
            if (session.sessionType == "focus") {
                val dayIndex = ((session.completedAt - weekStart) / dayMillis).toInt()
                if (dayIndex in 0..6) {
                    counts[dayIndex]++
                }
            }
        }
        return counts
    }
}
