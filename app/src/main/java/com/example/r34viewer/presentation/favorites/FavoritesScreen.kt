package com.example.r34viewer.presentation.favorites

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.FavoriteTag
import com.example.r34viewer.domain.model.FavoriteTagBookmark
import com.example.r34viewer.domain.model.FolderMembership
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.presentation.common.CompactTabTopBar
import com.example.r34viewer.presentation.common.EmptyState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onPostClick: (Post, List<Post>) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val folderMemberships by viewModel.folderMemberships.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val favoriteTags by viewModel.favoriteTags.collectAsStateWithLifecycle()
    val favoriteTagBookmarks by viewModel.favoriteTagBookmarks.collectAsStateWithLifecycle()
    val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()
    val tagPosts = viewModel.tagPosts.collectAsLazyPagingItems()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var profileToDelete by remember { mutableStateOf<FavoriteProfile?>(null) }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(4_000)
            viewModel.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val content = pendingExport
        pendingExport = null
        if (uri != null && content != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                            ?: throw IOException("No se pudo abrir el archivo de destino.")
                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(content)
                        }
                    }
                    viewModel.showMessage("Exportación guardada.")
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    viewModel.showMessage(
                        "No se pudo guardar la exportación: ${error.localizedMessage ?: "error desconocido"}"
                    )
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val backupText = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("No se pudo abrir el archivo seleccionado.")
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    viewModel.importFavorites(backupText)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    viewModel.showMessage(
                        "No se pudo leer el archivo: ${error.localizedMessage ?: "error desconocido"}"
                    )
                }
            }
        }
    }

    val favoritesGridState = rememberLazyGridState()
    val resultsGridState = rememberLazyGridState()
    var lastAppliedDescending by remember { mutableStateOf(uiState.favoritesDescending) }
    // When the direction changes, the old first post becomes the last one. Move the
    // viewport to the new first post instead of staying anchored on the same post.
    LaunchedEffect(favorites) {
        if (uiState.favoritesDescending != lastAppliedDescending) {
            lastAppliedDescending = uiState.favoritesDescending
            favoritesGridState.scrollToItem(0)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    LaunchedEffect(uiState.menuExpanded) {
        if (uiState.menuExpanded) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.currentValue) {
        val isOpen = drawerState.currentValue == DrawerValue.Open
        if (isOpen != uiState.menuExpanded) viewModel.setMenuExpanded(isOpen)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.88f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                FavoritesDrawerContent(
                    uiState = uiState,
                    folders = folders,
                    profiles = profiles,
                    favoriteTags = favoriteTags,
                    favoriteTagBookmarks = favoriteTagBookmarks,
                    onClose = { viewModel.setMenuExpanded(false) },
                    onTagsModeChange = viewModel::setTagsMode,
                    onSelectProfile = viewModel::selectProfile,
                    onDeleteProfile = { userId ->
                        profileToDelete = profiles.firstOrNull { it.userId == userId }
                        viewModel.setMenuExpanded(false)
                    },
                    onFolderSearchQueryChange = viewModel::onFolderSearchQueryChange,
                    onSelectFolder = viewModel::selectFolder,
                    onTagSuggestionClick = viewModel::acceptTagSuggestion,
                    onTagDraftChange = viewModel::onFavoriteTagDraftChange,
                    onAddTag = viewModel::addFavoriteTag,
                    onSelectTag = viewModel::selectFavoriteTag,
                    onRemoveTag = viewModel::removeFavoriteTag,
                    onOpenBookmark = viewModel::openFavoriteTagBookmark,
                    onDeleteBookmark = viewModel::deleteFavoriteTagBookmark,
                    onNewFolderNameChange = viewModel::onNewFolderNameChange,
                    onCreateFolder = viewModel::createFolder,
                    onExport = {
                        viewModel.setMenuExpanded(false)
                        viewModel.exportFavorites { content ->
                            pendingExport = content
                            exportLauncher.launch("Rule34Mobile_favoritos.r34")
                        }
                    },
                    onImport = {
                        viewModel.setMenuExpanded(false)
                        importLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CompactTabTopBar(
                    title = "Favorites",
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setMenuExpanded(!uiState.menuExpanded) }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú de favoritos"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleFavoritesOrder) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = if (uiState.favoritesDescending) {
                                    "Orden: mayor a menor. Toca para invertir."
                                } else {
                                    "Orden: menor a mayor. Toca para invertir."
                                }
                            )
                        }
                        // Only inside favorite tags, once a concrete tag is selected.
                        if (uiState.tagsMode && uiState.selectedFavoriteTagId != null) {
                            val selectedTag = favoriteTags
                                .firstOrNull { it.id == uiState.selectedFavoriteTagId }
                            val existingBookmark = favoriteTagBookmarks
                                .firstOrNull { it.tagId == uiState.selectedFavoriteTagId }
                            IconButton(
                                onClick = {
                                    val index = resultsGridState.firstVisibleItemIndex
                                        .coerceAtLeast(0)
                                        .coerceAtMost((tagPosts.itemCount - 1).coerceAtLeast(0))
                                    val post = tagPosts[index]
                                        ?: tagPosts.itemSnapshotList.items.firstOrNull()
                                    if (selectedTag != null && post != null) {
                                        viewModel.saveFavoriteTagBookmark(
                                            tagId = selectedTag.id,
                                            tagQuery = selectedTag.query,
                                            post = post,
                                            itemIndex = index,
                                            scrollOffset = resultsGridState.firstVisibleItemScrollOffset,
                                            currentStartPage = uiState.tagStartPage
                                        )
                                    }
                                },
                                enabled = tagPosts.itemCount > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Guardar marcador en este punto",
                                    tint = if (existingBookmark != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val selectedIndex = if (uiState.selectedPostIds.size == 1) {
                    val selectedId = uiState.selectedPostIds.first()
                    favorites.indexOfFirst { it.id == selectedId }
                } else {
                    -1
                }

                if (!uiState.tagsMode && uiState.selectionMode) {
                    SelectionActionBar(
                        selectedCount = uiState.selectedPostIds.size,
                        canReorder = selectedIndex >= 0,
                        canMoveUp = selectedIndex > 0,
                        canMoveDown = selectedIndex >= 0 && selectedIndex < favorites.lastIndex,
                        folders = folders,
                        memberships = folderMemberships,
                        selectedPostIds = uiState.selectedPostIds,
                        onSelectAll = { viewModel.selectAll(favorites) },
                        onClearSelection = viewModel::clearSelection,
                        onMoveUp = { viewModel.movePriority(-1) },
                        onMoveDown = { viewModel.movePriority(1) },
                        onToggleFolder = viewModel::toggleSelectedInFolder,
                        onClearFolders = viewModel::clearFoldersForSelection,
                        onRemove = viewModel::removeSelectedFromFavorites
                    )
                }

                uiState.message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                if (uiState.isImporting) {
                    if (uiState.importTotal > 0) {
                        LinearProgressIndicator(
                            progress = {
                                uiState.importProgress.toFloat() / uiState.importTotal.toFloat()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.importTotal > 0) {
                                "Restaurando favoritos: ${uiState.importProgress}/${uiState.importTotal}"
                            } else {
                                "Preparando importación..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::cancelImport) {
                            Text("Cancelar")
                        }
                    }
                }

                if (uiState.tagsMode) {
                    FavoriteTagResults(
                        selectedTag = favoriteTags
                            .firstOrNull { it.id == uiState.selectedFavoriteTagId },
                        posts = tagPosts,
                        columns = imageSize.columns,
                        gridState = resultsGridState,
                        pendingBookmark = uiState.pendingBookmark,
                        onBookmarkScrollConsumed = viewModel::consumePendingBookmark,
                        onPostClick = onPostClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (favorites.isEmpty()) {
                    EmptyState(
                        message = when (uiState.selectedFolder) {
                            FavoriteFolderFilter.All -> "No favorites yet.\nTap the heart on a post to save it here."
                            FavoriteFolderFilter.Unfiled -> "No hay favoritos sin carpeta."
                            is FavoriteFolderFilter.Folder -> "Esta carpeta está vacía."
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(imageSize.columns),
                        state = favoritesGridState,
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(favorites, key = { _, post -> post.id }) { _, post ->
                            val selected = post.id in uiState.selectedPostIds
                            FavoriteFolderImageItem(
                                post = post,
                                selected = selected,
                                selectionMode = uiState.selectionMode,
                                priority = if (uiState.selectionMode) post.favoritePriority else null,
                                onClick = {
                                    viewModel.onPostClick(post) { selectedPost ->
                                        onPostClick(selectedPost, favorites)
                                    }
                                },
                                onLongClick = { viewModel.onPostLongPress(post) }
                            )
                        }
                    }
                }
            }
        }
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Eliminar perfil compartido") },
            text = {
                Text("Se eliminarán sus carpetas y favoritos locales (${profile.label}).")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profile.userId)
                        profileToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FavoritesDrawerContent(
    uiState: FavoritesUiState,
    folders: List<FavoriteFolder>,
    profiles: List<FavoriteProfile>,
    favoriteTags: List<FavoriteTag>,
    favoriteTagBookmarks: List<FavoriteTagBookmark>,
    onClose: () -> Unit,
    onTagsModeChange: (Boolean) -> Unit,
    onSelectProfile: (String?) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onFolderSearchQueryChange: (String) -> Unit,
    onSelectFolder: (FavoriteFolderFilter) -> Unit,
    onTagSuggestionClick: (TagSuggestion) -> Unit,
    onTagDraftChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onSelectTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onOpenBookmark: (FavoriteTagBookmark) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onNewFolderNameChange: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val insideFolder = uiState.selectedFolder is FavoriteFolderFilter.Folder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Menú", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar menú")
            }
        }
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        if (!insideFolder) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Posts",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!uiState.tagsMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Switch(checked = uiState.tagsMode, onCheckedChange = onTagsModeChange)
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.tagsMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (!uiState.tagsMode) {
                FavoriteProfileSelector(
                    profiles = profiles,
                    selectedProfileUserId = uiState.selectedProfileUserId,
                    enabled = !uiState.isImporting,
                    onSelect = onSelectProfile,
                    onDelete = onDeleteProfile
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (uiState.tagsMode) {
            // The saved bookmark is only relevant for the tag currently being viewed.
            val selectedBookmark = favoriteTagBookmarks
                .firstOrNull { it.tagId == uiState.selectedFavoriteTagId }
            if (uiState.selectedFavoriteTagId != null && selectedBookmark != null) {
                Text("Marcador", style = MaterialTheme.typography.titleMedium)
                BookmarkRow(
                    bookmark = selectedBookmark,
                    onOpen = { onOpenBookmark(selectedBookmark) },
                    onDelete = { onDeleteBookmark(selectedBookmark.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            FavoriteTagManager(
                tags = favoriteTags,
                draft = uiState.favoriteTagDraft,
                suggestions = uiState.tagSuggestions,
                suggestionLoading = uiState.tagSuggestionLoading,
                suggestionError = uiState.tagSuggestionError,
                onDraftChange = onTagDraftChange,
                onSuggestionClick = onTagSuggestionClick,
                onAddTag = onAddTag,
                onSelectTag = onSelectTag,
                onRemoveTag = onRemoveTag
            )
        } else {
            Text(
                text = "Carpetas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = uiState.folderSearchQuery,
                onValueChange = onFolderSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar carpeta") },
                placeholder = { Text("Nombre de la carpeta") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(6.dp))

            val query = uiState.folderSearchQuery.trim()
            val visibleFolders = folders.filter { it.name.contains(query, ignoreCase = true) }
            val showAll = query.isBlank() || "todos".contains(query, ignoreCase = true)
            val showUnfiled = query.isBlank() || "sin carpeta".contains(query, ignoreCase = true)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    if (showAll) {
                        FolderRow(
                            label = "Todos",
                            selected = uiState.selectedFolder == FavoriteFolderFilter.All,
                            onClick = { onSelectFolder(FavoriteFolderFilter.All) }
                        )
                    }
                }
                item {
                    if (showUnfiled) {
                        FolderRow(
                            label = "Sin carpeta",
                            selected = uiState.selectedFolder == FavoriteFolderFilter.Unfiled,
                            onClick = { onSelectFolder(FavoriteFolderFilter.Unfiled) }
                        )
                    }
                }
                visibleFolders.forEach { folder ->
                    item(key = folder.id) {
                        FolderRow(
                            label = folder.name,
                            selected = (uiState.selectedFolder as? FavoriteFolderFilter.Folder)?.id == folder.id,
                            onClick = {
                                onSelectFolder(FavoriteFolderFilter.Folder(folder.id, folder.name))
                            }
                        )
                    }
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (uiState.selectedProfileUserId == null) {
                    TextButton(onClick = onExport, enabled = !uiState.isImporting) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Exportar")
                    }
                }
                TextButton(onClick = onImport, enabled = !uiState.isImporting) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Importar")
                }
            }

            if (uiState.selectedProfileUserId == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.newFolderName,
                        onValueChange = onNewFolderNameChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Nueva carpeta") },
                        placeholder = { Text("Nombre") }
                    )
                    Button(onClick = onCreateFolder) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Añadir")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    canReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    folders: List<FavoriteFolder>,
    memberships: List<FolderMembership>,
    selectedPostIds: Set<Int>,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleFolder: (Long) -> Unit,
    onClearFolders: () -> Unit,
    onRemove: () -> Unit
) {
    var folderMenuExpanded by remember { mutableStateOf(false) }
    val membershipPairs = remember(memberships, selectedPostIds) {
        memberships
            .filter { it.postId in selectedPostIds }
            .map { it.postId to it.folderId }
            .toSet()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "#$selectedCount",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = onMoveUp,
                enabled = canReorder && canMoveUp,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.ExpandLess, contentDescription = "Subir prioridad")
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canReorder && canMoveDown,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.ExpandMore, contentDescription = "Bajar prioridad")
            }
            Box {
                TextButton(
                    onClick = { folderMenuExpanded = true },
                    enabled = selectedCount > 0
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Añadir")
                }
                DropdownMenu(
                    expanded = folderMenuExpanded,
                    onDismissRequest = { folderMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin carpeta") },
                        onClick = onClearFolders,
                        trailingIcon = {
                            if (membershipPairs.isEmpty()) {
                                Icon(Icons.Default.Check, contentDescription = "Sin carpeta")
                            }
                        }
                    )
                    HorizontalDivider()
                    folders.forEach { folder ->
                        val allPresent = selectedPostIds.isNotEmpty() &&
                            selectedPostIds.all { id -> (id to folder.id) in membershipPairs }
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = { onToggleFolder(folder.id) },
                            trailingIcon = {
                                if (allPresent) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Ya está en esta carpeta"
                                    )
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Listo") },
                        onClick = { folderMenuExpanded = false }
                    )
                }
            }
            TextButton(onClick = onRemove, enabled = selectedCount > 0) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("Quitar", color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSelectAll, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.SelectAll, contentDescription = "Seleccionar todo")
            }
            IconButton(onClick = onClearSelection, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
            }
        }
    }
}

@Composable
private fun FavoriteProfileSelector(
    profiles: List<FavoriteProfile>,
    selectedProfileUserId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProfile = profiles.firstOrNull { it.userId == selectedProfileUserId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TextButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Perfil", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = selectedProfile?.label ?: "Mis favoritos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ExpandMore, contentDescription = "Elegir perfil")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Mis favoritos") },
                        onClick = {
                            expanded = false
                            onSelect(null)
                        }
                    )
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.label) },
                            onClick = {
                                expanded = false
                                onSelect(profile.userId)
                            }
                        )
                    }
                }
            }
            if (selectedProfile != null) {
                IconButton(
                    onClick = { onDelete(selectedProfile.userId) },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar perfil ${selectedProfile.label}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .combinedClickableNoIndication(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FavoriteFolderImageItem(
    modifier: Modifier = Modifier,
    post: Post,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    priority: Int? = null
) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context)
        .data(post.feedImageUrl)
        .decoderFactory(BitmapFactoryDecoder.Factory())
        .crossfade(false)
        .build()

    val ratio = if (post.width > 0 && post.height > 0) {
        post.width.toFloat() / post.height.toFloat().coerceAtLeast(0.1f)
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary) else Modifier
            )
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Favorite post ${post.id}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .heightIn(min = 120.dp),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        if (priority != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = priority.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (selectionMode) {
            val icon = if (selected) Icons.Default.CheckCircle else null
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Seleccionado",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableNoIndication(onClick: () -> Unit): Modifier {
    return this.combinedClickable(
        indication = null,
        interactionSource = null,
        onClick = onClick
    )
}
