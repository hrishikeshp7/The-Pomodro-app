package com.pomodoro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomodoro.app.navigation.AppNavigation
import com.pomodoro.app.ui.screens.timer.TimerViewModel
import com.pomodoro.app.ui.theme.PomodoroTheme
import com.pomodoro.app.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesManager = PreferencesManager(applicationContext)

        setContent {
            val darkMode by preferencesManager.darkMode.collectAsState(initial = false)
            val onboardingCompleted by preferencesManager.onboardingCompleted.collectAsState(
                initial = runBlocking { preferencesManager.onboardingCompleted.first() }
            )

            val timerViewModel: TimerViewModel = viewModel()

            PomodoroTheme(darkTheme = darkMode) {
                AppNavigation(
                    startOnboarding = !onboardingCompleted,
                    onOnboardingComplete = {
                        lifecycleScope.launch {
                            preferencesManager.setOnboardingCompleted(true)
                        }
                    },
                    timerViewModel = timerViewModel
                )
            }
        }
    }
}
