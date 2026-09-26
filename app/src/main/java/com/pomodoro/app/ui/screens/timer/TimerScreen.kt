package com.pomodoro.app.ui.screens.timer

import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomodoro.app.ui.components.AmbientTimerDisplay
import com.pomodoro.app.ui.components.SessionCompleteDialog
import com.pomodoro.app.ui.components.TimerControls
import com.pomodoro.app.ui.components.TimerDisplay
import com.pomodoro.app.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToTasks: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    // Optimization: Use collectAsStateWithLifecycle to stop flow collection when the screen is not visible.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Flip-to-Focus needs the accelerometer running only while this screen is
    // actually visible, so hook sensor start/stop to the screen's own lifecycle.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startFlipMonitoring()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopFlipMonitoring()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A real screen-off would pause this Activity and stop sensor callbacks,
    // so Flip-to-Focus simulates "off" by dimming brightness to near-black
    // and keeping the screen awake — the AmbientTimerDisplay below then makes
    // that dimmed state look and read like a genuine screen-off.
    val simulatingScreenOff = uiState.isFaceDown && !uiState.showSessionComplete
    DisposableEffect(simulatingScreenOff) {
        val window = context.findActivity()?.window
        val originalBrightness = window?.attributes?.screenBrightness
        if (simulatingScreenOff) {
            window?.let { win ->
                win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = win.attributes
                params.screenBrightness = 0.01f
                win.attributes = params
            }
        }
        onDispose {
            window?.let { win ->
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = win.attributes
                params.screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = params
            }
        }
    }

    if (uiState.showAmbientDisplay) {
        AmbientTimerDisplay(
            timeLeftSeconds = uiState.timeLeftSeconds,
            totalSeconds = uiState.totalSeconds,
            isBreak = uiState.isBreak
        )
        if (uiState.showSessionComplete) {
            SessionCompleteDialog(
                completedSessions = uiState.completedSessions,
                isLongBreak = uiState.isLongBreak,
                onDismiss = { viewModel.dismissSessionComplete() }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pomodoro Focus",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.flipToFocusEnabled) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = "Flip to Focus is on",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (uiState.currentStreak > 0) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.currentStreak}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Distraction banner — shown when the phone was picked up mid-focus
        // and the distraction timer (rather than a plain pause) is counting.
        AnimatedVisibility(visible = uiState.isDistracted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "Distracted for ${uiState.distractedSeconds}s — flip the phone back over to resume",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Session indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(uiState.sessionsBeforeLongBreak) { index ->
                val isCompleted = index < uiState.completedSessions % uiState.sessionsBeforeLongBreak
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isCompleted) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(width = 32.dp, height = 6.dp)
                ) {}
            }
        }

        // Timer Display
        TimerDisplay(
            timeLeftSeconds = uiState.timeLeftSeconds,
            totalSeconds = uiState.totalSeconds,
            isBreak = uiState.isBreak,
            isRunning = uiState.isRunning
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected task chip
        if (uiState.selectedTask != null) {
            AssistChip(
                onClick = {
                    viewModel.hapticManager.buttonClick()
                    onNavigateToTasks()
                },
                label = { Text(uiState.selectedTask!!.title) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        } else {
            TextButton(onClick = {
                viewModel.hapticManager.buttonClick()
                onNavigateToTasks()
            }) {
                Text("Select a task")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timer Controls — pass managers directly
        TimerControls(
            isRunning = uiState.isRunning,
            isPaused = uiState.isPaused,
            hapticManager = viewModel.hapticManager,
            soundManager = viewModel.soundManager,
            onStart = { viewModel.startTimer() },
            onPause = { viewModel.pauseTimer() },
            onReset = { viewModel.resetTimer() },
            onSkip = { viewModel.skipToNext() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Session complete dialog — celebratory confetti burst instead of a plain alert
    if (uiState.showSessionComplete) {
        SessionCompleteDialog(
            completedSessions = uiState.completedSessions,
            isLongBreak = uiState.isLongBreak,
            onDismiss = { viewModel.dismissSessionComplete() }
        )
    }
}
