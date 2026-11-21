package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteImportResult
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.FavoriteTag
import com.example.r34viewer.domain.model.FavoriteTagBookmark
import com.example.r34viewer.domain.model.FolderMembership
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(descending: Boolean = false): Flow<List<Post>> =
        repository.getFavorites(descending)

    operator fun invoke(
        filter: FavoriteFolderFilter,
        descending: Boolean = false
    ): Flow<List<Post>> = repository.getFavorites(filter, descending)

    operator fun invoke(
        filter: FavoriteFolderFilter,
        profileUserId: String?,
        descending: Boolean = false
    ): Flow<List<Post>> =
        if (profileUserId == null) repository.getFavorites(filter, descending)
        else repository.getProfileFavorites(profileUserId, filter, descending)
}

class ObserveFavoriteFoldersUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(profileUserId: String? = null): Flow<List<FavoriteFolder>> =
        if (profileUserId == null) repository.getFavoriteFolders()
        else repository.getProfileFavoriteFolders(profileUserId)
}

class ObserveFavoriteProfilesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<List<FavoriteProfile>> = repository.getFavoriteProfiles()
}

class DeleteFavoriteProfileUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(profileUserId: String) =
        repository.deleteFavoriteProfile(profileUserId)
}

class CreateFavoriteFolderUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(name: String): Boolean = repository.createFavoriteFolder(name)
}

class RemoveFavoritesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postIds: List<Int>) = repository.removeFavorites(postIds)
}

class AddFavoritesToFolderUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postIds: List<Int>, folderId: Long) =
        repository.addFavoritesToFolder(postIds, folderId)
}

class RemoveFavoritesFromFolderUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postIds: List<Int>, folderId: Long) =
        repository.removeFavoritesFromFolder(postIds, folderId)
}

class ClearFavoritesFoldersUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postIds: List<Int>) =
        repository.clearFavoritesFolders(postIds)
}

class ObserveFavoriteFolderMembershipsUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(profileUserId: String): Flow<List<FolderMembership>> =
        repository.getFavoriteFolderMemberships(profileUserId)
}

class IsFavoriteUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(postId: Int): Flow<Boolean> = repository.isFavorite(postId)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(post: Post) = repository.toggleFavorite(post)
}

class AddFavoriteUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(post: Post) = repository.addFavorite(post)
}

class GetFavoriteFolderIdsUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: Int): List<Long> = repository.getFavoriteFolderIds(postId)
}

class GetFavoriteByIdUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: Int): Post? = repository.getFavoriteById(postId)
}

class ExportFavoritesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(): String = repository.exportFavorites()
}

class ImportFavoritesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(
        backupText: String,
        onProgress: (completed: Int, total: Int) -> Unit
    ): FavoriteImportResult = repository.importFavorites(backupText, onProgress)
}

class ObserveFavoriteTagsUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<List<FavoriteTag>> = repository.getFavoriteTags()
}

class AddFavoriteTagUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(query: String): Boolean = repository.addFavoriteTag(query)
}

class RemoveFavoriteTagUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(tagId: Long) = repository.removeFavoriteTag(tagId)
}

class ObserveFavoriteTagBookmarksUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<List<FavoriteTagBookmark>> = repository.getFavoriteTagBookmarks()
}

class SaveFavoriteTagBookmarkUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(bookmark: FavoriteTagBookmark): Long =
        repository.saveFavoriteTagBookmark(bookmark)
}

class DeleteFavoriteTagBookmarkUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(bookmarkId: Long) = repository.deleteFavoriteTagBookmark(bookmarkId)
}

class SwapFavoritePrioritiesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(firstId: Int, secondId: Int) =
        repository.swapFavoritePriorities(firstId, secondId)
}
