package com.armen.spark.ui.nav

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.armen.spark.AppContainer
import com.armen.spark.ui.discover.DiscoverScreen
import com.armen.spark.ui.discover.DiscoverViewModel
import kotlinx.coroutines.flow.StateFlow

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DISCOVER("discover", "Discover", Icons.Filled.Star),
    SAVED("saved", "Saved", Icons.Filled.Favorite),
    HISTORY("history", "History", Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun SparkNavHost(
    container: AppContainer,
    pendingIdeaId: StateFlow<Long?>,
    onPendingIdeaConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Activity-scoped so the current card survives tab switches and can receive notification taps.
    val discoverViewModel: DiscoverViewModel = viewModel {
        DiscoverViewModel(container.ideaRepository, container.settingsRepository, container.picker)
    }

    val pendingId by pendingIdeaId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingId) {
        val id = pendingId ?: return@LaunchedEffect
        discoverViewModel.showIdea(id)
        navController.navigateToTab(Tab.DISCOVER)
        onPendingIdeaConsumed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (Tab.entries.any { it.route == currentRoute }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.DISCOVER.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.DISCOVER.route) { DiscoverScreen(discoverViewModel, snackbarHostState) }
            composable(Tab.SAVED.route) { Text("Saved") }
            composable(Tab.HISTORY.route) { Text("History") }
            composable(Tab.SETTINGS.route) { Text("Settings") }
        }
    }
}

private fun NavController.navigateToTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
