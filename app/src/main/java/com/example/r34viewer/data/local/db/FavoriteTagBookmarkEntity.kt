package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.r34viewer.domain.model.FavoriteTagBookmark

@Entity(
    tableName = "favorite_tag_bookmarks",
    indices = [Index(value = ["tagId"], unique = true)]
)
data class FavoriteTagBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tagId: Long,
    val tagQuery: String,
    val postId: Int,
    val previewUrl: String = "",
    val itemIndex: Int,
    val scrollOffset: Int = 0,
    val startPage: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun FavoriteTagBookmarkEntity.toDomain(): FavoriteTagBookmark = FavoriteTagBookmark(
    id = id,
    tagId = tagId,
    tagQuery = tagQuery,
    postId = postId,
    previewUrl = previewUrl,
    itemIndex = itemIndex,
    scrollOffset = scrollOffset,
    startPage = startPage,
    createdAt = createdAt
)

fun FavoriteTagBookmark.toEntity(): FavoriteTagBookmarkEntity = FavoriteTagBookmarkEntity(
    id = id,
    tagId = tagId,
    tagQuery = tagQuery,
    postId = postId,
    previewUrl = previewUrl,
    itemIndex = itemIndex,
    scrollOffset = scrollOffset,
    startPage = startPage,
    createdAt = createdAt
)
