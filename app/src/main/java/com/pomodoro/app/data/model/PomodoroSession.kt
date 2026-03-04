package com.pomodoro.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String = "",
    val durationMinutes: Int = 25,
    val completedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true,
    val reflectionNote: String = "",
    val sessionType: String = "focus" // "focus", "short_break", "long_break"
)
