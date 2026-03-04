package com.pomodoro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pomodoro.app.util.HapticManager
import com.pomodoro.app.util.SoundManager

@Composable
fun TimerControls(
    isRunning: Boolean,
    isPaused: Boolean,
    hapticManager: HapticManager,
    soundManager: SoundManager,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated scale for the main FAB — gives a spring "bounce" on press
    var isMainPressed by remember { mutableStateOf(false) }
    val mainScale by animateFloatAsState(
        targetValue = if (isMainPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "main_btn_scale"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Reset button
        FilledTonalIconButton(
            onClick = {
                hapticManager.timerReset()
                soundManager.playTimerReset()
                onReset()
            },
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "Reset",
                modifier = Modifier.size(24.dp)
            )
        }

        // Main play/pause button with spring animation
        val buttonColor by animateColorAsState(
            targetValue = if (isRunning) MaterialTheme.colorScheme.tertiary
                         else MaterialTheme.colorScheme.primary,
            label = "button_color"
        )

        LargeFloatingActionButton(
            onClick = {
                isMainPressed = true
                if (isRunning) {
                    hapticManager.timerPause()
                    soundManager.playTimerPause()
                    onPause()
                } else {
                    hapticManager.timerStart()
                    soundManager.playTimerStart()
                    onStart()
                }
                isMainPressed = false
            },
            containerColor = buttonColor,
            shape = CircleShape,
            modifier = Modifier
                .size(76.dp)
                .scale(mainScale)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Skip button
        FilledTonalIconButton(
            onClick = {
                hapticManager.timerSkip()
                soundManager.playTimerSkip()
                onSkip()
            },
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = "Skip",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
