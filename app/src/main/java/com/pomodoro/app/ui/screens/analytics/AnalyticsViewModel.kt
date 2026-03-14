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

        repository.getSessionsSince(weekStart).map { weeklySessionsList ->
            var todaySessions = 0
            var todayMinutes = 0
            var weekSessions = 0
            var weekMinutes = 0
            val counts = MutableList(7) { 0 }
            val dayMillis = TimeUnit.DAYS.toMillis(1)

            for (session in weeklySessionsList) {
                if (session.sessionType == "focus") {
                    weekSessions++
                    weekMinutes += session.durationMinutes

                    if (session.completedAt >= todayStart) {
                        todaySessions++
                        todayMinutes += session.durationMinutes
                    }

                    val dayIndex = ((session.completedAt - weekStart) / dayMillis).toInt()
                    if (dayIndex in 0..6) {
                        counts[dayIndex]++
                    }
                }
            }

            AnalyticsUiState(
                todaySessions = todaySessions,
                todayMinutes = todayMinutes,
                weekSessions = weekSessions,
                weekMinutes = weekMinutes,
                dailyCounts = counts
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
    }
}
