package com.example.r34viewer.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.r34viewer.data.remote.api.Rule34ApiService
import com.example.r34viewer.data.remote.dto.PostDto
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class Rule34PagingSource(
    private val apiService: Rule34ApiService,
    private val tags: String
) : PagingSource<Int, PostDto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostDto> {
        val page = params.key ?: 0
        val limit = params.loadSize.coerceIn(1, 100)

        var emptyBodyAttempts = 0
        while (true) {
            try {
                val posts = apiService.getPosts(
                    limit = limit,
                    pid = page,
                    tags = tags
                )
                return LoadResult.Page(
                    data = posts,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (posts.isEmpty()) null else page + 1
                )
            } catch (e: IOException) {
                return LoadResult.Error(e)
            } catch (e: HttpException) {
                val friendly = friendlyHttpError(e)
                return LoadResult.Error(if (friendly != null) IOException(friendly, e) else e)
            } catch (e: SerializationException) {
                val message = e.message.orEmpty()
                when {
                    message.contains("Missing authentication", ignoreCase = true) -> {
                        return LoadResult.Error(IOException(MISSING_AUTH_MESSAGE, e))
                    }

                    message.contains("search down", ignoreCase = true) ||
                        message.contains("\"success\"", ignoreCase = true) -> {
                        return LoadResult.Error(IOException(SEARCH_DOWN_MESSAGE, e))
                    }

                    e.isEmptyApiBody() && emptyBodyAttempts < MAX_EMPTY_BODY_RETRIES -> {
                        emptyBodyAttempts++
                        delay(EMPTY_BODY_RETRY_DELAY_MS)
                    }

                    e.isEmptyApiBody() -> {
                        // The API answers with an empty body (HTTP 200) when a search has no
                        // results or the server drops it. It means "no posts", not a bad key.
                        return LoadResult.Page(
                            data = emptyList(),
                            prevKey = if (page == 0) null else page - 1,
                            nextKey = null
                        )
                    }

                    else -> {
                        return LoadResult.Error(
                            IOException(
                                "Respuesta inesperada de la API. Inténtalo de nuevo en unos segundos.",
                                e
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                return LoadResult.Error(e)
            }
        }
    }

    private fun friendlyHttpError(e: HttpException): String? {
        val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val combined = "${e.message()} $errorBody"
        return when {
            combined.contains("Missing authentication", ignoreCase = true) ||
                (e.code() == 401 && errorBody.contains("Faltan credenciales", ignoreCase = true)) ->
                MISSING_AUTH_MESSAGE
            e.code() == 401 || e.code() == 403 ->
                "Acceso denegado. Verifica tu API Key y User ID en Ajustes."
            e.code() == 429 -> "Límite de solicitudes alcanzado. Espera un momento e inténtalo de nuevo."
            else -> null
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostDto>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    private fun SerializationException.isEmptyApiBody(): Boolean =
        message.orEmpty().let {
            it.contains("EOF", ignoreCase = true) ||
                it.contains("Unexpected end of input", ignoreCase = true)
        }

    private companion object {
        const val MISSING_AUTH_MESSAGE =
            "Faltan credenciales de API. Ve a Ajustes e introduce tu API Key y User ID."
        const val SEARCH_DOWN_MESSAGE =
            "La búsqueda de Rule34 no está disponible ahora mismo. Inténtalo de nuevo en unos segundos."
        const val EMPTY_BODY_RETRY_DELAY_MS = 1_000L
        const val MAX_EMPTY_BODY_RETRIES = 2
    }
}
