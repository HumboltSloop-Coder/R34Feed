package com.example.r34viewer.domain.model

/** A saved position inside the results of a favorite tag. There is one bookmark per tag. */
data class FavoriteTagBookmark(
    val id: Long = 0,
    val tagId: Long,
    val tagQuery: String,
    val postId: Int,
    val previewUrl: String = "",
    val itemIndex: Int,
    val scrollOffset: Int = 0,
    val startPage: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Index of the bookmarked post inside the page the pager starts from. */
    val indexInStartPage: Int
        get() = (itemIndex - startPage * SearchFilters.PAGE_SIZE).coerceAtLeast(0)
}
