package com.pomodoro.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val FOCUS_DURATION = intPreferencesKey("focus_duration")
        val SHORT_BREAK = intPreferencesKey("short_break")
        val LONG_BREAK = intPreferencesKey("long_break")
        val SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("sessions_before_long_break")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LAST_SESSION_DATE = longPreferencesKey("last_session_date")
    }

    val focusDuration: Flow<Int> = context.dataStore.data.map { it[Keys.FOCUS_DURATION] ?: 25 }
    val shortBreak: Flow<Int> = context.dataStore.data.map { it[Keys.SHORT_BREAK] ?: 5 }
    val longBreak: Flow<Int> = context.dataStore.data.map { it[Keys.LONG_BREAK] ?: 15 }
    val sessionsBeforeLongBreak: Flow<Int> = context.dataStore.data.map { it[Keys.SESSIONS_BEFORE_LONG_BREAK] ?: 4 }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val currentStreak: Flow<Int> = context.dataStore.data.map { it[Keys.CURRENT_STREAK] ?: 0 }
    val lastSessionDate: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SESSION_DATE] ?: 0L }

    suspend fun setFocusDuration(minutes: Int) {
        context.dataStore.edit { it[Keys.FOCUS_DURATION] = minutes }
    }
    suspend fun setShortBreak(minutes: Int) {
        context.dataStore.edit { it[Keys.SHORT_BREAK] = minutes }
    }
    suspend fun setLongBreak(minutes: Int) {
        context.dataStore.edit { it[Keys.LONG_BREAK] = minutes }
    }
    suspend fun setSessionsBeforeLongBreak(count: Int) {
        context.dataStore.edit { it[Keys.SESSIONS_BEFORE_LONG_BREAK] = count }
    }
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
    suspend fun setCurrentStreak(streak: Int) {
        context.dataStore.edit { it[Keys.CURRENT_STREAK] = streak }
    }
    suspend fun setLastSessionDate(date: Long) {
        context.dataStore.edit { it[Keys.LAST_SESSION_DATE] = date }
    }
}
