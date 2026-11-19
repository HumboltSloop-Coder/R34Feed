package com.example.r34viewer.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getRecentSearches(): Flow<List<String>>
    suspend fun addSearch(query: String)
    suspend fun removeSearch(query: String)
    suspend fun clearHistory()
}
