package com.example.r34viewer.data.repository

import com.example.r34viewer.data.local.datastore.SecureApiKeyStorage
import com.example.r34viewer.data.local.datastore.SettingsDataStore
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val secureApiKeyStorage: SecureApiKeyStorage,
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    // In-memory mirrors so UI can react immediately after save/clear in the same process.
    private val _apiKeyFlow = MutableStateFlow(secureApiKeyStorage.getApiKey())
    private val _userIdFlow = MutableStateFlow(secureApiKeyStorage.getUserId())

    override val apiKeyFlow: Flow<String?> = _apiKeyFlow.asStateFlow()
    override val userIdFlow: Flow<String?> = _userIdFlow.asStateFlow()

    override val hasCredentialsFlow: Flow<Boolean> = combine(apiKeyFlow, userIdFlow) { apiKey, userId ->
        !apiKey.isNullOrBlank() && !userId.isNullOrBlank()
    }

    override val isDarkThemeFlow: Flow<Boolean> = settingsDataStore.isDarkThemeFlow
    override val feedImageSizeFlow: Flow<FeedImageSize> = settingsDataStore.feedImageSizeFlow
    override val hideAiGeneratedFlow: Flow<Boolean> = settingsDataStore.hideAiGeneratedFlow

    override suspend fun getApiKey(): String? = secureApiKeyStorage.getApiKey()

    override suspend fun getUserId(): String? = secureApiKeyStorage.getUserId()

    override suspend fun saveCredentials(apiKey: String, userId: String) {
        val trimmedKey = apiKey.trim()
        val trimmedUserId = userId.trim()
        secureApiKeyStorage.saveCredentials(trimmedKey, trimmedUserId)
        _apiKeyFlow.value = trimmedKey.ifBlank { null }
        _userIdFlow.value = trimmedUserId.ifBlank { null }
    }

    override suspend fun clearCredentials() {
        secureApiKeyStorage.clearCredentials()
        _apiKeyFlow.value = null
        _userIdFlow.value = null
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        settingsDataStore.setDarkTheme(enabled)
    }

    override suspend fun setFeedImageSize(size: FeedImageSize) {
        settingsDataStore.setFeedImageSize(size)
    }

    override suspend fun setHideAiGenerated(enabled: Boolean) {
        settingsDataStore.setHideAiGenerated(enabled)
    }
}
