package com.example.r34viewer.domain.repository

import com.example.r34viewer.domain.model.FeedImageSize
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val apiKeyFlow: Flow<String?>
    val userIdFlow: Flow<String?>
    val hasCredentialsFlow: Flow<Boolean>
    val isDarkThemeFlow: Flow<Boolean>
    val feedImageSizeFlow: Flow<FeedImageSize>
    val hideAiGeneratedFlow: Flow<Boolean>

    suspend fun getApiKey(): String?
    suspend fun getUserId(): String?
    suspend fun saveCredentials(apiKey: String, userId: String)
    suspend fun clearCredentials()
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setFeedImageSize(size: FeedImageSize)
    suspend fun setHideAiGenerated(enabled: Boolean)
}
