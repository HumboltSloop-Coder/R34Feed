package com.example.r34viewer.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.FavoriteTag
import com.example.r34viewer.domain.model.FavoriteTagBookmark
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.domain.model.FolderMembership
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.SearchFilters
import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.domain.usecase.AddFavoriteTagUseCase
import com.example.r34viewer.domain.usecase.AddFavoritesToFolderUseCase
import com.example.r34viewer.domain.usecase.ClearFavoritesFoldersUseCase
import com.example.r34viewer.domain.usecase.CreateFavoriteFolderUseCase
import com.example.r34viewer.domain.usecase.DeleteFavoriteProfileUseCase
import com.example.r34viewer.domain.usecase.ExportFavoritesUseCase
import com.example.r34viewer.domain.usecase.GetPostsUseCase
import com.example.r34viewer.domain.usecase.ImportFavoritesUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteFolderMembershipsUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteFoldersUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteProfilesUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteTagBookmarksUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteTagsUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoritesUseCase
import com.example.r34viewer.domain.usecase.ObserveFeedImageSizeUseCase
import com.example.r34viewer.domain.usecase.ObserveHideAiGeneratedUseCase
import com.example.r34viewer.domain.usecase.ObserveNegativeTagsUseCase
import com.example.r34viewer.domain.usecase.DeleteFavoriteTagBookmarkUseCase
import com.example.r34viewer.domain.usecase.RemoveFavoriteTagUseCase
import com.example.r34viewer.domain.usecase.RemoveFavoritesFromFolderUseCase
import com.example.r34viewer.domain.usecase.SaveFavoriteTagBookmarkUseCase
import com.example.r34viewer.domain.usecase.RemoveFavoritesUseCase
import com.example.r34viewer.domain.usecase.SearchTagsUseCase
import com.example.r34viewer.domain.usecase.SwapFavoritePrioritiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val selectedFolder: FavoriteFolderFilter = FavoriteFolderFilter.All,
    val selectedProfileUserId: String? = null,
    val selectedPostIds: Set<Int> = emptySet(),
    val selectionMode: Boolean = false,
    val newFolderName: String = "",
    val menuExpanded: Boolean = false,
    val favoritesDescending: Boolean = false,
    val folderSearchQuery: String = "",
    val tagsMode: Boolean = false,
    val favoriteTagDraft: String = "",
    val tagSuggestions: List<TagSuggestion> = emptyList(),
    val tagSuggestionLoading: Boolean = false,
    val tagSuggestionError: Boolean = false,
    val selectedFavoriteTagId: Long? = null,
    val tagStartPage: Int = 0,
    /** Set when the user taps a bookmark in the drawer; consumed by the results grid. */
    val pendingBookmark: FavoriteTagBookmark? = null,
    val message: String? = null,
    val isImporting: Boolean = false,
    val importProgress: Int = 0,
    val importTotal: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    observeFavoriteFoldersUseCase: ObserveFavoriteFoldersUseCase,
    observeFavoriteProfilesUseCase: ObserveFavoriteProfilesUseCase,
    observeFavoriteTagsUseCase: ObserveFavoriteTagsUseCase,
    observeFavoriteTagBookmarksUseCase: ObserveFavoriteTagBookmarksUseCase,
    observeFeedImageSizeUseCase: ObserveFeedImageSizeUseCase,
    observeNegativeTagsUseCase: ObserveNegativeTagsUseCase,
    observeHideAiGeneratedUseCase: ObserveHideAiGeneratedUseCase,
    private val getPostsUseCase: GetPostsUseCase,
    private val createFavoriteFolderUseCase: CreateFavoriteFolderUseCase,
    private val deleteFavoriteProfileUseCase: DeleteFavoriteProfileUseCase,
    private val exportFavoritesUseCase: ExportFavoritesUseCase,
    private val importFavoritesUseCase: ImportFavoritesUseCase,
    private val removeFavoritesUseCase: RemoveFavoritesUseCase,
    private val addFavoritesToFolderUseCase: AddFavoritesToFolderUseCase,
    private val removeFavoritesFromFolderUseCase: RemoveFavoritesFromFolderUseCase,
    private val clearFavoritesFoldersUseCase: ClearFavoritesFoldersUseCase,
    observeFavoriteFolderMembershipsUseCase: ObserveFavoriteFolderMembershipsUseCase,
    private val addFavoriteTagUseCase: AddFavoriteTagUseCase,
    private val removeFavoriteTagUseCase: RemoveFavoriteTagUseCase,
    private val saveFavoriteTagBookmarkUseCase: SaveFavoriteTagBookmarkUseCase,
    private val deleteFavoriteTagBookmarkUseCase: DeleteFavoriteTagBookmarkUseCase,
    private val swapFavoritePrioritiesUseCase: SwapFavoritePrioritiesUseCase,
    private val searchTagsUseCase: SearchTagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    val profiles: StateFlow<List<FavoriteProfile>> = observeFavoriteProfilesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FavoriteFolder>> = _uiState
        .map { it.selectedProfileUserId }
        .distinctUntilChanged()
        .flatMapLatest { profileUserId -> observeFavoriteFoldersUseCase(profileUserId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Post>> = _uiState
        .map { state ->
            Triple(state.selectedFolder, state.selectedProfileUserId, state.favoritesDescending)
        }
        .distinctUntilChanged()
        .flatMapLatest { (filter, profileUserId, descending) ->
            observeFavoritesUseCase(filter, profileUserId, descending)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folderMemberships: StateFlow<List<FolderMembership>> = _uiState
        .map { it.selectedProfileUserId.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { profileUserId ->
            observeFavoriteFolderMembershipsUseCase(profileUserId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val imageSize: StateFlow<FeedImageSize> = observeFeedImageSizeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedImageSize.DEFAULT)

    val favoriteTags: StateFlow<List<FavoriteTag>> = observeFavoriteTagsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteTagBookmarks: StateFlow<List<FavoriteTagBookmark>> =
        observeFavoriteTagBookmarksUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagPosts: StateFlow<PagingData<Post>> = combine(
        _uiState.map { it.selectedFavoriteTagId to it.tagStartPage }.distinctUntilChanged(),
        favoriteTags,
        observeNegativeTagsUseCase(),
        observeHideAiGeneratedUseCase()
    ) { (selectedId, startPage), tags, negativeTags, hideAiGenerated ->
        val query = tags.firstOrNull { it.id == selectedId }?.query
        SearchFilters(
            query = query.orEmpty(),
            negativeTags = negativeTags,
            hideAiGenerated = hideAiGenerated,
            startPage = startPage
        )
    }
        .distinctUntilChanged()
        .flatMapLatest { filters ->
            if (filters.query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                getPostsUseCase(filters)
            }
        }
        .cachedIn(viewModelScope)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )

    private var importJob: Job? = null
    private var tagSuggestionJob: Job? = null

    fun setMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(menuExpanded = expanded) }
    }

    fun toggleFavoritesOrder() {
        _uiState.update { state ->
            val descending = !state.favoritesDescending
            state.copy(
                favoritesDescending = descending,
                message = if (descending) {
                    "Orden: mayor a menor prioridad."
                } else {
                    "Orden: menor a mayor prioridad."
                }
            )
        }
    }

    fun onNewFolderNameChange(value: String) {
        _uiState.update { it.copy(newFolderName = value, message = null) }
    }

    fun onFolderSearchQueryChange(value: String) {
        _uiState.update { it.copy(folderSearchQuery = value) }
    }

    fun setTagsMode(enabled: Boolean) {
        tagSuggestionJob?.cancel()
        _uiState.update {
            it.copy(
                tagsMode = enabled,
                selectionMode = false,
                selectedPostIds = emptySet(),
                selectedFavoriteTagId = null,
                tagStartPage = 0,
                pendingBookmark = null,
                tagSuggestions = emptyList(),
                tagSuggestionLoading = false,
                tagSuggestionError = false,
                message = null
            )
        }
    }

    fun selectFolder(filter: FavoriteFolderFilter) {
        _uiState.update {
            it.copy(
                selectedFolder = filter,
                tagsMode = false,
                menuExpanded = false,
                selectionMode = false,
                selectedPostIds = emptySet(),
                message = null
            )
        }
    }

    fun selectProfile(profileUserId: String?) {
        _uiState.update {
            it.copy(
                selectedProfileUserId = profileUserId,
                selectedFolder = FavoriteFolderFilter.All,
                selectionMode = false,
                selectedPostIds = emptySet(),
                menuExpanded = false,
                message = null
            )
        }
    }

    fun deleteProfile(profileUserId: String) {
        viewModelScope.launch {
            try {
                deleteFavoriteProfileUseCase(profileUserId)
                _uiState.update {
                    it.copy(
                        selectedProfileUserId =
                            if (it.selectedProfileUserId == profileUserId) null else it.selectedProfileUserId,
                        selectedFolder = FavoriteFolderFilter.All,
                        message = "Perfil compartido eliminado."
                    )
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update {
                    it.copy(message = "No se pudo eliminar el perfil: ${error.localizedMessage ?: "error desconocido"}")
                }
            }
        }
    }

    fun createFolder() {
        if (_uiState.value.selectedProfileUserId != null) return
        val name = _uiState.value.newFolderName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(message = "Escribe un nombre para la carpeta.") }
            return
        }
        viewModelScope.launch {
            val created = createFavoriteFolderUseCase(name)
            _uiState.update {
                it.copy(
                    newFolderName = if (created) "" else it.newFolderName,
                    message = if (created) "Carpeta creada." else "No se pudo crear (nombre vacío o duplicado)."
                )
            }
        }
    }

    fun exportFavorites(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onExportReady(exportFavoritesUseCase())
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update {
                    it.copy(message = "No se pudo preparar la exportación: ${error.localizedMessage ?: "error desconocido"}")
                }
            }
        }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun importFavorites(backupText: String) {
        if (_uiState.value.isImporting) return
        _uiState.update {
            it.copy(
                isImporting = true,
                importProgress = 0,
                importTotal = 0,
                message = null
            )
        }
        importJob = viewModelScope.launch {
            try {
                val result = importFavoritesUseCase(backupText) { completed, total ->
                    _uiState.update { it.copy(importProgress = completed, importTotal = total) }
                }
                val message = buildString {
                    val destination = result.profileUserId?.let { "Favoritos de $it" } ?: "tus favoritos"
                    append("Restaurados ${result.restoredPostCount} de ${result.totalPostCount} en $destination.")
                    if (result.unavailablePostIds.isNotEmpty()) {
                        val shownIds = result.unavailablePostIds.take(8).joinToString()
                        append(" No encontrados: $shownIds")
                        if (result.unavailablePostIds.size > 8) append(", ...")
                        append('.')
                    }
                    result.stoppedReason?.let {
                        append(" Se detuvo en el ID ${result.stoppedAtPostId}; los IDs restantes no se procesaron: $it")
                    }
                }
                _uiState.update {
                    it.copy(
                        selectedProfileUserId = result.profileUserId,
                        selectedFolder = FavoriteFolderFilter.All,
                        selectionMode = false,
                        selectedPostIds = emptySet(),
                        message = message
                    )
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                _uiState.update {
                    it.copy(message = "Importación cancelada; los favoritos ya restaurados se conservaron.")
                }
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        message = "No se pudo importar el archivo: ${error.localizedMessage ?: "formato no válido"}"
                    )
                }
            } finally {
                importJob = null
                _uiState.update {
                    it.copy(isImporting = false, importProgress = 0, importTotal = 0)
                }
            }

        }
    }

    fun cancelImport() {
        importJob?.cancel()
    }

    fun onPostClick(post: Post, onOpen: (Post) -> Unit) {
        val state = _uiState.value
        if (state.selectionMode) {
            toggleSelection(post.id)
        } else {
            onOpen(post)
        }
    }

    fun onPostLongPress(post: Post) {
        if (_uiState.value.selectedProfileUserId != null) return
        _uiState.update { state ->
            val selected = state.selectedPostIds + post.id
            state.copy(selectionMode = true, selectedPostIds = selected)
        }
    }

    fun toggleSelection(postId: Int) {
        if (_uiState.value.selectedProfileUserId != null) return
        _uiState.update { state ->
            val newSet = if (postId in state.selectedPostIds) {
                state.selectedPostIds - postId
            } else {
                state.selectedPostIds + postId
            }
            state.copy(
                selectedPostIds = newSet,
                selectionMode = newSet.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectionMode = false, selectedPostIds = emptySet()) }
    }

    fun movePriority(direction: Int) {
        val selectedIds = _uiState.value.selectedPostIds
        if (selectedIds.size != 1) return
        val postId = selectedIds.single()
        val visible = favorites.value
        val index = visible.indexOfFirst { it.id == postId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex !in visible.indices) return
        viewModelScope.launch {
            swapFavoritePrioritiesUseCase(postId, visible[targetIndex].id)
        }
    }

    fun selectAll(visiblePosts: List<Post>) {
        if (_uiState.value.selectedProfileUserId != null) return
        _uiState.update {
            it.copy(
                selectionMode = visiblePosts.isNotEmpty(),
                selectedPostIds = visiblePosts.map { post -> post.id }.toSet()
            )
        }
    }

    fun removeSelectedFromFavorites() {
        if (_uiState.value.selectedProfileUserId != null) return
        val ids = _uiState.value.selectedPostIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            removeFavoritesUseCase(ids)
            _uiState.update {
                it.copy(
                    selectionMode = false,
                    selectedPostIds = emptySet(),
                    message = "Favoritos eliminados."
                )
            }
        }
    }

    fun toggleSelectedInFolder(folderId: Long) {
        if (_uiState.value.selectedProfileUserId != null) return
        val ids = _uiState.value.selectedPostIds.toList()
        if (ids.isEmpty()) return
        val memberPairs = folderMemberships.value
            .filter { it.postId in ids }
            .map { it.postId to it.folderId }
            .toSet()
        val allPresent = ids.all { id -> (id to folderId) in memberPairs }
        viewModelScope.launch {
            if (allPresent) {
                removeFavoritesFromFolderUseCase(ids, folderId)
            } else {
                addFavoritesToFolderUseCase(ids, folderId)
            }
            _uiState.update {
                it.copy(
                    message = if (allPresent) {
                        "Favoritos quitados de la carpeta."
                    } else {
                        "Favoritos añadidos a la carpeta."
                    }
                )
            }
        }
    }

    fun clearFoldersForSelection() {
        if (_uiState.value.selectedProfileUserId != null) return
        val ids = _uiState.value.selectedPostIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            clearFavoritesFoldersUseCase(ids)
            _uiState.update { it.copy(message = "Favoritos dejados sin carpeta.") }
        }
    }

    fun onFavoriteTagDraftChange(value: String) {
        _uiState.update {
            it.copy(
                favoriteTagDraft = value,
                message = null,
                tagSuggestions = emptyList(),
                tagSuggestionLoading = false,
                tagSuggestionError = false
            )
        }
        tagSuggestionJob?.cancel()
        val token = currentTagToken(value)
        if (token.length < MIN_TAG_SUGGESTION_LENGTH) return

        tagSuggestionJob = viewModelScope.launch {
            delay(TAG_SUGGESTION_DEBOUNCE_MS)
            _uiState.update { it.copy(tagSuggestionLoading = true) }
            try {
                val results = searchTagsUseCase(token)
                if (currentTagToken(_uiState.value.favoriteTagDraft) == token) {
                    _uiState.update {
                        it.copy(
                            tagSuggestions = results,
                            tagSuggestionLoading = false,
                            tagSuggestionError = false
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (currentTagToken(_uiState.value.favoriteTagDraft) == token) {
                    _uiState.update {
                        it.copy(
                            tagSuggestions = emptyList(),
                            tagSuggestionLoading = false,
                            tagSuggestionError = true
                        )
                    }
                }
            }
        }
    }

    fun acceptTagSuggestion(suggestion: TagSuggestion) {
        tagSuggestionJob?.cancel()
        _uiState.update { state ->
            val trimmed = state.favoriteTagDraft.trimEnd()
            val lastSpace = trimmed.lastIndexOf(' ')
            val prefix = if (lastSpace >= 0) trimmed.substring(0, lastSpace + 1) else ""
            state.copy(
                favoriteTagDraft = "$prefix${suggestion.name}",
                tagSuggestions = emptyList(),
                tagSuggestionLoading = false,
                tagSuggestionError = false,
                message = null
            )
        }
    }

    fun addFavoriteTag() {
        tagSuggestionJob?.cancel()
        val query = FavoriteTag.normalize(_uiState.value.favoriteTagDraft)
        if (query == null) {
            _uiState.update { it.copy(message = "Escribe al menos un tag.") }
            return
        }
        val alreadyExists = _uiState.value.favoriteTagDraft.isNotBlank() &&
            favoriteTags.value.any { it.query.equals(query, ignoreCase = true) }
        if (alreadyExists) {
            _uiState.update { it.copy(message = "Ese tag ya está en la lista.") }
            return
        }
        viewModelScope.launch {
            val added = addFavoriteTagUseCase(query)
            _uiState.update {
                it.copy(
                    favoriteTagDraft = if (added) "" else it.favoriteTagDraft,
                    tagSuggestions = emptyList(),
                    tagSuggestionLoading = false,
                    tagSuggestionError = false,
                    message = if (added) "Tag favorito añadido." else "No se pudo añadir el tag."
                )
            }
        }
    }

    fun removeFavoriteTag(tagId: Long) {
        viewModelScope.launch {
            removeFavoriteTagUseCase(tagId)
            if (_uiState.value.selectedFavoriteTagId == tagId) {
                _uiState.update {
                    it.copy(
                        selectedFavoriteTagId = null,
                        tagStartPage = 0,
                        pendingBookmark = null
                    )
                }
            }
        }
    }

    fun selectFavoriteTag(tagId: Long) {
        tagSuggestionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedFavoriteTagId = tagId,
                tagStartPage = 0,
                pendingBookmark = null,
                menuExpanded = false,
                tagSuggestions = emptyList(),
                tagSuggestionLoading = false,
                tagSuggestionError = false,
                message = null
            )
        }
    }

    /** Saves (or replaces) the bookmark for the selected favorite tag at the current scroll position. */
    fun saveFavoriteTagBookmark(
        tagId: Long,
        tagQuery: String,
        post: Post,
        itemIndex: Int,
        scrollOffset: Int,
        currentStartPage: Int
    ) {
        // The visible index is relative to the page the pager started from. Convert it to an
        // absolute index so bookmarks keep working when they are updated after a resume.
        val absoluteIndex = currentStartPage * SearchFilters.PAGE_SIZE + itemIndex
        val startPage = (absoluteIndex / SearchFilters.PAGE_SIZE).coerceAtLeast(0)
        viewModelScope.launch {
            saveFavoriteTagBookmarkUseCase(
                FavoriteTagBookmark(
                    tagId = tagId,
                    tagQuery = tagQuery,
                    postId = post.id,
                    previewUrl = post.feedImageUrl,
                    itemIndex = absoluteIndex,
                    scrollOffset = scrollOffset,
                    startPage = startPage
                )
            )
            _uiState.update { it.copy(message = "Marcador guardado en «$tagQuery».") }
        }
    }

    fun deleteFavoriteTagBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            deleteFavoriteTagBookmarkUseCase(bookmarkId)
            _uiState.update { it.copy(message = "Marcador eliminado.") }
        }
    }

    /** Selects the bookmarked tag and asks the results grid to jump to the saved position. */
    fun openFavoriteTagBookmark(bookmark: FavoriteTagBookmark) {
        tagSuggestionJob?.cancel()
        _uiState.update {
            it.copy(
                tagsMode = true,
                selectionMode = false,
                selectedPostIds = emptySet(),
                selectedFavoriteTagId = bookmark.tagId,
                tagStartPage = bookmark.startPage,
                pendingBookmark = bookmark,
                menuExpanded = false,
                tagSuggestions = emptyList(),
                tagSuggestionLoading = false,
                tagSuggestionError = false,
                message = null
            )
        }
    }

    fun consumePendingBookmark() {
        _uiState.update { it.copy(pendingBookmark = null) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun currentTagToken(draft: String): String =
        draft.trimEnd().substringAfterLast(' ').trim().removePrefix("-")

    private companion object {
        const val TAG_SUGGESTION_DEBOUNCE_MS = 350L
        const val MIN_TAG_SUGGESTION_LENGTH = 2
    }
}
