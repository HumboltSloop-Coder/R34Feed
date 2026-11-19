package com.example.r34viewer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.r34viewer.data.local.db.FavoriteFolderEntity
import com.example.r34viewer.data.local.db.FavoriteProfileEntity
import com.example.r34viewer.data.local.db.FavoritesDao
import com.example.r34viewer.data.local.db.toDomain
import com.example.r34viewer.data.local.db.toFavoriteEntity
import com.example.r34viewer.data.remote.Rule34PagingSource
import com.example.r34viewer.data.remote.api.Rule34ApiService
import com.example.r34viewer.data.remote.dto.toDomain
import com.example.r34viewer.domain.model.FavoriteFolder
import com.example.r34viewer.domain.model.FavoriteFolderFilter
import com.example.r34viewer.domain.model.FavoriteImportResult
import com.example.r34viewer.domain.model.FavoriteProfile
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
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
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

    override fun getFavorites(): Flow<List<Post>> =
        favoritesDao.getAllFavorites().map { entities -> entities.map { it.toDomain() } }

    override fun getFavorites(filter: FavoriteFolderFilter): Flow<List<Post>> {
        val flow = when (filter) {
            FavoriteFolderFilter.All -> favoritesDao.getAllFavorites()
            FavoriteFolderFilter.Unfiled -> favoritesDao.getFavoritesWithoutFolder()
            is FavoriteFolderFilter.Folder -> favoritesDao.getFavoritesByFolder(filter.id)
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override fun getProfileFavorites(
        profileUserId: String,
        filter: FavoriteFolderFilter
    ): Flow<List<Post>> {
        val flow = when (filter) {
            FavoriteFolderFilter.All -> favoritesDao.getProfileFavorites(profileUserId)
            FavoriteFolderFilter.Unfiled -> favoritesDao.getProfileFavoritesWithoutFolder(profileUserId)
            is FavoriteFolderFilter.Folder ->
                favoritesDao.getProfileFavoritesByFolder(profileUserId, filter.id)
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

    override suspend fun toggleFavorite(post: Post) {
        val existing = favoritesDao.getFavoriteById(post.id)
        if (existing != null) {
            favoritesDao.deleteFavorite(post.id)
        } else {
            favoritesDao.insertFavorite(post.toFavoriteEntity())
        }
    }

    override suspend fun getFavoriteById(postId: Int): Post? =
        favoritesDao.getFavoriteById(postId)?.toDomain()

    override suspend fun exportFavorites(): String {
        val userId = settingsRepository.getUserId()?.trim().orEmpty()
        require(userId.isNotBlank() && userId.all(Char::isDigit)) {
            "Configura un User ID válido antes de exportar tus favoritos."
        }
        val folders = favoritesDao.getFoldersSnapshot()
        val favorites = favoritesDao.getFavoritesSnapshot()

        return buildString {
            appendLine(FAVORITES_BACKUP_HEADER_V2)
            appendLine("$FAVORITES_BACKUP_USER_ID_PREFIX$userId")
            folders.forEach { folder ->
                appendLine("[Carpeta: \"${folder.name.escapeExportName()}\"]")
                favorites.asSequence()
                    .filter { it.folderId == folder.id }
                    .forEach { appendLine(it.id) }
                appendLine()
            }
            appendLine("[Sin carpeta]")
            favorites.asSequence()
                .filter { it.folderId == null }
                .forEach { appendLine(it.id) }
        }
    }

    override suspend fun importFavorites(
        backupText: String,
        onProgress: (completed: Int, total: Int) -> Unit
    ): FavoriteImportResult {
        val backup = parseFavoriteBackup(backupText)
        val total = backup.sections.sumOf { it.postIds.size }
        val unavailableIds = mutableListOf<Int>()
        var restoredCount = 0
        var completed = 0
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

        importLoop@ for (section in backup.sections) {
            val folderId = section.folderName?.let { folderIds.getValue(it) }
            postLoop@ for (postId in section.postIds) {
                try {
                    val existing = if (profileUserId.isBlank()) {
                        favoritesDao.getFavoriteById(postId)
                    } else {
                        favoritesDao.getProfileFavoriteById(postId, profileUserId)
                    }
                    if (existing != null) {
                        if (profileUserId.isBlank()) {
                            favoritesDao.moveFavoritesToFolder(listOf(postId), folderId)
                        } else {
                            favoritesDao.moveProfileFavoritesToFolder(
                                listOf(postId),
                                folderId,
                                profileUserId
                            )
                        }
                        restoredCount++
                    } else {
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
                            completed++
                            onProgress(completed, total)
                            break@importLoop
                        }
                        if (post == null) {
                            if (!receivedEmptyBody) unavailableIds += postId
                        } else {
                            favoritesDao.insertFavorite(
                                post.toDomain().toFavoriteEntity(
                                    folderId = folderId,
                                    profileUserId = profileUserId
                                )
                            )
                            restoredCount++
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    stoppedAtPostId = postId
                    stoppedReason = favoriteImportError(error)
                    completed++
                    onProgress(completed, total)
                    break@importLoop
                }

                completed++
                onProgress(completed, total)
            }
        }

        return FavoriteImportResult(
            totalPostCount = total,
            restoredPostCount = restoredCount,
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
    }

    override suspend fun moveFavoritesToFolder(postIds: List<Int>, folderId: Long?) {
        if (postIds.isEmpty()) return
        favoritesDao.moveFavoritesToFolder(postIds, folderId)
    }

    companion object {
        private const val PAGE_SIZE = 20
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
    val postIds = mutableSetOf<Int>()
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
                require(postIds.add(postId)) { "El archivo repite el ID $postId." }
                currentPostIds += postId
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
