package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.repository.TagRepository
import com.example.r34viewer.domain.model.TagTypeLookupResult
import javax.inject.Inject

class GetTagTypesUseCase @Inject constructor(
    private val repository: TagRepository
) {
    suspend fun getCached(tags: List<String>): Map<String, Int?> =
        repository.getCachedTagTypes(tags)

    suspend operator fun invoke(tags: List<String>): TagTypeLookupResult =
        repository.getTagTypes(tags)
}
