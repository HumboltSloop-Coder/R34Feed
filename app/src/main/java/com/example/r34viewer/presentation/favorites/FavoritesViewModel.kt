package com.example.r34viewer.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.usecase.CreateFavoriteFolderUseCase
import com.example.r34viewer.domain.usecase.DeleteFavoriteProfileUseCase
import com.example.r34viewer.domain.usecase.ExportFavoritesUseCase
import com.example.r34viewer.domain.usecase.ImportFavoritesUseCase
import com.example.r34viewer.domain.usecase.MoveFavoritesToFolderUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteFoldersUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoriteProfilesUseCase
import com.example.r34viewer.domain.usecase.ObserveFavoritesUseCase
import com.example.r34viewer.domain.usecase.RemoveFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
    val folderMenuExpanded: Boolean = false,
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
    private val createFavoriteFolderUseCase: CreateFavoriteFolderUseCase,
    private val deleteFavoriteProfileUseCase: DeleteFavoriteProfileUseCase,
    private val exportFavoritesUseCase: ExportFavoritesUseCase,
    private val importFavoritesUseCase: ImportFavoritesUseCase,
    private val removeFavoritesUseCase: RemoveFavoritesUseCase,
    private val moveFavoritesToFolderUseCase: MoveFavoritesToFolderUseCase
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
        .map { state -> state.selectedFolder to state.selectedProfileUserId }
        .distinctUntilChanged()
        .flatMapLatest { (filter, profileUserId) ->
            observeFavoritesUseCase(filter, profileUserId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var importJob: Job? = null

    fun setFolderMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(folderMenuExpanded = expanded) }
    }

    fun onNewFolderNameChange(value: String) {
        _uiState.update { it.copy(newFolderName = value, message = null) }
    }

    fun selectFolder(filter: FavoriteFolderFilter) {
        _uiState.update {
            it.copy(
                selectedFolder = filter,
                folderMenuExpanded = false,
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
                folderMenuExpanded = false,
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

    fun moveSelectedToFolder(folderId: Long?) {
        if (_uiState.value.selectedProfileUserId != null) return
        val ids = _uiState.value.selectedPostIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            moveFavoritesToFolderUseCase(ids, folderId)
            _uiState.update {
                it.copy(
                    selectionMode = false,
                    selectedPostIds = emptySet(),
                    message = "Favoritos movidos."
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
