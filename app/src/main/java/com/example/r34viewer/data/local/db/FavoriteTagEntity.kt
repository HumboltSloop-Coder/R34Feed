package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.r34viewer.domain.model.FavoriteTag

@Entity(
    tableName = "favorite_tags",
    indices = [Index(value = ["query"], unique = true)]
)
data class FavoriteTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val createdAt: Long = System.currentTimeMillis()
)

fun FavoriteTagEntity.toDomain(): FavoriteTag = FavoriteTag(
    id = id,
    query = query,
    createdAt = createdAt
)
