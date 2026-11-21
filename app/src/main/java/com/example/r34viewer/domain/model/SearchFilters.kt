package com.example.r34viewer.domain.model

enum class SortOrder(val apiTag: String?, val label: String) {
    DEFAULT(null, "Default"),
    SCORE_DESC("sort:score:desc", "Score"),
    ID_DESC("sort:id:desc", "Newest"),
    ID_ASC("sort:id:asc", "Oldest")
}

data class SearchFilters(
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val hideAiGenerated: Boolean = false,
    val negativeTags: List<String> = emptyList(),
    /** Paging page used as the first load key; lets a bookmark resume far down the results. */
    val startPage: Int = 0
) {
    fun toApiTags(): String {
        val parts = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addTag(token: String) {
            val trimmed = token.trim()
            val normalized = trimmed.removePrefix("-").trim().lowercase()
            if (normalized.isNotBlank() && seen.add(normalized)) {
                parts += trimmed
            }
        }

        if (query.isNotBlank()) {
            query.trim().split(Regex("\\s+")).forEach { addTag(it) }
        }
        sortOrder.apiTag?.let { addTag(it) }
        negativeTags.forEach { addTag("-$it") }
        if (hideAiGenerated) addTag(AI_GENERATED_TAG)
        return parts.joinToString(" ")
    }

    companion object {
        const val AI_GENERATED_TAG = "-ai_generated"
        const val PAGE_SIZE = 20
    }
}
