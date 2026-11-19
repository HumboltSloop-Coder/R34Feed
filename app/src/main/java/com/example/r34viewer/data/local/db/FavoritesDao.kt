package com.example.r34viewer.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_posts WHERE profileUserId = '' ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = '' ORDER BY savedAt DESC")
    suspend fun getFavoritesSnapshot(): List<FavoritePostEntity>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = '' AND folderId = :folderId ORDER BY savedAt DESC")
    fun getFavoritesByFolder(folderId: Long): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = '' AND folderId IS NULL ORDER BY savedAt DESC")
    fun getFavoritesWithoutFolder(): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = :profileUserId ORDER BY savedAt DESC")
    fun getProfileFavorites(profileUserId: String): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = :profileUserId AND folderId = :folderId ORDER BY savedAt DESC")
    fun getProfileFavoritesByFolder(profileUserId: String, folderId: Long): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = :profileUserId AND folderId IS NULL ORDER BY savedAt DESC")
    fun getProfileFavoritesWithoutFolder(profileUserId: String): Flow<List<FavoritePostEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_posts WHERE id = :postId AND profileUserId = '')")
    fun isFavorite(postId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(post: FavoritePostEntity)

    @Query("DELETE FROM favorite_posts WHERE id = :postId AND profileUserId = ''")
    suspend fun deleteFavorite(postId: Int)

    @Query("DELETE FROM favorite_posts WHERE id IN (:postIds) AND profileUserId = ''")
    suspend fun deleteFavorites(postIds: List<Int>)

    @Query("SELECT * FROM favorite_posts WHERE id = :postId AND profileUserId = '' LIMIT 1")
    suspend fun getFavoriteById(postId: Int): FavoritePostEntity?

    @Query("SELECT * FROM favorite_posts WHERE id = :postId AND profileUserId = :profileUserId LIMIT 1")
    suspend fun getProfileFavoriteById(postId: Int, profileUserId: String): FavoritePostEntity?

    @Query("UPDATE favorite_posts SET folderId = :folderId WHERE id IN (:postIds) AND profileUserId = ''")
    suspend fun moveFavoritesToFolder(postIds: List<Int>, folderId: Long?)

    @Query("UPDATE favorite_posts SET folderId = :folderId WHERE id IN (:postIds) AND profileUserId = :profileUserId")
    suspend fun moveProfileFavoritesToFolder(postIds: List<Int>, folderId: Long?, profileUserId: String)

    @Query("SELECT * FROM favorite_folders WHERE profileUserId = '' ORDER BY name COLLATE NOCASE ASC")
    fun getAllFolders(): Flow<List<FavoriteFolderEntity>>

    @Query("SELECT * FROM favorite_folders WHERE profileUserId = '' ORDER BY name COLLATE NOCASE ASC")
    suspend fun getFoldersSnapshot(): List<FavoriteFolderEntity>

    @Query("SELECT * FROM favorite_folders WHERE profileUserId = :profileUserId ORDER BY name COLLATE NOCASE ASC")
    fun getProfileFolders(profileUserId: String): Flow<List<FavoriteFolderEntity>>

    @Query("SELECT * FROM favorite_folders WHERE name = :name AND profileUserId = '' LIMIT 1")
    suspend fun getFolderByName(name: String): FavoriteFolderEntity?

    @Query("SELECT * FROM favorite_folders WHERE name = :name AND profileUserId = :profileUserId LIMIT 1")
    suspend fun getProfileFolderByName(name: String, profileUserId: String): FavoriteFolderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FavoriteFolderEntity): Long

    @Query("SELECT * FROM favorite_profiles ORDER BY createdAt DESC")
    fun getFavoriteProfiles(): Flow<List<FavoriteProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavoriteProfile(profile: FavoriteProfileEntity)

    @Query("DELETE FROM favorite_posts WHERE profileUserId = :userId")
    suspend fun deleteProfileFavorites(userId: String)

    @Query("DELETE FROM favorite_folders WHERE profileUserId = :userId")
    suspend fun deleteProfileFolders(userId: String)

    @Query("DELETE FROM favorite_profiles WHERE userId = :userId")
    suspend fun deleteFavoriteProfile(userId: String)
}
