package com.example.r34viewer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.r34viewer.data.local.db.FavoriteFolderEntity
import com.example.r34viewer.data.local.db.FavoritePostFolderEntity
import com.example.r34viewer.data.local.db.FavoriteProfileEntity
import com.example.r34viewer.data.local.db.FavoriteTagEntity
import com.example.r34viewer.data.local.db.FavoritesDao
import com.example.r34viewer.data.local.db.toDomain
import com.example.r34viewer.data.local.db.toEntity
import com.example.r34viewer.data.local.db.toFavoriteEntity
import com.example.r34viewer.data.remote.Rule34PagingSource
import com.example.r34viewer.data.remote.api.Rule34ApiService
import com.example.r34viewer.data.remote.dto.toDomain
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteImportResult
import com.example.r34viewer.domain.model.FavoriteProfile
import com.example.r34viewer.domain.model.FavoriteTag
import com.example.r34viewer.domain.model.FavoriteTagBookmark
import com.example.r34viewer.domain.model.FolderMembership
import com.example.r34viewer.domain.model.Post
import com.example.r34viewer.domain.model.SearchFilters
import com.example.r34viewer.domain.repository.PostRepository
import com.example.r34viewer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: Rule34ApiService,
    private val favoritesDao: FavoritesDao,
    private val settingsRepository: SettingsRepository
) : PostRepository {

    override fun getPosts(filters: SearchFilters): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = SearchFilters.PAGE_SIZE,
                initialLoadSize = SearchFilters.PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            // A bookmarked position resumes from its page instead of loading everything again.
            initialKey = filters.startPage.coerceAtLeast(0),
            pagingSourceFactory = {
                Rule34PagingSource(
                    apiService = apiService,
                    tags = filters.toApiTags()
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { dto -> dto.toDomain() }
        }
    }

    override fun getFavorites(descending: Boolean): Flow<List<Post>> =
        favoritesDao.getAllFavorites(descending).map { entities -> entities.map { it.toDomain() } }

    override fun getFavorites(
        filter: FavoriteFolderFilter,
        descending: Boolean
    ): Flow<List<Post>> {
        val flow = when (filter) {
            FavoriteFolderFilter.All -> favoritesDao.getAllFavorites(descending)
            FavoriteFolderFilter.Unfiled -> favoritesDao.getFavoritesWithoutFolder(descending)
            is FavoriteFolderFilter.Folder ->
                favoritesDao.getFavoritesByFolder(filter.id, descending)
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override fun getProfileFavorites(
        profileUserId: String,
        filter: FavoriteFolderFilter,
        descending: Boolean
    ): Flow<List<Post>> {
        val flow = when (filter) {
            FavoriteFolderFilter.All -> favoritesDao.getProfileFavorites(profileUserId, descending)
            FavoriteFolderFilter.Unfiled ->
                favoritesDao.getProfileFavoritesWithoutFolder(profileUserId, descending)
            is FavoriteFolderFilter.Folder ->
                favoritesDao.getProfileFavoritesByFolder(profileUserId, filter.id, descending)
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override fun getFavoriteFolders(): Flow<List<FavoriteFolder>> =
        favoritesDao.getAllFolders().map { entities -> entities.map { it.toDomain() } }

    override fun getProfileFavoriteFolders(profileUserId: String): Flow<List<FavoriteFolder>> =
        favoritesDao.getProfileFolders(profileUserId).map { entities -> entities.map { it.toDomain() } }

    override fun getFavoriteProfiles(): Flow<List<FavoriteProfile>> =
        favoritesDao.getFavoriteProfiles().map { profiles -> profiles.map { it.toDomain() } }

    override fun isFavorite(postId: Int): Flow<Boolean> = favoritesDao.isFavorite(postId)

    override suspend fun toggleFavorite(post: Post) =
        favoritesDao.toggleFavoriteAtomic(post.toFavoriteEntity())

    override suspend fun addFavorite(post: Post) =
        favoritesDao.addFavoriteAtomic(post.toFavoriteEntity())

    override suspend fun getFavoriteById(postId: Int): Post? =
        favoritesDao.getFavoriteById(postId)?.toDomain()

    override suspend fun exportFavorites(): String {
        val userId = settingsRepository.getUserId()?.trim().orEmpty()
        require(userId.isNotBlank() && userId.all(Char::isDigit)) {
            "Configura un User ID válido antes de exportar tus favoritos."
        }
        val folders = favoritesDao.getFoldersSnapshot()
        val favorites = favoritesDao.getFavoritesSnapshot()
        val folderIdsByPost = favoritesDao.getFavoriteFolderLinksSnapshot()
            .groupBy({ it.postId }) { it.folderId }

        return buildString {
            appendLine(FAVORITES_BACKUP_HEADER_V2)
            appendLine("$FAVORITES_BACKUP_USER_ID_PREFIX$userId")
            folders.forEach { folder ->
                appendLine("[Carpeta: \"${folder.name.escapeExportName()}\"]")
                favorites.asSequence()
                    .filter { folder.id in folderIdsByPost[it.id].orEmpty() }
                    .forEach { appendLine(it.id) }
                appendLine()
            }
            appendLine("[Sin carpeta]")
            favorites.asSequence()
                .filter { folderIdsByPost[it.id].isNullOrEmpty() }
                .forEach { appendLine(it.id) }
        }
    }

    override suspend fun importFavorites(
        backupText: String,
        onProgress: (completed: Int, total: Int) -> Unit
    ): FavoriteImportResult {
        val backup = parseFavoriteBackup(backupText)
        val allPostIds = backup.sections.flatMap { it.postIds }.toSet()
        val total = allPostIds.size
        val processedIds = mutableSetOf<Int>()
        val restoredIds = mutableSetOf<Int>()
        val unavailableIds = mutableListOf<Int>()
        var requestsMade = 0
        var consecutiveEmptyResponses = 0
        var stoppedAtPostId: Int? = null
        var stoppedReason: String? = null
        val currentUserId = settingsRepository.getUserId()?.trim().orEmpty()
        val profileUserId = backup.userId?.takeIf { it != currentUserId }.orEmpty()
        if (profileUserId.isNotBlank()) {
            favoritesDao.insertFavoriteProfile(FavoriteProfileEntity(userId = profileUserId))
        }
        val folderIds = backup.sections.mapNotNull { it.folderName }
            .distinct()
            .associateWith { getOrCreateFolderId(it, profileUserId) }

        fun markProcessed(postId: Int) {
            if (processedIds.add(postId)) onProgress(processedIds.size, total)
        }

        importLoop@ for (section in backup.sections) {
            val folderId = section.folderName?.let { folderIds.getValue(it) }
            for (postId in section.postIds) {
                try {
                    val existing = if (profileUserId.isBlank()) {
                        favoritesDao.getFavoriteById(postId)
                    } else {
                        favoritesDao.getProfileFavoriteById(postId, profileUserId)
                    }
                    var favoriteReady = existing != null
                    if (!favoriteReady) {
                        var post: com.example.r34viewer.data.remote.dto.PostDto? = null
                        var responseReceived = false
                        var emptyBodyRetries = 0
                        var receivedEmptyBody = false
                        while (!responseReceived) {
                            if (requestsMade > 0) delay(IMPORT_REQUEST_INTERVAL_MS)
                            requestsMade++
                            try {
                                post = apiService.getPosts(limit = 1, tags = "id:$postId")
                                    .firstOrNull { it.id == postId }
                                responseReceived = true
                                consecutiveEmptyResponses = 0
                            } catch (error: SerializationException) {
                                if (!error.isEmptyApiBody()) throw error
                                if (emptyBodyRetries == 0) {
                                    emptyBodyRetries++
                                    delay(EMPTY_RESPONSE_RETRY_DELAY_MS)
                                } else {
                                    receivedEmptyBody = true
                                    unavailableIds += postId
                                    consecutiveEmptyResponses++
                                    responseReceived = true
                                    if (consecutiveEmptyResponses >= MAX_CONSECUTIVE_EMPTY_RESPONSES) {
                                        stoppedAtPostId = postId
                                        stoppedReason =
                                            "La API devolvió respuestas vacías varias veces; se detuvo la importación."
                                    }
                                }
                            }
                        }
                        if (stoppedReason != null) {
                            markProcessed(postId)
                            break@importLoop
                        }
                        if (post == null) {
                            if (!receivedEmptyBody) unavailableIds += postId
                        } else {
                            val nextPriority = (favoritesDao.getMaxPriority(profileUserId) ?: 0) + 1
                            favoritesDao.insertFavorite(
                                post.toDomain().toFavoriteEntity(
                                    priority = nextPriority,
                                    profileUserId = profileUserId
                                )
                            )
                            favoriteReady = true
                        }
                    }
                    if (favoriteReady) {
                        if (folderId != null) {
                            favoritesDao.insertFavoritePostFolders(
                                listOf(
                                    FavoritePostFolderEntity(
                                        postId = postId,
                                        folderId = folderId,
                                        profileUserId = profileUserId
                                    )
                                )
                            )
                        } else {
                            favoritesDao.clearFavoriteFolders(listOf(postId), profileUserId)
                        }
                        restoredIds += postId
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    stoppedAtPostId = postId
                    stoppedReason = favoriteImportError(error)
                    markProcessed(postId)
                    break@importLoop
                }

                markProcessed(postId)
            }
        }

        return FavoriteImportResult(
            totalPostCount = total,
            restoredPostCount = restoredIds.size,
            unavailablePostIds = unavailableIds,
            profileUserId = profileUserId.takeIf { it.isNotBlank() },
            stoppedAtPostId = stoppedAtPostId,
            stoppedReason = stoppedReason
        )
    }

    private suspend fun getOrCreateFolderId(name: String, profileUserId: String): Long {
        val existing = if (profileUserId.isBlank()) {
            favoritesDao.getFolderByName(name)
        } else {
            favoritesDao.getProfileFolderByName(name, profileUserId)
        }
        existing?.let { return it.id }
        val insertedId = favoritesDao.insertFolder(
            FavoriteFolderEntity(name = name, profileUserId = profileUserId)
        )
        if (insertedId != -1L) return insertedId
        val concurrentInsert = if (profileUserId.isBlank()) {
            favoritesDao.getFolderByName(name)
        } else {
            favoritesDao.getProfileFolderByName(name, profileUserId)
        }
        return concurrentInsert?.id
            ?: throw IllegalStateException("No se pudo crear la carpeta \"$name\".")
    }

    override suspend fun deleteFavoriteProfile(profileUserId: String) {
        favoritesDao.deleteProfileFavorites(profileUserId)
        favoritesDao.deleteProfileFavoriteFolders(profileUserId)
        favoritesDao.deleteProfileFolders(profileUserId)
        favoritesDao.deleteFavoriteProfile(profileUserId)
    }

    override suspend fun createFavoriteFolder(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val id = favoritesDao.insertFolder(FavoriteFolderEntity(name = trimmed))
        return id != -1L
    }

    override suspend fun removeFavorites(postIds: List<Int>) {
        if (postIds.isEmpty()) return
        favoritesDao.deleteFavorites(postIds)
        favoritesDao.clearFavoriteFolders(postIds, "")
    }

    override suspend fun addFavoritesToFolder(postIds: List<Int>, folderId: Long) {
        if (postIds.isEmpty()) return
        favoritesDao.insertFavoritePostFolders(
            postIds.map { postId ->
                FavoritePostFolderEntity(postId = postId, folderId = folderId, profileUserId = "")
            }
        )
    }

    override suspend fun removeFavoritesFromFolder(postIds: List<Int>, folderId: Long) {
        if (postIds.isEmpty()) return
        favoritesDao.removeFavoritesFromFolder(postIds, folderId, "")
    }

    override suspend fun clearFavoritesFolders(postIds: List<Int>) {
        if (postIds.isEmpty()) return
        favoritesDao.clearFavoriteFolders(postIds, "")
    }

    override fun getFavoriteFolderMemberships(profileUserId: String): Flow<List<FolderMembership>> =
        favoritesDao.getFavoriteFolderLinks(profileUserId).map { links ->
            links.map { it.toDomain() }
        }

    override suspend fun getFavoriteFolderIds(postId: Int): List<Long> =
        favoritesDao.getFolderIdsForPost(postId)

    override fun getFavoriteTags(): Flow<List<FavoriteTag>> =
        favoritesDao.getFavoriteTags().map { tags -> tags.map { it.toDomain() } }

    override suspend fun addFavoriteTag(query: String): Boolean {
        val normalized = FavoriteTag.normalize(query) ?: return false
        val id = favoritesDao.insertFavoriteTag(FavoriteTagEntity(query = normalized))
        return id != -1L
    }

    override suspend fun removeFavoriteTag(tagId: Long) {
        favoritesDao.deleteBookmarksForTag(tagId)
        favoritesDao.deleteFavoriteTag(tagId)
    }

    override fun getFavoriteTagBookmarks(): Flow<List<FavoriteTagBookmark>> =
        favoritesDao.getFavoriteTagBookmarks().map { bookmarks -> bookmarks.map { it.toDomain() } }

    override suspend fun saveFavoriteTagBookmark(bookmark: FavoriteTagBookmark): Long =
        favoritesDao.insertFavoriteTagBookmark(bookmark.toEntity())

    override suspend fun deleteFavoriteTagBookmark(bookmarkId: Long) {
        favoritesDao.deleteFavoriteTagBookmark(bookmarkId)
    }

    override suspend fun swapFavoritePriorities(firstId: Int, secondId: Int) =
        favoritesDao.swapFavoritePrioritiesAtomic(firstId, secondId)

    companion object {
        private const val PREFETCH_DISTANCE = 4
        private const val IMPORT_REQUEST_INTERVAL_MS = 600L
        private const val EMPTY_RESPONSE_RETRY_DELAY_MS = 1_000L
        private const val MAX_CONSECUTIVE_EMPTY_RESPONSES = 3
    }
}

private data class FavoriteBackupSection(
    val folderName: String?,
    val postIds: List<Int>
)

private data class FavoriteBackup(
    val userId: String?,
    val sections: List<FavoriteBackupSection>
)

private fun parseFavoriteBackup(text: String): FavoriteBackup {
    val lines = text.removePrefix("\uFEFF").lines()
    val header = lines.firstOrNull()?.trim()
    require(header == FAVORITES_BACKUP_HEADER_V1 || header == FAVORITES_BACKUP_HEADER_V2) {
        "El archivo no tiene el formato de exportación de favoritos reconocido."
    }
    val userId = if (header == FAVORITES_BACKUP_HEADER_V2) {
        val userIdLine = lines.getOrNull(1)?.trim().orEmpty()
        require(userIdLine.startsWith(FAVORITES_BACKUP_USER_ID_PREFIX)) {
            "El archivo compartido no contiene el User ID de origen."
        }
        userIdLine.removePrefix(FAVORITES_BACKUP_USER_ID_PREFIX).trim().also {
            require(it.isNotBlank() && it.all(Char::isDigit)) {
                "El archivo contiene un User ID no válido."
            }
        }
    } else {
        null
    }

    val sections = mutableListOf<FavoriteBackupSection>()
    val folderNames = mutableSetOf<String>()
    var currentFolder: String? = null
    var currentSectionStarted = false
    var currentPostIds = mutableListOf<Int>()
    var hasUnfiledSection = false

    fun finishSection() {
        if (currentSectionStarted) {
            sections += FavoriteBackupSection(currentFolder, currentPostIds.toList())
        }
    }

    for (rawLine in lines.drop(1)) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) continue

        when {
            line == "[Sin carpeta]" -> {
                require(!hasUnfiledSection) { "El archivo repite la sección «Sin carpeta»." }
                finishSection()
                currentFolder = null
                currentPostIds = mutableListOf()
                currentSectionStarted = true
                hasUnfiledSection = true
            }

            line.startsWith(FOLDER_SECTION_PREFIX) && line.endsWith("\"]") -> {
                finishSection()
                val folderName = decodeExportName(
                    line.removePrefix(FOLDER_SECTION_PREFIX).removeSuffix("\"]")
                )
                require(folderName.isNotBlank()) { "El archivo contiene una carpeta sin nombre." }
                require(folderNames.add(folderName)) { "El archivo repite la carpeta \"$folderName\"." }
                currentFolder = folderName
                currentPostIds = mutableListOf()
                currentSectionStarted = true
            }

            line.all(Char::isDigit) -> {
                require(currentSectionStarted) { "El archivo contiene un ID fuera de una carpeta." }
                val postId = line.toIntOrNull()
                require(postId != null && postId > 0) { "El archivo contiene un ID no válido: $line." }
                // A post may legitimately appear in several folder sections.
                if (postId !in currentPostIds) currentPostIds += postId
            }

            else -> throw IllegalArgumentException("Línea no reconocida en el archivo: $line")
        }
    }

    finishSection()
    require(sections.isNotEmpty()) { "El archivo no contiene secciones de favoritos." }
    return FavoriteBackup(userId, sections)
}

private fun decodeExportName(value: String): String {
    val decoded = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index++]
        if (character != '\\') {
            decoded.append(character)
            continue
        }

        require(index < value.length) { "El nombre de carpeta tiene un escape incompleto." }
        decoded.append(
            when (val escaped = value[index++]) {
                '\\' -> '\\'
                '"' -> '"'
                'r' -> '\r'
                'n' -> '\n'
                't' -> '\t'
                else -> throw IllegalArgumentException("Escape no reconocido en nombre de carpeta: \\$escaped")
            }
        )
    }
    return decoded.toString()
}

private const val FAVORITES_BACKUP_HEADER_V1 = "# Rule34Mobile favorites export v1"
private const val FAVORITES_BACKUP_HEADER_V2 = "# Rule34Mobile favorites export v2"
private const val FAVORITES_BACKUP_USER_ID_PREFIX = "# user_id: "
private const val FOLDER_SECTION_PREFIX = "[Carpeta: \""

private fun favoriteImportError(error: Exception): String = when (error) {
    is HttpException -> when (error.code()) {
        401, 403 -> "Acceso denegado. Verifica la API Key y el User ID en Ajustes."
        429 -> "Rule34 limitó las solicitudes. Espera un momento antes de importar de nuevo."
        else -> error.localizedMessage ?: "Error HTTP al consultar la API."
    }

    is SerializationException -> {
        val detail = error.message?.lineSequence()?.firstOrNull()?.take(140)
        if (detail.isNullOrBlank()) {
            "La API devolvió una respuesta con formato inesperado para este post."
        } else {
            "La respuesta de la API no coincide con el formato esperado: $detail"
        }
    }

    else -> error.localizedMessage ?: "Error al consultar la API."
}

private fun SerializationException.isEmptyApiBody(): Boolean =
    message.orEmpty().contains("EOF", ignoreCase = true)

private fun String.escapeExportName(): String = replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\r", "\\r")
    .replace("\n", "\\n")
    .replace("\t", "\\t")
