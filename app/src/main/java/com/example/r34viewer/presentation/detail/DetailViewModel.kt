package com.example.r34viewer.presentation.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.r34viewer.BuildConfig
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.usecase.AddFavoriteUseCase
import com.example.r34viewer.domain.usecase.AddFavoritesToFolderUseCase
import com.example.r34viewer.domain.usecase.CreateFavoriteFolderUseCase
import com.example.r34viewer.domain.usecase.GetFavoriteByIdUseCase
import com.example.r34viewer.domain.usecase.GetFavoriteFolderIdsUseCase
import com.example.r34viewer.domain.usecase.GetTagTypesUseCase
import com.example.r34viewer.domain.usecase.IsFavoriteUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteFoldersUseCase
import com.example.r34viewer.domain.usecase.ObserveFullscreenMaxQualityUseCase
import com.example.r34viewer.domain.usecase.RemoveFavoritesFromFolderUseCase
import com.example.r34viewer.domain.usecase.ToggleFavoriteUseCase
import com.example.r34viewer.util.SelectedPostCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val post: Post) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedPostCache: SelectedPostCache,
    private val getFavoriteByIdUseCase: GetFavoriteByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val addFavoritesToFolderUseCase: AddFavoritesToFolderUseCase,
    private val createFavoriteFolderUseCase: CreateFavoriteFolderUseCase,
    private val removeFavoritesFromFolderUseCase: RemoveFavoritesFromFolderUseCase,
    private val getFavoriteFolderIdsUseCase: GetFavoriteFolderIdsUseCase,
    private val getTagTypesUseCase: GetTagTypesUseCase,
    observeFavoriteFoldersUseCase: ObserveFavoriteFoldersUseCase,
    observeFullscreenMaxQualityUseCase: ObserveFullscreenMaxQualityUseCase,
    isFavoriteUseCase: IsFavoriteUseCase
) : ViewModel() {

    private val postId: Int = savedStateHandle.get<String>("postId")?.toIntOrNull() ?: -1

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    private val _tagTypes = MutableStateFlow<Map<String, Int?>>(emptyMap())
    val tagTypes: StateFlow<Map<String, Int?>> = _tagTypes.asStateFlow()

    private val _tagTypesLoading = MutableStateFlow(false)
    val tagTypesLoading: StateFlow<Boolean> = _tagTypesLoading.asStateFlow()

    private val _tagTypesError = MutableStateFlow(false)
    val tagTypesError: StateFlow<Boolean> = _tagTypesError.asStateFlow()

    val folders: StateFlow<List<FavoriteFolder>> = observeFavoriteFoldersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val fullscreenMaxQuality: StateFlow<Boolean> = observeFullscreenMaxQualityUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isFavorite: StateFlow<Boolean> = if (postId != -1) {
        isFavoriteUseCase(postId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    } else {
        MutableStateFlow(false)
    }

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            if (postId == -1) {
                _uiState.value = DetailUiState.Error("Invalid post id.")
                return@launch
            }
            // Prefer in-memory selection (from feed/search), fall back to local favorites for offline access.
            val cached = selectedPostCache.get(postId)
            if (cached != null) {
                showPost(cached)
                return@launch
            }
            val favorite = getFavoriteByIdUseCase(postId)
            if (favorite != null) {
                showPost(favorite)
            } else {
                _uiState.value = DetailUiState.Error("Post not found. It may not be cached; open it from the feed again.")
            }
        }
    }

    private fun showPost(post: Post) {
        _uiState.value = DetailUiState.Success(post)
        if (post.tagList.isEmpty()) return

        viewModelScope.launch {
            _tagTypesLoading.value = true
            _tagTypesError.value = false
            try {
                _tagTypes.value = getTagTypesUseCase.getCached(post.tagList)
                val result = getTagTypesUseCase(post.tagList)
                _tagTypes.value = result.types
                _tagTypesError.value = result.hasFailures
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _tagTypesError.value = true
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Tag type lookup failed: ${error::class.simpleName}")
                }
            } finally {
                _tagTypesLoading.value = false
            }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value
        if (current is DetailUiState.Success) {
            viewModelScope.launch { toggleFavoriteUseCase(current.post) }
        }
    }

    /** Creates a new folder from the save dialog. Reports whether it was created. */
    fun createFolder(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(createFavoriteFolderUseCase(name))
        }
    }

    /** Adds the post to favorites and reports the folders it already belongs to. */
    fun addFavoriteAndGetFolders(onReady: (Set<Long>) -> Unit) {
        val current = _uiState.value
        if (current !is DetailUiState.Success) return
        viewModelScope.launch {
            addFavoriteUseCase(current.post)
            onReady(getFavoriteFolderIdsUseCase(current.post.id).toSet())
        }
    }

    /** Adds the post if needed and makes its folder memberships match the dialog selection. */
    fun saveFavorite(folderIds: List<Long> = emptyList()) {
        val current = _uiState.value
        if (current !is DetailUiState.Success) return
        viewModelScope.launch {
            addFavoriteUseCase(current.post)
            val currentFolders = getFavoriteFolderIdsUseCase(current.post.id).toSet()
            val desiredFolders = folderIds.toSet()
            (desiredFolders - currentFolders).forEach { folderId ->
                addFavoritesToFolderUseCase(listOf(current.post.id), folderId)
            }
            (currentFolders - desiredFolders).forEach { folderId ->
                removeFavoritesFromFolderUseCase(listOf(current.post.id), folderId)
            }
        }
    }

    private companion object {
        const val TAG = "DetailViewModel"
    }
}