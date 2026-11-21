package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.Index
import com.example.r34viewer.domain.model.FolderMembership

@Entity(
    tableName = "favorite_post_folders",
    primaryKeys = ["postId", "profileUserId", "folderId"],
    indices = [Index(value = ["profileUserId", "folderId"])]
)
data class FavoritePostFolderEntity(
    val postId: Int,
    val folderId: Long,
    val profileUserId: String = ""
)

/** Projection used by Room for membership queries. */
data class FavoritePostFolderLink(
    val postId: Int,
    val folderId: Long
)

fun FavoritePostFolderLink.toDomain(): FolderMembership = FolderMembership(
    postId = postId,
    folderId = folderId
)
