package com.pomodoro.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Outlined.Timer,
                title = "Focus Timer",
                description = "Use the Pomodoro technique to break work into focused intervals with short breaks in between."
            ),
            OnboardingPage(
                icon = Icons.Outlined.FitnessCenter,
                title = "Build Habits",
                description = "Track your daily streaks, manage tasks, and build a consistent focus practice."
            ),
            OnboardingPage(
                icon = Icons.Outlined.TrendingUp,
                title = "See Progress",
                description = "View your analytics to understand your focus patterns and celebrate your growth."
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = currentPage,
            label = "onboarding_page"
        ) { page ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = pages[page].icon,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = pages[page].description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            repeat(pages.size) { index ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (index == currentPage) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(
                        width = if (index == currentPage) 24.dp else 8.dp,
                        height = 8.dp
                    )
                ) {}
            }
        }

        // Navigation button
        Button(
            onClick = {
                if (currentPage < pages.lastIndex) {
                    currentPage++
                } else {
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (currentPage < pages.lastIndex) "Next" else "Get Started",
                style = MaterialTheme.typography.labelLarge
            )
            if (currentPage < pages.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
