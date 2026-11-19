package com.example.r34viewer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AutocompleteDto(
    val label: String,
    val value: String
)
