package com.pomodoro.app.data.model

data class TimerPreset(
    val name: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val sessionsBeforeLongBreak: Int = 4
)

object DefaultPresets {
    val classic = TimerPreset(
        name = "Classic",
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15
    )
    val deepWork = TimerPreset(
        name = "Deep Work",
        focusMinutes = 50,
        shortBreakMinutes = 10,
        longBreakMinutes = 30
    )
    val quickFocus = TimerPreset(
        name = "Quick Focus",
        focusMinutes = 15,
        shortBreakMinutes = 3,
        longBreakMinutes = 10
    )
    val all = listOf(classic, deepWork, quickFocus)
}
