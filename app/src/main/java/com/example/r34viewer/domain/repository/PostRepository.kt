package com.example.r34viewer.domain.repository

import androidx.paging.PagingData
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteImportResult
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.SearchFilters
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(filters: SearchFilters): Flow<PagingData<Post>>
    fun getFavorites(): Flow<List<Post>>
    fun getFavorites(filter: FavoriteFolderFilter): Flow<List<Post>>
    fun getProfileFavorites(profileUserId: String, filter: FavoriteFolderFilter): Flow<List<Post>>
    fun getFavoriteFolders(): Flow<List<FavoriteFolder>>
    fun getProfileFavoriteFolders(profileUserId: String): Flow<List<FavoriteFolder>>
    fun getFavoriteProfiles(): Flow<List<FavoriteProfile>>
    fun isFavorite(postId: Int): Flow<Boolean>
    suspend fun toggleFavorite(post: Post)
    suspend fun getFavoriteById(postId: Int): Post?
    suspend fun exportFavorites(): String
    suspend fun importFavorites(
        backupText: String,
        onProgress: (completed: Int, total: Int) -> Unit
    ): FavoriteImportResult
    suspend fun deleteFavoriteProfile(profileUserId: String)
    suspend fun createFavoriteFolder(name: String): Boolean
    suspend fun removeFavorites(postIds: List<Int>)
    suspend fun moveFavoritesToFolder(postIds: List<Int>, folderId: Long?)
}
