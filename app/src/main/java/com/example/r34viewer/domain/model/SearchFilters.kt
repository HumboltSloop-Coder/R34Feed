package com.example.r34viewer.domain.model

enum class RatingFilter(val apiValue: String?, val label: String) {
    ALL(null, "All"),
    SAFE("safe", "Safe"),
    QUESTIONABLE("questionable", "Questionable"),
    EXPLICIT("explicit", "Explicit")
}

enum class SortOrder(val apiTag: String?, val label: String) {
    DEFAULT(null, "Default"),
    SCORE_DESC("sort:score:desc", "Score"),
    ID_DESC("sort:id:desc", "Newest"),
    ID_ASC("sort:id:asc", "Oldest")
}

data class SearchFilters(
    val query: String = "",
    val rating: RatingFilter = RatingFilter.ALL,
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val hideAiGenerated: Boolean = false
) {
    fun toApiTags(): String {
        val parts = mutableListOf<String>()
        if (query.isNotBlank()) parts += query.trim()
        rating.apiValue?.let { parts += "rating:$it" }
        sortOrder.apiTag?.let { parts += it }
        if (hideAiGenerated && AI_GENERATED_TAG !in parts) parts += AI_GENERATED_TAG
        return parts.joinToString(" ")
    }

    companion object {
        const val AI_GENERATED_TAG = "-ai_generated"
    }
}
