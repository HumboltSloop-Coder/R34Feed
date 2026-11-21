package com.example.r34viewer.domain.repository

import androidx.paging.PagingData
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteImportResult
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.FavoriteTag
import com.example.r34viewer.domain.model.FavoriteTagBookmark
import com.example.r34viewer.domain.model.FolderMembership
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.SearchFilters
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(filters: SearchFilters): Flow<PagingData<Post>>
    fun getFavorites(descending: Boolean = false): Flow<List<Post>>
    fun getFavorites(filter: FavoriteFolderFilter, descending: Boolean = false): Flow<List<Post>>
    fun getProfileFavorites(
        profileUserId: String,
        filter: FavoriteFolderFilter,
        descending: Boolean = false
    ): Flow<List<Post>>
    fun getFavoriteFolders(): Flow<List<FavoriteFolder>>
    fun getProfileFavoriteFolders(profileUserId: String): Flow<List<FavoriteFolder>>
    fun getFavoriteProfiles(): Flow<List<FavoriteProfile>>
    fun isFavorite(postId: Int): Flow<Boolean>
    suspend fun toggleFavorite(post: Post)
    /** Adds the post to favorites without removing it when it is already saved. */
    suspend fun addFavorite(post: Post)
    suspend fun getFavoriteById(postId: Int): Post?
    suspend fun exportFavorites(): String
    suspend fun importFavorites(
        backupText: String,
        onProgress: (completed: Int, total: Int) -> Unit
    ): FavoriteImportResult
    suspend fun deleteFavoriteProfile(profileUserId: String)
    suspend fun createFavoriteFolder(name: String): Boolean
    suspend fun removeFavorites(postIds: List<Int>)
    suspend fun addFavoritesToFolder(postIds: List<Int>, folderId: Long)
    suspend fun removeFavoritesFromFolder(postIds: List<Int>, folderId: Long)
    suspend fun clearFavoritesFolders(postIds: List<Int>)
    fun getFavoriteFolderMemberships(profileUserId: String): Flow<List<FolderMembership>>
    suspend fun getFavoriteFolderIds(postId: Int): List<Long>
    fun getFavoriteTags(): Flow<List<FavoriteTag>>
    suspend fun addFavoriteTag(query: String): Boolean
    suspend fun removeFavoriteTag(tagId: Long)
    fun getFavoriteTagBookmarks(): Flow<List<FavoriteTagBookmark>>
    suspend fun saveFavoriteTagBookmark(bookmark: FavoriteTagBookmark): Long
    suspend fun deleteFavoriteTagBookmark(bookmarkId: Long)
    suspend fun swapFavoritePriorities(firstId: Int, secondId: Int)
}
