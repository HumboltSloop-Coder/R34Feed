package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteImportResult
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<List<Post>> = repository.getFavorites()
    operator fun invoke(filter: FavoriteFolderFilter): Flow<List<Post>> = repository.getFavorites(filter)
    operator fun invoke(filter: FavoriteFolderFilter, profileUserId: String?): Flow<List<Post>> =
        if (profileUserId == null) repository.getFavorites(filter)
        else repository.getProfileFavorites(profileUserId, filter)
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

class MoveFavoritesToFolderUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postIds: List<Int>, folderId: Long?) =
        repository.moveFavoritesToFolder(postIds, folderId)
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
