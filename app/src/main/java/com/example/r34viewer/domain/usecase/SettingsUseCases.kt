package com.example.r34viewer.domain.usecase

import com.example.r34viewer.domain.model.AppColors
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveApiKeyUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<String?> = repository.apiKeyFlow
}

class ObserveUserIdUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<String?> = repository.userIdFlow
}

class ObserveHasCredentialsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.hasCredentialsFlow
}

class SaveCredentialsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(apiKey: String, userId: String) =
        repository.saveCredentials(apiKey, userId)
}

class ClearCredentialsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke() = repository.clearCredentials()
}

class ObserveDarkThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isDarkThemeFlow
}

class SetDarkThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDarkTheme(enabled)
}

class ObserveFeedImageSizeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<FeedImageSize> = repository.feedImageSizeFlow
}

class SetFeedImageSizeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(size: FeedImageSize) = repository.setFeedImageSize(size)
}

class ObserveHideAiGeneratedUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.hideAiGeneratedFlow
}

class SetHideAiGeneratedUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setHideAiGenerated(enabled)
}

class ObserveFullscreenMaxQualityUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.fullscreenMaxQualityFlow
}

class SetFullscreenMaxQualityUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setFullscreenMaxQuality(enabled)
}

class ObserveCustomColorsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppColors> = repository.customColorsFlow
}

class SetCustomColorsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(colors: AppColors) = repository.setCustomColors(colors)
}

class ObserveNegativeTagsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.negativeTagsFlow
}

class AddNegativeTagUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(tag: String) = repository.addNegativeTag(tag)
}

class RemoveNegativeTagUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(tag: String) = repository.removeNegativeTag(tag)
}
