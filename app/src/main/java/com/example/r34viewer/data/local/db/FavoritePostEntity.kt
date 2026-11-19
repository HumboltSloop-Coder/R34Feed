package com.example.r34viewer.data.local.db

import androidx.room.Entity
import com.example.r34viewer.domain.model.Post

@Entity(
    tableName = "favorite_posts",
    primaryKeys = ["id", "profileUserId"]
)
data class FavoritePostEntity(
    val id: Int,
    val score: Int,
    val rating: String,
    val tags: String,
    val fileUrl: String,
    val sampleUrl: String,
    val previewUrl: String,
    val width: Int,
    val height: Int,
    val sampleWidth: Int,
    val sampleHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    val owner: String,
    val source: String,
    val hasNotes: Boolean,
    val createdAt: String?,
    val savedAt: Long,
    val folderId: Long? = null,
    val profileUserId: String = ""
)

fun FavoritePostEntity.toDomain(): Post = Post(
    id = id,
    score = score,
    rating = rating,
    tags = tags,
    fileUrl = fileUrl,
    sampleUrl = sampleUrl,
    previewUrl = previewUrl,
    width = width,
    height = height,
    sampleWidth = sampleWidth,
    sampleHeight = sampleHeight,
    previewWidth = previewWidth,
    previewHeight = previewHeight,
    owner = owner,
    source = source,
    hasNotes = hasNotes,
    createdAt = createdAt
)

fun Post.toFavoriteEntity(
    savedAt: Long = System.currentTimeMillis(),
    folderId: Long? = null,
    profileUserId: String = ""
): FavoritePostEntity =
    FavoritePostEntity(
        id = id,
        score = score,
        rating = rating,
        tags = tags,
        fileUrl = fileUrl,
        sampleUrl = sampleUrl,
        previewUrl = previewUrl,
        width = width,
        height = height,
        sampleWidth = sampleWidth,
        sampleHeight = sampleHeight,
        previewWidth = previewWidth,
        previewHeight = previewHeight,
        owner = owner,
        source = source,
        hasNotes = hasNotes,
        createdAt = createdAt,
        savedAt = savedAt,
        folderId = folderId,
        profileUserId = profileUserId
    )
