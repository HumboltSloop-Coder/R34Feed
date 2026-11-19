package com.example.r34viewer.domain.model

data class FavoriteProfile(
    val userId: String
) {
    val label: String
        get() = "Favoritos de $userId"
}
