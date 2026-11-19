package com.example.r34viewer.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.r34viewer.data.remote.api.Rule34ApiService
import com.example.r34viewer.data.remote.dto.PostDto
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

        return try {
            val posts = apiService.getPosts(
                limit = limit,
                pid = page,
                tags = tags
            )
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (posts.isEmpty()) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            val friendly = friendlyHttpError(e)
            LoadResult.Error(if (friendly != null) IOException(friendly, e) else e)
        } catch (e: SerializationException) {
            // Defensive: if the API returns a non-JSON auth/error string, avoid surfacing a raw parser crash.
            val msg = e.message.orEmpty()
            val friendly = if (msg.contains("Missing authentication", ignoreCase = true)) {
                "Faltan credenciales de API. Ve a Ajustes e introduce tu API Key y User ID."
            } else {
                "Respuesta inesperada de la API. Verifica tu API Key y User ID e inténtalo de nuevo."
            }
            LoadResult.Error(IOException(friendly, e))
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun friendlyHttpError(e: HttpException): String? {
        val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val combined = "${e.message()} $errorBody"
        return when {
            combined.contains("Missing authentication", ignoreCase = true) ||
                (e.code() == 401 && errorBody.contains("Faltan credenciales", ignoreCase = true)) ->
                "Faltan credenciales de API. Ve a Ajustes e introduce tu API Key y User ID."
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
}
