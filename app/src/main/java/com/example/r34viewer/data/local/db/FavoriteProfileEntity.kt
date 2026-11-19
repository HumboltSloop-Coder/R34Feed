package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.r34viewer.domain.model.FavoriteProfile

@Entity(tableName = "favorite_profiles")
data class FavoriteProfileEntity(
    @PrimaryKey val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

fun FavoriteProfileEntity.toDomain(): FavoriteProfile = FavoriteProfile(userId = userId)
