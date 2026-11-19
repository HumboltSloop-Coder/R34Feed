package com.example.r34viewer.presentation.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.r34viewer.BuildConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.RatingFilter
import com.example.r34viewer.domain.model.SearchFilters
import com.example.r34viewer.domain.model.SortOrder
import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.domain.usecase.AddSearchHistoryUseCase
import com.example.r34viewer.domain.usecase.ClearSearchHistoryUseCase
import com.example.r34viewer.domain.usecase.GetPostsUseCase
import com.example.r34viewer.domain.usecase.GetSearchHistoryUseCase
import com.example.r34viewer.domain.usecase.ObserveFeedImageSizeUseCase
import com.example.r34viewer.domain.usecase.ObserveHideAiGeneratedUseCase
import com.example.r34viewer.domain.usecase.RemoveSearchHistoryUseCase
import com.example.r34viewer.domain.usecase.SearchTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val inputQuery: String = "",
    val submittedQuery: String = "",
    val hasSearched: Boolean = false,
    val rating: RatingFilter = RatingFilter.ALL,
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val hideAiGenerated: Boolean = false
) {
    val filters: SearchFilters
        get() = SearchFilters(
            query = submittedQuery,
            rating = rating,
            sortOrder = sortOrder,
            hideAiGenerated = hideAiGenerated
        )

    val currentTokens: List<String>
        get() = inputQuery.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val searchTagsUseCase: SearchTagsUseCase,
    getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val removeSearchHistoryUseCase: RemoveSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    observeHideAiGeneratedUseCase: ObserveHideAiGeneratedUseCase,
    observeFeedImageSizeUseCase: ObserveFeedImageSizeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _suggestionError = MutableStateFlow(false)
    val suggestionError: StateFlow<Boolean> = _suggestionError.asStateFlow()

    private val _suggestionLoading = MutableStateFlow(false)
    val suggestionLoading: StateFlow<Boolean> = _suggestionLoading.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TagSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TagSuggestion>> = _suggestions.asStateFlow()

    private var suggestionSearchJob: Job? = null

    val recentSearches: StateFlow<List<String>> = getSearchHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val imageSize = observeFeedImageSizeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedImageSize.DEFAULT)

    @OptIn(ExperimentalCoroutinesApi::class)
    val posts: Flow<PagingData<Post>> = _uiState
        .flatMapLatest { state ->
            if (state.hasSearched) getPostsUseCase(state.filters) else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            observeHideAiGeneratedUseCase().collect { hideAi ->
                _uiState.update { it.copy(hideAiGenerated = hideAi) }
            }
        }
    }

    fun onQueryChange(value: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onQueryChange input length=${value.length}")
        _suggestionError.value = false
        _suggestionLoading.value = false
        _uiState.update { it.copy(inputQuery = value) }

        suggestionSearchJob?.cancel()
        _suggestions.value = emptyList()
        val token = currentToken(value).removePrefix("-")
        if (BuildConfig.DEBUG) Log.d(TAG, "Autocomplete token length=${token.length}")
        if (token.length < MIN_SUGGESTION_LENGTH) {
            return
        }

        suggestionSearchJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            _suggestionLoading.value = true
            if (BuildConfig.DEBUG) Log.d(TAG, "Starting tag autocomplete request")
            try {
                val results = searchTagsUseCase(token)
                if (currentToken(_uiState.value.inputQuery).removePrefix("-") == token) {
                    _suggestions.value = results
                    _suggestionLoading.value = false
                    if (BuildConfig.DEBUG) Log.d(TAG, "Tag autocomplete returned ${results.size} items")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (currentToken(_uiState.value.inputQuery).removePrefix("-") == token) {
                    _suggestionError.value = true
                    _suggestions.value = emptyList()
                    _suggestionLoading.value = false
                    if (BuildConfig.DEBUG) Log.e(TAG, "Tag autocomplete failed", error)
                }
            }
        }
    }

    fun submitSearch(query: String? = null) {
        val q = (query ?: _uiState.value.inputQuery).trim()
        suggestionSearchJob?.cancel()
        _suggestions.value = emptyList()
        _suggestionError.value = false
        _suggestionLoading.value = false
        _uiState.update { it.copy(inputQuery = q, submittedQuery = q, hasSearched = true) }
        viewModelScope.launch { addSearchHistoryUseCase(q) }
    }

    /** Adds a suggestion to the current query, keeping the trailing space for the next tag. */
    fun acceptSuggestion(suggestion: TagSuggestion) {
        val current = currentToken(_uiState.value.inputQuery)
        val isNegative = current.startsWith("-")
        val tag = if (isNegative) "-${suggestion.name}" else suggestion.name
        suggestionSearchJob?.cancel()
        _suggestions.value = emptyList()
        _suggestionError.value = false
        _suggestionLoading.value = false
        _uiState.update { state -> state.copy(inputQuery = replaceCurrentToken(state.inputQuery, tag)) }
    }

    fun removeTag(token: String) {
        _uiState.update { state ->
            val remaining = state.currentTokens.filterNot { it == token }
            state.copy(inputQuery = remaining.joinToString(" ") + if (remaining.isEmpty()) "" else " ")
        }
    }

    fun onRatingChange(rating: RatingFilter) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun onSortChange(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun removeHistory(query: String) {
        viewModelScope.launch { removeSearchHistoryUseCase(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
    }

    private fun currentToken(query: String): String =
        query.trimEnd().substringAfterLast(' ').trim()

    private fun replaceCurrentToken(query: String, token: String): String {
        val trimmedQuery = query.trimEnd()
        if (trimmedQuery.isBlank()) return "$token "
        val lastSpace = trimmedQuery.lastIndexOf(' ')
        val prefix = if (lastSpace >= 0) trimmedQuery.substring(0, lastSpace + 1) else ""
        return "$prefix$token "
    }

    private companion object {
        const val TAG = "SearchViewModel"
        const val SUGGESTION_DEBOUNCE_MS = 350L
        const val MIN_SUGGESTION_LENGTH = 2
    }
}
