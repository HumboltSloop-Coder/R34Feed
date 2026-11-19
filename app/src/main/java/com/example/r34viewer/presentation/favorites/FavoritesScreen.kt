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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.SubcomposeAsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.presentation.common.CompactTabTopBar
import com.example.r34viewer.presentation.common.EmptyState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var profileToDelete by remember { mutableStateOf<FavoriteProfile?>(null) }
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

    Scaffold(
        topBar = {
            CompactTabTopBar(
                title = "Favorites",
                actions = {
                    if (uiState.selectionMode) {
                        IconButton(onClick = { viewModel.selectAll(favorites) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                        }
                    } else {
                        if (uiState.selectedProfileUserId == null) {
                            IconButton(
                                onClick = {
                                    viewModel.exportFavorites { content ->
                                        pendingExport = content
                                        exportLauncher.launch("Rule34Mobile_favoritos.r34")
                                    }
                                },
                                enabled = !uiState.isImporting
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Exportar favoritos")
                            }
                        }
                        IconButton(
                            onClick = {
                                importLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                            },
                            enabled = !uiState.isImporting
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Importar favoritos")
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
            FavoriteProfileSelector(
                profiles = profiles,
                selectedProfileUserId = uiState.selectedProfileUserId,
                enabled = !uiState.isImporting,
                onSelect = viewModel::selectProfile,
                onDelete = { userId ->
                    profileToDelete = profiles.firstOrNull { it.userId == userId }
                }
            )

            FolderAndActionsSection(
                uiState = uiState,
                folders = folders,
                readOnly = uiState.selectedProfileUserId != null,
                selectedCount = uiState.selectedPostIds.size,
                onExpandedChange = viewModel::setFolderMenuExpanded,
                onFolderSelect = viewModel::selectFolder,
                onNewFolderNameChange = viewModel::onNewFolderNameChange,
                onCreateFolder = viewModel::createFolder,
                onRemoveSelected = viewModel::removeSelectedFromFavorites,
                onMoveSelected = viewModel::moveSelectedToFolder
            )

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restaurando favoritos: ${uiState.importProgress}/${uiState.importTotal}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::cancelImport) {
                            Text("Cancelar")
                        }
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preparando importación...",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::cancelImport) {
                            Text("Cancelar")
                        }
                    }
                }
            }

            if (favorites.isEmpty()) {
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
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites, key = { it.id }) { post ->
                        val selected = post.id in uiState.selectedPostIds
                        FavoriteFolderImageItem(
                            post = post,
                            selected = selected,
                            selectionMode = uiState.selectionMode,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
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
private fun FolderAndActionsSection(
    uiState: FavoritesUiState,
    folders: List<FavoriteFolder>,
    readOnly: Boolean,
    selectedCount: Int,
    onExpandedChange: (Boolean) -> Unit,
    onFolderSelect: (FavoriteFolderFilter) -> Unit,
    onNewFolderNameChange: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onRemoveSelected: () -> Unit,
    onMoveSelected: (Long?) -> Unit
) {
    var moveMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickableNoIndication { onExpandedChange(!uiState.folderMenuExpanded) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = "Carpetas",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentFolderLabel(uiState.selectedFolder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (uiState.folderMenuExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (uiState.folderMenuExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FolderRow(
                        label = "Todos",
                        selected = uiState.selectedFolder == FavoriteFolderFilter.All,
                        onClick = { onFolderSelect(FavoriteFolderFilter.All) }
                    )
                    FolderRow(
                        label = "Sin carpeta",
                        selected = uiState.selectedFolder == FavoriteFolderFilter.Unfiled,
                        onClick = { onFolderSelect(FavoriteFolderFilter.Unfiled) }
                    )
                    folders.forEach { folder ->
                        FolderRow(
                            label = folder.name,
                            selected = (uiState.selectedFolder as? FavoriteFolderFilter.Folder)?.id == folder.id,
                            onClick = { onFolderSelect(FavoriteFolderFilter.Folder(folder.id, folder.name)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    if (!readOnly) {
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

            if (uiState.selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedCount seleccionados",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        TextButton(onClick = { moveMenuExpanded = true }, enabled = selectedCount > 0) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Mover")
                        }
                        DropdownMenu(expanded = moveMenuExpanded, onDismissRequest = { moveMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Sin carpeta") },
                                onClick = {
                                    moveMenuExpanded = false
                                    onMoveSelected(null)
                                }
                            )
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        moveMenuExpanded = false
                                        onMoveSelected(folder.id)
                                    }
                                )
                            }
                        }
                    }
                    TextButton(onClick = onRemoveSelected, enabled = selectedCount > 0) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Quitar", color = MaterialTheme.colorScheme.error)
                    }
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
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
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
private fun FavoriteFolderImageItem(
    post: Post,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
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

        if (selectionMode) {
            val icon = if (selected) Icons.Default.CheckCircle else null
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
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

private fun currentFolderLabel(filter: FavoriteFolderFilter): String = when (filter) {
    FavoriteFolderFilter.All -> "Todos"
    FavoriteFolderFilter.Unfiled -> "Sin carpeta"
    is FavoriteFolderFilter.Folder -> filter.name
}


@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableNoIndication(onClick: () -> Unit): Modifier {
    return this.combinedClickable(
        indication = null,
        interactionSource = null,
        onClick = onClick
    )
}