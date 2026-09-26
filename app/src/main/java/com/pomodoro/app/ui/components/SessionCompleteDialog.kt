package com.pomodoro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val angle: Float,
    val distance: Float,
    val color: Color,
    val size: Float,
    val spinSpeed: Float
)

/**
 * A celebratory replacement for the plain "session complete" AlertDialog:
 * a spring-bounced card with a radial confetti burst behind the trophy icon.
 */
@Composable
fun SessionCompleteDialog(
    completedSessions: Int,
    isLongBreak: Boolean,
    onDismiss: () -> Unit
) {
    val confettiColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )
    val confetti = remember {
        List(24) {
            ConfettiPiece(
                angle = Random.nextFloat() * 360f,
                distance = 90f + Random.nextFloat() * 60f,
                color = confettiColors[it % confettiColors.size],
                size = 5f + Random.nextFloat() * 5f,
                spinSpeed = 180f + Random.nextFloat() * 360f
            )
        }
    }

    // Burst progress: 0 -> 1 drives both the outward travel and the fade-out.
    val burstProgress = remember { Animatable(0f) }
    // Card entrance: a slightly overshooting spring pop instead of a flat fade.
    val cardScale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
                visibilityThreshold = Spring.DefaultDisplacementThreshold
            )
        )
    }
    LaunchedEffect(Unit) {
        burstProgress.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "trophy_bounce")
    val trophyBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_bounce_offset"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    // Confetti burst radiating outward and fading as it travels.
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val eased = 1f - (1f - burstProgress.value) * (1f - burstProgress.value)
                        val fade = (1f - burstProgress.value).coerceIn(0f, 1f)
                        confetti.forEach { piece ->
                            val rad = Math.toRadians(piece.angle.toDouble())
                            val travel = piece.distance * eased
                            val x = center.x + cos(rad).toFloat() * travel
                            val y = center.y + sin(rad).toFloat() * travel
                            drawCircle(
                                color = piece.color.copy(alpha = fade),
                                radius = piece.size,
                                center = Offset(x, y)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer { translationY = trophyBounce }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Session Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Great focus! You've completed $completedSessions session${if (completedSessions != 1) "s" else ""} today.\nTime for a ${if (isLongBreak) "long " else ""}break.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
