package com.example.r34viewer.util

import com.example.r34viewer.domain.model.Post
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small in-memory cache to pass selected posts to the detail screen without large nav args.
 * Detail falls back to favorites DB if not found here.
 */
@Singleton
class SelectedPostCache @Inject constructor() {
    private val cache = ConcurrentHashMap<Int, Post>()
    @Volatile
    private var selectedPostIds: List<Int> = emptyList()

    fun put(post: Post) {
        cache[post.id] = post
        // keep cache bounded
        if (cache.size > 200) {
            val firstKey = cache.keys().nextElement()
            cache.remove(firstKey)
        }
    }

    fun setPostSequence(posts: List<Post>, selectedPostId: Int) {
        val selectedIndex = posts.indexOfFirst { it.id == selectedPostId }
        val start = if (posts.size <= MAX_SEQUENCE_SIZE || selectedIndex < 0) {
            0
        } else {
            (selectedIndex - MAX_SEQUENCE_SIZE / 2).coerceIn(0, posts.size - MAX_SEQUENCE_SIZE)
        }
        val sequence = posts.drop(start).take(MAX_SEQUENCE_SIZE)
        cache.clear()
        sequence.forEach(::put)
        selectedPostIds = sequence.map(Post::id)
    }

    fun get(id: Int): Post? = cache[id]

    fun getAdjacent(id: Int, offset: Int): Post? {
        val index = selectedPostIds.indexOf(id)
        if (index < 0) return null
        return selectedPostIds.getOrNull(index + offset)?.let(cache::get)
    }

    private companion object {
        const val MAX_SEQUENCE_SIZE = 200
    }
}
