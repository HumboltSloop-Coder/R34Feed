package com.example.r34viewer.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.r34viewer.domain.model.FeedImageSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val FEED_IMAGE_SIZE = stringPreferencesKey("feed_image_size")
        val HIDE_AI_GENERATED = booleanPreferencesKey("hide_ai_generated")
    }

    val isDarkThemeFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true // Dark theme by default (NSFW-friendly)
    }

    val feedImageSizeFlow: Flow<FeedImageSize> = context.settingsDataStore.data.map { prefs ->
        FeedImageSize.fromStoredValue(prefs[Keys.FEED_IMAGE_SIZE])
    }

    val hideAiGeneratedFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.HIDE_AI_GENERATED] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setFeedImageSize(size: FeedImageSize) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FEED_IMAGE_SIZE] = size.name
        }
    }

    suspend fun setHideAiGenerated(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HIDE_AI_GENERATED] = enabled
        }
    }
}
