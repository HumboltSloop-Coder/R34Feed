package com.example.r34viewer.domain.model

data class Post(
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
    val favoritePriority: Int = 0
) {
    val displayImageUrl: String
        get() = sampleUrl.ifBlank { previewUrl.ifBlank { fileUrl } }

    val detailImageUrl: String
        get() = sampleUrl.ifBlank { fileUrl.ifBlank { previewUrl } }

    val tagList: List<String>
        get() = tags.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }

    /** Static thumbnail used in grids: a real image URL for videos and GIFs when available. */
    val feedImageUrl: String
        get() = if (isVideo || isAnimated) {
            previewUrl.ifBlank { sampleUrl.ifBlank { fileUrl } }
        } else {
            displayImageUrl
        }

    /** URL sent to ExoPlayer. Only meaningful for video posts. */
    val playbackUrl: String
        get() = fileUrl.ifBlank { detailImageUrl }

    val isVideo: Boolean
        get() = VIDEO_EXTENSIONS.any { fileUrl.contains(it, ignoreCase = true) } ||
            tagList.any { it in VIDEO_TAGS }

    val isAnimated: Boolean
        get() = !isVideo && (
            fileUrl.contains(".gif", ignoreCase = true) ||
                tagList.any { it in ANIMATED_TAGS }
            )

    /** Single special tag to show in the feed under the tag preview. */
    val animatedBadge: String?
        get() = when {
            isVideo -> "video"
            isAnimated -> "animated"
            else -> null
        }

    private companion object {
        val VIDEO_EXTENSIONS = listOf(".webm", ".mp4", ".m4v", ".mov", ".avi", ".mkv", ".flv")
        val VIDEO_TAGS = setOf("video", "webm", "mp4", "flv", "flash")
        val ANIMATED_TAGS = setOf("animated", "gif", "looping_animation", "loop")
    }
}
