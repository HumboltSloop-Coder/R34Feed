package com.example.r34viewer.presentation.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.RatingFilter
import com.example.r34viewer.domain.model.SortOrder
import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.presentation.common.CompactTabTopBar
import com.example.r34viewer.presentation.common.EmptyState
import com.example.r34viewer.presentation.common.ErrorState
import com.example.r34viewer.presentation.common.LoadingState
import com.example.r34viewer.presentation.common.PostGridItem
import com.example.r34viewer.presentation.common.TagChip
import com.example.r34viewer.presentation.common.TagClassifier
import com.example.r34viewer.presentation.common.formatPostCount
import com.example.r34viewer.presentation.feed.userFriendlyError

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onPostClick: (Post, List<Post>) -> Unit,
    initialQuery: String? = null,
    onInitialQueryConsumed: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val suggestionError by viewModel.suggestionError.collectAsStateWithLifecycle()
    val suggestionLoading by viewModel.suggestionLoading.collectAsStateWithLifecycle()
    val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()

    LaunchedEffect(initialQuery) {
        initialQuery?.let { query ->
            viewModel.onQueryChange(query)
            viewModel.submitSearch(query)
            onInitialQueryConsumed()
        }
    }

    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var searchBarVisible by rememberSaveable { mutableStateOf(true) }
    val gridState = rememberLazyGridState()

    // Oculta la barra al hacer scroll hacia abajo y la muestra al hacer scroll hacia arriba.
    LaunchedEffect(gridState) {
        var lastIndex = gridState.firstVisibleItemIndex
        var lastOffset = gridState.firstVisibleItemScrollOffset
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrollingDown = index > lastIndex || (index == lastIndex && offset > lastOffset + 8)
                val scrollingUp = index < lastIndex || (index == lastIndex && offset < lastOffset - 8)
                if (scrollingDown) searchBarVisible = false
                if (scrollingUp || index == 0) searchBarVisible = true
                lastIndex = index
                lastOffset = offset
            }
    }

    val visibleSuggestions = suggestions.take(MAX_VISIBLE_SUGGESTIONS)
    val isEditingQuery = !uiState.hasSearched ||
        uiState.inputQuery.trim() != uiState.submittedQuery
    val showSuggestions = isEditingQuery &&
        (visibleSuggestions.isNotEmpty() || suggestionError || suggestionLoading)

    Scaffold(
        topBar = { CompactTabTopBar("Search") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.inputQuery,
                        onValueChange = { value ->
                            viewModel.onQueryChange(value)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        placeholder = { Text("Search tags (e.g. cat_ears -male)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.inputQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.onQueryChange("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.submitSearch()
                        })
                    )

                    if (showSuggestions) {
                        if (suggestionError) {
                            Text(
                                text = "No se pudieron cargar las sugerencias. Inténtalo de nuevo.",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else if (suggestionLoading) {
                            Text(
                                text = "Buscando sugerencias...",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 2.dp
                            ) {
                                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                                    itemsIndexed(visibleSuggestions) { index, suggestion ->
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.acceptSuggestion(suggestion)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TagSuggestionRow(suggestion)
                                            }
                                            if (index < visibleSuggestions.lastIndex) {
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected tags are rendered as colour-coded chips (meta tags / artists / normal).
                    if (uiState.currentTokens.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.currentTokens.forEach { token ->
                                TagChip(tag = token, onClick = { viewModel.removeTag(token) })
                            }
                        }
                    }

                    SearchFilterMenu(
                        expanded = filtersExpanded,
                        onExpandedChange = { filtersExpanded = !filtersExpanded },
                        selectedRating = uiState.rating,
                        selectedSort = uiState.sortOrder,
                        onRatingSelected = viewModel::onRatingChange,
                        onSortSelected = viewModel::onSortChange
                    )
                }
            }

            if (uiState.hasSearched) {
                Text(
                    text = "Results for: \"${uiState.submittedQuery.ifBlank { "(all)" }}\"",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            if (!uiState.hasSearched) {
                RecentSearchesSection(
                    searches = recentSearches,
                    onSelect = { q ->
                        viewModel.onQueryChange(q)
                        viewModel.submitSearch(q)
                    },
                    onRemove = viewModel::removeHistory,
                    onClear = viewModel::clearHistory
                )
            } else {
                when (val refresh = posts.loadState.refresh) {
                    is LoadState.Error -> ErrorState(
                        message = userFriendlyError(refresh.error),
                        onRetry = { posts.retry() }
                    )
                    is LoadState.Loading -> {
                        if (posts.itemCount == 0) LoadingState(message = "Searching...")
                        else ResultsGrid(posts, imageSize.columns, onPostClick, gridState)
                    }
                    is LoadState.NotLoading -> {
                        if (posts.itemCount == 0) {
                            EmptyState(message = "No results for this query.\nIf the problem persists, verify your API Key and User ID in Settings.")
                        } else {
                            ResultsGrid(posts, imageSize.columns, onPostClick, gridState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSuggestionRow(suggestion: TagSuggestion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TagChip(
            tag = suggestion.name,
            category = TagClassifier.fromSuggestion(suggestion),
            modifier = Modifier.weight(1f)
        )
        formatPostCount(suggestion.count)?.let { count ->
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultsGrid(
    posts: LazyPagingItems<Post>,
    columns: Int,
    onPostClick: (Post, List<Post>) -> Unit,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(count = posts.itemCount, key = { index -> "search-${posts[index]?.id ?: index}" }) { index ->
            posts[index]?.let { post ->
                PostGridItem(
                    post = post,
                    onClick = { onPostClick(post, posts.itemSnapshotList.items) }
                )
            }
        }
        when (posts.loadState.append) {
            is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = { posts.retry() }, modifier = Modifier.padding(16.dp)) { Text("Retry") }
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterMenu(
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    selectedRating: RatingFilter,
    selectedSort: SortOrder,
    onRatingSelected: (RatingFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Filters: ${selectedRating.label} · ${selectedSort.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Ocultar filtros" else "Mostrar filtros"
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rating", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(RatingFilter.ALL, RatingFilter.EXPLICIT, RatingFilter.QUESTIONABLE, RatingFilter.SAFE).forEach { r ->
                        FilterChip(selected = selectedRating == r, onClick = { onRatingSelected(r) }, label = { Text(r.label) })
                    }
                }
                Text("Orden", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(SortOrder.DEFAULT, SortOrder.SCORE_DESC, SortOrder.ID_DESC).forEach { s ->
                        FilterChip(selected = selectedSort == s, onClick = { onSortSelected(s) }, label = { Text(s.label) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchesSection(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent searches", style = MaterialTheme.typography.titleMedium)
            if (searches.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear all") }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (searches.isEmpty()) {
            Text(
                "No recent searches. Try tags like `blue_hair`, `1girl`, or use `-tag` to exclude.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                items(searches, key = { it }) { query ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(query) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = query,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { onRemove(query) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_VISIBLE_SUGGESTIONS = 10
