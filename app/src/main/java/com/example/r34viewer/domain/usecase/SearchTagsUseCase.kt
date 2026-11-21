package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.domain.repository.TagRepository
import javax.inject.Inject

class SearchTagsUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // Trimming, negative-prefix removal and the minimum length are enforced by the
    // repository, which is the single source of truth for autocomplete requests.
    suspend operator fun invoke(prefix: String): List<TagSuggestion> = repository.searchTags(prefix)
}
