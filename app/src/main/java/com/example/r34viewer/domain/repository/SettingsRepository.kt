package com.example.r34viewer.domain.repository

import com.example.r34viewer.domain.model.AppColors
import com.example.r34viewer.domain.model.FeedImageSize
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val apiKeyFlow: Flow<String?>
    val userIdFlow: Flow<String?>
    val hasCredentialsFlow: Flow<Boolean>
    val isDarkThemeFlow: Flow<Boolean>
    val feedImageSizeFlow: Flow<FeedImageSize>
    val hideAiGeneratedFlow: Flow<Boolean>
    val fullscreenMaxQualityFlow: Flow<Boolean>
    val customColorsFlow: Flow<AppColors>
    val negativeTagsFlow: Flow<List<String>>

    suspend fun getUserId(): String?
    suspend fun saveCredentials(apiKey: String, userId: String)
    suspend fun clearCredentials()
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setFeedImageSize(size: FeedImageSize)
    suspend fun setHideAiGenerated(enabled: Boolean)
    suspend fun setFullscreenMaxQuality(enabled: Boolean)
    suspend fun setCustomColors(colors: AppColors)
    suspend fun addNegativeTag(tag: String)
    suspend fun removeNegativeTag(tag: String)
}
