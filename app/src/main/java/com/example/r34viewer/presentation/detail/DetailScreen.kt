package com.example.r34viewer.presentation.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.presentation.common.ErrorState
import com.example.r34viewer.presentation.common.LoadingState
import com.example.r34viewer.presentation.common.TagChip
import com.example.r34viewer.presentation.common.TagCategory
import com.example.r34viewer.presentation.common.TagClassifier
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    onSearchTag: (String) -> Unit,
    onSwipePrevious: (Int) -> Unit,
    onSwipeNext: (Int) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tagTypes by viewModel.tagTypes.collectAsStateWithLifecycle()
    val tagTypesLoading by viewModel.tagTypesLoading.collectAsStateWithLifecycle()
    val tagTypesError by viewModel.tagTypesError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var animatedImageLoading by remember { mutableStateOf(false) }
    var showSaveToFolderDialog by rememberSaveable { mutableStateOf(false) }
    val activePostId = (uiState as? DetailUiState.Success)?.post?.id
    var zoomState by remember(activePostId) { mutableStateOf(ImageZoomState()) }
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    val toggleFullscreen = {
        if (isFullscreen) zoomState = ImageZoomState()
        onToggleFullscreen()
    }
    BackHandler(enabled = isFullscreen || zoomState.scale > 1f) {
        if (zoomState.scale > 1f) {
            zoomState = ImageZoomState()
        } else {
            onToggleFullscreen()
        }
    }

    DisposableEffect(activity, view, isFullscreen) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            if (isFullscreen) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Scaffold(
        contentWindowInsets = if (isFullscreen) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text("Post detail") },
                    colors = TopAppBarDefaults.topAppBarColors(),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState is DetailUiState.Success) {
                            val post = (uiState as DetailUiState.Success).post
                            IconButton(onClick = onToggleFullscreen) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Full screen")
                            }
                            IconButton(onClick = {
                                if (isFavorite) viewModel.toggleFavorite()
                                else showSaveToFolderDialog = true
                            }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { sharePost(context, post) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = { openInBrowser(context, post.fileUrl.ifBlank { post.detailImageUrl }) }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open original")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> LoadingState()
                is DetailUiState.Error -> ErrorState(message = state.message)
                is DetailUiState.Success -> {
                    PostDetailContent(
                        post = state.post,
                        tagTypes = tagTypes,
                        tagTypesLoading = tagTypesLoading,
                        tagTypesError = tagTypesError,
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = toggleFullscreen,
                        zoomState = zoomState,
                        onZoomChange = { zoomState = it },
                        onSearchTag = onSearchTag,
                        onSwipePrevious = { onSwipePrevious(state.post.id) },
                        onSwipeNext = { onSwipeNext(state.post.id) },
                        onAnimatedLoadingChanged = { animatedImageLoading = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (animatedImageLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }

    if (showSaveToFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveToFolderDialog = false
                viewModel.saveFavorite()
            },
            title = { Text("Guardar en favoritos") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Elige una carpeta para este post.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    folders.forEach { folder ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showSaveToFolderDialog = false
                                viewModel.saveFavorite(folder.id)
                            }
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Text(folder.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showSaveToFolderDialog = false
                            viewModel.saveFavorite()
                        }
                    ) {
                        Text("Sin carpeta")
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostDetailContent(
    post: Post,
    tagTypes: Map<String, Int?>,
    tagTypesLoading: Boolean,
    tagTypesError: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    zoomState: ImageZoomState,
    onZoomChange: (ImageZoomState) -> Unit,
    onSearchTag: (String) -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onAnimatedLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(if (isFullscreen) Color.Black else Color.Transparent)) {
        Column(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            },
            verticalArrangement = if (isFullscreen) Arrangement.Center else Arrangement.spacedBy(12.dp)
        ) {
            PostMedia(
                post = post,
                onSwipePrevious = onSwipePrevious,
                onSwipeNext = onSwipeNext,
                zoomState = zoomState,
                onZoomChange = onZoomChange,
                isFullscreen = isFullscreen,
                onEnterFullscreen = onToggleFullscreen,
                onAnimatedLoadingChanged = onAnimatedLoadingChanged,
                backgroundColor = if (isFullscreen) Color.Black else MaterialTheme.colorScheme.surfaceVariant,
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (post.width > 0 && post.height > 0) {
                                Modifier.aspectRatio(post.width.toFloat() / post.height.toFloat().coerceAtLeast(0.1f))
                            } else {
                                Modifier.height(360.dp)
                            }
                        )
                }
            )

            if (!isFullscreen) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MetaChip(label = "ID", value = "#${post.id}")
                        MetaChip(label = "Score", value = post.score.toString())
                        MetaChip(label = "Rating", value = post.rating.ifBlank { "unknown" })
                    }

                    InfoRow(title = "Dimensions", value = "${post.width} x ${post.height}")
                    if (post.owner.isNotBlank()) {
                        InfoRow(title = "Owner", value = post.owner)
                    }
                    if (post.source.isNotBlank()) {
                        InfoRow(title = "Source", value = post.source)
                    }
                    InfoRow(title = "Has notes", value = if (post.hasNotes) "Yes" else "No")

                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (tagTypesLoading) {
                        Text(
                            text = "Clasificando tags...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (tagTypesError) {
                        Text(
                            text = "No se pudieron consultar todas las categorías; algunos tags aparecen en Otros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (post.tagList.isEmpty()) {
                        Text(
                            text = "No tags",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        listOf(
                            TagCategory.META to "Meta",
                            TagCategory.ARTIST to "Artistas",
                            TagCategory.COPYRIGHT to "Copyright",
                            TagCategory.CHARACTER to "Personajes",
                            TagCategory.NORMAL to "Otros"
                        ).forEach { (category, label) ->
                            val categoryTags = post.tagList.filter { tag ->
                                TagClassifier.categorize(tag, tagTypes[tag.lowercase()] ?: tagTypes[tag]) == category
                            }
                            if (categoryTags.isNotEmpty()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categoryTags.forEach { tag ->
                                        TagChip(
                                            tag = tag,
                                            category = category,
                                            onClick = { onSearchTag(tag) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isFullscreen) {
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit full screen",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Media is only animated/played here, never in the feed.
 * A double tap reloads the image (restarting a frozen GIF) or recreates the player (video).
 * GIFs are requested from fileUrl when available for proper animation and loaded with disk/memory cache
 * to optimize repeated/slow loads; reload bypasses memory cache to restart a frozen animation.
 */
@Composable
private fun PostMedia(
    post: Post,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    zoomState: ImageZoomState,
    onZoomChange: (ImageZoomState) -> Unit,
    isFullscreen: Boolean,
    onEnterFullscreen: () -> Unit,
    onAnimatedLoadingChanged: (Boolean) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var reloadKey by remember(post.id) { mutableIntStateOf(0) }
    val onReload = { reloadKey++ }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    if (post.isVideo) {
        LaunchedEffect(post.id) { onAnimatedLoadingChanged(false) }
        VideoPlayer(
            url = post.playbackUrl,
            reloadKey = reloadKey,
            modifier = modifier
                .background(Color.Black)
                .pointerInput(post.id) { detectTapGestures(onDoubleTap = { onReload() }) }
                .postSwipeGestures(post.id, swipeThresholdPx, onSwipePrevious, onSwipeNext)
        )
        return
    }

    val context = LocalContext.current
    val latestZoomState = rememberUpdatedState(zoomState)
    val latestOnZoomChange = rememberUpdatedState(onZoomChange)
    val latestIsFullscreen = rememberUpdatedState(isFullscreen)
    val latestOnEnterFullscreen = rememberUpdatedState(onEnterFullscreen)
    val latestOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val latestOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val imageUrl = if (post.isAnimated) post.fileUrl.ifBlank { post.detailImageUrl } else post.detailImageUrl
    val isGif = post.isAnimated
    var isLoading by remember(post.id, reloadKey, imageUrl) { mutableStateOf(true) }

    LaunchedEffect(isLoading, isGif) {
        onAnimatedLoadingChanged(isGif && isLoading)
    }
    DisposableEffect(Unit) {
        onDispose { onAnimatedLoadingChanged(false) }
    }

    val imageRequest = remember(context, imageUrl, reloadKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .allowHardware(false) // mejora compatibilidad/decodificación de GIFs animados
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(if (reloadKey > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clipToBounds()
            .pointerInput(post.id, isGif) {
                detectTapGestures(
                    onDoubleTap = {
                        if (isGif) onReload()
                    }
                )
            }
            .imageManipulationGestures(
                postId = post.id,
                imageWidth = post.width,
                imageHeight = post.height,
                zoomState = latestZoomState,
                isFullscreen = latestIsFullscreen,
                onZoomChange = latestOnZoomChange,
                onEnterFullscreen = latestOnEnterFullscreen,
                swipeThresholdPx = swipeThresholdPx,
                onSwipePrevious = latestOnSwipePrevious,
                onSwipeNext = latestOnSwipeNext
            )
    ) {
        key(reloadKey) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "Post ${post.id} image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { isLoading = false },
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                success = {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.graphicsLayer {
                            scaleX = zoomState.scale
                            scaleY = zoomState.scale
                            translationX = zoomState.offset.x
                            translationY = zoomState.offset.y
                        }
                    )
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

private data class ImageZoomState(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
)

private fun Modifier.imageManipulationGestures(
    postId: Int,
    imageWidth: Int,
    imageHeight: Int,
    zoomState: State<ImageZoomState>,
    isFullscreen: State<Boolean>,
    onZoomChange: State<(ImageZoomState) -> Unit>,
    onEnterFullscreen: State<() -> Unit>,
    swipeThresholdPx: Float,
    onSwipePrevious: State<() -> Unit>,
    onSwipeNext: State<() -> Unit>
): Modifier = pointerInput(postId) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var previousPosition = down.position
        var previousCentroid = down.position
        var previousAverageDistance = 0f
        var previousPointerCount = 1
        var horizontalDistance = 0f
        var verticalDistance = 0f
        var horizontalGesture = false
        var hadMultiplePointers = false
        var transformStarted = false

        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }

            when {
                pressed.size >= 2 -> {
                    hadMultiplePointers = true
                    val centroid = pressed.fold(Offset.Zero) { sum, change ->
                        sum + change.position
                    } / pressed.size.toFloat()
                    val averageDistance = (
                        pressed.sumOf { (it.position - centroid).getDistance().toDouble() } /
                            pressed.size
                        ).toFloat()
                    val zoomChange = if (previousAverageDistance > 0f) {
                        averageDistance / previousAverageDistance
                    } else {
                        1f
                    }
                    val panChange = centroid - previousCentroid
                    if (!transformStarted && zoomChange.isFinite() &&
                        abs(zoomChange - 1f) * minOf(size.width, size.height) >= viewConfiguration.touchSlop
                    ) {
                        transformStarted = true
                    }

                    if (transformStarted && zoomChange.isFinite() && zoomChange > 0f) {
                        val oldState = zoomState.value
                        val newScale = (oldState.scale * zoomChange).coerceIn(1f, 5f)
                        val mediaWidth = size.width.toFloat().coerceAtLeast(1f)
                        val mediaHeight = size.height.toFloat().coerceAtLeast(1f)
                        val fitScale = if (imageWidth > 0 && imageHeight > 0) {
                            minOf(mediaWidth / imageWidth, mediaHeight / imageHeight)
                        } else {
                            1f
                        }
                        val renderedWidth = if (imageWidth > 0) imageWidth * fitScale else mediaWidth
                        val renderedHeight = if (imageHeight > 0) imageHeight * fitScale else mediaHeight
                        val centroidOffset = centroid -
                            Offset(mediaWidth / 2f, mediaHeight / 2f)
                        val scaleRatio = newScale / oldState.scale
                        val proposedOffset = oldState.offset * scaleRatio +
                            centroidOffset * (1f - scaleRatio) + panChange
                        val maxX = ((renderedWidth * newScale - mediaWidth) / 2f).coerceAtLeast(0f)
                        val maxY = ((renderedHeight * newScale - mediaHeight) / 2f).coerceAtLeast(0f)
                        onZoomChange.value(
                            ImageZoomState(
                                scale = newScale,
                                offset = Offset(
                                    x = proposedOffset.x.coerceIn(-maxX, maxX),
                                    y = proposedOffset.y.coerceIn(-maxY, maxY)
                                )
                            )
                        )
                        if (newScale > 1f && !isFullscreen.value) {
                            onEnterFullscreen.value()
                        }
                        pressed.forEach { it.consume() }
                    }
                    previousCentroid = centroid
                    previousAverageDistance = averageDistance
                }

                pressed.size == 1 -> {
                    previousAverageDistance = 0f
                    val change = pressed.first()
                    val delta = change.position - previousPosition
                    val currentZoom = zoomState.value
                    if (previousPointerCount > 1) {
                        previousPosition = change.position
                    } else if (currentZoom.scale > 1f) {
                        val mediaWidth = size.width.toFloat().coerceAtLeast(1f)
                        val mediaHeight = size.height.toFloat().coerceAtLeast(1f)
                        val fitScale = if (imageWidth > 0 && imageHeight > 0) {
                            minOf(mediaWidth / imageWidth, mediaHeight / imageHeight)
                        } else {
                            1f
                        }
                        val renderedWidth = if (imageWidth > 0) imageWidth * fitScale else mediaWidth
                        val renderedHeight = if (imageHeight > 0) imageHeight * fitScale else mediaHeight
                        val maxX = ((renderedWidth * currentZoom.scale - mediaWidth) / 2f).coerceAtLeast(0f)
                        val maxY = ((renderedHeight * currentZoom.scale - mediaHeight) / 2f).coerceAtLeast(0f)
                        onZoomChange.value(
                            currentZoom.copy(
                                offset = Offset(
                                    x = (currentZoom.offset.x + delta.x).coerceIn(-maxX, maxX),
                                    y = (currentZoom.offset.y + delta.y).coerceIn(-maxY, maxY)
                                )
                            )
                        )
                        if (delta != Offset.Zero) change.consume()
                    } else if (!hadMultiplePointers) {
                        horizontalDistance += delta.x
                        verticalDistance += delta.y
                        if (!horizontalGesture &&
                            abs(horizontalDistance) > viewConfiguration.touchSlop &&
                            abs(horizontalDistance) > abs(verticalDistance)
                        ) {
                            horizontalGesture = true
                        }
                        if (horizontalGesture) change.consume()
                    }
                    previousPosition = change.position
                }
            }

            previousPointerCount = pressed.size
        } while (event.changes.any { it.pressed })

        val finalZoom = zoomState.value
        if (!hadMultiplePointers && finalZoom.scale <= 1f && horizontalGesture) {
            when {
                horizontalDistance >= swipeThresholdPx -> onSwipePrevious.value()
                horizontalDistance <= -swipeThresholdPx -> onSwipeNext.value()
            }
        }
    }
}

private fun Modifier.postSwipeGestures(
    postId: Int,
    thresholdPx: Float,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit
): Modifier = pointerInput(postId, thresholdPx, onSwipePrevious, onSwipeNext) {
    var horizontalDrag = 0f
    detectHorizontalDragGestures(
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            horizontalDrag += dragAmount
        },
        onDragEnd = {
            when {
                horizontalDrag >= thresholdPx -> onSwipePrevious()
                horizontalDrag <= -thresholdPx -> onSwipeNext()
            }
            horizontalDrag = 0f
        },
        onDragCancel = { horizontalDrag = 0f }
    )
}

/** Media3/ExoPlayer instance scoped to the detail screen; released when disposed. */
@Composable
private fun VideoPlayer(
    url: String,
    reloadKey: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val player = remember(url, reloadKey) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                player.playWhenReady = true
            }

            override fun onPause(owner: LifecycleOwner) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        update = { view -> view.player = player },
        modifier = modifier
    )
}

@Composable
private fun MetaChip(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun openInBrowser(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

private fun sharePost(context: android.content.Context, post: Post) {
    val url = post.fileUrl.ifBlank { post.detailImageUrl }
    val text = buildString {
        append("Rule34 post #${post.id}\n")
        append("Score: ${post.score} | Rating: ${post.rating}\n")
        if (url.isNotBlank()) append(url)
    }

    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share post"))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
