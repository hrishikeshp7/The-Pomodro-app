package com.pomodoro.app.ui.screens.timer

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomodoro.app.ui.components.TimerControls
import com.pomodoro.app.ui.components.TimerDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToTasks: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            if (uiState.currentStreak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            isBreak = uiState.isBreak
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected task chip
        if (uiState.selectedTask != null) {
            AssistChip(
                onClick = onNavigateToTasks,
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
            TextButton(onClick = onNavigateToTasks) {
                Text("Select a task")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timer Controls
        TimerControls(
            isRunning = uiState.isRunning,
            isPaused = uiState.isPaused,
            onStart = { viewModel.startTimer() },
            onPause = { viewModel.pauseTimer() },
            onReset = { viewModel.resetTimer() },
            onSkip = { viewModel.skipToNext() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Session complete dialog
    if (uiState.showSessionComplete) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSessionComplete() },
            title = { Text("Session Complete! 🎉") },
            text = {
                Text(
                    "Great focus! You've completed ${uiState.completedSessions} session${if (uiState.completedSessions != 1) "s" else ""} today.\nTime for a ${if (uiState.isLongBreak) "long " else ""}break.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSessionComplete() }) {
                    Text("Continue")
                }
            }
        )
    }
}
