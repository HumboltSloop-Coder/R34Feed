package com.example.r34viewer.presentation.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.RatingFilter
import com.example.r34viewer.domain.model.SortOrder
import com.example.r34viewer.presentation.common.CompactTabTopBar
import com.example.r34viewer.presentation.common.EmptyState
import com.example.r34viewer.presentation.common.ErrorState
import com.example.r34viewer.presentation.common.LoadingState
import com.example.r34viewer.presentation.common.PostGridItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (Post, List<Post>) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { CompactTabTopBar("R34Feed") },
        floatingActionButton = {
            if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
                FloatingActionButton(
                    onClick = {
                        scope.launch { gridState.animateScrollToItem(0) }
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Volver arriba")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterRow(
                selectedRating = uiState.filters.rating,
                selectedSort = uiState.filters.sortOrder,
                onRatingSelected = viewModel::setRating,
                onSortSelected = viewModel::setSortOrder
            )

            val isRefreshing = posts.loadState.refresh is LoadState.Loading
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { posts.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val refresh = posts.loadState.refresh) {
                    is LoadState.Error -> {
                        ErrorState(
                            message = userFriendlyError(refresh.error),
                            onRetry = { posts.retry() }
                        )
                    }
                    is LoadState.Loading -> {
                        if (posts.itemCount == 0) {
                            LoadingState(message = "Loading feed...")
                        } else {
                            PostsGrid(posts, imageSize.columns, onPostClick, gridState)
                        }
                    }
                    is LoadState.NotLoading -> {
                        if (posts.itemCount == 0) {
                            EmptyState(message = "No posts found.\nTry different filters or check your API Key and User ID in Settings.")
                        } else {
                            PostsGrid(posts, imageSize.columns, onPostClick, gridState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostsGrid(
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
        items(count = posts.itemCount, key = { index -> posts[index]?.id ?: index }) { index ->
            val post = posts[index]
            if (post != null) {
                PostGridItem(post = post, onClick = { onPostClick(post, posts.itemSnapshotList.items) })
            }
        }

        when (val append = posts.loadState.append) {
            is LoadState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = userFriendlyError(append.error),
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { posts.retry() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterRow(
    selectedRating: RatingFilter,
    selectedSort: SortOrder,
    onRatingSelected: (RatingFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(RatingFilter.EXPLICIT, RatingFilter.QUESTIONABLE, RatingFilter.SAFE, RatingFilter.ALL).forEach { rating ->
                FilterChip(
                    selected = selectedRating == rating,
                    onClick = { onRatingSelected(rating) },
                    label = { Text(rating.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(SortOrder.DEFAULT, SortOrder.SCORE_DESC, SortOrder.ID_DESC).forEach { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { onSortSelected(sort) },
                    label = { Text(sort.label) }
                )
            }
        }
    }
}

fun userFriendlyError(t: Throwable): String {
    val msg = t.message.orEmpty()
    return when {
        msg.contains("Faltan credenciales", ignoreCase = true) ||
            msg.contains("Missing authentication", ignoreCase = true) ||
            msg.contains("Missing credentials", ignoreCase = true) ->
            "Faltan credenciales de API. Ve a Ajustes e introduce tu API Key y User ID."

        msg.contains("401") || msg.contains("403") ||
            msg.contains("Acceso denegado", ignoreCase = true) ||
            msg.contains("Access denied", ignoreCase = true) ->
            "Acceso denegado. Verifica tu API Key y User ID en Ajustes."

        msg.contains("429") || msg.contains("Límite de solicitudes", ignoreCase = true) ->
            "Límite de solicitudes alcanzado. Espera un momento e inténtalo de nuevo."

        msg.contains("Respuesta inesperada", ignoreCase = true) -> msg

        t is java.io.IOException ->
            "Error de red. Verifica tu conexión e inténtalo de nuevo."

        else -> "Error al cargar posts: ${t.message ?: "error desconocido"}"
    }
}
