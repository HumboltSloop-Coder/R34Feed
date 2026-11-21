package com.example.r34viewer.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query(
        """
        SELECT * FROM favorite_posts WHERE profileUserId = ''
        ORDER BY
          CASE WHEN :descending = 0 THEN priority ELSE -priority END ASC,
          CASE WHEN :descending = 0 THEN -savedAt ELSE savedAt END ASC
        """
    )
    fun getAllFavorites(descending: Boolean = false): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE profileUserId = '' ORDER BY priority ASC, savedAt DESC")
    suspend fun getFavoritesSnapshot(): List<FavoritePostEntity>

    @Query(
        """
        SELECT * FROM favorite_posts
        WHERE profileUserId = '' AND id IN (
          SELECT postId FROM favorite_post_folders WHERE profileUserId = '' AND folderId = :folderId
        )
        ORDER BY
          CASE WHEN :descending = 0 THEN priority ELSE -priority END ASC,
          CASE WHEN :descending = 0 THEN -savedAt ELSE savedAt END ASC
        """
    )
    fun getFavoritesByFolder(
        folderId: Long,
        descending: Boolean = false
    ): Flow<List<FavoritePostEntity>>

    @Query(
        """
        SELECT * FROM favorite_posts
        WHERE profileUserId = '' AND id NOT IN (
          SELECT postId FROM favorite_post_folders WHERE profileUserId = ''
        )
        ORDER BY
          CASE WHEN :descending = 0 THEN priority ELSE -priority END ASC,
          CASE WHEN :descending = 0 THEN -savedAt ELSE savedAt END ASC
        """
    )
    fun getFavoritesWithoutFolder(descending: Boolean = false): Flow<List<FavoritePostEntity>>

    @Query(
        """
        SELECT * FROM favorite_posts WHERE profileUserId = :profileUserId
        ORDER BY
          CASE WHEN :descending = 0 THEN priority ELSE -priority END ASC,
          CASE WHEN :descending = 0 THEN -savedAt ELSE savedAt END ASC
        """
    )
    fun getProfileFavorites(
        profileUserId: String,
        descending: Boolean = false
    ): Flow<List<FavoritePostEntity>>

    @Query(
        """
        SELECT * FROM favorite_posts
        WHERE profileUserId = :profileUserId AND id IN (
          SELECT postId FROM favorite_post_folders
          WHERE profileUserId = :profileUserId AND folderId = :folderId
        )
        ORDER BY
          CASE WHEN :descending = 0 THEN priority ELSE -priority END ASC,
          CASE WHEN :descending = 0 THEN -savedAt ELSE savedAt END ASC
        """
    )
    fun getProfileFavoritesByFolder(
        profileUserId: String,
        folderId: Long,
        descending: Boolean = false
    ): Flow<List<FavoritePostEntity>>

    @Query(
        """
        SELECT * FROM favorite_posts
        WHERE profileUserId = :profileUserId AND id NOT IN (
          SELECT postId FROM favorite_post_folders WHERE profileUserId = :profileUserId
        )
        ORDER BY
          CASE WHEN :descending = 0 THEN priority ELSE -priority END ASC,
          CASE WHEN :descending = 0 THEN -savedAt ELSE savedAt END ASC
        """
    )
    fun getProfileFavoritesWithoutFolder(
        profileUserId: String,
        descending: Boolean = false
    ): Flow<List<FavoritePostEntity>>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavoritePostFolders(links: List<FavoritePostFolderEntity>)

    @Query(
        """
        DELETE FROM favorite_post_folders
        WHERE postId IN (:postIds) AND profileUserId = :profileUserId AND folderId = :folderId
        """
    )
    suspend fun removeFavoritesFromFolder(
        postIds: List<Int>,
        folderId: Long,
        profileUserId: String
    )

    @Query("DELETE FROM favorite_post_folders WHERE postId IN (:postIds) AND profileUserId = :profileUserId")
    suspend fun clearFavoriteFolders(postIds: List<Int>, profileUserId: String)

    @Query("DELETE FROM favorite_post_folders WHERE postId = :postId AND profileUserId = :profileUserId")
    suspend fun deleteFavoriteFolders(postId: Int, profileUserId: String)

    @Query("DELETE FROM favorite_post_folders WHERE profileUserId = :profileUserId")
    suspend fun deleteProfileFavoriteFolders(profileUserId: String)

    @Query("SELECT postId, folderId FROM favorite_post_folders WHERE profileUserId = :profileUserId")
    fun getFavoriteFolderLinks(profileUserId: String): Flow<List<FavoritePostFolderLink>>

    @Query("SELECT postId, folderId FROM favorite_post_folders WHERE profileUserId = ''")
    suspend fun getFavoriteFolderLinksSnapshot(): List<FavoritePostFolderLink>

    @Query("SELECT folderId FROM favorite_post_folders WHERE postId = :postId AND profileUserId = ''")
    suspend fun getFolderIdsForPost(postId: Int): List<Long>

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

    @Query("SELECT * FROM favorite_tags ORDER BY createdAt DESC")
    fun getFavoriteTags(): Flow<List<FavoriteTagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavoriteTag(tag: FavoriteTagEntity): Long

    @Query("DELETE FROM favorite_tags WHERE id = :tagId")
    suspend fun deleteFavoriteTag(tagId: Long)

    @Query("SELECT * FROM favorite_tag_bookmarks ORDER BY createdAt DESC")
    fun getFavoriteTagBookmarks(): Flow<List<FavoriteTagBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTagBookmark(bookmark: FavoriteTagBookmarkEntity): Long

    @Query("DELETE FROM favorite_tag_bookmarks WHERE id = :bookmarkId")
    suspend fun deleteFavoriteTagBookmark(bookmarkId: Long)

    @Query("DELETE FROM favorite_tag_bookmarks WHERE tagId = :tagId")
    suspend fun deleteBookmarksForTag(tagId: Long)

    @Query("SELECT MAX(priority) FROM favorite_posts WHERE profileUserId = :profileUserId")
    suspend fun getMaxPriority(profileUserId: String): Int?

    @Query("UPDATE favorite_posts SET priority = :priority WHERE id = :postId AND profileUserId = ''")
    suspend fun updateFavoritePriority(postId: Int, priority: Int)

    @Query("SELECT priority FROM favorite_posts WHERE id = :postId AND profileUserId = '' LIMIT 1")
    suspend fun getFavoritePriority(postId: Int): Int?

    // Read-modify-write operations must be atomic so rapid taps cannot create duplicate
    // priorities or lose a swap.
    @Transaction
    suspend fun toggleFavoriteAtomic(post: FavoritePostEntity) {
        if (getFavoriteById(post.id) != null) {
            deleteFavorite(post.id)
            deleteFavoriteFolders(post.id, "")
        } else {
            insertFavorite(post.copy(priority = (getMaxPriority("") ?: 0) + 1))
        }
    }

    @Transaction
    suspend fun addFavoriteAtomic(post: FavoritePostEntity) {
        if (getFavoriteById(post.id) != null) return
        insertFavorite(post.copy(priority = (getMaxPriority("") ?: 0) + 1))
    }

    @Transaction
    suspend fun swapFavoritePrioritiesAtomic(firstId: Int, secondId: Int) {
        val firstPriority = getFavoritePriority(firstId) ?: return
        val secondPriority = getFavoritePriority(secondId) ?: return
        if (firstPriority == secondPriority) return
        updateFavoritePriority(firstId, secondPriority)
        updateFavoritePriority(secondId, firstPriority)
    }
}
