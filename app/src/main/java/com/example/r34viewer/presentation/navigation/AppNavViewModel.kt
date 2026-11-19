package com.example.r34viewer.presentation.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.r34viewer.domain.usecase.ImportTagCatalogUseCase
import com.example.r34viewer.domain.usecase.ObserveDarkThemeUseCase
import com.example.r34viewer.domain.usecase.ObserveHasCredentialsUseCase
import com.example.r34viewer.util.SelectedPostCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    observeHasCredentialsUseCase: ObserveHasCredentialsUseCase,
    observeDarkThemeUseCase: ObserveDarkThemeUseCase,
    importTagCatalogUseCase: ImportTagCatalogUseCase,
    val selectedPostCache: SelectedPostCache
) : ViewModel() {

    private val _pendingTagSearch = MutableStateFlow<String?>(null)
    val pendingTagSearch: StateFlow<String?> = _pendingTagSearch.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    val hasCredentials: StateFlow<Boolean?> = observeHasCredentialsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isDarkTheme: StateFlow<Boolean> = observeDarkThemeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            try {
                importTagCatalogUseCase()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Bundled tag catalog import failed", error)
            }
        }
    }

    fun requestTagSearch(tag: String) {
        _pendingTagSearch.value = tag
    }

    fun consumeTagSearch() {
        _pendingTagSearch.value = null
    }

    fun setFullscreen(enabled: Boolean) {
        _isFullscreen.value = enabled
    }

    private companion object {
        const val TAG = "AppNavViewModel"
    }
}
