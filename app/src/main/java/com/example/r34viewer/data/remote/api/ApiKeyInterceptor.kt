package com.example.r34viewer.data.remote.api

import com.example.r34viewer.data.local.datastore.SecureApiKeyStorage
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

/**
 * Injects the Rule34 credentials (api_key + user_id) into every request.
 * Rule34 requires both parameters since 2025; requests are blocked locally with a
 * clear error if either value is missing to avoid confusing API/parsing failures.
 */
class ApiKeyInterceptor @Inject constructor(
    private val secureApiKeyStorage: SecureApiKeyStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.url.encodedPath == AUTOCOMPLETE_PATH) {
            return chain.proceed(originalRequest)
        }

        val apiKey = secureApiKeyStorage.getApiKey()?.trim().orEmpty()
        val userId = secureApiKeyStorage.getUserId()?.trim().orEmpty()

        if (apiKey.isBlank() || userId.isBlank()) {
            // Controlled failure: return a synthetic 401 with a friendly, parse-safe message
            // (Paging/UI can surface it without crashing on unexpected non-JSON payloads).
            val body = MISSING_AUTH_MESSAGE.toResponseBody("text/plain".toMediaType())
            return Response.Builder()
                .request(originalRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Missing credentials")
                .body(body)
                .build()
        }

        val newUrl = originalRequest.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("user_id", userId)
            .build()

        val request = originalRequest.newBuilder().url(newUrl).build()
        val response = chain.proceed(request)

        // Rule34 returns its auth error as a plain-text body with HTTP 200 and an
        // application/json content type, so we must inspect the body on every response.
        val raw = response.peekBody(1024).string()
        if (raw.contains("Missing authentication", ignoreCase = true)) {
            response.close()
            val body = MISSING_AUTH_MESSAGE.toResponseBody("text/plain".toMediaType())
            return Response.Builder()
                .request(originalRequest)
                .protocol(response.protocol)
                .code(401)
                .message("Missing authentication")
                .body(body)
                .build()
        }

        return response
    }

    companion object {
        private const val AUTOCOMPLETE_PATH = "/autocomplete.php"
        const val MISSING_AUTH_MESSAGE =
            "Faltan credenciales de API. Ve a Ajustes e introduce tu API Key y User ID."
    }
}
