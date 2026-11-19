package com.example.r34viewer.data.repository

import com.example.r34viewer.data.local.db.SearchHistoryDao
import com.example.r34viewer.data.local.db.SearchHistoryEntity
import com.example.r34viewer.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getRecentSearches(): Flow<List<String>> =
        searchHistoryDao.getRecentSearches().map { list -> list.map { it.query } }

    override suspend fun addSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        searchHistoryDao.insertSearch(
            SearchHistoryEntity(query = trimmed, timestamp = System.currentTimeMillis())
        )
    }

    override suspend fun removeSearch(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    override suspend fun clearHistory() {
        searchHistoryDao.clearAll()
    }
}
