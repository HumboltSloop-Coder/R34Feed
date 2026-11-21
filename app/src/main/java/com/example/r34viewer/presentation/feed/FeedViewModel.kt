package com.example.r34viewer.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.SearchFilters
import com.example.r34viewer.domain.model.SortOrder
import com.example.r34viewer.domain.usecase.GetPostsUseCase
import com.example.r34viewer.domain.usecase.ObserveFeedImageSizeUseCase
import com.example.r34viewer.domain.usecase.ObserveHideAiGeneratedUseCase
import com.example.r34viewer.domain.usecase.ObserveNegativeTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val filters: SearchFilters = SearchFilters()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    getPostsUseCase: GetPostsUseCase,
    observeHideAiGeneratedUseCase: ObserveHideAiGeneratedUseCase,
    observeNegativeTagsUseCase: ObserveNegativeTagsUseCase,
    observeFeedImageSizeUseCase: ObserveFeedImageSizeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    val imageSize: StateFlow<FeedImageSize> = observeFeedImageSizeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedImageSize.DEFAULT)

    @OptIn(ExperimentalCoroutinesApi::class)
    val posts: Flow<PagingData<Post>> = _uiState
        .flatMapLatest { state -> getPostsUseCase(state.filters) }
        .cachedIn(viewModelScope)

    init {
        // Changing the settings must refresh the main feed automatically.
        viewModelScope.launch {
            observeHideAiGeneratedUseCase().collect { hideAi ->
                _uiState.update { it.copy(filters = it.filters.copy(hideAiGenerated = hideAi)) }
            }
        }
        viewModelScope.launch {
            observeNegativeTagsUseCase().collect { tags ->
                _uiState.update { it.copy(filters = it.filters.copy(negativeTags = tags)) }
            }
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(filters = it.filters.copy(sortOrder = sortOrder)) }
    }

}
