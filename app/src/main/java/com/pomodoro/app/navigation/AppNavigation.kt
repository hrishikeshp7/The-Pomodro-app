package com.pomodoro.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pomodoro.app.ui.screens.analytics.AnalyticsScreen
import com.pomodoro.app.ui.screens.history.HistoryScreen
import com.pomodoro.app.ui.screens.onboarding.OnboardingScreen
import com.pomodoro.app.ui.screens.settings.SettingsScreen
import com.pomodoro.app.ui.screens.tasks.TasksScreen
import com.pomodoro.app.ui.screens.timer.TimerScreen
import com.pomodoro.app.ui.screens.timer.TimerViewModel

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Timer : Screen("timer")
    data object Tasks : Screen("tasks")
    data object History : Screen("history")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation(
    startOnboarding: Boolean,
    onOnboardingComplete: () -> Unit,
    timerViewModel: TimerViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Screen.Timer, "Timer", Icons.Filled.Timer, Icons.Outlined.Timer),
            BottomNavItem(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History),
            BottomNavItem(Screen.Analytics, "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
            BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }
    val bottomNavRoutes = remember(bottomNavItems) { bottomNavItems.map { it.screen.route } }
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    fun navigateToBottomRoute(route: String) {
        if (currentRoute == route) return
        if (navController.currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return

        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.screen.route == dest.route }
    } == true

    val swipeModifier = if (currentRoute in bottomNavRoutes) {
        Modifier.pointerInput(currentRoute) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    totalHorizontalDrag += dragAmount
                    change.consume()
                },
                onDragCancel = {
                    totalHorizontalDrag = 0f
                },
                onDragEnd = {
                    val currentIndex = bottomNavRoutes.indexOf(currentRoute)
                    val targetIndex = when {
                        totalHorizontalDrag <= -swipeThresholdPx && currentIndex < bottomNavRoutes.lastIndex -> currentIndex + 1
                        totalHorizontalDrag >= swipeThresholdPx && currentIndex > 0 -> currentIndex - 1
                        else -> -1
                    }
                    if (targetIndex != -1) {
                        navigateToBottomRoute(bottomNavRoutes[targetIndex])
                    }
                    totalHorizontalDrag = 0f
                }
            )
        }
    } else {
        Modifier
    }

    // Lightweight fade + scale used between bottom-nav sibling tabs — cheap
    // enough to stay smooth on low-end devices while still feeling alive.
    val tabEnter = fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))
    val tabExit = fadeOut(tween(140))

    // Tasks opens as a modal sheet over the timer, so it slides up from below.
    val modalEnter = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 6 },
        animationSpec = tween(280)
    ) + fadeIn(tween(280))
    val modalExit = fadeOut(tween(120))
    val modalPopEnter = fadeIn(tween(200))
    val modalPopExit = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight / 6 },
        animationSpec = tween(220)
    ) + fadeOut(tween(220))

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                selected = selected,
                                onClick = {
                                    navigateToBottomRoute(item.screen.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (startOnboarding) Screen.Onboarding.route else Screen.Timer.route,
            enterTransition = { tabEnter },
            exitTransition = { tabExit },
            popEnterTransition = { tabEnter },
            popExitTransition = { tabExit },
            modifier = Modifier
                .padding(paddingValues)
                .then(swipeModifier)
                .background(MaterialTheme.colorScheme.background)
                .clipToBounds()
        ) {
            composable(
                route = Screen.Onboarding.route,
                enterTransition = { fadeIn(tween(280)) },
                exitTransition = { fadeOut(tween(280)) + scaleOut(targetScale = 1.04f, animationSpec = tween(280)) },
                popEnterTransition = { fadeIn(tween(280)) },
                popExitTransition = { fadeOut(tween(280)) }
            ) {
                OnboardingScreen(
                    onComplete = {
                        onOnboardingComplete()
                        navController.navigate(Screen.Timer.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.Timer.route,
                enterTransition = { tabEnter },
                exitTransition = { tabExit },
                popEnterTransition = { tabEnter },
                popExitTransition = { tabExit }
            ) {
                TimerScreen(
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route)
                    },
                    viewModel = timerViewModel
                )
            }
            composable(
                route = Screen.Tasks.route,
                enterTransition = { modalEnter },
                exitTransition = { modalExit },
                popEnterTransition = { modalPopEnter },
                popExitTransition = { modalPopExit }
            ) {
                TasksScreen(
                    onTaskSelected = { task ->
                        timerViewModel.selectTask(task)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.History.route,
                enterTransition = { tabEnter },
                exitTransition = { tabExit },
                popEnterTransition = { tabEnter },
                popExitTransition = { tabExit }
            ) {
                HistoryScreen()
            }
            composable(
                route = Screen.Analytics.route,
                enterTransition = { tabEnter },
                exitTransition = { tabExit },
                popEnterTransition = { tabEnter },
                popExitTransition = { tabExit }
            ) {
                AnalyticsScreen()
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = { tabEnter },
                exitTransition = { tabExit },
                popEnterTransition = { tabEnter },
                popExitTransition = { tabExit }
            ) {
                SettingsScreen()
            }
        }
    }
}
