package com.pomodoro.app

import com.pomodoro.app.data.model.DefaultPresets
import com.pomodoro.app.data.model.TimerPreset
import org.junit.Assert.*
import org.junit.Test

class TimerPresetTest {

    @Test
    fun classicPreset_hasCorrectDurations() {
        val preset = DefaultPresets.classic
        assertEquals("Classic", preset.name)
        assertEquals(25, preset.focusMinutes)
        assertEquals(5, preset.shortBreakMinutes)
        assertEquals(15, preset.longBreakMinutes)
        assertEquals(4, preset.sessionsBeforeLongBreak)
    }

    @Test
    fun deepWorkPreset_hasCorrectDurations() {
        val preset = DefaultPresets.deepWork
        assertEquals("Deep Work", preset.name)
        assertEquals(50, preset.focusMinutes)
        assertEquals(10, preset.shortBreakMinutes)
        assertEquals(30, preset.longBreakMinutes)
        assertEquals(4, preset.sessionsBeforeLongBreak)
    }

    @Test
    fun quickFocusPreset_hasCorrectDurations() {
        val preset = DefaultPresets.quickFocus
        assertEquals("Quick Focus", preset.name)
        assertEquals(15, preset.focusMinutes)
        assertEquals(3, preset.shortBreakMinutes)
        assertEquals(10, preset.longBreakMinutes)
        assertEquals(4, preset.sessionsBeforeLongBreak)
    }

    @Test
    fun allPresets_containsThreeItems() {
        assertEquals(3, DefaultPresets.all.size)
    }

    @Test
    fun customPreset_canBeCreated() {
        val custom = TimerPreset(
            name = "Custom",
            focusMinutes = 45,
            shortBreakMinutes = 10,
            longBreakMinutes = 20,
            sessionsBeforeLongBreak = 3
        )
        assertEquals(45, custom.focusMinutes)
        assertEquals(3, custom.sessionsBeforeLongBreak)
    }

    @Test
    fun defaultSessionsBeforeLongBreak_isFour() {
        val preset = TimerPreset(
            name = "Test",
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15
        )
        assertEquals(4, preset.sessionsBeforeLongBreak)
    }
}
