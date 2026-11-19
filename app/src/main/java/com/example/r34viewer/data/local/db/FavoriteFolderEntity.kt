package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.r34viewer.domain.model.FavoriteFolder

@Entity(
    tableName = "favorite_folders",
    indices = [Index(value = ["profileUserId", "name"], unique = true)]
)
data class FavoriteFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val profileUserId: String = ""
)

fun FavoriteFolderEntity.toDomain(): FavoriteFolder = FavoriteFolder(id = id, name = name)
