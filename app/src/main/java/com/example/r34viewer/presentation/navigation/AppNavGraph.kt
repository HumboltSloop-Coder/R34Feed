package com.example.r34viewer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.presentation.detail.DetailScreen
import com.example.r34viewer.presentation.settings.SettingsScreen

@Composable
fun AppNavGraph(
    appNavViewModel: AppNavViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val hasCredentials by appNavViewModel.hasCredentials.collectAsStateWithLifecycle()
    val isFullscreen by appNavViewModel.isFullscreen.collectAsStateWithLifecycle()

    // Keep setup/main roots in sync with credentials presence (both api_key and user_id required)
    LaunchedEffect(hasCredentials) {
        if (hasCredentials == null) return@LaunchedEffect
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (hasCredentials == false && currentRoute != Routes.API_KEY_SETUP) {
            navController.navigate(Routes.API_KEY_SETUP) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (hasCredentials == true && currentRoute == Routes.API_KEY_SETUP) {
            navController.navigate(Routes.MAIN) {
                popUpTo(Routes.API_KEY_SETUP) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val onPostClick: (Post, List<Post>) -> Unit = { post, posts ->
        appNavViewModel.selectedPostCache.setPostSequence(posts.ifEmpty { listOf(post) }, post.id)
        navController.navigate(Routes.detail(post.id))
    }

    if (hasCredentials == null) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = if (hasCredentials == true) Routes.MAIN else Routes.API_KEY_SETUP
        ) {
            composable(Routes.API_KEY_SETUP) {
                SettingsScreen(
                    forceSetup = true,
                    onApiKeyConfigured = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.API_KEY_SETUP) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.MAIN) {
                MainScreen(
                    onPostClick = onPostClick,
                    pendingTagSearch = appNavViewModel.pendingTagSearch,
                    onTagSearchConsumed = appNavViewModel::consumeTagSearch
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) {
                DetailScreen(
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = { appNavViewModel.setFullscreen(!isFullscreen) },
                    onBack = {
                        appNavViewModel.setFullscreen(false)
                        navController.popBackStack()
                    },
                    onSearchTag = { tag ->
                        appNavViewModel.setFullscreen(false)
                        appNavViewModel.requestTagSearch(tag)
                        navController.popBackStack(Routes.MAIN, inclusive = false)
                    },
                    onSwipePrevious = { postId ->
                        val adjacent = appNavViewModel.selectedPostCache.getAdjacent(postId, -1)
                        if (adjacent == null) {
                            if (!isFullscreen) navController.popBackStack()
                        } else {
                            navController.navigate(Routes.detail(adjacent.id)) {
                                popUpTo(Routes.DETAIL) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onSwipeNext = { postId ->
                        appNavViewModel.selectedPostCache.getAdjacent(postId, 1)?.let { adjacent ->
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
