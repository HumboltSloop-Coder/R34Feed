package com.example.r34viewer.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.presentation.favorites.FavoritesScreen
import com.example.r34viewer.presentation.feed.FeedScreen
import com.example.r34viewer.presentation.search.SearchScreen
import com.example.r34viewer.presentation.settings.SettingsScreen

private data class BottomItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomItem(Routes.FAVORITES, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MainScreen(
    onPostClick: (Post, List<Post>) -> Unit,
    pendingTagSearch: StateFlow<String?>,
    onTagSearchConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val tagSearch by pendingTagSearch.collectAsStateWithLifecycle()
    var initialTagQuery by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(tagSearch) {
        tagSearch?.let { tag ->
            initialTagQuery = tag
            navController.navigate(Routes.SEARCH) { launchSingleTop = true }
            onTagSearchConsumed()
        }
    }

    Scaffold(
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                FeedScreen(onPostClick = onPostClick)
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onPostClick = onPostClick,
                    initialQuery = initialTagQuery,
                    onInitialQueryConsumed = { initialTagQuery = null }
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(onPostClick = onPostClick)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(forceSetup = false)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}