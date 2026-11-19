package com.example.r34viewer.domain.model

data class FavoriteFolder(
    val id: Long,
    val name: String
)

sealed interface FavoriteFolderFilter {
    data object All : FavoriteFolderFilter
    data object Unfiled : FavoriteFolderFilter
    data class Folder(val id: Long, val name: String) : FavoriteFolderFilter
}
