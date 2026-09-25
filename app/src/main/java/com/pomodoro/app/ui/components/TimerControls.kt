package com.pomodoro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    // Scale is driven from real press-interaction state (down/up), not a manual
    // true-then-false flag flip in onClick — the latter never actually animates
    // because Compose only observes the final value within a single click handler.
    val mainInteractionSource = remember { MutableInteractionSource() }
    val isMainPressed by mainInteractionSource.collectIsPressedAsState()
    val mainScale by animateFloatAsState(
        targetValue = if (isMainPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "main_btn_scale"
    )

    // Same tactile press-bounce for the secondary buttons, so every control
    // in the row responds consistently instead of only the FAB reacting.
    val resetInteractionSource = remember { MutableInteractionSource() }
    val isResetPressed by resetInteractionSource.collectIsPressedAsState()
    val resetScale by animateFloatAsState(
        targetValue = if (isResetPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "reset_btn_scale"
    )
    val skipInteractionSource = remember { MutableInteractionSource() }
    val isSkipPressed by skipInteractionSource.collectIsPressedAsState()
    val skipScale by animateFloatAsState(
        targetValue = if (isSkipPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "skip_btn_scale"
    )

    // A full spin on the reset icon reinforces "starting over" at a glance.
    val resetRotation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    fun spinReset() {
        coroutineScope.launch {
            resetRotation.animateTo(
                targetValue = resetRotation.value - 360f,
                animationSpec = tween(durationMillis = 450)
            )
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Reset button
        FilledTonalIconButton(
            onClick = {
                if (isRunning && !isPaused) {
                    showResetDialog = true
                } else {
                    hapticManager.timerReset()
                    soundManager.playTimerReset()
                    spinReset()
                    onReset()
                }
            },
            interactionSource = resetInteractionSource,
            modifier = Modifier.size(52.dp).scale(resetScale),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Reset",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(resetRotation.value)
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
                if (isRunning) {
                    hapticManager.timerPause()
                    soundManager.playTimerPause()
                    onPause()
                } else {
                    hapticManager.timerStart()
                    soundManager.playTimerStart()
                    onStart()
                }
            },
            interactionSource = mainInteractionSource,
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
            interactionSource = skipInteractionSource,
            modifier = Modifier.size(52.dp).scale(skipScale),
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Timer?") },
            text = { Text("Are you sure you want to reset the current timer? Your progress for this session will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        hapticManager.timerReset()
                        soundManager.playTimerReset()
                        spinReset()
                        onReset()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
