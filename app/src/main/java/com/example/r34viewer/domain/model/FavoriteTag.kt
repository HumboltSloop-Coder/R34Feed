package com.example.r34viewer.domain.model

data class FavoriteTag(
    val id: Long,
    val query: String,
    val createdAt: Long = 0L
) {
    val tokens: List<String>
        get() = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    val isComplex: Boolean
        get() = tokens.size > 1

    companion object {
        fun normalize(query: String): String? {
            val tokens = query.trim()
                .split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ")
        }
    }
}
