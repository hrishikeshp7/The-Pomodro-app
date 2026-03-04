package com.pomodoro.app.data.repository

import com.pomodoro.app.data.db.SessionDao
import com.pomodoro.app.data.model.PomodoroSession
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    fun getAllSessions(): Flow<List<PomodoroSession>> = sessionDao.getAllSessions()
    fun getSessionsSince(startTime: Long): Flow<List<PomodoroSession>> = sessionDao.getSessionsSince(startTime)
    fun getFocusSessionCountSince(startTime: Long): Flow<Int> = sessionDao.getFocusSessionCountSince(startTime)
    fun getTotalFocusMinutesSince(startTime: Long): Flow<Int?> = sessionDao.getTotalFocusMinutesSince(startTime)
    suspend fun insertSession(session: PomodoroSession): Long = sessionDao.insertSession(session)
    suspend fun deleteSession(session: PomodoroSession) = sessionDao.deleteSession(session)
}
