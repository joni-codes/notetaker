package com.jonicodes.notetaker.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jonicodes.notetaker.presentation.history.HistoryScreen
import com.jonicodes.notetaker.presentation.history.SummaryDetailScreen
import com.jonicodes.notetaker.presentation.paste.PasteScreen
import com.jonicodes.notetaker.presentation.recording.RecordingScreen
import com.jonicodes.notetaker.presentation.summary.SummaryResultScreen
import com.jonicodes.notetaker.presentation.unsupported.UnsupportedScreen
import java.net.URLDecoder
import java.net.URLEncoder

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Paste, "Paste", Icons.Filled.ContentPaste, Icons.Outlined.ContentPaste),
    BottomNavItem(Screen.Recording, "Record", Icons.Filled.Mic, Icons.Outlined.Mic),
    BottomNavItem(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History),
)

@Composable
fun AppNavigation(isAiAvailable: Boolean) {
    val navController = rememberNavController()

    if (!isAiAvailable) {
        UnsupportedScreen()
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Paste.route,
        Screen.Recording.route,
        Screen.History.route,
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recording.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Screen.Paste.route) {
                PasteScreen(
                    onNavigateToSummary = { transcript, participants ->
                        val encodedTranscript = URLEncoder.encode(transcript, "UTF-8")
                        val encodedParticipants = URLEncoder.encode(participants, "UTF-8")
                        navController.navigate(
                            Screen.SummaryResult.createRoute(encodedTranscript, encodedParticipants)
                        )
                    },
                )
            }

            composable(Screen.Recording.route) {
                RecordingScreen(
                    onNavigateToSummary = { transcript, participants ->
                        val encodedTranscript = URLEncoder.encode(transcript, "UTF-8")
                        val encodedParticipants = URLEncoder.encode(participants, "UTF-8")
                        navController.navigate(
                            Screen.SummaryResult.createRoute(encodedTranscript, encodedParticipants)
                        )
                    },
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToDetail = { summaryId ->
                        navController.navigate(Screen.SummaryDetail.createRoute(summaryId))
                    },
                )
            }

            composable(
                route = Screen.SummaryResult.route,
                arguments = listOf(
                    navArgument("transcript") { type = NavType.StringType },
                    navArgument("participants") { type = NavType.StringType },
                ),
            ) {
                SummaryResultScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack(
                            route = Screen.Recording.route,
                            inclusive = false,
                        )
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(
                route = Screen.SummaryDetail.route,
                arguments = listOf(
                    navArgument("summaryId") { type = NavType.LongType },
                ),
            ) {
                SummaryDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Unsupported.route) {
                UnsupportedScreen()
            }
        }
    }
}
