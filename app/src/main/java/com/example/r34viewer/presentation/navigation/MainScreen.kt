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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.presentation.detail.DetailScreen
import com.example.r34viewer.presentation.favorites.FavoritesScreen
import com.example.r34viewer.presentation.feed.FeedScreen
import com.example.r34viewer.presentation.search.SearchScreen
import com.example.r34viewer.presentation.settings.SettingsScreen
import com.example.r34viewer.util.SelectedPostCache
import kotlinx.coroutines.flow.StateFlow

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
    onPostSelected: (Post, List<Post>) -> Unit,
    pendingTagSearch: StateFlow<String?>,
    onTagSearchConsumed: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onSetFullscreen: (Boolean) -> Unit,
    onRequestTagSearch: (String) -> Unit,
    selectedPostCache: SelectedPostCache
) {
    val navController = rememberNavController()
    val tagSearch by pendingTagSearch.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var initialTagQuery by remember { mutableStateOf<String?>(null) }

    val isDetail = currentDestination?.route == Routes.DETAIL

    LaunchedEffect(tagSearch) {
        tagSearch?.let { tag ->
            initialTagQuery = tag
            navController.navigate(Routes.SEARCH) {
                popUpTo(Routes.DETAIL) { inclusive = true }
                launchSingleTop = true
            }
            onTagSearchConsumed()
        }
    }

    val onPostClick: (Post, List<Post>) -> Unit = { post, posts ->
        onPostSelected(post, posts)
        navController.navigate(Routes.detail(post.id))
    }

    Scaffold(
        bottomBar = {
            if (!isDetail) BottomBar(navController, currentDestination)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // The detail screen is edge-to-edge and handles its own insets.
            modifier = if (isDetail) Modifier else Modifier.padding(innerPadding)
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
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) {
                DetailScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onSetFullscreen = onSetFullscreen,
                    onBack = {
                        onSetFullscreen(false)
                        navController.popBackStack()
                    },
                    onSearchTag = { tag ->
                        onSetFullscreen(false)
                        onRequestTagSearch(tag)
                    },
                    onSwipePrevious = { postId ->
                        val adjacent = selectedPostCache.getAdjacent(postId, -1)
                        if (adjacent == null) {
                            if (!isFullscreen) {
                                onSetFullscreen(false)
                                navController.popBackStack()
                            }
                        } else {
                            navController.navigate(Routes.detail(adjacent.id)) {
                                popUpTo(Routes.DETAIL) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onSwipeNext = { postId ->
                        selectedPostCache.getAdjacent(postId, 1)?.let { adjacent ->
                            navController.navigate(Routes.detail(adjacent.id)) {
                                popUpTo(Routes.DETAIL) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(navController: androidx.navigation.NavHostController, currentDestination: NavDestination?) {
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
