package com.example.r34viewer.domain.usecase

import androidx.paging.PagingData
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.SearchFilters
import com.example.r34viewer.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(filters: SearchFilters): Flow<PagingData<Post>> =
        repository.getPosts(filters)
}
