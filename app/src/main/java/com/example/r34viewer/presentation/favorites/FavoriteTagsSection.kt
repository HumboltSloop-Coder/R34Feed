package com.example.r34viewer.presentation.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.example.r34viewer.domain.model.FavoriteTag
import com.example.r34viewer.domain.model.FavoriteTagBookmark
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.presentation.common.EmptyState
import com.example.r34viewer.presentation.common.ErrorState
import com.example.r34viewer.presentation.common.LoadingState
import com.example.r34viewer.presentation.common.TagChip
import com.example.r34viewer.presentation.common.TagClassifier
import com.example.r34viewer.presentation.common.formatPostCount
import com.example.r34viewer.presentation.feed.userFriendlyError

/** Tag editor shown inside the favorites drawer. */
@Composable
internal fun FavoriteTagManager(
    tags: List<FavoriteTag>,
    draft: String,
    suggestions: List<TagSuggestion>,
    suggestionLoading: Boolean,
    suggestionError: Boolean,
    onDraftChange: (String) -> Unit,
    onSuggestionClick: (TagSuggestion) -> Unit,
    onAddTag: () -> Unit,
    onSelectTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSuggestions = suggestions.take(MAX_VISIBLE_SUGGESTIONS)
    val draftField = remember { mutableStateOf(TextFieldValue(draft)) }

    // Keep the caret at the end of the selected tag when the draft changes from outside
    // (for example after picking a suggestion).
    LaunchedEffect(draft) {
        if (draftField.value.text != draft) {
            val caret = if (draft.endsWith(" ")) draft.length - 1 else draft.length
            draftField.value = TextFieldValue(draft, TextRange(caret))
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = draftField.value,
            onValueChange = { value ->
                draftField.value = value
                onDraftChange(value.text)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tag o tags") },
            placeholder = { Text("Ej. 1girl blue_hair") },
            singleLine = true
        )

        if (suggestionError) {
            Text(
                text = "No se pudieron cargar las sugerencias. Inténtalo de nuevo.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else if (suggestionLoading && visibleSuggestions.isEmpty()) {
            Text(
                text = "Buscando sugerencias...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else if (visibleSuggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(count = visibleSuggestions.size) { index ->
                        val suggestion = visibleSuggestions[index]
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionClick(suggestion) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TagChip(
                                    tag = suggestion.name,
                                    category = TagClassifier.fromSuggestion(suggestion),
                                    modifier = Modifier.weight(1f)
                                )
                                formatPostCount(suggestion.count)?.let { count ->
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = count,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (index < visibleSuggestions.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }

        Button(onClick = onAddTag, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(4.dp))
            Text("Añadir tag")
        }

        Text(
            text = "Tus tags (${tags.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (tags.isEmpty()) {
            Text(
                text = "Aún no hay tags favoritos. Añade el primero arriba.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    item(key = tag.id) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTag(tag.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp)
                                ) {
                                    Text(tag.query, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = if (tag.isComplex) {
                                            "Complejo · ${tag.tokens.size} tags"
                                        } else {
                                            "Simple"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onRemoveTag(tag.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar ${tag.query}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BookmarkRow(
    bookmark: FavoriteTagBookmark,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bookmark.previewUrl.isNotBlank()) {
                AsyncImage(
                    model = bookmark.previewUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.size(10.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.tagQuery, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Reanudar en la imagen #${bookmark.itemIndex + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar marcador de ${bookmark.tagQuery}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Live results for the selected favorite tag, shown in the main content. */
@Composable
internal fun FavoriteTagResults(
    selectedTag: FavoriteTag?,
    posts: LazyPagingItems<Post>,
    columns: Int,
    gridState: LazyGridState,
    pendingBookmark: FavoriteTagBookmark?,
    onBookmarkScrollConsumed: () -> Unit,
    onPostClick: (Post, List<Post>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedQuery = selectedTag?.query
    if (selectedQuery == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyState(message = "Abre el menú y elige un tag para ver sus resultados.")
        }
        return
    }

    val targetBookmark = pendingBookmark
    val refreshState = posts.loadState.refresh
    var hasSeenRefreshLoading by remember { mutableStateOf(false) }

    LaunchedEffect(targetBookmark) {
        hasSeenRefreshLoading = false
    }
    LaunchedEffect(refreshState) {
        if (targetBookmark != null && refreshState is LoadState.Loading) {
            hasSeenRefreshLoading = true
        }
    }

    // Only scroll when a different favorite tag is selected; returning from the detail
    // screen restores the saved scroll position for this tag. A tapped bookmark wins and
    // jumps to its saved page/offset once that page is loaded (not over the previous list).
    var lastScrolledQuery by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedQuery, refreshState, posts.itemCount, targetBookmark, hasSeenRefreshLoading) {
        if (targetBookmark != null) {
            val targetIndex = targetBookmark.indexInStartPage
            val refreshFinished = refreshState !is LoadState.Loading
            val targetAvailable = refreshFinished && posts.itemCount > targetIndex
            val appendState = posts.loadState.append
            val pagesExhausted = refreshFinished && hasSeenRefreshLoading &&
                appendState is LoadState.NotLoading && appendState.endOfPaginationReached
            when {
                refreshState is LoadState.Error -> {
                    // Keep the bookmark, but stop waiting for a page that failed to load.
                    onBookmarkScrollConsumed()
                    lastScrolledQuery = selectedQuery
                }
                targetAvailable && (hasSeenRefreshLoading ||
                    posts[targetIndex]?.id == targetBookmark.postId) -> {
                    gridState.scrollToItem(targetIndex, targetBookmark.scrollOffset)
                    onBookmarkScrollConsumed()
                    lastScrolledQuery = selectedQuery
                }
                pagesExhausted -> {
                    // The saved post no longer exists in this tag; don't block future scrolls.
                    onBookmarkScrollConsumed()
                    lastScrolledQuery = selectedQuery
                }
            }
        } else if (lastScrolledQuery != selectedQuery) {
            gridState.scrollToItem(0)
            lastScrolledQuery = selectedQuery
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val refresh = posts.loadState.refresh) {
            is LoadState.Error -> ErrorState(
                message = userFriendlyError(refresh.error),
                onRetry = { posts.retry() }
            )
            is LoadState.Loading -> {
                if (posts.itemCount == 0) {
                    LoadingState(message = "Buscando...")
                } else {
                    TagResultsGrid(posts, columns, onPostClick, gridState)
                }
            }
            is LoadState.NotLoading -> {
                if (posts.itemCount == 0) {
                    EmptyState(message = "No hay resultados para este tag.")
                } else {
                    TagResultsGrid(posts, columns, onPostClick, gridState)
                }
            }
        }
    }
}

@Composable
private fun TagResultsGrid(
    posts: LazyPagingItems<Post>,
    columns: Int,
    onPostClick: (Post, List<Post>) -> Unit,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(count = posts.itemCount, key = { index -> "favtag-${posts[index]?.id ?: index}" }) { index ->
            posts[index]?.let { post ->
                FavoriteFolderImageItem(
                    post = post,
                    selected = false,
                    selectionMode = false,
                    onClick = { onPostClick(post, posts.itemSnapshotList.items) },
                    onLongClick = {}
                )
            }
        }
        when (posts.loadState.append) {
            is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = { posts.retry() }, modifier = Modifier.padding(16.dp)) {
                    Text("Reintentar")
                }
            }
            else -> Unit
        }
    }
}

private const val MAX_VISIBLE_SUGGESTIONS = 10
