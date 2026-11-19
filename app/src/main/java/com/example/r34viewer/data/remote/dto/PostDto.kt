package com.example.r34viewer.data.remote.dto

import com.example.r34viewer.domain.model.Post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("score") val score: Int = 0,
    @SerialName("rating") val rating: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    @SerialName("sample_url") val sampleUrl: String = "",
    @SerialName("preview_url") val previewUrl: String = "",
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
    @SerialName("sample_width") val sampleWidth: Int = 0,
    @SerialName("sample_height") val sampleHeight: Int = 0,
    @SerialName("preview_width") val previewWidth: Int = 0,
    @SerialName("preview_height") val previewHeight: Int = 0,
    @SerialName("owner") val owner: String = "",
    @SerialName("source") val source: String = "",
    @SerialName("has_notes") val hasNotes: Boolean = false,
    @SerialName("change") val change: Long = 0L
)

fun PostDto.toDomain(): Post = Post(
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
    createdAt = if (change > 0) change.toString() else null
)
