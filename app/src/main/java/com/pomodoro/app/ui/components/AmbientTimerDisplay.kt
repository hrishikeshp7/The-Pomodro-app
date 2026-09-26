package com.pomodoro.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

/**
 * Ultra-minimal, near-black timer face used for two things: Flip-to-Focus's
 * simulated "screen off" state, and the optional Always-On-Display style
 * mode. Deliberately avoids the ring, glow and spring bounces of the main
 * TimerDisplay — an AOD panel is meant to be glanced at, not watched, so
 * every motion here is slow and low-amplitude. The whole face also drifts a
 * few dp on a slow cycle, the same burn-in mitigation real AOD panels use.
 */
@Composable
fun AmbientTimerDisplay(
    timeLeftSeconds: Int,
    totalSeconds: Int,
    isBreak: Boolean,
    modifier: Modifier = Modifier
) {
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    val progress = if (totalSeconds > 0) timeLeftSeconds.toFloat() / totalSeconds else 1f

    // A very slow, low-amplitude breathing fade — present enough to signal
    // "alive" without drawing the eye the way the main screen's pulse does.
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_alpha"
    )

    // Burn-in mitigation: nudge the whole face by a few dp once a minute.
    var shiftIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            shiftIndex = (shiftIndex + 1) % 4
        }
    }
    val shiftX = listOf(0.dp, 6.dp, 0.dp, (-6).dp)[shiftIndex]
    val shiftY = listOf(0.dp, (-4).dp, 4.dp, 0.dp)[shiftIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = shiftX, y = shiftY)
        ) {
            Text(
                text = timeText,
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.Thin,
                fontSize = 88.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isBreak) "BREAK" else "FOCUS",
                color = Color.White.copy(alpha = alpha * 0.5f),
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(Color.White.copy(alpha = alpha * 0.6f))
                )
            }
        }
    }
}
