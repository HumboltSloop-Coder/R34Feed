package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.domain.repository.TagRepository
import javax.inject.Inject

class SearchTagsUseCase @Inject constructor(
    private val repository: TagRepository
) {
    suspend operator fun invoke(prefix: String): List<TagSuggestion> {
        val token = prefix.trim().removePrefix("-")
        if (token.length < MIN_QUERY_LENGTH) return emptyList()
        return repository.searchTags(token)
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2
    }
}
