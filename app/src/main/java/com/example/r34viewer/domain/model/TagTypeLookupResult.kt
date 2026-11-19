package com.example.r34viewer.domain.model

data class TagTypeLookupResult(
    val types: Map<String, Int?>,
    val hasFailures: Boolean
)
