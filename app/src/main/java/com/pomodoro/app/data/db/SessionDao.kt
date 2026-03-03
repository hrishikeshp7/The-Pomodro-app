package com.pomodoro.app.data.db

import androidx.room.*
import com.pomodoro.app.data.model.PomodoroSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<PomodoroSession>>

    @Query("SELECT * FROM sessions WHERE isCompleted = 1 AND completedAt >= :startTime ORDER BY completedAt DESC")
    fun getSessionsSince(startTime: Long): Flow<List<PomodoroSession>>

    @Query("SELECT COUNT(*) FROM sessions WHERE isCompleted = 1 AND sessionType = 'focus' AND completedAt >= :startTime")
    fun getFocusSessionCountSince(startTime: Long): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM sessions WHERE isCompleted = 1 AND sessionType = 'focus' AND completedAt >= :startTime")
    fun getTotalFocusMinutesSince(startTime: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSession): Long

    @Delete
    suspend fun deleteSession(session: PomodoroSession)
}
