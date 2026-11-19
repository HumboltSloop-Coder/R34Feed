package com.example.r34viewer.domain.model

data class FavoriteImportResult(
    val totalPostCount: Int,
    val restoredPostCount: Int,
    val unavailablePostIds: List<Int>,
    val profileUserId: String? = null,
    val stoppedAtPostId: Int? = null,
    val stoppedReason: String? = null
)
