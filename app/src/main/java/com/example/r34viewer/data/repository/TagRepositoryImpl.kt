package com.example.r34viewer.data.repository

import android.util.Xml
import android.util.Log
import com.example.r34viewer.BuildConfig
import com.example.r34viewer.data.local.db.TagTypeDao
import com.example.r34viewer.data.local.db.TagTypeEntity
import com.example.r34viewer.data.remote.api.Rule34ApiService
import com.example.r34viewer.domain.model.TagSuggestion
import com.example.r34viewer.domain.model.TagTypeLookupResult
import com.example.r34viewer.domain.repository.TagRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.xmlpull.v1.XmlPullParser
import retrofit2.HttpException
import java.io.StringReader
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val apiService: Rule34ApiService,
    private val json: Json,
    private val tagTypeDao: TagTypeDao,
    private val tagCatalogImporter: TagCatalogImporter
) : TagRepository {

    override suspend fun importBundledCatalogIfNeeded(): Boolean =
        tagCatalogImporter.importIfNeeded()

    override suspend fun searchTags(prefix: String): List<TagSuggestion> {
        val query = prefix.trim().removePrefix("-")
        if (query.length < MIN_PREFIX_LENGTH) return emptyList()

        return apiService.autocompleteTags(query)
            .asSequence()
            .mapNotNull { suggestion ->
                val name = suggestion.value.trim().takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                TagSuggestion(
                    name = name,
                    count = suggestion.label.extractPostCount(),
                    apiType = null
                )
            }
            .take(MAX_SUGGESTIONS)
            .toList()
    }

    override suspend fun getCachedTagTypes(tags: List<String>): Map<String, Int?> {
        tagCatalogImporter.importIfNeeded()
        val uniqueTags = tags.distinctBy { it.lowercase() }
        if (uniqueTags.isEmpty()) return emptyMap()

        val cachedTypes = tagTypeDao.getTagTypes(uniqueTags.map { it.lowercase() })
            .associate { it.name to it.type }
        return uniqueTags.associate { tag -> tag to cachedTypes[tag.lowercase()] }
    }

    override suspend fun getTagTypes(tags: List<String>): TagTypeLookupResult {
        tagCatalogImporter.importIfNeeded()
        val uniqueTags = tags.distinctBy { it.lowercase() }
        if (uniqueTags.isEmpty()) return TagTypeLookupResult(emptyMap(), hasFailures = false)

        val cachedTypes = tagTypeDao.getTagTypes(uniqueTags.map { it.lowercase() })
            .associateBy { it.name }
        val firstDayOfMonth = LocalDate.now().withDayOfMonth(1).toEpochDay()
        val missingTags = uniqueTags.filter { tag ->
            val cached = cachedTypes[tag.lowercase()]
            cached == null ||
                (cached.apiCheckedAtEpochDay != null && cached.apiCheckedAtEpochDay < firstDayOfMonth)
        }
        val missingTagNames = missingTags.map { it.lowercase() }.toSet()
        val semaphore = Semaphore(TAG_TYPE_LOOKUP_CONCURRENCY)
        val fetchedTypes = supervisorScope {
            missingTags.map { tag ->
                async {
                    val result = try {
                        semaphore.withPermit {
                            TagTypeFetchResult(type = fetchTagType(tag), failed = false)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Tag type lookup failed: ${error::class.simpleName}")
                        }
                        TagTypeFetchResult(
                            type = null,
                            failed = true,
                            shouldCache = !error.isAuthenticationFailure()
                        )
                    }
                    tag.lowercase() to result
                }
            }.awaitAll().toMap()
        }

        val cacheEntries = fetchedTypes.mapNotNull { (name, result) ->
            if (!result.shouldCache) {
                null
            } else {
                TagTypeEntity(
                    name = name,
                    type = result.type,
                    apiCheckedAtEpochDay = LocalDate.now().toEpochDay()
                )
            }
        }
        if (cacheEntries.isNotEmpty()) tagTypeDao.insertAll(cacheEntries)

        return TagTypeLookupResult(
            types = uniqueTags.associate { tag ->
                val key = tag.lowercase()
                tag to if (key in missingTagNames) {
                    fetchedTypes[key]?.type
                } else {
                    cachedTypes[key]?.type
                }
            },
            hasFailures = fetchedTypes.values.any { it.failed }
        )
    }

    private suspend fun fetchTagType(tag: String): Int? {
        val body = apiService.getTagDetails(
            namePattern = tag,
            limit = TAG_TYPE_LOOKUP_LIMIT
        ).use { it.string() }.removePrefix("\uFEFF").trim()
        val response = try {
            json.parseToJsonElement(body)
        } catch (_: SerializationException) {
            return fetchTagTypeFromXml(body, tag)
        }
        if (response is JsonPrimitive && response.isString &&
            response.content.contains("authentication", ignoreCase = true)
        ) {
            throw IllegalStateException("Tag API authentication failed")
        }
        val entries = when (response) {
            is JsonArray -> response
            is JsonObject -> (response["tags"] ?: response["tag"] ?: response["data"]) as? JsonArray
                ?: throw IllegalStateException("Unexpected tag response")
            else -> throw IllegalStateException("Unexpected tag response")
        }

        return entries.firstNotNullOfOrNull { item ->
            val obj = item as? JsonObject ?: return@firstNotNullOfOrNull null
            val name = obj.readString("name") ?: return@firstNotNullOfOrNull null
            if (!name.equals(tag, ignoreCase = true)) return@firstNotNullOfOrNull null
            obj.readInt("type")
        }
    }

    private fun fetchTagTypeFromXml(body: String, tag: String): Int? {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(body))
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "tag") {
                val name = parser.getAttributeValue(null, "name")
                if (name.equals(tag, ignoreCase = true)) {
                    return parser.getAttributeValue(null, "type")?.toIntOrNull()
                }
            }
        }
        return null
    }

    private fun String.extractPostCount(): Long? =
        POST_COUNT_PATTERN.find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toLongOrNull()

    private fun JsonObject.readString(key: String): String? =
        (this[key] as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotBlank() }

    private fun JsonObject.readInt(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private data class TagTypeFetchResult(
        val type: Int?,
        val failed: Boolean,
        val shouldCache: Boolean = true
    )

    private fun Exception.isAuthenticationFailure(): Boolean =
        ((this as? HttpException)?.code()?.let { it in AUTH_ERROR_CODES } == true) ||
            message.orEmpty().contains("authentication", ignoreCase = true)

    private companion object {
        const val MIN_PREFIX_LENGTH = 2
        const val MAX_SUGGESTIONS = 10
        const val TAG_TYPE_LOOKUP_CONCURRENCY = 4
        const val TAG_TYPE_LOOKUP_LIMIT = 10
        const val TAG = "TagRepositoryImpl"
        val AUTH_ERROR_CODES = setOf(401, 403)
        val POST_COUNT_PATTERN = Regex("""\(([\d,]+)\)\s*$""")
    }
}
