package com.pomodoro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val initialOnboardingCompleted = runBlocking { preferencesManager.onboardingCompleted.first() }

        setContent {
            // ⚡ Bolt Performance Optimization:
            // Using collectAsStateWithLifecycle() instead of collectAsState() ensures that the flow collection
            // is safely paused when the activity drops below the STARTED lifecycle state (e.g., when the app goes to the background).
            // This prevents unnecessary background recompositions, saving CPU and battery life.
            val darkMode by preferencesManager.darkMode.collectAsStateWithLifecycle(initialValue = false)
            val onboardingCompleted by preferencesManager.onboardingCompleted.collectAsStateWithLifecycle(
                initialValue = initialOnboardingCompleted
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
