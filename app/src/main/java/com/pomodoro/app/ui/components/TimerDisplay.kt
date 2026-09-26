package com.pomodoro.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerDisplay(
    timeLeftSeconds: Int,
    totalSeconds: Int,
    isBreak: Boolean,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Spring-based progress feels alive on start/pause/reset instead of a flat linear tween.
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) timeLeftSeconds.toFloat() / totalSeconds else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timer_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val targetArcColor = if (isBreak) tertiaryColor else primaryColor
    val arcColor by animateColorAsState(
        targetValue = targetArcColor,
        animationSpec = tween(400),
        label = "arc_color"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    // Gentle breathing pulse behind the ring while the timer is actively running —
    // a subtle "alive" cue instead of a static disc.
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )
    val glowScale = if (isRunning) breathingScale else 1f
    val glowAlpha = if (isRunning) breathingAlpha else 0f

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val minutesText = String.format("%02d", minutes)
    val secondsText = String.format("%02d", seconds)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(280.dp)
    ) {
        // Soft radial glow that breathes while the timer is running.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * glowScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(arcColor.copy(alpha = glowAlpha), arcColor.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val timeTextStyle = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Light,
                fontSize = 64.sp
            )
            val digitTransitionSpec: AnimatedContentTransitionScope<String>.() -> ContentTransform = {
                (slideInVertically(animationSpec = tween(220)) { h -> h / 4 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(animationSpec = tween(220)) { h -> -h / 4 } + fadeOut(tween(160)))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Minutes and seconds animate independently so the minute digits
                // only slide when the minute actually changes, instead of
                // refreshing on every second tick.
                AnimatedContent(
                    targetState = minutesText,
                    transitionSpec = digitTransitionSpec,
                    label = "minutes_text"
                ) { text ->
                    Text(text = text, style = timeTextStyle, color = MaterialTheme.colorScheme.onBackground)
                }
                Text(text = ":", style = timeTextStyle, color = MaterialTheme.colorScheme.onBackground)
                AnimatedContent(
                    targetState = secondsText,
                    transitionSpec = digitTransitionSpec,
                    label = "seconds_text"
                ) { text ->
                    Text(text = text, style = timeTextStyle, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Text(
                text = if (isBreak) "Break Time" else "Focus",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
