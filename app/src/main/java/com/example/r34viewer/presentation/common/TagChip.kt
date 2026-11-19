package com.example.r34viewer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.r34viewer.domain.model.TagSuggestion

enum class TagCategory { NORMAL, META, ARTIST, COPYRIGHT, CHARACTER }

/**
 * Classifies tags so meta tags and artist tags get a distinctive style.
 * Post DTOs do not expose tag types, so well-known meta tags and artist name
 * patterns and a local list of common meta tags are used until the official API
 * type has been cached; tag suggestions use the official API type directly.
 */
object TagClassifier {
    private val META_TAGS = setOf(
        "ai_generated", "animated", "animated_gif", "audio", "colorized", "commentary",
        "comic", "cropped", "dated", "flash", "game_cg", "gif", "highres",
        "jpeg_artifacts", "large_filesize", "long_image", "long_playtime", "loop",
        "looping_animation", "lowres", "monochrome", "mosaic_censoring",
        "mp4", "multiple_views", "music", "official_art", "photoshopped",
        "pixel_art", "png_conversion", "poor_quality", "rotation", "scanned",
        "short_playtime", "sketch", "sound", "text", "translated", "translation",
        "unfinished", "vector", "video", "watermark", "webm"
    )
    private val ARTIST_MARKERS = listOf("artist", "_by_", "by_")
    private const val API_TYPE_ARTIST = 1
    private const val API_TYPE_COPYRIGHT = 3
    private const val API_TYPE_CHARACTER = 4
    private const val API_TYPE_META = 5

    fun categorize(tag: String, apiType: Int? = null): TagCategory {
        val normalized = tag.trim().lowercase().removePrefix("-")
        when (apiType) {
            API_TYPE_META -> return TagCategory.META
            API_TYPE_ARTIST -> return TagCategory.ARTIST
            API_TYPE_COPYRIGHT -> return TagCategory.COPYRIGHT
            API_TYPE_CHARACTER -> return TagCategory.CHARACTER
        }
        if (normalized in META_TAGS) return TagCategory.META
        if (ARTIST_MARKERS.any { it in normalized }) return TagCategory.ARTIST
        return TagCategory.NORMAL
    }

    fun fromSuggestion(suggestion: TagSuggestion): TagCategory =
        categorize(suggestion.name, suggestion.apiType)
}

/** Reusable tag chip used in the search bar and in the post detail screen. */
@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier,
    category: TagCategory = TagClassifier.categorize(tag),
    onClick: (() -> Unit)? = null
) {
    val negative = tag.startsWith("-")
    val cleanTag = tag.removePrefix("-")

    val colors = tagChipColors(category)
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.container)
            .then(
                if (colors.border == Color.Transparent) Modifier
                else Modifier.border(1.dp, colors.border, shape)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (negative) "-$cleanTag" else cleanTag,
            style = colors.textStyle,
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun tagChipColors(category: TagCategory): TagChipColors {
    val typography = MaterialTheme.typography.labelMedium
    val scheme = MaterialTheme.colorScheme
    return when (category) {
        TagCategory.META -> TagChipColors(
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
            border = scheme.tertiary,
            textStyle = typography.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        )
        TagCategory.ARTIST -> TagChipColors(
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            border = scheme.secondary,
            textStyle = typography.copy(
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        )
        TagCategory.COPYRIGHT -> TagChipColors(
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            border = scheme.primary,
            textStyle = typography.copy(fontWeight = FontWeight.SemiBold)
        )
        TagCategory.CHARACTER -> TagChipColors(
            container = scheme.secondaryContainer.copy(alpha = 0.6f),
            content = scheme.onSecondaryContainer,
            border = Color.Transparent,
            textStyle = typography
        )
        TagCategory.NORMAL -> TagChipColors(
            container = scheme.surfaceVariant,
            content = scheme.onSurfaceVariant,
            border = Color.Transparent,
            textStyle = typography
        )
    }
}

private data class TagChipColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val textStyle: TextStyle
)

fun formatPostCount(count: Long?): String? {
    if (count == null || count <= 0) return null
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 10_000 -> "${count / 1_000}K"
        count >= 1_000 -> "${"%.1f".format(count / 1_000.0)}K"
        else -> count.toString()
    }
}
