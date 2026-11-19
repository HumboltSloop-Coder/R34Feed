package com.example.r34viewer.domain.model

/**
 * A tag returned by the Rule34 tag endpoint, used by the search autocomplete.
 *
 * @param apiType Rule34 tag type: 1 = artist, 5 = meta, others = general/copyright/character.
 */
data class TagSuggestion(
    val name: String,
    val count: Long?,
    val apiType: Int? = null
)
