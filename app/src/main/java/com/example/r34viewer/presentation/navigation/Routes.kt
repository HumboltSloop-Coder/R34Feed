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

