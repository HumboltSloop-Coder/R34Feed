package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_types")
data class TagTypeEntity(
    @PrimaryKey val name: String,
    val type: Int?,
    val apiCheckedAtEpochDay: Long? = null
)
