package com.example.r34viewer.domain.repository

import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.domain.model.TagTypeLookupResult

interface TagRepository {
    suspend fun importBundledCatalogIfNeeded(): Boolean
    suspend fun searchTags(prefix: String): List<TagSuggestion>
    suspend fun getCachedTagTypes(tags: List<String>): Map<String, Int?>
    suspend fun getTagTypes(tags: List<String>): TagTypeLookupResult
}
