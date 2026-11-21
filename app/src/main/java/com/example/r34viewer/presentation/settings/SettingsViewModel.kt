package com.example.r34viewer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.r34viewer.domain.model.AppColors
import com.example.r34viewer.domain.model.CustomColorSlot
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.domain.usecase.AddNegativeTagUseCase
import com.example.r34viewer.domain.usecase.ClearCredentialsUseCase
import com.example.r34viewer.domain.usecase.ObserveApiKeyUseCase
import com.example.r34viewer.domain.usecase.ObserveCustomColorsUseCase
import com.example.r34viewer.domain.usecase.ObserveDarkThemeUseCase
import com.example.r34viewer.domain.usecase.ObserveFeedImageSizeUseCase
import com.example.r34viewer.domain.usecase.ObserveFullscreenMaxQualityUseCase
import com.example.r34viewer.domain.usecase.ObserveHideAiGeneratedUseCase
import com.example.r34viewer.domain.usecase.ObserveNegativeTagsUseCase
import com.example.r34viewer.domain.usecase.ObserveUserIdUseCase
import com.example.r34viewer.domain.usecase.RemoveNegativeTagUseCase
import com.example.r34viewer.domain.usecase.SaveCredentialsUseCase
import com.example.r34viewer.domain.usecase.SetCustomColorsUseCase
import com.example.r34viewer.domain.usecase.SetDarkThemeUseCase
import com.example.r34viewer.domain.usecase.SetFeedImageSizeUseCase
import com.example.r34viewer.domain.usecase.SetFullscreenMaxQualityUseCase
import com.example.r34viewer.domain.usecase.SetHideAiGeneratedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val userId: String = "",
    val hasApiKey: Boolean = false,
    val hasUserId: Boolean = false,
    val hasCredentials: Boolean = false,
    val isDarkTheme: Boolean = true,
    val feedImageSize: FeedImageSize = FeedImageSize.DEFAULT,
    val hideAiGenerated: Boolean = false,
    val fullscreenMaxQuality: Boolean = false,
    val customColors: AppColors = AppColors(),
    val negativeTags: List<String> = emptyList(),
    val negativeTagDraft: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeApiKeyUseCase: ObserveApiKeyUseCase,
    observeUserIdUseCase: ObserveUserIdUseCase,
    observeDarkThemeUseCase: ObserveDarkThemeUseCase,
    observeFeedImageSizeUseCase: ObserveFeedImageSizeUseCase,
    observeHideAiGeneratedUseCase: ObserveHideAiGeneratedUseCase,
    observeFullscreenMaxQualityUseCase: ObserveFullscreenMaxQualityUseCase,
    observeNegativeTagsUseCase: ObserveNegativeTagsUseCase,
    observeCustomColorsUseCase: ObserveCustomColorsUseCase,
    private val setCustomColorsUseCase: SetCustomColorsUseCase,
    private val addNegativeTagUseCase: AddNegativeTagUseCase,
    private val removeNegativeTagUseCase: RemoveNegativeTagUseCase,
    private val saveCredentialsUseCase: SaveCredentialsUseCase,
    private val clearCredentialsUseCase: ClearCredentialsUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase,
    private val setFeedImageSizeUseCase: SetFeedImageSizeUseCase,
    private val setHideAiGeneratedUseCase: SetHideAiGeneratedUseCase,
    private val setFullscreenMaxQualityUseCase: SetFullscreenMaxQualityUseCase
) : ViewModel() {

    private val inputApiKey = MutableStateFlow("")
    private val inputUserId = MutableStateFlow("")
    private val apiKeyTouched = MutableStateFlow(false)
    private val userIdTouched = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val customColors = MutableStateFlow(AppColors())
    private val negativeTagDraft = MutableStateFlow("")

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SettingsUiState> = combine(
        observeApiKeyUseCase(),
        observeUserIdUseCase(),
        observeDarkThemeUseCase(),
        observeFeedImageSizeUseCase(),
        observeHideAiGeneratedUseCase(),
        observeFullscreenMaxQualityUseCase(),
        inputApiKey,
        inputUserId,
        apiKeyTouched,
        userIdTouched,
        isSaving,
        message,
        customColors,
        observeNegativeTagsUseCase(),
        negativeTagDraft
    ) { values ->
        val storedApiKey = values[0] as String?
        val storedUserId = values[1] as String?
        val dark = values[2] as Boolean
        val imageSize = values[3] as FeedImageSize
        val hideAi = values[4] as Boolean
        val fullscreenMaxQuality = values[5] as Boolean
        val inputKey = values[6] as String
        val inputUid = values[7] as String
        val keyTouched = values[8] as Boolean
        val uidTouched = values[9] as Boolean
        val saving = values[10] as Boolean
        val msg = values[11] as String?
        val colors = values[12] as AppColors
        val negativeTags = values[13] as List<String>
        val tagDraft = values[14] as String

        val effectiveApiKey = if (keyTouched) inputKey else (inputKey.ifBlank { storedApiKey.orEmpty() })
        val effectiveUserId = if (uidTouched) inputUid else (inputUid.ifBlank { storedUserId.orEmpty() })

        SettingsUiState(
            apiKey = effectiveApiKey,
            userId = effectiveUserId,
            hasApiKey = !storedApiKey.isNullOrBlank(),
            hasUserId = !storedUserId.isNullOrBlank(),
            hasCredentials = !storedApiKey.isNullOrBlank() && !storedUserId.isNullOrBlank(),
            isDarkTheme = dark,
            feedImageSize = imageSize,
            hideAiGenerated = hideAi,
            fullscreenMaxQuality = fullscreenMaxQuality,
            customColors = colors,
            negativeTags = negativeTags,
            negativeTagDraft = tagDraft,
            isSaving = saving,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            observeCustomColorsUseCase().collect { stored ->
                customColors.value = stored
            }
        }
    }

    fun onApiKeyInputChange(value: String) {
        inputApiKey.value = value
        apiKeyTouched.value = true
        message.value = null
    }

    fun onUserIdInputChange(value: String) {
        inputUserId.value = value
        userIdTouched.value = true
        message.value = null
    }

    fun saveCredentials() {
        val key = uiState.value.apiKey.trim()
        val userId = uiState.value.userId.trim()
        when {
            key.isBlank() && userId.isBlank() -> {
                message.value = "API Key y User ID son obligatorios."
                return
            }
            key.isBlank() -> {
                message.value = "La API Key es obligatoria."
                return
            }
            userId.isBlank() -> {
                message.value = "El User ID es obligatorio."
                return
            }
        }
        viewModelScope.launch {
            isSaving.value = true
            try {
                saveCredentialsUseCase(key, userId)
                inputApiKey.value = ""
                inputUserId.value = ""
                apiKeyTouched.value = false
                userIdTouched.value = false
                message.value = "Credenciales guardadas correctamente."
            } catch (e: Exception) {
                message.value = "Error al guardar credenciales: ${e.message ?: "desconocido"}"
            } finally {
                isSaving.value = false
            }
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            try {
                clearCredentialsUseCase()
                inputApiKey.value = ""
                inputUserId.value = ""
                apiKeyTouched.value = false
                userIdTouched.value = false
                message.value = "Credenciales eliminadas."
            } catch (e: Exception) {
                message.value = "Error al eliminar credenciales."
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            setDarkThemeUseCase(enabled)
        }
    }

    fun setFeedImageSize(size: FeedImageSize) {
        viewModelScope.launch {
            setFeedImageSizeUseCase(size)
        }
    }

    fun setHideAiGenerated(enabled: Boolean) {
        viewModelScope.launch {
            setHideAiGeneratedUseCase(enabled)
        }
    }

    fun setFullscreenMaxQuality(enabled: Boolean) {
        viewModelScope.launch {
            setFullscreenMaxQualityUseCase(enabled)
        }
    }

    fun setCustomColor(slot: CustomColorSlot, hex: String?) {
        val updated = customColors.value.with(slot, AppColors.normalizeHex(hex))
        customColors.value = updated
        viewModelScope.launch {
            setCustomColorsUseCase(updated)
        }
    }

    fun resetCustomColors() {
        val reset = AppColors()
        customColors.value = reset
        viewModelScope.launch {
            setCustomColorsUseCase(reset)
        }
        message.value = "Colores restablecidos."
    }

    fun onNegativeTagInputChange(value: String) {
        negativeTagDraft.value = value
        message.value = null
    }

    fun addNegativeTag() {
        val tag = normalizeNegativeTag(negativeTagDraft.value)
        if (tag == null) {
            message.value = "Introduce un tag válido (sin espacios)."
            return
        }
        if (tag in uiState.value.negativeTags) {
            message.value = "Ese tag negativo ya está añadido."
            return
        }
        viewModelScope.launch {
            addNegativeTagUseCase(tag)
            negativeTagDraft.value = ""
            message.value = "Tag negativo añadido: -$tag"
        }
    }

    fun removeNegativeTag(tag: String) {
        viewModelScope.launch {
            removeNegativeTagUseCase(tag)
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun normalizeNegativeTag(value: String): String? {
        val trimmed = value.trim().removePrefix("-").trim()
        if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) return null
        return trimmed.lowercase()
    }
}
