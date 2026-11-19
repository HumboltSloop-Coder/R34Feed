package com.example.r34viewer.presentation.navigation

object Routes {
    const val API_KEY_SETUP = "api_key_setup"
    const val MAIN = "main"
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{postId}"

    fun detail(postId: Int): String = "detail/$postId"
}

sealed class BottomNavItem(val route: String, val label: String) {
    data object Home : BottomNavItem(Routes.HOME, "Home")
    data object Search : BottomNavItem(Routes.SEARCH, "Search")
    data object Favorites : BottomNavItem(Routes.FAVORITES, "Favorites")
    data object Settings : BottomNavItem(Routes.SETTINGS, "Settings")
}
