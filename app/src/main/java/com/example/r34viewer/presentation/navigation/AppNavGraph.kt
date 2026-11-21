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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.r34viewer.domain.model.Post
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
            // Never leave a detail screen stuck in fullscreen after credentials are removed.
            appNavViewModel.setFullscreen(false)
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

    // The detail destination lives in MainScreen's inner NavHost so the current tab and
    // its grid scroll position are kept while the post is open.
    val onPostSelected: (Post, List<Post>) -> Unit = { post, posts ->
        appNavViewModel.selectedPostCache.setPostSequence(posts.ifEmpty { listOf(post) }, post.id)
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
                    onPostSelected = onPostSelected,
                    pendingTagSearch = appNavViewModel.pendingTagSearch,
                    onTagSearchConsumed = appNavViewModel::consumeTagSearch,
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = { appNavViewModel.setFullscreen(!isFullscreen) },
                    onSetFullscreen = appNavViewModel::setFullscreen,
                    onRequestTagSearch = appNavViewModel::requestTagSearch,
                    selectedPostCache = appNavViewModel.selectedPostCache
                )
            }
        }
    }
}
