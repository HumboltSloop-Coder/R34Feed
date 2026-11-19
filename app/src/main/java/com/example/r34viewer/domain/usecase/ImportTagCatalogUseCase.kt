package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.repository.TagRepository
import javax.inject.Inject

class ImportTagCatalogUseCase @Inject constructor(
    private val repository: TagRepository
) {
    suspend operator fun invoke(): Boolean = repository.importBundledCatalogIfNeeded()
}
