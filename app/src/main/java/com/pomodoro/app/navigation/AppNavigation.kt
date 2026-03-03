package com.pomodoro.app.navigation

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
import androidx.compose.ui.graphics.vector.ImageVector
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

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Screen.Timer, "Timer", Icons.Filled.Timer, Icons.Outlined.Timer),
            BottomNavItem(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History),
            BottomNavItem(Screen.Analytics, "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
            BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.screen.route == dest.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
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
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (startOnboarding) Screen.Onboarding.route else Screen.Timer.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        onOnboardingComplete()
                        navController.navigate(Screen.Timer.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Timer.route) {
                TimerScreen(
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route)
                    },
                    viewModel = timerViewModel
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(
                    onTaskSelected = { task ->
                        timerViewModel.selectTask(task)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
